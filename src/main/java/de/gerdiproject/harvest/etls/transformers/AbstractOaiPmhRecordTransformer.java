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
package de.gerdiproject.harvest.etls.transformers;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.OaiPmhETL;
import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.etls.transformers.constants.DataCiteConstants;
import de.gerdiproject.harvest.utils.HtmlUtils;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Date;
import de.gerdiproject.json.datacite.DateRange;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.Subject;
import de.gerdiproject.json.datacite.abstr.AbstractDate;
import de.gerdiproject.json.datacite.enums.DateType;
import de.gerdiproject.json.datacite.extension.generic.WebLink;
import de.gerdiproject.json.datacite.extension.generic.enums.WebLinkType;

/**
 * This class offers a skeleton for transforming OAI-PMH records to {@linkplain DataCiteJson} objects.
 * The identifier and repository identifier are set by this class and do not have to be
 * specified by the sub-classes.
 *
 * @author Robin Weiss
 */
public abstract class AbstractOaiPmhRecordTransformer extends AbstractIteratorTransformer<Element, DataCiteJson>
{
    protected String repositoryIdentifier;
    protected List<WebLink> defaultLinks;


    /**
     * This method parses the OAI-PMH record and adds metadata to the transformed
     * {@linkplain DataCiteJson} document.
     *
     * @param document the document to which metadata is added
     * @param record the record that is to be parsed
     */
    protected abstract void setDocumentFieldsFromRecord(DataCiteJson document, Element record);


    @Override
    public void init(final AbstractETL<?, ?> etl)
    {
        final OaiPmhETL oaiEtl = (OaiPmhETL) etl;

        // retrieve info from the ETL
        this.repositoryIdentifier = oaiEtl.getRepositoryName();

        // set default links
        final WebLink logoLink = createLogoWebLink(oaiEtl.getLogoUrl());
        final WebLink viewLink = createViewWebLink(oaiEtl.getViewUrl());
        this.defaultLinks = Arrays.asList(logoLink, viewLink);
    }


    @Override
    protected DataCiteJson transformElement(final Element record) throws TransformerException
    {
        final Element header = getHeader(record);
        final DataCiteJson document;

        if (isRecordDeleted(header))
            document = null;
        else {
            final String identifierString = parseIdentifierFromHeader(header);
            document = new DataCiteJson(identifierString);
            document.setIdentifier(new Identifier(identifierString));
            document.setRepositoryIdentifier(repositoryIdentifier);
            document.addSubjects(parseSubjectsFromHeader(header));

            if (defaultLinks != null)
                document.addWebLinks(defaultLinks);

            setDocumentFieldsFromRecord(document, record);
        }

        return document;
    }


    /**
     * Retrieves the "header" element from the record.
     *
     * @param record the record that is to be parsed
     *
     * @return the header element
     */
    protected Element getHeader(final Element record)
    {
        return record.selectFirst(OaiPmhConstants.RECORD_HEADER);

    }


    /**
     * Retrieves the "metadata" element from the record.
     *
     * @param record the record that is to be parsed
     *
     * @return the metadata element
     */
    protected Element getMetadata(final Element record)
    {
        return record.selectFirst(OaiPmhConstants.RECORD_METADATA);
    }


    /**
     * Checks if the record is marked as deleted.
     *
     * @param header the record header that is to be parsed
     *
     * @return true if the record is marked as deleted
     */
    protected boolean isRecordDeleted(final Element header)
    {
        final String recordStatus = header.attr(OaiPmhConstants.HEADER_STATUS_ATTRIBUTE);
        return  recordStatus.equals(OaiPmhConstants.HEADER_STATUS_ATTRIBUTE_DELETED);
    }


    /**
     * Returns a logo {@linkplain WebLink} of a specified URL.
     *
     * @param url a URL that points to a logo
     *
     * @return a logo {@linkplain WebLink} or null, if the URL is empty
     */
    protected WebLink createLogoWebLink(final String url)
    {
        WebLink logoLink = null;

        if (url != null && !url.isEmpty()) {
            logoLink = new WebLink(
                url,
                OaiPmhConstants.LOGO_URL_TITLE,
                WebLinkType.ProviderLogoURL);
        }

        return logoLink;
    }


    /**
     * Returns an optional {@linkplain WebLink} that points to an official
     * record browser of the repository.
     *
     * @param url a URL that points to an official record browser of the repository
     *
     * @return a view {@linkplain WebLink} or null, if the URL is empty
     */
    protected WebLink createViewWebLink(final String url)
    {
        WebLink viewLink = null;

        if (url != null && !url.isEmpty()) {
            viewLink = new WebLink(
                url,
                OaiPmhConstants.VIEW_URL_NAME,
                WebLinkType.ViewURL);
        }

        return viewLink;
    }


    /**
     * Parses the record identifer from the header's "identifier" element.
     *
     * @param header the record header that is to be parsed
     *
     * @return the identifier of the record
     */
    protected String parseIdentifierFromHeader(final Element header)
    {
        return header.selectFirst(OaiPmhConstants.HEADER_IDENTIFIER).text();
    }


    /**
     * Parses the "setSpec" elements of the record header and returns
     * them in a list of {@linkplain Subject}s.
     *
     * @param header the record header that is to be parsed
     *
     * @return a list of "setSpec" {@linkplain Subject}s
     */
    protected List<Subject> parseSubjectsFromHeader(final Element header)
    {
        final List<Subject> subjectList = new LinkedList<>();

        final Elements setSpecs = header.select(OaiPmhConstants.HEADER_SET_SPEC);

        for (final Element s : setSpecs)
            subjectList.add(new Subject(s.text()));

        return subjectList;
    }


    /**
     * Parses a list of already parsed {@linkplain AbstractDate}s and
     * attempts to retrieve the publication year.
     *
     * @param datesList a collection of parsed {@linkplain AbstractDate}s
     *
     * @return the publication year or null, if no such date exists
     */
    protected Integer parsePublicationYearFromDates(final Collection<AbstractDate> datesList)
    {
        Integer publicationYear = null;

        if (datesList != null) {
            for (final AbstractDate d : datesList) {
                if (d.getType() == DateType.Issued) {
                    if (d instanceof Date)
                        publicationYear = ((Date)d).getValueAsDateTime().getYear();
                    else if (d instanceof DateRange)
                        publicationYear = ((DateRange)d).getRangeFromAsDateTime().getYear();

                    break;
                }
            }
        }

        return publicationYear;
    }


    /**
     * Assembles a prefix of an error message.
     *
     * @param record the record that caused the error
     *
     * @return an error message prefix stating that the record could not be harvested
     */
    protected String getErrorPrefix(final Element record)
    {
        final String identifier = HtmlUtils.getString(record, OaiPmhConstants.HEADER_IDENTIFIER);
        final String dateStamp = HtmlUtils.getString(record, OaiPmhConstants.HEADER_DATESTAMP);

        return String.format(
                   DataCiteConstants.RECORD_ERROR_PREFIX,
                   identifier,
                   dateStamp);
    }
}
