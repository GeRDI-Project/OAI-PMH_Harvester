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
package de.gerdiproject.harvest.etls.transformers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.gerdiproject.harvest.etls.transformers.constants.DublinCoreConstants;
import de.gerdiproject.json.datacite.Contributor;
import de.gerdiproject.json.datacite.Creator;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Date;
import de.gerdiproject.json.datacite.Description;
import de.gerdiproject.json.datacite.Rights;
import de.gerdiproject.json.datacite.Subject;
import de.gerdiproject.json.datacite.Title;
import de.gerdiproject.json.datacite.abstr.AbstractDate;
import de.gerdiproject.json.datacite.enums.ContributorType;
import de.gerdiproject.json.datacite.enums.DateType;
import de.gerdiproject.json.datacite.enums.DescriptionType;
import de.gerdiproject.json.datacite.extension.generic.WebLink;
import de.gerdiproject.json.datacite.extension.generic.enums.WebLinkType;

/**
 * This class is a transformer for OAI-PMH DublinCore records.
 *
 * @author Jan Frömberg, Robin Weiss
 */
public class DublinCoreTransformer extends AbstractOaiPmhRecordTransformer
{
    @Override
    protected void setDocumentFieldsFromRecord(DataCiteJson document, Element record)
    {
        // get header and meta data for each record
        Element metadata = getMetadata(record);

        // parse dates and publication date
        final List<AbstractDate> dateList = parseDates(metadata);
        document.addDates(dateList);
        document.setPublicationYear(parsePublicationYearFromDateList(dateList));

        document.addCreators(parseCreators(metadata));
        document.addContributors(parseContributors(metadata));
        document.addTitles(parseTitles(metadata));
        document.addDescriptions(parseDescriptions(metadata));
        document.setPublisher(parsePublisher(metadata));
        document.addFormats(parseFormats(metadata));
        document.addSubjects(parseSubjects(metadata));
        document.addRights(parseRights(metadata));
        document.setLanguage(parseLanguage(metadata));

        document.addWebLinks(parseWebLinks(metadata));
    }


    /**
     * Parses {@linkplain AbstractDate}s from the record metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of {@linkplain AbstractDate}s
     */
    private List<AbstractDate> parseDates(Element metadata)
    {
        final List<AbstractDate> dateList = new LinkedList<>();

        final Elements dateElements = metadata.select(DublinCoreConstants.METADATA_DATE);

        for (Element e : dateElements)
            dateList.add(new Date(e.text(), DateType.Issued));

        return dateList;
    }


    /**
     * Parses {@linkplain WebLink}s from the record metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of {@linkplain WebLink}s
     */
    private List<WebLink> parseWebLinks(Element metadata)
    {
        final List<WebLink> webLinkList = new LinkedList<>();

        final Elements resourceIdentifierElements = metadata.select(DublinCoreConstants.METADATA_IDENTIFIER);

        for (Element e : resourceIdentifierElements) {
            try {
                // check if URL is valid
                new URL(e.text());

                webLinkList.add(new WebLink(e.text(), "View URL", WebLinkType.ViewURL));

            } catch (MalformedURLException ex) {
                continue;
            }
        }

        return webLinkList;
    }

    /**
     * Parses {@linkplain Creator}s from the record metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of {@linkplain Creator}s
     */
    private List<Creator> parseCreators(Element metadata)
    {
        final List<Creator> creatorList = new LinkedList<>();

        final Elements creatorElements = metadata.select(DublinCoreConstants.DOC_CREATORS);

        for (Element e : creatorElements)
            creatorList.add(new Creator(e.text()));

        return creatorList;
    }


    /**
     * Parses {@linkplain Contributor}s from the record metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of {@linkplain Contributor}s
     */
    private List<Contributor> parseContributors(Element metadata)
    {
        final List<Contributor> contributorList = new LinkedList<>();

        final Elements contributorElements = metadata.select(DublinCoreConstants.DOC_CONTRIBUTORS);

        for (Element e : contributorElements)
            contributorList.add(new Contributor(e.text(), ContributorType.ContactPerson));

        return contributorList;
    }


    /**
     * Parses {@linkplain Title}s from the record metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of {@linkplain Title}s
     */
    private List<Title> parseTitles(Element metadata)
    {
        final List<Title> titleList = new LinkedList<>();

        final Elements titleElements = metadata.select(DublinCoreConstants.DOC_TITLE);

        for (Element title : titleElements)
            titleList.add(new Title(title.text()));

        return titleList;
    }


    /**
     * Parses {@linkplain Description}s from the record metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of {@linkplain Description}s
     */
    private List<Description> parseDescriptions(Element metadata)
    {
        final List<Description> descriptionList = new LinkedList<>();

        final Elements descriptionElements = metadata.select(DublinCoreConstants.DOC_DESCRIPTIONS);

        for (Element descElement : descriptionElements)
            descriptionList.add(new Description(descElement.text(), DescriptionType.Abstract));

        return descriptionList;
    }


    /**
     * Parses the publisher string from the record metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return the publisher name or null, if it does not occur in the metadata
     */
    private String parsePublisher(Element metadata)
    {
        final Element publisherElement = metadata.selectFirst(DublinCoreConstants.PUBLISHER);
        return publisherElement != null
               ? publisherElement.text()
               : null;
    }


    /**
     * Parses formats from the record metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of format strings
     */
    private List<String> parseFormats(Element metadata)
    {
        final List<String> formatList = new LinkedList<>();

        // parse formats
        final Elements formatElements = metadata.select(DublinCoreConstants.METADATA_FORMATS);

        for (Element e : formatElements)
            formatList.add(e.text());

        // parse DC types
        final Elements typeElements = metadata.select(DublinCoreConstants.RES_TYPE);

        for (Element e : typeElements)
            formatList.add(e.text());

        return formatList;
    }


    /**
     * Parses {@linkplain Subject}s from the record metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of {@linkplain Subject}s
     */
    private List<Subject> parseSubjects(Element metadata)
    {
        final List<Subject> subjectList = new LinkedList<>();

        final Elements subjectElements = metadata.select(DublinCoreConstants.SUBJECTS);

        for (Element subject : subjectElements)
            subjectList.add(new Subject(subject.text()));

        return subjectList;
    }


    /**
     * Parses a language string from the record metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a language string or null, if it is not part of the metadata
     */
    private String parseLanguage(Element metadata)
    {
        final Element languageElement = metadata.selectFirst(DublinCoreConstants.LANG);
        return languageElement != null
               ? languageElement.text()
               : null;
    }


    /**
     * Parses {@linkplain Rights} from the record metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of {@linkplain Rights}
     */
    private List<Rights> parseRights(Element metadata)
    {
        final List<Rights> rightsList = new LinkedList<>();

        final Elements rightsElements = metadata.select(DublinCoreConstants.RIGHTS);

        for (Element e : rightsElements)
            rightsList.add(new Rights(e.text()));

        return rightsList;
    }
}
