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
package de.gerdiproject.harvest.oaipmh.strategies.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.gerdiproject.harvest.IDocument;
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
 * An OAI-PMH DublinCore metadata strategy for harvested elements
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
    		// sub-elements are header and metadata
        DataCiteJson document = new DataCiteJson();
        
        // get header and meta data stuff for each record
        Elements children = record.children();
        Elements headers = children.select("header");
        Boolean deleted = children.first().attr("status").equals("deleted") ? true : false;
        Elements metadata = children.select("metadata");

        List<WebLink> links = new LinkedList<>();
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
        Element identifier = headers.select("identifier").first();
        Identifier mainIdentifier = new Identifier(identifier.text());

        // get last updated
        String recorddate = headers.select("datestamp").first().text();
        Date updatedDate = new Date(recorddate, DateType.Updated);
        dates.add(updatedDate);

        //check if Entry is "deleted"
        if (deleted) {
            document.setVersion("deleted");
            document.setIdentifier(mainIdentifier);

            // add dates if there are any
            if (!dates.isEmpty())
                document.setDates(dates);

            return document;
        }

        // get publication date
        Calendar cal = Calendar.getInstance();
        Elements pubdate = metadata.select("dc|date");

        for (Element e : pubdate) {
	        try {
	            cal.setTime(dateFormat.parse(e.text()));
	            document.setPublicationYear((short) cal.get(Calendar.YEAR));
	
	            Date publicationDate = new Date(e.text(), DateType.Available);
	            dates.add(publicationDate);
	        } catch (ParseException ex) { //NOPMD do nothing. just do not add the date if it does not exist
	        }
        }

        // get resource types
        Elements dctypes = metadata.select("dc|type");

        for (Element e : dctypes)
            dctype.add(e.text());

        document.setFormats(dctype);

        // get creators
        Elements creatorElements = metadata.select("dc|creator");

        for (Element e : creatorElements) {
            Creator creator = new Creator(e.text());
            creators.add(creator);
        }

        document.setCreators(creators);

        // get contributors
        Elements contribElements = metadata.select("dc|contributor");

        for (Element e : contribElements) {
            Contributor contrib = new Contributor(e.text(), ContributorType.ContactPerson);
            contributors.add(contrib);
        }

        document.setContributors(contributors);

        // get titles
        Elements dctitles = metadata.select("dc|title");

        for (Element title : dctitles) {
            Title dctitle = new Title(title.text());
            titles.add(dctitle);
        }

        document.setTitles(titles);

        // get descriptions
        Elements descriptionElements = metadata.select("dc|description");

        for (Element descElement : descriptionElements) {
            Description description = new Description(descElement.text(), DescriptionType.Abstract);
            descriptions.add(description);
        }

        document.setDescriptions(descriptions);
        
        // get publisher
        Elements pubElem = metadata.select("dc|publisher");

        for (Element e : pubElem) {
            String pub = e.text();
            document.setPublisher(pub);
        }
        
        // get formats
        Elements fmts = metadata.select("dc|format");

        for (Element e : fmts) {
            String fmt = e.text();
            formats.add(fmt);
        }
        
        document.setFormats(formats);

        // get identifier URLs
        Elements identEles = metadata.select("dc|identifier");
        int numidents = identEles.size();

        for (Element identElement : identEles) {
            WebLink viewLink = new WebLink(identElement.text());
            viewLink.setName("Identifier" + numidents);
            viewLink.setType(WebLinkType.ViewURL);
            links.add(viewLink);
            numidents--;
        }

        // get keyword subjects
        Elements dcsubjects = metadata.select("dc|subject");

        for (Element subject : dcsubjects) {
            Subject dcsubject = new Subject(subject.text());
            subjects.add(dcsubject);
        }

        document.setSubjects(subjects);
        
        // get rights
        Elements rgs = metadata.select("dc|rights");

        for (Element e : rgs) {
        		Rights rg = new Rights(e.text());
          	rightslist.add(rg);
        }

        document.setRightsList(rightslist);
        
        // get source, relation, coverage

        // get language
        Elements langs = metadata.select("dc|language");

        for (Element e : langs) {
            String lang = e.text();
            document.setLanguage(lang);
        }

        // parse references; DOI is no dublin core item
        /*Elements referenceElements = metadata.select("DOI");

        for (Element doiRef : referenceElements) {
            relatedIdentifiers.add(new RelatedIdentifier(
                                       doiRef.text(),
                                       RelatedIdentifierType.DOI,
                                       RelationType.IsReferencedBy));
        }*/

        // compile a document
        document.setIdentifier(mainIdentifier);
        document.setWebLinks(links);

        // add dates if there are any
        if (!dates.isEmpty())
            document.setDates(dates);

        // add related identifiers if there are any
        if (!relatedIdentifiers.isEmpty())
            document.setRelatedIdentifiers(relatedIdentifiers);

        return document;
    }
}
