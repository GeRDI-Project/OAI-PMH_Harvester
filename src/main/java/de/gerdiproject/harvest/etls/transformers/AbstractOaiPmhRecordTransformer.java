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
import java.util.function.Function;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.OaiPmhETL;
import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
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
    protected String repositoryIdentifier = null;
    protected List<WebLink> defaultLinks = null;


    /**
     * This method parses the OAI-PMH record and adds metadata to the transformed
     * {@linkplain DataCiteJson} document.
     *
     * @param document the document to which metadata is added
     * @param record the record that is to be parsed
     */
    protected abstract void setDocumentFieldsFromRecord(DataCiteJson document, Element record);


    @Override
    public void init(AbstractETL<?, ?> etl)
    {
        super.init(etl);
        final OaiPmhETL oaiEtl = (OaiPmhETL) etl;

        // retrieve info from the ETL
        this.repositoryIdentifier = oaiEtl.getRepositoryName();

        // set default links
        final WebLink logoLink = createLogoWebLink(oaiEtl.getLogoUrl());
        this.defaultLinks = logoLink != null ? Arrays.asList(logoLink) : null;
    }


    @Override
    protected DataCiteJson transformElement(Element record) throws TransformerException
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
    protected Element getHeader(Element record)
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
    protected Element getMetadata(Element record)
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
    protected boolean isRecordDeleted(Element header)
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
    protected WebLink createLogoWebLink(String url)
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
     * Parses the record identifer from the header's "identifier" element.
     *
     * @param header the record header that is to be parsed
     *
     * @return the identifier of the record
     */
    protected String parseIdentifierFromHeader(Element header)
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
    protected List<Subject> parseSubjectsFromHeader(Element header)
    {
        final List<Subject> subjectList = new LinkedList<>();

        final Elements setSpecs = header.select(OaiPmhConstants.HEADER_SET_SPEC);

        for (Element s : setSpecs)
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
    protected Integer parsePublicationYearFromDates(Collection<AbstractDate> datesList)
    {
        Integer publicationYear = null;

        if (datesList != null) {
            for (AbstractDate d : datesList) {
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
     * Retrieves the text of the first occurrence of a specified tag.
     *
     * @param ele the HTML element that is to be parsed
     * @param tagName the tag of which the text is to be retrieved
     *
     * @return the text inside the first occurrence of a specified tag,
     *          or null if the tag could not be found
     */
    protected String getString(Element ele, String tagName)
    {
        final Element stringElement = ele.selectFirst(tagName);
        return stringElement == null ? null : stringElement.text();
    }


    /**
     * Retrieves the texts of all child tags of an {@linkplain Element}.
     *
     * @param ele the HTML {@linkplain Element} that contains the parent tag
     * @param tagName the name of the parent {@linkplain Element} of the child tags
     *
     * @return a {@linkplain List} of {@linkplain String}s
     *          or null if the tag could not be found
     */
    protected List<String> getStrings(Element ele, String tagName)
    {
        final Element parent = ele.selectFirst(tagName);
        return parent == null ? null : elementsToStringList(parent.children());
    }


    /**
     * Retrieves the first occurrence of a specified tag and maps it to a specified class.
     *
     * @param ele the HTML {@linkplain Element} that contains the requested tag
     * @param tagName the name of the requested tag
     * @param eleToObject a mapping function that generates the requested class
     * @param <T> the requested type of the converted tag
     *
     * @return an object representation of the tag or null if it does not exist
     */
    protected <T> T getObject(Element ele, String tagName, Function<Element, T> eleToObject)
    {
        final Element requestedTag = ele.selectFirst(tagName);
        return requestedTag == null ? null : eleToObject.apply(requestedTag);
    }


    /**
     * Retrieves all child tags of a specified tag and maps them to a {@linkplain List} of a specified class.
     *
     * @param ele the HTML {@linkplain Element} that contains the parent tag
     * @param tagName the name of the parent tag
     * @param eleToObject a mapping function that maps a single child to the specified class
     * @param <T> the requested type of the converted tag
     *
     * @return a {@linkplain List} of objects of the tag or null if it does not exist
     */
    protected <T> List<T> getObjects(Element ele, String tagName, Function<Element, T> eleToObject)
    {
        final Element parent = ele.selectFirst(tagName);
        return parent == null
               ? null
               : elementsToList(parent.children(), eleToObject);
    }


    /**
     * Retrieves the value of a HTML attribute.
     *
     * @param ele the HTML element that possibly has the attribute
     * @param attributeKey the key of the attribute
     *
     * @return the attribute value, or null if no such attribute exists
     */
    protected String getAttribute(Element ele, String attributeKey)
    {
        final String attr = ele.attr(attributeKey);
        return attr.isEmpty() ? null : attr;
    }


    /**
     * Retrieves the value of a HTML attribute and attempts to map it to an {@linkplain Enum}.
     *
     * @param ele the HTML element that possibly has the attribute
     * @param attributeKey the key of the attribute
     * @param enumClass the class to which the attribute value must be mapped
     * @param <T> the type of the {@linkplain Enum}
     *
     * @return the enum representation of the attribute value, or null if no such attribute exists or could not be mapped
     */
    protected <T extends Enum<T>> T getEnumAttribute(Element ele, String attributeKey, Class<T> enumClass)
    {
        T returnValue = null;

        try {
            if (ele.hasAttr(attributeKey))
                returnValue = Enum.valueOf(enumClass, ele.attr(attributeKey).trim());
        } catch (IllegalArgumentException e) {
            returnValue = null;
        }

        return returnValue;
    }


    /**
     * Applies a mapping function to a {@linkplain Collection} of {@linkplain Element}s,
     * generating a {@linkplain List} of specified objects.
     *
     * @param elements the elements that are to be mapped
     * @param eleToObject the mapping function
     * @param <T> the type to which the elements are to be mapped
     *
     * @return a list of objects that were mapped or null if no object could be mapped
     */
    protected <T> List<T> elementsToList(Collection<Element> elements, Function<Element, T> eleToObject)
    {
        if (elements == null || elements.isEmpty())
            return null;

        final List<T> list = new LinkedList<>();

        for (Element ele : elements) {
            final T obj = eleToObject.apply(ele);

            if (obj != null)
                list.add(obj);
        }

        return list.isEmpty() ? null : list;
    }


    /**
     * Maps a {@linkplain Collection} of {@linkplain Element}s to a {@linkplain List} of {@linkplain String}s
     * by retrieving the text of the tag elements.
     *
     * @param elements the elements that are to be converted to strings
     *
     * @return a {@linkplain List} of {@linkplain String}s
     */
    protected List<String> elementsToStringList(Collection<Element> elements)
    {
        return elementsToList(elements, (Element ele) -> ele.text());
    }
}
