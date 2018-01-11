/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package de.gerdiproject.harvest.harvester;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.oaipmh.constants.OaiPmhParameterConstants;
import de.gerdiproject.harvest.oaipmh.constants.OaiPmhUrlConstants;
import de.gerdiproject.harvest.oaipmh.strategies.IStrategy;
import de.gerdiproject.harvest.oaipmh.strategies.OaiPmhStrategyFactory;
//import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.security.NoSuchAlgorithmException;

/**
 * An OAI-PMH-Protocol Harvester capable to harvest oai_dc, oai_datacite and datacite3 documents through a strategy pattern.
 * Each meta data standard is implemented in a strategy.
 * It supports OAI-PMH functionality like from and to date stamps to filter the result set.
 * Furthermore a so called Resumption-Token is implemented to get all records of a repository.
 *
 * @author Jan Frömberg, Robin Weiss
 */
public class OaipmhHarvester extends AbstractHarvester
{
    protected boolean isAborting;

    @Override
    protected boolean harvestInternal(int startIndex, int endIndex) throws Exception //NOPMD
    {
        String metadataPrefix = getProperty(OaiPmhParameterConstants.METADATA_PREFIX_KEY);
        IStrategy strategy = OaiPmhStrategyFactory.createStrategy(metadataPrefix);

        String url = assembleMainUrl();

        while (url != null) {

            // abort harvest, if it is flagged for cancellation
            if (isAborting) {
                currentHarvestingProcess.cancel(false);
                return false;
            }

            Document doc = httpRequester.getHtmlFromUrl(url);

            if (doc == null)
                break;

            // add records to list
            Elements records = doc.select("record");

            for (Element r : records) {
                // abort this inner loop if we abort the harvest
                if (isAborting)
                    break;

                IDocument jsonRecord = strategy.harvestRecord(r);
                addDocument(jsonRecord);
            }

            // get next URL
            Element token = doc.select("resumptionToken").first();
            url = (token != null)
                  ? assembleResumptionUrl(token.text())
                  : null;
        }

        return true;
    }


    @Override
    protected int initMaxNumberOfDocuments()
    {
        //Returns -1, because it is not feasible to count the maximum number of documents before harvesting.
        return -1;
    }

    /**
     * Assemble an OAI-PMH compliant Query-URL. Harvester preconfigured parameters are used,
     * but can also be manually configured via REST.
     */
    private String assembleMainUrl()
    {
        String mainUrl = null;
        String host = getProperty(OaiPmhParameterConstants.HOST_URL_KEY);

        if (host != null) {
            StringBuilder queryBuilder = new StringBuilder();

            String from = getProperty(OaiPmhParameterConstants.DATE_FROM_KEY);

            if (from != null && !from.isEmpty())
                queryBuilder.append(OaiPmhUrlConstants.DATE_FROM_QUERY).append(from);

            String until = getProperty(OaiPmhParameterConstants.DATE_TO_KEY);

            if (until != null && !until.isEmpty())
                queryBuilder.append(OaiPmhUrlConstants.DATE_TO_QUERY).append(until);

            String metadataPrefix = getProperty(OaiPmhParameterConstants.METADATA_PREFIX_KEY);

            if (metadataPrefix != null && !metadataPrefix.isEmpty())
                queryBuilder.append(OaiPmhUrlConstants.METADATA_PREFIX_QUERY).append(metadataPrefix);

            mainUrl = String.format(OaiPmhUrlConstants.BASE_URL, host, queryBuilder.toString());
        }

        return mainUrl;
    }

    /**
     * To fully support the OAI-PMH resumption Token for very large data-query answers,
     * a URL-string has to be compiled with a specific URL and an automatically generated token.
     * @return an URL-string to retrieve the next batch of records
     */
    private String assembleResumptionUrl(String resumptionToken)
    {
        String host = getProperty(OaiPmhParameterConstants.HOST_URL_KEY);
        return String.format(OaiPmhUrlConstants.RESUMPTION_URL, host, resumptionToken);
    }


    @Override
    protected String initHash() throws NoSuchAlgorithmException, NullPointerException
    {
        // TODO the hash cannot be calculated over such a large amount of records, a solution needs to be found once it becomes relevant
        return null;
    }


    @Override
    protected void abortHarvest()
    {
        if (currentHarvestingProcess != null)
            isAborting = true;
    }


    @Override
    protected void onHarvestAborted()
    {
        isAborting = false;
        super.onHarvestAborted();
    }
}