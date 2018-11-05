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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.etls.transformers.AbstractIteratorTransformer;
import de.gerdiproject.harvest.etls.transformers.TransformerException;
import de.gerdiproject.harvest.etls.transformers.constants.DataCiteConstants;
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
import de.gerdiproject.json.datacite.extension.WebLink;
import de.gerdiproject.json.datacite.extension.enums.WebLinkType;
import de.gerdiproject.json.datacite.nested.NameIdentifier;
import de.gerdiproject.json.geo.GeoJson;
import de.gerdiproject.json.geo.Point;

/**
 * A transformer for the Datacite3 metadata standard.<br>
 * https://schema.datacite.org/meta/kernel-3.0/doc/DataCite-MetadataKernel_v3.0.pdf
 *
 * @author Jan Frömberg
 */
public class Datacite3Transformer extends AbstractIteratorTransformer<Element, DataCiteJson>
{
    @Override
    protected DataCiteJson transformElement(Element record) throws TransformerException
    {
        Elements children = record.children();
        Boolean deleted = children.first().attr(DataCiteConstants.RECORD_STATUS).equals(
                              DataCiteConstants.RECORD_STATUS_DEL) ? true : false;

        // check if entry/record is "deleted" from repository
        if (deleted)
            return null;

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
        List<WebLink> links = new LinkedList<>();

        // get header and meta data for each record
        Elements header = children.select(DataCiteConstants.RECORD_HEADER);
        Elements metadata = children.select(DataCiteConstants.RECORD_METADATA);

        // get identifier and date stamp
        String identifier = header.select(DataCiteConstants.IDENTIFIER).first().text();

        final DataCiteJson document = new DataCiteJson(identifier);
        document.setRepositoryIdentifier(identifier);

        // get last updated
        String recorddate = header.select(DataCiteConstants.DATESTAMP).first().text();
        Date updatedDate = new Date(recorddate, DateType.Updated);
        dates.add(updatedDate);

        // get identifiers (normally one element/identifier)
        Identifier doiIdentifier = new Identifier(metadata.select(DataCiteConstants.IDENTIFIER).first().text());
        document.setIdentifier(doiIdentifier);

        // set URL of the article
        final String doiUrl = doiIdentifier.getValue().startsWith("http")
                              ? doiIdentifier.getValue()
                              : String.format(OaiPmhConstants.DOI_URL, doiIdentifier.getValue());

        WebLink viewLink = new WebLink(doiUrl);
        viewLink.setType(WebLinkType.ViewURL);
        viewLink.setName("View URL");
        links.add(viewLink);
        document.setWebLinks(links);

        // get creators
        Elements creatorElements = metadata.select(DataCiteConstants.DOC_CREATORS);

        for (Element e : creatorElements) {
            Elements ccreator = e.children();
            Creator creator;

            for (Element ec : ccreator) {
                creator = new Creator(ec.select(DataCiteConstants.DOC_CREATORNAME).text());
                Elements nameIds = ec.select(DataCiteConstants.DOC_CREATOR_NAMEIDENT);
                NameIdentifier nameIdent;

                for (Element enids : nameIds) {
                    nameIdent = new NameIdentifier(
                        enids.text(),
                        enids.attr(DataCiteConstants.DOC_CREATOR_NAMEIDENTSCHEME));
                    nameIdent.setSchemeURI(enids.attr(DataCiteConstants.DOC_CREATOR_NAMEIDENTSCHEMEURI));
                    nameIdentifiers.add(nameIdent);
                }

                if (!nameIdentifiers.isEmpty())
                    creator.setNameIdentifiers(nameIdentifiers);

                Elements ecaffils = ec.select(DataCiteConstants.DOC_CREATOR_AFFILIATION);

                for (Element eaffil : ecaffils)
                    affiliations.add(eaffil.text());

                if (!affiliations.isEmpty())
                    creator.setAffiliations(affiliations);

                creators.add(creator);
            }
        }

        document.setCreators(creators);

        // get titles
        Elements etitles = metadata.select(DataCiteConstants.DOC_TITLE);

        for (Element e : etitles)
            titles.add(new Title(e.text()));

        document.setTitles(titles);

        // get publisher
        Elements epubs = metadata.select(DataCiteConstants.PUBLISHER);

        for (Element e : epubs)
            document.setPublisher(e.text());

        // get publication year (a required field which is not always provided)
        Elements pubYears = metadata.select(DataCiteConstants.PUB_YEAR);

        for (Element year : pubYears) {

            try {
                short pubyear = Short.parseShort(year.text());
                document.setPublicationYear(pubyear);
            } catch (NumberFormatException e) {//NOPMD do nothing.
            }
        }

        // get subjects
        Elements esubj = metadata.select(DataCiteConstants.SUBJECT);

        for (Element e : esubj) {
            String scheme = e.attr(DataCiteConstants.SUBJECT_SCHEME);
            Subject sub = new Subject(e.text());

            if (!scheme.equals(""))
                sub.setSubjectScheme(scheme);

            subjects.add(sub);
        }

        document.setSubjects(subjects);

        // get contributors
        Elements contribs = metadata.select(DataCiteConstants.CONTRIBUTORS);
        Contributor contrib;

        for (Element ec : contribs) {

            Elements c = ec.children();

            for (Element ci : c) {

                String cType = ci.attr(DataCiteConstants.CONTRIB_TYPE);
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
        Elements edates = metadata.select(DataCiteConstants.METADATA_DATE);

        for (Element e : edates) {
            String datetype = e.attr(DataCiteConstants.METADATA_DATETYPE);
            Date edate = new Date(e.text(), DateType.valueOf(datetype));
            dates.add(edate);
        }

        document.setDates(dates);

        // get language
        Elements elang = metadata.select(DataCiteConstants.LANG);

        if (!elang.isEmpty())
            document.setLanguage(elang.first().text());

        // get resourceType
        Elements erest = metadata.select(DataCiteConstants.RES_TYPE);

        if (!erest.isEmpty()) {
            ResourceType restype = new ResourceType(
                erest.first().text(),
                ResourceTypeGeneral.valueOf(erest.attr(DataCiteConstants.RES_TYPE_GENERAL)));
            document.setResourceType(restype);
        }

        // get relatedIdentifiers
        Elements erelidents = metadata.select(DataCiteConstants.REL_IDENTIFIERS);

        for (Element e : erelidents) {
            Elements erel = e.children();

            for (Element ei : erel) {
                String itype = ei.attr(DataCiteConstants.REL_IDENT_TYPE);
                String reltype = ei.attr(DataCiteConstants.REL_TYPE);
                String relatedident = ei.text();
                RelatedIdentifier rident = new RelatedIdentifier(
                    relatedident,
                    RelatedIdentifierType.valueOf(itype),
                    RelationType.valueOf(reltype));
                relatedIdentifiers.add(rident);
            }
        }

        document.setRelatedIdentifiers(relatedIdentifiers);

        // get sizes
        Elements esize = metadata.select(DataCiteConstants.SIZE);

        if (!esize.isEmpty())
            document.setSizes(Arrays.asList(esize.first().text()));

        // get formats
        Elements eformats = metadata.select(DataCiteConstants.METADATA_FORMATS);

        for (Element e : eformats) {

            Elements ef = e.children();

            for (Element ei : ef) {
                String temp = ei.text();
                formats.add(temp);
            }
        }

        document.setFormats(formats);

        // get version (min occ. 0, type string)
        Elements versions = metadata.select(DataCiteConstants.VERSION);

        for (Element version : versions)
            document.setVersion(version.text());

        // get rightsList
        Elements elements = metadata.select(DataCiteConstants.RIGHTS_LIST);

        for (Element e : elements) {

            Elements ef = e.children();

            for (Element ei : ef) {
                String temp = ei.text();
                Rights rights = new Rights(temp);
                rights.setURI(ei.attr(DataCiteConstants.RIGHTS_URI));
                docrights.add(rights);
            }
        }

        document.setRightsList(docrights);

        // get descriptions
        Elements edesc = metadata.select(DataCiteConstants.DESCRIPTIONS);

        for (Element e : edesc) {

            Elements ef = e.children();

            for (Element ei : ef) {
                String tmp = ei.text();
                String desct = ei.attr(DataCiteConstants.DESC_TYPE);
                Description desc;

                try {
                    desc = new Description(tmp, DescriptionType.valueOf(desct));
                } catch (Exception e2) {
                    //LOGGER.info("Desc Type Error on ("+ desct +") : " + e2.toString());
                    desc = new Description(tmp, DescriptionType.Other);
                }

                descriptions.add(desc);
            }
        }

        document.setDescriptions(descriptions);

        // get geoLocations
        Elements egeolocs = metadata.select(DataCiteConstants.GEOLOCS);

        for (Element e : egeolocs) {

            Elements ec = e.children();

            //for each geoLocation
            for (Element ei : ec) {

                Elements eigeo = ei.children();

                //for each geobox, point, place ...
                for (Element gle : eigeo) {

                    String geoTag = gle.tagName().toLowerCase();
                    GeoLocation gl = new GeoLocation();
                    String[] temp;

                    switch (geoTag) {

                        case DataCiteConstants.GEOLOC_BOX:
                            temp = gle.text().split(" ");
                            double latitudeSouthWest = Double.parseDouble(temp[0]);
                            double longitudeSouthWest = Double.parseDouble(temp[1]);
                            double latitudeNorthEast = Double.parseDouble(temp[2]);
                            double longitudeNorthEast = Double.parseDouble(temp[3]);
                            gl.setBox(longitudeSouthWest, longitudeNorthEast, latitudeSouthWest, latitudeNorthEast);
                            geoLocations.add(gl);
                            break;

                        case DataCiteConstants.GEOLOC_POINT:
                            temp = gle.text().split(" ");
                            double latitude = Double.parseDouble(temp[0]);
                            double longitude = Double.parseDouble(temp[1]);

                            GeoJson geoPoint =
                                new GeoJson(new Point(longitude, latitude));
                            gl.setPoint(geoPoint);
                            geoLocations.add(gl);
                            break;

                        case DataCiteConstants.GEOLOC_PLACE:
                            gl.setPlace(gle.text());
                            geoLocations.add(gl);
                            break;

                        default:
                            break;
                    }

                }
            }
        }

        document.setGeoLocations(geoLocations);

        return document;
    }
}
