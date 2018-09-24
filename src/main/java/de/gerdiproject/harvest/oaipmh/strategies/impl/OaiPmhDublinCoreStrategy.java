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
package de.gerdiproject.harvest.oaipmh.strategies.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.oaipmh.constants.DublinCoreStrategyConstants;
import de.gerdiproject.harvest.oaipmh.strategies.IStrategy;
import de.gerdiproject.json.datacite.Contributor;
import de.gerdiproject.json.datacite.Creator;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Date;
import de.gerdiproject.json.datacite.Description;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.RelatedIdentifier;
import de.gerdiproject.json.datacite.Rights;
import de.gerdiproject.json.datacite.Subject;
import de.gerdiproject.json.datacite.Title;
import de.gerdiproject.json.datacite.abstr.AbstractDate;
import de.gerdiproject.json.datacite.enums.ContributorType;
import de.gerdiproject.json.datacite.enums.DateType;
import de.gerdiproject.json.datacite.enums.DescriptionType;
import de.gerdiproject.json.datacite.extension.WebLink;
import de.gerdiproject.json.datacite.extension.enums.WebLinkType;

/**
 * An OAI-PMH DublinCore metadata strategy for harvesting documents from records
 *
 * @author Jan Frömberg
 */
public class OaiPmhDublinCoreStrategy implements IStrategy
{
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy'-'MM'-'dd");


    @Override
    public IDocument harvestRecord(Element record)
    {
        // each entry-node starts with a record element.
        Elements children = record.children();
        Boolean deleted = children.first().attr(DublinCoreStrategyConstants.RECORD_STATUS).equals(
                              DublinCoreStrategyConstants.RECORD_STATUS_DEL) ? true : false;

        //check if Entry is "deleted"
        if (deleted)
            return null;

        // get header and meta data for each record
        Elements header = children.select(DublinCoreStrategyConstants.RECORD_HEADER);
        Elements metadata = children.select(DublinCoreStrategyConstants.RECORD_METADATA);

        List<WebLink> webLinks = new LinkedList<>();
        List<RelatedIdentifier> relatedIdentifiers = new LinkedList<>();
        List<AbstractDate> dates = new LinkedList<>();
        List<Title> titles = new LinkedList<>();
        List<Description> descriptions = new LinkedList<>();
        List<Subject> subjects = new LinkedList<>();
        List<Creator> creators = new LinkedList<>();
        List<Contributor> contributors = new LinkedList<>();
        List<String> dctype = new LinkedList<>();
        List<String> formats = new LinkedList<>();
        List<Rights> rightslist = new LinkedList<>();

        // get identifier and datestamp
        Element identifier = header.select(DublinCoreStrategyConstants.IDENTIFIER).first();
        DataCiteJson document = new DataCiteJson(identifier.text());
        Identifier mainIdentifier = new Identifier(identifier.text());

        // get last updated
        String recorddate = header.select(DublinCoreStrategyConstants.RECORD_DATESTAMP).first().text();
        Date updatedDate = new Date(recorddate, DateType.Updated);
        dates.add(updatedDate);

        // based XSD schema -> http://dublincore.org/schemas/xmls/simpledc20021212.xsd
        // get publication date
        Calendar cal = Calendar.getInstance();
        Elements dateElements = metadata.select(DublinCoreStrategyConstants.METADATA_DATE);

        for (Element e : dateElements) {
            try {
                cal.setTime(dateFormat.parse(e.text()));
                document.setPublicationYear((short) cal.get(Calendar.YEAR));

                Date publicationDate = new Date(e.text(), DateType.Available);
                dates.add(publicationDate);
            } catch (ParseException ex) { //NOPMD do nothing. just do not add the date if it does not exist
            }
        }

        Elements resourceIdentifierElements = metadata.select(DublinCoreStrategyConstants.METADATA_IDENTIFIER);

        for (Element e : resourceIdentifierElements) {
            try {
                // check if URL is valid
                new URL(e.text());

                WebLink viewLink = new WebLink(e.text());
                viewLink.setType(WebLinkType.ViewURL);
                viewLink.setName("View URL");
                webLinks.add(viewLink);

            } catch (MalformedURLException e1) {
                continue;
            }
        }

        document.setWebLinks(webLinks);

        // get resource types
        Elements typeElements = metadata.select(DublinCoreStrategyConstants.RES_TYPE);

        for (Element e : typeElements)
            dctype.add(e.text());

        document.setFormats(dctype);

        // get creators
        Elements creatorElements = metadata.select(DublinCoreStrategyConstants.DOC_CREATORS);

        for (Element e : creatorElements)
            creators.add(new Creator(e.text()));

        document.setCreators(creators);

        // get contributors
        Elements contributorElements = metadata.select(DublinCoreStrategyConstants.DOC_CONTRIBUTORS);

        for (Element e : contributorElements) {
            Contributor contrib = new Contributor(e.text(), ContributorType.ContactPerson);
            contributors.add(contrib);
        }

        document.setContributors(contributors);

        // get titles
        Elements titleElements = metadata.select(DublinCoreStrategyConstants.DOC_TITLE);

        for (Element title : titleElements)
            titles.add(new Title(title.text()));

        document.setTitles(titles);

        // get descriptions
        Elements descriptionElements = metadata.select(DublinCoreStrategyConstants.DOC_DESCRIPTIONS);

        for (Element descElement : descriptionElements) {
            Description description = new Description(descElement.text(), DescriptionType.Abstract);
            descriptions.add(description);
        }

        document.setDescriptions(descriptions);

        // get publisher
        Elements publisherElements = metadata.select(DublinCoreStrategyConstants.PUBLISHER);
        document.setPublisher(publisherElements.first().text());

        // get formats
        Elements formatElements = metadata.select(DublinCoreStrategyConstants.METADATA_FORMATS);

        for (Element e : formatElements)
            formats.add(e.text());

        document.setFormats(formats);

        // get keyword subjects
        Elements subjectElements = metadata.select(DublinCoreStrategyConstants.SUBJECTS);

        for (Element subject : subjectElements) {
            Subject dcsubject = new Subject(subject.text());
            subjects.add(dcsubject);
        }

        document.setSubjects(subjects);

        // get rights
        Elements rightsElements = metadata.select(DublinCoreStrategyConstants.RIGHTS);

        for (Element e : rightsElements)
            rightslist.add(new Rights(e.text()));

        document.setRightsList(rightslist);

        // get source, relation, coverage -> missing in document-Class

        // get language
        Elements languageElements = metadata.select(DublinCoreStrategyConstants.LANG);

        if (!languageElements.isEmpty())
            document.setLanguage(languageElements.first().text());

        // compile a document
        document.setIdentifier(mainIdentifier);

        // add dates if there are any
        if (!dates.isEmpty())
            document.setDates(dates);

        // add related identifiers if there are any
        if (!relatedIdentifiers.isEmpty())
            document.setRelatedIdentifiers(relatedIdentifiers);

        return document;
    }
}
