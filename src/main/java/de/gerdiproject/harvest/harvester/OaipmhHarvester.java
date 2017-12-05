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
 * An OAI-PMH Abstract-Harvester
 *
 * @author Jan Frömberg, Robin Weiss
 */
public class OaipmhHarvester extends AbstractHarvester
{
    protected boolean isAborting;

    @Override
    protected boolean harvestInternal(int startIndex, int endIndex) throws Exception
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

            //logger.info("resumptionUrl: " + url);

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
        return -1;
    }

    /**
     * Assemble an OAI-PMH complaint Query-URL. Harvester preconfigured parameters are used.
     * But can also be manually configured via REST.
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
     * an URL has to be compiled with an specific URL and an automatically generated token.
     * @return a url-string to get the next items
     */
    private String assembleResumptionUrl(String resumptionToken)
    {
        String host = getProperty(OaiPmhParameterConstants.HOST_URL_KEY);
        return String.format(OaiPmhUrlConstants.RESUMPTION_URL, host, resumptionToken);
    }


    @Override
    protected String initHash() throws NoSuchAlgorithmException, NullPointerException
    {
        // TODO Auto-generated method stub
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
