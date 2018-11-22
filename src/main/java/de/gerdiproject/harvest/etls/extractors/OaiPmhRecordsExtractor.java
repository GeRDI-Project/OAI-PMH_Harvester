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

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;

import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.OaiPmhETL;
import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.etls.transformers.constants.DublinCoreConstants;
import de.gerdiproject.harvest.utils.data.HttpRequester;

/**
 * This extractor retrieves the HTML records from the harvested OAI-PMH repository,
 * considering resumption tokens.
 *
 * @author Robin Weiss
 */
public class OaiPmhRecordsExtractor extends AbstractIteratorExtractor<Element>
{
    private final HttpRequester httpRequester = new HttpRequester(new Gson(), StandardCharsets.UTF_8);
    private String recordsBaseUrl;
    private String resumptionUrlFormat;

    private String versionString;
    private int size = -1;


    @Override
    public String getUniqueVersionString()
    {
        return versionString;
    }


    @Override
    public int size()
    {
        return size;
    }




    @Override
    protected Iterator<Element> extractAll() throws ExtractorException
    {
        return new OaiPmhRecordsIterator(recordsBaseUrl);
    }


    @Override
    public void init(AbstractETL<?, ?> etl)
    {
        super.init(etl);

        final OaiPmhETL oaiEtl = (OaiPmhETL) etl;
        this.recordsBaseUrl = oaiEtl.getListRecordsUrl();
        this.resumptionUrlFormat = oaiEtl.getResumptionUrl("%s");

        if (recordsBaseUrl == null)
            throw new IllegalStateException(OaiPmhConstants.NO_METADATA_PREFIX_ERROR);

        // retrieve version as first record
        final Document doc = httpRequester.getHtmlFromUrl(recordsBaseUrl);
        final Element identifier = doc != null ? doc.select(DublinCoreConstants.IDENTIFIER).first() : null;
        this.versionString = identifier != null ? identifier.text() : null;

        // retrieve number of documents, if known
        final Element resumptionToken = doc != null ? doc.select(OaiPmhConstants.RESUMPTION_TOKEN_ELEMENT).first() : null;
        final String listSizeString = resumptionToken != null ? resumptionToken.attr(OaiPmhConstants.LIST_SIZE_ATTRIBUTE) : "";
        this.size = listSizeString.isEmpty() ? -1 : Integer.parseInt(listSizeString);
    }


    /**
     * An OAI-PMH iterator that iterates through records using the resumption token.
     *
     * @author Robin Weiss
     */
    private class OaiPmhRecordsIterator implements Iterator<Element>
    {
        final Queue<Element> records = new LinkedList<>();
        String recordsUrl;


        /**
         * Constructor that requires a URL that leads to OAI-PMH records.
         *
         * @param recordsUrl a URL that leads to OAI-PMH records
         */
        public OaiPmhRecordsIterator(String recordsUrl)
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
                retrieveRecords();

            return records.remove();
        }


        /**
         * Retrieves records and if possible the resumption URL from the OAI-PMH repository.
         */
        private void retrieveRecords()
        {
            final Document doc = httpRequester.getHtmlFromUrl(recordsUrl);

            if (doc == null)
                throw new ExtractorException(String.format(OaiPmhConstants.NO_RECORDS_ERROR, recordsUrl));

            final Elements newRecords = doc.select(OaiPmhConstants.RECORD_ELEMENT);

            if (newRecords.isEmpty())
                throw new ExtractorException(String.format(OaiPmhConstants.NO_RECORDS_ERROR, recordsUrl));

            final Element resumptionToken = doc.select(OaiPmhConstants.RESUMPTION_TOKEN_ELEMENT).first();

            this.records.addAll(newRecords);
            this.recordsUrl = (resumptionToken != null)
                              ? String.format(resumptionUrlFormat, resumptionToken.text())
                              : null;
        }
    }
}
