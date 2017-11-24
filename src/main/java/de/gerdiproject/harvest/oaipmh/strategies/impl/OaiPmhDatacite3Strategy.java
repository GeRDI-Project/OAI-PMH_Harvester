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
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.oaipmh.strategies.IStrategy;
//import de.gerdiproject.json.datacite.Contributor;
import de.gerdiproject.json.datacite.*;
import de.gerdiproject.json.datacite.Creator;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Date;
import de.gerdiproject.json.datacite.Description;
//import de.gerdiproject.json.datacite.GeoLocation;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.RelatedIdentifier;
import de.gerdiproject.json.datacite.ResourceType;
import de.gerdiproject.json.datacite.Subject;
import de.gerdiproject.json.datacite.Title;
import de.gerdiproject.json.datacite.abstr.AbstractDate;
import de.gerdiproject.json.datacite.enums.DateType;
import de.gerdiproject.json.datacite.enums.DescriptionType;
import de.gerdiproject.json.datacite.enums.IdentifierType;
import de.gerdiproject.json.datacite.enums.RelatedIdentifierType;
import de.gerdiproject.json.datacite.enums.RelationType;
import de.gerdiproject.json.datacite.enums.ResourceTypeGeneral;
//import de.gerdiproject.json.geo.GeoJson;

/**
 * @author Jan Frömberg, Robin Weiss
 *
 */
public class OaiPmhDatacite3Strategy implements IStrategy
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OaiPmhDatacite3Strategy.class);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy'-'MM'-'dd");

    @Override
    public IDocument harvestRecord(Element record)
    {
    		//Example: http://ws.pangaea.de/oai/provider?verb=GetRecord&metadataPrefix=datacite3&identifier=oai:pangaea.de:doi:10.1594/PANGAEA.52726
        DataCiteJson document = new DataCiteJson();
        
        // get header and meta data stuff for each record
        Elements children = record.children();
        Elements headers = children.select("header");
        Boolean deleted = children.first().attr("status").equals("deleted") ? true : false;
        LOGGER.info("Identifier deleted?: " + deleted.toString() + " (" + children.first().attr("status") + ")");
      
        Elements metadata = children.select("metadata");

        List<RelatedIdentifier> relatedIdentifiers = new LinkedList<>();
        List<AbstractDate> dates = new LinkedList<>();
        List<Title> titles = new LinkedList<>();
        List<Description> descriptions = new LinkedList<>();
        List<Subject> subjects = new LinkedList<>();
        //List<ResourceType> rtypes = new LinkedList<>();
        List<Creator> creators = new LinkedList<>();
        //List<Contributor> contributors = new LinkedList<>();
        List<String> formats = new LinkedList<>();
        List<Rights> docrights = new LinkedList<>();

        // get identifier and date stamp
        Element identifier = headers.select("identifier").first();
        //String identifier_handle = identifier.text().split(":")[2];
        //LOGGER.info("Identifier Handle (Header): " + identifier_handle);
        Identifier mainIdentifier = new Identifier(identifier.text());
        mainIdentifier.setType(IdentifierType.DOI);
        document.setIdentifier(mainIdentifier);
        
        // get last updated
        String recorddate = headers.select("datestamp").first().text();
        Date updatedDate = new Date(recorddate, DateType.Updated);
        dates.add(updatedDate);

        // check if Entry is "deleted"
        if (deleted) {
            document.setVersion("deleted");
            document.setIdentifier(mainIdentifier);

            // add dates if there are any
            if (!dates.isEmpty())
                document.setDates(dates);

            return document;
        }

        // get identifiers
        //Elements eidentifiers = metadata.select("identifier");
       
        // get creators
        Elements ecreators = metadata.select("creators");
        for (Element e : ecreators) {
        		Elements ccreator = e.children();
        		for (Element ec : ccreator) {
        			creators.add(new Creator(ec.select("creatorName").text()));
        		}
        }
        document.setCreators(creators);
        
        // get contributors
        
        // get titles
        Elements etitles = metadata.select("title");
        for (Element e : etitles) {
        		titles.add(new Title(e.text()));
        }
        document.setTitles(titles);
        
        // get publisher
        Elements epubs = metadata.select("publisher");
        for (Element e : epubs) {
        		document.setPublisher(e.text());
        }
        
        // get subjects
        Elements esubj = metadata.select("subject");
        for (Element e : esubj) {
        		String scheme = e.attr("subjectScheme");
        		Subject sub = new Subject(e.text());
        		if (!scheme.equals(""))
        			sub.setSubjectScheme(scheme);
        		subjects.add(sub);
        }
        document.setSubjects(subjects);
        
        // get dates
        Elements edates = metadata.select("date");
        for (Element e : edates) {
        		String datetype = e.attr("dateType");
        		Date edate;
        		switch (datetype) {
        			case "Updated" :	
        				edate = new Date(e.text(), DateType.Updated);
        				break;
        				
        			case "Collected" :
        				edate = new Date(e.text(), DateType.Collected);
        				break;
        				
        			case "Created" :
        				edate = new Date(e.text(), DateType.Created);
					break;
					
        			case "Submitted" :
        				edate = new Date(e.text(), DateType.Submitted);
					break;
					
        			default :
        				edate = new Date(e.text(), DateType.Collected);
        				break;
        		}
        		dates.add(edate);
        }
        
        // get language
        Elements elang = metadata.select("language");
        if (!elang.equals(null)) {
        		document.setLanguage(elang.first().text());
        }
        // get resourceType
        Elements erest = metadata.select("resourceType");
        if (!erest.equals(null)) {
        		ResourceType restype = new ResourceType(erest.first().text(), ResourceTypeGeneral.Dataset);
        		document.setResourceType(restype);
        }
        
        // get relatedIdentifiers
        Elements eidents = metadata.select("relatedIdentifiers");
        // TODO: create Typed method for that and return type? or better let all that stuff be done automatically
        for (Element e : eidents) {
        	    Elements erel = e.children();
        	    
        	    for (Element ei : erel) {
        	    		String relident = ei.text();
            		RelatedIdentifier rident = new RelatedIdentifier(relident, RelatedIdentifierType.Handle, RelationType.IsSupplementTo);
            		relatedIdentifiers.add(rident);
            		document.setRelatedIdentifiers(relatedIdentifiers);
        	    }
     
        }
        
        // get sizes
        Elements esize = metadata.select("size");
        if (!esize.equals(null)) {
        		document.setSizes(Arrays.asList(esize.first().text()));
        }
        
        // get formats
        Elements eformats = metadata.select("format");
        for (Element e : eformats) {
        	
        		Elements ef = e.children();
        		for (Element ei : ef) {
    	    			String temp = ei.text();
    	    			formats.add(temp);
        		}
        }
        document.setFormats(formats);
        
        // get rightsList
        Elements elements = metadata.select("rights");
        for (Element e : elements) {
        	
        		Elements ef = e.children();
        		for (Element ei : ef) {
    	    			String temp = ei.text();
    	    			Rights rights =  new Rights(temp);
    	    			rights.setURI(ei.attr("rightsURI"));
    	    			docrights.add(rights);
        		}
        }
        document.setRightsList(docrights);
        
        // get descriptions
        Elements edesc = metadata.select("descriptions");
        for (Element e : edesc) {
        	
        		Elements ef = e.children();
        		for (Element ei : ef) {
    	    			String temp = ei.text();
    	    			Description desc = new Description(temp, DescriptionType.Abstract);
    	    			descriptions.add(desc);
        		}
        }
        document.setDescriptions(descriptions);
        
        // get geoLocations
        //new GeoJson(metadata.select("geoLocationPoint").first().text())
        //document.setGeoLocations(Arrays.asList(new GeoLocation().setPoint()));
        
        // get publication year
        Calendar cal = Calendar.getInstance();
        Elements pubdates = metadata.select("publicationYear");
        String pubyear = pubdates.first().text();
        //TODO: nothing to parse? fix this: 0 is always the output
        try {
            cal.setTime(dateFormat.parse(pubyear));
           
            document.setPublicationYear((short) cal.get(Calendar.YEAR));

        } catch (ParseException e) { //NOPMD do nothing. just do not add the date if it does not exist
        }
        document.setDates(dates);
        
        return document;
    }

}
