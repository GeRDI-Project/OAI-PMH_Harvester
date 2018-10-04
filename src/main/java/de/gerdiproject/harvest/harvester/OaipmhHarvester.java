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
package de.gerdiproject.harvest.harvester;

import java.security.NoSuchAlgorithmException;

//import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.harvester.events.GetProviderNameEvent;
import de.gerdiproject.harvest.oaipmh.constants.DublinCoreStrategyConstants;
import de.gerdiproject.harvest.oaipmh.constants.OaiPmhConstants;
import de.gerdiproject.harvest.oaipmh.constants.OaiPmhParameterConstants;
import de.gerdiproject.harvest.oaipmh.strategies.IStrategy;
import de.gerdiproject.harvest.oaipmh.strategies.OaiPmhStrategyFactory;
import de.gerdiproject.harvest.utils.HashGenerator;

/**
 * An OAI-PMH-Protocol Harvester capable to harvest oai_dc, oai_datacite and
 * datacite3 documents through a strategy pattern. Each meta data standard is
 * implemented in a strategy. It supports OAI-PMH functionality like from and to
 * date stamps to filter the result set. Furthermore a so called
 * Resumption-Token is implemented to get all records of a repository.
 *
 * @author Jan Frömberg, Robin Weiss
 */
public class OaipmhHarvester extends AbstractHarvester
{
    private String repositoryUrl;
    private String queryMetadataPrefix;
    private String queryFrom;
    private String queryUntil;


    @Override
    public void init()
    {
        queryFrom = getProperty(OaiPmhParameterConstants.DATE_FROM_KEY);
        queryUntil = getProperty(OaiPmhParameterConstants.DATE_TO_KEY);
        repositoryUrl = getProperty(OaiPmhParameterConstants.HOST_URL_KEY);
        queryMetadataPrefix = getProperty(OaiPmhParameterConstants.METADATA_PREFIX_KEY);

        super.init();
    }


    @Override
    protected boolean harvestInternal(int startIndex, int endIndex) throws Exception // NOPMD - we want this inheriting class to be able to throw any exception
    {
        final IStrategy harvestingStrategy = OaiPmhStrategyFactory.createStrategy(queryMetadataPrefix);
        String url = getListRecordsUrl();

        int processedRecords = 0;

        while (url != null) {

            // abort harvest, if it is flagged for cancellation
            if (isAborting) {
                currentHarvestingProcess.cancel(false);
                return false;
            }

            final Document doc = httpRequester.getHtmlFromUrl(url);

            if (doc == null)
                break;

            // add records to list
            final Elements records = doc.select(OaiPmhConstants.RECORD_ELEMENT);
            final int numberOfRecords = records.size();

            // skip records if they are out of range
            if (startIndex >= processedRecords + numberOfRecords)
                processedRecords += numberOfRecords;
            else {
                final int from = Math.max(0, startIndex - processedRecords);
                final int until = endIndex == -1
                                  ? numberOfRecords
                                  : Math.min(numberOfRecords, endIndex - processedRecords);

                processedRecords += from;

                for (int i = from; i < until; i++) {
                    // abort this inner loop if we abort the harvest
                    if (isAborting)
                        break;

                    IDocument jsonRecord = harvestingStrategy.harvestRecord(records.get(i));
                    addDocument(jsonRecord);
                    processedRecords++;
                }
            }

            // get next URL if we have not reached our max range yet
            if (!isAborting && endIndex == -1 || processedRecords < endIndex) {
                final Element token = doc.select(OaiPmhConstants.RESUMPTION_TOKEN_ELEMENT).first();
                url = (token != null) ? getResumptionUrl(token.text()) : null;
            } else
                break;
        }

        return true;
    }


    @Override
    protected int initMaxNumberOfDocuments()
    {
        // Returns -1, because it is not feasible to count the maximum number of
        // documents before harvesting.
        return -1;
    }


    @Override
    protected String onGetDataProviderName(GetProviderNameEvent event)
    {
        String providerName = OaiPmhConstants.DEFAULT_PROVIDER;

        if (repositoryUrl != null) {
            Document identifyDoc = httpRequester.getHtmlFromUrl(String.format(OaiPmhConstants.IDENTIFY_URL, repositoryUrl));

            if (identifyDoc != null)
                providerName = identifyDoc.select(OaiPmhConstants.REPOSITORY_NAME_ELEMENT).first().text();
        }

        return providerName;
    }


    /**
     * Assemble an OAI-PMH compliant Query-URL for retrieving a record list. Harvester preconfigured parameters
     * are used, but can also be manually configured via REST.
     */
    private String getListRecordsUrl()
    {
        String listRecordsUrl = null;

        if (repositoryUrl != null) {
            StringBuilder queryBuilder = new StringBuilder();

            if (queryFrom != null && !queryFrom.isEmpty())
                queryBuilder.append(OaiPmhConstants.DATE_FROM_QUERY).append(queryFrom);

            if (queryUntil != null && !queryUntil.isEmpty())
                queryBuilder.append(OaiPmhConstants.DATE_TO_QUERY).append(queryUntil);

            if (queryMetadataPrefix != null && !queryMetadataPrefix.isEmpty())
                queryBuilder.append(OaiPmhConstants.METADATA_PREFIX_QUERY).append(queryMetadataPrefix);

            listRecordsUrl = String.format(OaiPmhConstants.LIST_RECORDS_URL, repositoryUrl, queryBuilder.toString());
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
    private String getResumptionUrl(String resumptionToken)
    {
        return String.format(OaiPmhConstants.RESUMPTION_URL, repositoryUrl, resumptionToken);
    }


    @Override
    protected void setProperty(String key, String value)
    {
        super.setProperty(key, value);

        // if the query or URL changes, init() must be called again
        switch (key) {
            case OaiPmhParameterConstants.DATE_FROM_KEY:
            case OaiPmhParameterConstants.DATE_TO_KEY:
            case OaiPmhParameterConstants.HOST_URL_KEY:
            case OaiPmhParameterConstants.METADATA_PREFIX_KEY:
                init();
                break;

            default:
                // do nothing
        }
    }


    @Override
    protected String initHash() throws NoSuchAlgorithmException, NullPointerException
    {
        final String baseUrl = getListRecordsUrl();

        if (baseUrl == null)
            return null;

        // retrieve records
        final Document recordsDoc = httpRequester.getHtmlFromUrl(baseUrl);

        // retrieve identifier of the latest record
        if (recordsDoc != null) {
            final Element identifier = recordsDoc.select(DublinCoreStrategyConstants.IDENTIFIER).first();

            if (identifier != null)
                return HashGenerator.instance().getShaHash(identifier.text());
        }

        return null;
    }
}