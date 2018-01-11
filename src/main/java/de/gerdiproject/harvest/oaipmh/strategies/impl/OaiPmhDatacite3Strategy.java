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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.oaipmh.constants.DataCiteStrategyConstants;
import de.gerdiproject.harvest.oaipmh.strategies.IStrategy;
import de.gerdiproject.json.datacite.Contributor;
import de.gerdiproject.json.datacite.Creator;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Date;
import de.gerdiproject.json.datacite.Description;
import de.gerdiproject.json.datacite.GeoLocation;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.RelatedIdentifier;
import de.gerdiproject.json.datacite.ResourceType;
import de.gerdiproject.json.datacite.Rights;
import de.gerdiproject.json.datacite.Subject;
import de.gerdiproject.json.datacite.Title;
import de.gerdiproject.json.datacite.abstr.AbstractDate;
import de.gerdiproject.json.datacite.enums.ContributorType;
import de.gerdiproject.json.datacite.enums.DateType;
import de.gerdiproject.json.datacite.enums.DescriptionType;
import de.gerdiproject.json.datacite.enums.RelatedIdentifierType;
import de.gerdiproject.json.datacite.enums.RelationType;
import de.gerdiproject.json.datacite.enums.ResourceTypeGeneral;
import de.gerdiproject.json.datacite.nested.NameIdentifier;
import de.gerdiproject.json.geo.GeoJson;
import de.gerdiproject.json.geo.Point;

/**
 * A harvesting strategy for the Datacite3 metadata standard.
 *
 * @author Jan Frömberg
 *
 */
public class OaiPmhDatacite3Strategy implements IStrategy
{
    @Override
    public IDocument harvestRecord(Element record)
    {
        DataCiteJson document = new DataCiteJson();

        List<RelatedIdentifier> relatedIdentifiers = new LinkedList<>();
        List<AbstractDate> dates = new LinkedList<>();
        List<Title> titles = new LinkedList<>();
        List<Description> descriptions = new LinkedList<>();
        List<Subject> subjects = new LinkedList<>();
        List<Creator> creators = new LinkedList<>();
        List<String> formats = new LinkedList<>();
        List<Rights> docrights = new LinkedList<>();
        List<GeoLocation> geoLocations = new LinkedList<>();
        List<Contributor> contributors = new LinkedList<>();
        List<NameIdentifier> nameIdentifiers = new LinkedList<>();
        List<String> affiliations = new LinkedList<>();

        // get header and meta data stuff for each record
        Elements children = record.children();

        Boolean deleted = children.first().attr(DataCiteStrategyConstants.RECORD_STATUS).equals(DataCiteStrategyConstants.RECORD_STATUS_DEL) ? true : false;

        Elements header = children.select(DataCiteStrategyConstants.RECORD_HEADER);
        Elements metadata = children.select(DataCiteStrategyConstants.RECORD_METADATA);

        // get identifier and date stamp
        String identifier = header.select(DataCiteStrategyConstants.IDENTIFIER).first().text();
        document.setRepositoryIdentifier(identifier);

        // get last updated
        String recorddate = header.select(DataCiteStrategyConstants.DATESTAMP).first().text();
        Date updatedDate = new Date(recorddate, DateType.Updated);
        dates.add(updatedDate);

        // check if entry/record is "deleted" from repository
        // stop crawling and create empty doc; maybe left out?
        if (deleted) {
            document.setVersion(DataCiteStrategyConstants.RECORD_STATUS_DEL);

            // add dates if there are any
            if (!dates.isEmpty())
                document.setDates(dates);

            return document;
        }

        // based DataCite3 schema -> http://schema.datacite.org/meta/kernel-3.0/metadata.xsd

        // get identifiers (normally one element/identifier)
        Element docident = metadata.select(DataCiteStrategyConstants.IDENTIFIER).first();
        Identifier i = new Identifier(docident.text());
        document.setIdentifier(i);
 
        // get creators
        Elements ecreators = metadata.select(DataCiteStrategyConstants.DOC_CREATORS);

        for (Element e : ecreators) {
            Elements ccreator = e.children();
            Creator creator;

            for (Element ec : ccreator) {
                creator = new Creator(ec.select(DataCiteStrategyConstants.DOC_CREATORNAME).text());
                Elements nameIds = ec.select(DataCiteStrategyConstants.DOC_CREATOR_NAMEIDENT);
                NameIdentifier nameIdent;

                for (Element enids : nameIds) {
                    nameIdent = new NameIdentifier(enids.text(), enids.attr(DataCiteStrategyConstants.DOC_CREATOR_NAMEIDENTSCHEME));
                    nameIdent.setSchemeURI(enids.attr(DataCiteStrategyConstants.DOC_CREATOR_NAMEIDENTSCHEMEURI));
                    nameIdentifiers.add(nameIdent);
                }

                if (!nameIdentifiers.isEmpty())
                    creator.setNameIdentifiers(nameIdentifiers);

                Elements ecaffils = ec.select(DataCiteStrategyConstants.DOC_CREATOR_AFFILIATION);

                for (Element eaffil : ecaffils)
                    affiliations.add(eaffil.text());

                if (!affiliations.isEmpty())
                    creator.setAffiliations(affiliations);

                creators.add(creator);
            }
        }

        document.setCreators(creators);

        // get titles
        Elements etitles = metadata.select(DataCiteStrategyConstants.DOC_TITLE);

        for (Element e : etitles)
            titles.add(new Title(e.text()));

        document.setTitles(titles);

        // get publisher
        Elements epubs = metadata.select(DataCiteStrategyConstants.PUBLISHER);

        for (Element e : epubs)
            document.setPublisher(e.text());

        // get publication year (a required field which is not always provided)
        Elements pubYears = metadata.select(DataCiteStrategyConstants.PUB_YEAR);

        for (Element year : pubYears) {

            try {
                short pubyear = Short.parseShort(year.text());
                document.setPublicationYear(pubyear);
            } catch (NumberFormatException e) {//NOPMD do nothing.
            }
        }

        // get subjects
        Elements esubj = metadata.select(DataCiteStrategyConstants.SUBJECT);

        for (Element e : esubj) {
            String scheme = e.attr(DataCiteStrategyConstants.SUBJECT_SCHEME);
            Subject sub = new Subject(e.text());

            if (!scheme.equals(""))
                sub.setSubjectScheme(scheme);

            subjects.add(sub);
        }

        document.setSubjects(subjects);

        // get contributors
        Elements contribs = metadata.select(DataCiteStrategyConstants.CONTRIBUTORS);
        Contributor contrib;

        for (Element ec : contribs) {

            Elements c = ec.children();

            for (Element ci : c) {

                String cType = ci.attr(DataCiteStrategyConstants.CONTRIB_TYPE);
                //LOGGER.info("ContibutorsType: " + cType);
                Elements cns = ci.children();

                for (Element cn : cns) {
                    String cname = cn.text();
                    contrib = new Contributor(cname, ContributorType.valueOf(cType));
                    contributors.add(contrib);
                }
            }
        }

        document.setContributors(contributors);
        
        // get dates
        Elements edates = metadata.select(DataCiteStrategyConstants.METADATA_DATE);

        for (Element e : edates) {
            String datetype = e.attr(DataCiteStrategyConstants.METADATA_DATETYPE);
            Date edate = new Date(e.text(), DateType.valueOf(datetype));
            dates.add(edate);
        }

        document.setDates(dates);

        // get language
        Elements elang = metadata.select(DataCiteStrategyConstants.LANG);

        if (!elang.isEmpty())
            document.setLanguage(elang.first().text());

        // get resourceType
        Elements erest = metadata.select(DataCiteStrategyConstants.RES_TYPE);

        if (!erest.isEmpty()) {
            ResourceType restype = new ResourceType(erest.first().text(), ResourceTypeGeneral.valueOf(erest.attr(DataCiteStrategyConstants.RES_TYPE_GENERAL)));
            document.setResourceType(restype);
        }

        // get relatedIdentifiers
        Elements erelidents = metadata.select(DataCiteStrategyConstants.REL_IDENTIFIERS);

        for (Element e : erelidents) {
            Elements erel = e.children();

            for (Element ei : erel) {
                String itype = ei.attr(DataCiteStrategyConstants.REL_IDENT_TYPE);
                String reltype = ei.attr(DataCiteStrategyConstants.REL_TYPE);
                String relatedident = ei.text();
                RelatedIdentifier rident = new RelatedIdentifier(relatedident, RelatedIdentifierType.valueOf(itype), RelationType.valueOf(reltype));
                relatedIdentifiers.add(rident);
            }
        }

        document.setRelatedIdentifiers(relatedIdentifiers);

        // get sizes
        Elements esize = metadata.select(DataCiteStrategyConstants.SIZE);

        if (!esize.isEmpty())
            document.setSizes(Arrays.asList(esize.first().text()));

        // get formats
        Elements eformats = metadata.select(DataCiteStrategyConstants.METADATA_FORMATS);

        for (Element e : eformats) {

            Elements ef = e.children();

            for (Element ei : ef) {
                String temp = ei.text();
                formats.add(temp);
            }
        }

        document.setFormats(formats);
        
        // get version (min occ. 0, type string)
        Elements versions = metadata.select(DataCiteStrategyConstants.VERSION);
        for (Element version : versions)
        	    document.setVersion(version.text());

        // get rightsList
        Elements elements = metadata.select(DataCiteStrategyConstants.RIGHTS_LIST);

        for (Element e : elements) {

            Elements ef = e.children();

            for (Element ei : ef) {
                String temp = ei.text();
                Rights rights =  new Rights(temp);
                rights.setURI(ei.attr(DataCiteStrategyConstants.RIGHTS_URI));
                docrights.add(rights);
            }
        }

        document.setRightsList(docrights);

        // get descriptions
        Elements edesc = metadata.select(DataCiteStrategyConstants.DESCRIPTIONS);

        for (Element e : edesc) {

            Elements ef = e.children();

            for (Element ei : ef) {
                String tmp = ei.text();
                String desct = ei.attr(DataCiteStrategyConstants.DESC_TYPE);
                Description desc = new Description(tmp, DescriptionType.valueOf(desct));
                descriptions.add(desc);
            }
        }

        document.setDescriptions(descriptions);

        // get geoLocations
        Elements egeolocs = metadata.select(DataCiteStrategyConstants.GEOLOCS);

        for (Element e : egeolocs) {

            Elements ec = e.children();

            //for each geoLocation
            for (Element ei : ec) {

                Elements eigeo = ei.children();

                //for each geobox, point ...
                for (Element gle : eigeo) {

                    String geoTag = gle.tagName().toLowerCase();
                    GeoLocation gl = new GeoLocation();
                    String[] temp;

                    switch (geoTag) {

                        case DataCiteStrategyConstants.GEOLOC_BOX:
                            temp = gle.text().split(" ");
                            gl.setBox(Double.parseDouble(temp[0]),
                                      Double.parseDouble(temp[1]),
                                      Double.parseDouble(temp[2]),
                                      Double.parseDouble(temp[3]));
                            geoLocations.add(gl);
                            break;

                        case DataCiteStrategyConstants.GEOLOC_POINT:
                            temp = gle.text().split(" ");
                            GeoJson geoPoint = new GeoJson(new Point(Double.parseDouble(temp[0]), Double.parseDouble(temp[1])));
                            gl.setPoint(geoPoint);
                            geoLocations.add(gl);
                            break;

                        case DataCiteStrategyConstants.GEOLOC_PLACE:
                            gl.setPlace(gle.text());
                            geoLocations.add(gl);
                            break;

                        default :
                            break;
                    }

                }
            }
        }

        document.setGeoLocations(geoLocations);

        return document;
    }
}