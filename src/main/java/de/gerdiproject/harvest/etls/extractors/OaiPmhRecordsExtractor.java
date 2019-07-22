/*
 *  Copyright © 2018 Robin Weiss (http://www.gerdi-project.de/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.etls.extractors;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.OaiPmhETL;
import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.utils.HtmlUtils;
import de.gerdiproject.harvest.utils.data.HttpRequester;

/**
 * This extractor retrieves the HTML records from the harvested OAI-PMH repository,
 * considering resumption tokens.
 *
 * @author Robin Weiss
 */
public class OaiPmhRecordsExtractor extends AbstractIteratorExtractor<Element>
{
    // protected fields used by the inner class
    protected final static Logger LOGGER = LoggerFactory.getLogger(OaiPmhRecordsExtractor.class);
    protected final HttpRequester httpRequester = new HttpRequester();
    protected String lastHarvestedDate;
    protected String fallbackUrlFormat;
    protected String resumptionUrlFormat;

    private String recordsBaseUrl;
    private String versionString;
    private int recordCount = -1;


    @Override
    public String getUniqueVersionString()
    {
        return versionString;
    }


    @Override
    public int size()
    {
        return recordCount;
    }


    @Override
    protected Iterator<Element> extractAll() throws ExtractorException
    {
        return new OaiPmhRecordsIterator(recordsBaseUrl);
    }


    @Override
    public void init(final AbstractETL<?, ?> etl)
    {
        super.init(etl);

        this.lastHarvestedDate = null;

        final OaiPmhETL oaiEtl = (OaiPmhETL) etl;
        this.recordsBaseUrl = oaiEtl.getListRecordsUrl();
        this.resumptionUrlFormat = oaiEtl.getResumptionUrlFormat();
        this.fallbackUrlFormat = oaiEtl.getFallbackResumptionUrlFormat();

        // retrieve version as first record
        final Document doc = httpRequester.getHtmlFromUrl(recordsBaseUrl);
        final Element identifier = doc == null ? null : doc.selectFirst(OaiPmhConstants.HEADER_IDENTIFIER);
        this.versionString = identifier == null ? null : identifier.text();

        // retrieve number of documents, if known
        final Element resumptionToken = doc == null ? null : doc.selectFirst(OaiPmhConstants.RESUMPTION_TOKEN_ELEMENT);
        final String listSizeString = resumptionToken == null ? "" : resumptionToken.attr(OaiPmhConstants.LIST_SIZE_ATTRIBUTE);
        this.recordCount = listSizeString.isEmpty() ? -1 : Integer.parseInt(listSizeString);
    }


    /**
     * Retrieves the date stamp of the most recent extracted batch of records.
     * This date can be logged if the harvest fails, and can be used to re-attempt
     * the harvest from where it left off.
     *
     * @return the Datestamp header field of the first record of the currently extracted batch
     */
    public String getLastHarvestedDate()
    {
        return lastHarvestedDate;
    }


    @Override
    public void clear()
    {
        // nothing to clean up
    }


    /**
     * An OAI-PMH iterator that iterates through records using the resumption token.
     *
     * @author Robin Weiss
     */
    private class OaiPmhRecordsIterator implements Iterator<Element>
    {
        private final Queue<Element> records = new LinkedList<>();
        private String recordsUrl;


        /**
         * Constructor that requires a URL that leads to OAI-PMH records.
         *
         * @param recordsUrl a URL that leads to OAI-PMH records
         */
        public OaiPmhRecordsIterator(final String recordsUrl)
        {
            this.recordsUrl = recordsUrl;
        }


        @Override
        public boolean hasNext()
        {
            return !records.isEmpty() || recordsUrl != null;
        }


        @Override
        public Element next()
        {
            // if the current records queue is empty, retrieve more via the resumption url
            if (records.isEmpty())
                retrieveRecords(false);

            // retrieve the next record
            final Element nextRecord = records.remove();

            // memorize the datestamp of the first record, in case the harvest fails
            lastHarvestedDate = HtmlUtils.getString(nextRecord, OaiPmhConstants.HEADER_DATESTAMP);

            return nextRecord;
        }


        /**
         * Retrieves records and if possible the resumption URL from the OAI-PMH repository.
         *
         * @param isUsingFallbackUrl if true, this is a fallback attempt to retrieve records
         * using an alternative URL
         */
        private void retrieveRecords(final boolean isUsingFallbackUrl)
        {
            final Document doc = httpRequester.getHtmlFromUrl(recordsUrl);

            final Elements newRecords = doc == null
                                        ? null
                                        : doc.select(OaiPmhConstants.RECORD_ELEMENT);

            // make sure the web request returns a set of records
            if (newRecords == null || newRecords.isEmpty()) {
                // if no records could be retrieved even via the fallback URL, abort
                if (isUsingFallbackUrl || lastHarvestedDate == null) {

                    // display a different error message if this problem occurs in the middle of the harvest
                    final String errorMessage = lastHarvestedDate == null
                                                ? OaiPmhConstants.NO_RECORDS_ERROR
                                                : OaiPmhConstants.NO_RECORDS_RESUMED_ERROR;

                    // abort the harvest
                    throw new ExtractorException(String.format(errorMessage, recordsUrl));

                } else {
                    // assemble fallback URL, using the date of the last successfully harvested record
                    final String fallbackUrl = String.format(fallbackUrlFormat, lastHarvestedDate);

                    // log the fallback
                    if (LOGGER.isInfoEnabled())
                        LOGGER.info(String.format(OaiPmhConstants.FALLBACK_URL_INFO, recordsUrl, fallbackUrl));

                    // try to retrieve records again, via the fallback URL
                    this.recordsUrl = fallbackUrl;
                    retrieveRecords(true);
                }

            } else {
                final Element resumptionToken = doc.selectFirst(OaiPmhConstants.RESUMPTION_TOKEN_ELEMENT);
                this.records.addAll(newRecords);

                if (resumptionToken == null || resumptionToken.text() == null || resumptionToken.text().isEmpty())
                    this.recordsUrl = null;
                else
                    this.recordsUrl = String.format(resumptionUrlFormat, resumptionToken.text());
            }
        }
    }
}
