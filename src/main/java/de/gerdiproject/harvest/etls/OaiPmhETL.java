/**
 * Copyright © 2017 Jan Frömberg (http://www.gerdi-project.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.harvest.etls;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.events.ParameterChangedEvent;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.config.parameters.constants.ParameterConstants;
import de.gerdiproject.harvest.config.parameters.constants.ParameterMappingFunctions;
import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.etls.constants.OaiPmhParameterConstants;
import de.gerdiproject.harvest.etls.events.GetRepositoryNameEvent;
import de.gerdiproject.harvest.etls.extractors.IExtractor;
import de.gerdiproject.harvest.etls.extractors.OaiPmhRecordsExtractor;
import de.gerdiproject.harvest.etls.transformers.ITransformer;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.utils.data.HttpRequester;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * An OAI-PMH-Protocol ETL capable to harvest various metadata standardsdocuments
 * by exchanging the transform component depending on a set parameter.
 *
 * @author Jan Frömberg, Robin Weiss
 */
public class OaiPmhETL extends AbstractIteratorETL<Element, DataCiteJson>
{
    private StringParameter fromParam;
    private StringParameter untilParam;
    private StringParameter hostUrlParam;
    private StringParameter metadataPrefixParam;
    private StringParameter logoUrlParam;
    private StringParameter viewUrlParam;
    private StringParameter setParam;

    private Map<String, String> schemaUrlMap = new HashMap<>();


    @Override
    public void addEventListeners()
    {
        super.addEventListeners();
        EventSystem.addSynchronousListener(GetRepositoryNameEvent.class, this::getRepositoryName);
    }


    @Override
    public void removeEventListeners()
    {
        EventSystem.removeSynchronousListener(GetRepositoryNameEvent.class);
    }


    @Override
    protected IExtractor<Iterator<Element>> createExtractor()
    {
        return new OaiPmhRecordsExtractor();
    }


    @Override
    protected void registerParameters()
    {
        super.registerParameters();

        // define parameter mapping functions
        final Function<String, String> stringMappingFunction =
            ParameterMappingFunctions.createMapperForETL(ParameterMappingFunctions::mapToString, this);

        final Function<String, String> urlMappingFunction =
            ParameterMappingFunctions.createMapperForETL(ParameterMappingFunctions::mapToUrlString, this);

        final Function<String, String> metadataPrefixFunction =
            ParameterMappingFunctions.createMapperForETL(this::mapStringToMetadataPrefix, this);

        // register parameters
        this.fromParam = Configuration.registerParameter(
                             new StringParameter(
                                 OaiPmhParameterConstants.FROM_KEY,
                                 getName(),
                                 OaiPmhParameterConstants.FROM_DEFAULT_VALUE,
                                 stringMappingFunction));

        this.untilParam = Configuration.registerParameter(
                              new StringParameter(
                                  OaiPmhParameterConstants.UNTIL_KEY,
                                  getName(),
                                  OaiPmhParameterConstants.UNTIL_DEFAULT_VALUE,
                                  stringMappingFunction));

        this.hostUrlParam = Configuration.registerParameter(
                                new StringParameter(
                                    OaiPmhParameterConstants.HOST_URL_KEY,
                                    getName(),
                                    OaiPmhParameterConstants.HOST_URL_DEFAULT_VALUE,
                                    urlMappingFunction));

        this.metadataPrefixParam = Configuration.registerParameter(
                                       new StringParameter(
                                           OaiPmhParameterConstants.METADATA_PREFIX_KEY,
                                           getName(),
                                           OaiPmhParameterConstants.METADATA_PREFIX_DEFAULT_VALUE,
                                           metadataPrefixFunction));

        this.logoUrlParam = Configuration.registerParameter(
                                new StringParameter(
                                    OaiPmhParameterConstants.LOGO_URL_KEY,
                                    getName(),
                                    OaiPmhParameterConstants.LOGO_URL_DEFAULT_VALUE,
                                    urlMappingFunction));

        this.viewUrlParam = Configuration.registerParameter(
                                new StringParameter(
                                    OaiPmhParameterConstants.VIEW_URL_KEY,
                                    getName(),
                                    OaiPmhParameterConstants.VIEW_URL_DEFAULT_VALUE,
                                    urlMappingFunction));

        this.setParam = Configuration.registerParameter(
                            new StringParameter(
                                OaiPmhParameterConstants.SET_KEY,
                                getName(),
                                OaiPmhParameterConstants.SET_DEFAULT_VALUE,
                                stringMappingFunction));
    }


    /**
     * Checks if the specified metadataPrefix parameter value is valid and returns it.
     *
     * @param metadataPrefix the new value of the metadataPrefix parameter
     *
     * @throws IllegalArgumentException thrown if the metadataPrefix is invalid or not supported
     *
     * @return the metadataPrefix that was passed as an argument
     */
    private String mapStringToMetadataPrefix(final String metadataPrefix) throws IllegalArgumentException
    {
        // check for errors
        checkMetadataPrefix(metadataPrefix);

        return metadataPrefix;
    }


    @Override
    protected ITransformer<Iterator<Element>, Iterator<DataCiteJson>> createTransformer()
    {
        // set the transformer to null, if something is broken
        try {
            // if for the map is not initialized yet, do it
            if (this.schemaUrlMap.isEmpty())
                this.schemaUrlMap = createSchemaUrlMap();

            // get the metadata schema name
            final String metadataPrefix = metadataPrefixParam.getValue();

            // check for errors
            checkMetadataPrefix(metadataPrefix);

            // get the unique schema URL of the metadata schema
            final String schemaUrl = schemaUrlMap.get(metadataPrefix);

            // execute the corresponding transformer constructor
            return OaiPmhParameterConstants.METADATA_SCHEMA_MAP
                   .get(schemaUrl)
                   .get();
        } catch (final RuntimeException e) {
            logger.warn(OaiPmhConstants.CANNOT_CREATE_TRANSFORMER, e);
            return null;
        }
    }


    /**
     * This method checks if the metadataPrefix parameter is set correctly, by testing
     * if it is not null and supported by both the repository and the harvester itself.
     *
     * @param metadataPrefix the metadataPrefix query parameter
     *
     * @throws IllegalArgumentException thrown if the metadataPrefix is not supported
     */
    private void checkMetadataPrefix(final String metadataPrefix) throws IllegalArgumentException
    {
        final StringBuilder errorMessageBuilder = new StringBuilder();

        if (metadataPrefix == null || metadataPrefix.isEmpty())
            errorMessageBuilder.append(OaiPmhConstants.NO_METADATA_PREFIX_ERROR);

        else if (hostUrlParam.getValue() == null || hostUrlParam.getValue().isEmpty())
            errorMessageBuilder.append(OaiPmhConstants.NO_HOST_URL_ERROR);

        else if (schemaUrlMap.isEmpty())
            errorMessageBuilder.append(OaiPmhConstants.CANNOT_GET_METADATA_SCHEMAS_ERROR);

        else {
            final String schemaUrl = schemaUrlMap.get(metadataPrefix);

            // edge case: the metadata prefix was loaded from disk or only the host url changed
            if (schemaUrl == null)
                errorMessageBuilder.append(String.format(
                                               OaiPmhConstants.REPOSITORY_UNSUPPORTED_METADATA_PREFIX_ERROR,
                                               metadataPrefix));

            // edge case: the metadata prefix was loaded from disk
            else if (!OaiPmhParameterConstants.METADATA_SCHEMA_MAP.containsKey(schemaUrl))
                errorMessageBuilder.append(String.format(
                                               OaiPmhConstants.HARVESTER_UNSUPPORTED_METADATA_PREFIX_ERROR,
                                               metadataPrefix));
        }

        if (errorMessageBuilder.length() != 0) {

            // if there is an error, append the allowed values to it, if viable
            if (!schemaUrlMap.keySet().isEmpty()) {

                // intersect the metadata schema values of schemaUrlMap with the keys of the METADATA_SCHEMA_MAP
                final String allowedValuesString =
                    schemaUrlMap.keySet().stream()
                    .filter((final String key) -> OaiPmhParameterConstants.METADATA_SCHEMA_MAP.containsKey(schemaUrlMap.get(key)))
                    .collect(Collectors.toSet())
                    .toString();
                errorMessageBuilder.append(' ');
                errorMessageBuilder.append(ParameterConstants.ALLOWED_VALUES);
                errorMessageBuilder.append(allowedValuesString.substring(1, allowedValuesString.length() - 1));
            }

            throw new IllegalArgumentException(errorMessageBuilder.toString());
        }
    }


    @Override
    protected void onParameterChanged(final ParameterChangedEvent event)
    {
        super.onParameterChanged(event);
        final AbstractParameter<?> param = event.getParameter();

        if (param == metadataPrefixParam)
            this.transformer = createTransformer();

        else if (param == hostUrlParam) {
            this.extractor.init(this);
            this.schemaUrlMap = createSchemaUrlMap();
            this.transformer = createTransformer();
        }
    }


    /**
     * Creates a map of (non-unique) metadataPrefix names to unique
     * schema URLs, which helps to determine the correct metadata schema to apply.
     *
     * @return a map of (non-unique) metadataPrefix names to unique schema URLs
     */
    private Map<String, String> createSchemaUrlMap()
    {
        final Map<String, String> map = new HashMap<>();

        // make a request to retrieve metadata formats
        try {
            final String metadataFormatsUrl = getMetadataFormatsUrl();
            final Document schemasDoc = new HttpRequester().getHtmlFromUrl(metadataFormatsUrl);

            final Elements schemaElements =
                schemasDoc.select(OaiPmhConstants.ALL_METADATA_PREFIXES_SELECTION);

            for (final Element ele : schemaElements)
                map.put(ele.selectFirst(OaiPmhConstants.METADATA_PREFIX_SELECTION).text(),
                        ele.selectFirst(OaiPmhConstants.METADATA_SCHEMA_SELECTION).text());

        } catch (IllegalStateException | NullPointerException e) { // NOPMD errors are handled when the schema map is used
        }

        return map;
    }


    /**
     * Assembles an OAI-PMH compliant Query-URL for retrieving a record list. Harvester preconfigured parameters
     * are used, but can also be manually configured via REST.
     *
     * @throws IllegalStateException if either the host URL or the metadata prefix is not set
     *
     * @return a ListRecords URL, e.g. https://ws.pangaea.de/oai/provider?verb=ListRecords&metadataPrefix=datacite3
     */
    public String getListRecordsUrl() throws IllegalStateException
    {
        return getListRecordsUrl(fromParam.getValue());
    }


    /**
     * Assembles an OAI-PMH compliant Query-URL for retrieving the allowed metadata schemas.
     *
     * @throws IllegalStateException if the host URL is not set
     *
     * @return a ListMetadataFormats URL, e.g. https://api.figshare.com/v2/oai?verb=ListMetadataFormats
     */
    public String getMetadataFormatsUrl() throws IllegalStateException
    {
        if (hostUrlParam.getValue() == null || hostUrlParam.getValue().isEmpty())
            throw new IllegalStateException(OaiPmhConstants.NO_HOST_URL_ERROR);

        return String.format(OaiPmhConstants.METADATA_FORMATS_URL, hostUrlParam.getValue());
    }


    /**
     * To fully support the OAI-PMH resumption Token for very large data-query
     * answers, a URL-string has to be compiled with a specific URL and a
     * back-end generated token. This method returns this resumption URL with
     * a {@linkplain String} placeholder for the token.
     *
     * @return a URL string that must be formatted to include a resumption token
     */
    public String getResumptionUrlFormat()
    {
        return String.format(OaiPmhConstants.RESUMPTION_URL, hostUrlParam.getValue());
    }


    /**
     * Some OAI-PMH providers have a limited capacity of records that can be harvested via
     * a resumption token. If the download limit is reached, the start date of the most recently
     * harvested batch is used to be able to continue the record extraction.
     * This method returns such a fallback URL as a {@linkplain String} that must be formatted
     * to replace a placeholder for the start date.
     *
     * @return a URL string that must be formatted to include the starting date
     */
    public String getFallbackResumptionUrlFormat()
    {
        return getListRecordsUrl("%s");
    }


    /**
     * Retrieves the name of the OAI-PMH repository that is to be harvested.
     *
     * @return the name of the OAI-PMH repository that is to be harvested
     */
    public String getRepositoryName()
    {
        final HttpRequester httpRequester = new HttpRequester();

        if (hostUrlParam.getValue() != null && !hostUrlParam.getValue().isEmpty()) {
            final Document identifyDoc = httpRequester.getHtmlFromUrl(String.format(OaiPmhConstants.IDENTIFY_URL, hostUrlParam.getValue()));

            if (identifyDoc != null)
                return identifyDoc.select(OaiPmhConstants.REPOSITORY_NAME_ELEMENT).first().text();
        }

        return OaiPmhConstants.UNKNOWN_PROVIDER;
    }


    /**
     * Returns a URL that should point to the repository provider logo.
     * The URL is directly retrieved from the corresponding parameter.
     *
     * @return a logo URL set up by the "logoUrl"-parameter
     */
    public String getLogoUrl()
    {
        return logoUrlParam.getValue();
    }

    /**
     * Returns a URL that should point to a central repository access point.
     * The URL is directly retrieved from the corresponding parameter.
     *
     * @return a logo URL set up by the "logoUrl"-parameter
     */
    public String getViewUrl()
    {
        return viewUrlParam.getValue();
    }


    /**
     * Assembles an OAI-PMH compliant Query-URL for retrieving a record list. Harvester preconfigured parameters
     * are used, but can also be manually configured via REST.
     *
     * @param dateFrom the minimum date stamp of records
     *
     * @return a ListRecords URL, e.g. https://ws.pangaea.de/oai/provider?verb=ListRecords&metadataPrefix=datacite3
     *
     * @throws IllegalStateException if either the host URL or the metadata prefix is not set
     */
    private String getListRecordsUrl(final String dateFrom) throws IllegalStateException
    {
        if (hostUrlParam.getValue() == null || hostUrlParam.getValue().isEmpty())
            throw new IllegalStateException(OaiPmhConstants.NO_HOST_URL_ERROR);

        if (metadataPrefixParam.getValue() == null || metadataPrefixParam.getValue().isEmpty())
            throw new IllegalStateException(OaiPmhConstants.NO_METADATA_PREFIX_ERROR);

        final StringBuilder queryBuilder = new StringBuilder();

        if (dateFrom != null && !dateFrom.isEmpty())
            queryBuilder.append(OaiPmhConstants.DATE_FROM_QUERY).append(dateFrom);

        if (untilParam.getValue() != null && !untilParam.getValue().isEmpty())
            queryBuilder.append(OaiPmhConstants.DATE_TO_QUERY).append(untilParam.getValue());

        if (setParam.getValue() != null && !setParam.getValue().isEmpty())
            queryBuilder.append(OaiPmhConstants.SET_QUERY).append(setParam.getValue());

        queryBuilder.append(OaiPmhConstants.METADATA_PREFIX_QUERY).append(metadataPrefixParam.getValue());

        return String.format(OaiPmhConstants.LIST_RECORDS_URL, hostUrlParam.getValue(), queryBuilder.toString());
    }


    @Override
    protected void finishHarvestExceptionally(final Throwable reason)
    {
        super.finishHarvestExceptionally(reason);

        // make sure the extractor was initialized
        if (extractor != null) {
            // retrieve the datestamp of the records at which the harvest failed
            final String lastHarvestedDate = ((OaiPmhRecordsExtractor)extractor).getLastHarvestedDate();

            // log the record date stamp if it was retrieved
            if (lastHarvestedDate != null)
                logger.info(String.format(OaiPmhConstants.LAST_DATE_INFO, lastHarvestedDate, getName()));
        }
    }
}
