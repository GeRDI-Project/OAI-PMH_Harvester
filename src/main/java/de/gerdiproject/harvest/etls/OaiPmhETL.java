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

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.gson.Gson;

import de.gerdiproject.harvest.config.Configuration;
import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.config.parameters.constants.ParameterMappingFunctions;
import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.etls.constants.OaiPmhParameterConstants;
import de.gerdiproject.harvest.etls.events.GetRepositoryNameEvent;
import de.gerdiproject.harvest.etls.extractors.IExtractor;
import de.gerdiproject.harvest.etls.extractors.OaiPmhRecordsExtractor;
import de.gerdiproject.harvest.etls.transformers.Datacite3Transformer;
import de.gerdiproject.harvest.etls.transformers.Datacite4Transformer;
import de.gerdiproject.harvest.etls.transformers.DublinCoreTransformer;
import de.gerdiproject.harvest.etls.transformers.ITransformer;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.utils.data.HttpRequester;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * An OAI-PMH-Protocol ETL capable to harvest various metadata standardsdocuments
 * by exchaning the transform component depending on a set parameter.
 *
 * @author Jan Frömberg, Robin Weiss
 */
public class OaiPmhETL extends AbstractIteratorETL<Element, DataCiteJson>
{
    private StringParameter fromParam;
    private StringParameter untilParam;
    private StringParameter hostUrlParam;
    private StringParameter metadataPrefixParam;


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
    protected ITransformer<Iterator<Element>, Iterator<DataCiteJson>> createTransformer()
    {
        switch (metadataPrefixParam.getValue()) {
            case OaiPmhParameterConstants.DATACITE_3_METADATA_PREFIX:
            case OaiPmhParameterConstants.DATACITE_3_METADATA_PREFIX_2:
            case OaiPmhParameterConstants.DATACITE_3_METADATA_PREFIX_3:
                return new Datacite3Transformer();

            case OaiPmhParameterConstants.DATACITE_4_METADATA_PREFIX:
            case OaiPmhParameterConstants.DATACITE_4_METADATA_PREFIX_2:
            case OaiPmhParameterConstants.DATACITE_4_METADATA_PREFIX_3:
                return new Datacite4Transformer();

            case OaiPmhParameterConstants.DUBLIN_CORE_METADATA_PREFIX:
                return new DublinCoreTransformer();

            default:
                logger.error(String.format(OaiPmhConstants.WRONG_METADATA_PREFIX_ERROR, metadataPrefixParam.getValue()));
                return null;
        }
    }


    @Override
    protected void registerParameters()
    {
        super.registerParameters();
        this.fromParam = Configuration.registerParameter(
                             new StringParameter(
                                 OaiPmhParameterConstants.FROM_KEY,
                                 getName(),
                                 OaiPmhParameterConstants.FROM_DEFAULT_VALUE));

        this.untilParam = Configuration.registerParameter(
                              new StringParameter(
                                  OaiPmhParameterConstants.UNTIL_KEY,
                                  getName(),
                                  OaiPmhParameterConstants.UNTIL_DEFAULT_VALUE));

        this.hostUrlParam = Configuration.registerParameter(
                                new StringParameter(
                                    OaiPmhParameterConstants.HOST_URL_KEY,
                                    getName(),
                                    OaiPmhParameterConstants.HOST_URL_DEFAULT_VALUE,
                                    ParameterMappingFunctions::mapToUrlString));
            
        this.metadataPrefixParam = Configuration.registerParameter(
                                       new StringParameter(
                                           OaiPmhParameterConstants.METADATA_PREFIX_KEY,
                                           getName(),
                                           OaiPmhParameterConstants.METADATA_PREFIX_DEFAULT_VALUE,
                                           ParameterMappingFunctions.createStringListMapper(OaiPmhParameterConstants.METADATA_PREFIX_ALLOWED_VALUES)));
    }


    @Override
    protected void onParameterChanged(AbstractParameter<?> param)
    {
        super.onParameterChanged(param);

        if (param == metadataPrefixParam)
            this.transformer = createTransformer();
    }


    /**
     * Assemble an OAI-PMH compliant Query-URL for retrieving a record list. Harvester preconfigured parameters
     * are used, but can also be manually configured via REST.
     */
    public String getListRecordsUrl()
    {
        String listRecordsUrl = null;

        if (hostUrlParam.getValue() != null && !hostUrlParam.getValue().isEmpty()) {
            StringBuilder queryBuilder = new StringBuilder();

            if (fromParam.getValue() != null && !fromParam.getValue().isEmpty())
                queryBuilder.append(OaiPmhConstants.DATE_FROM_QUERY).append(fromParam.getValue());

            if (untilParam.getValue() != null && !untilParam.getValue().isEmpty())
                queryBuilder.append(OaiPmhConstants.DATE_TO_QUERY).append(untilParam.getValue());

            if (metadataPrefixParam.getValue() != null && !metadataPrefixParam.getValue().isEmpty())
                queryBuilder.append(OaiPmhConstants.METADATA_PREFIX_QUERY).append(metadataPrefixParam.getValue());

            listRecordsUrl = String.format(OaiPmhConstants.LIST_RECORDS_URL, hostUrlParam.getValue(), queryBuilder.toString());
        }

        return listRecordsUrl;
    }


    /**
     * To fully support the OAI-PMH resumption Token for very large data-query
     * answers, a URL-string has to be compiled with a specific URL and an
     * automatically generated token.
     *
     * @return an URL-string to retrieve the next batch of records
     */
    public String getResumptionUrl(String resumptionToken)
    {
        return String.format(OaiPmhConstants.RESUMPTION_URL, hostUrlParam.getValue(), resumptionToken);
    }


    /**
     * Retrieves the name of the OAI-PMH repository that is to be harvested.
     *
     * @return the name of the OAI-PMH repository that is to be harvested
     */
    public String getRepositoryName()
    {
        final HttpRequester httpRequester = new HttpRequester(new Gson(), StandardCharsets.UTF_8);

        if (hostUrlParam.getValue() != null && !hostUrlParam.getValue().isEmpty()) {
            Document identifyDoc = httpRequester.getHtmlFromUrl(String.format(OaiPmhConstants.IDENTIFY_URL, hostUrlParam.getValue()));

            if (identifyDoc != null)
                return identifyDoc.select(OaiPmhConstants.REPOSITORY_NAME_ELEMENT).first().text();
        }

        return OaiPmhConstants.UNKNOWN_PROVIDER;
    }
}