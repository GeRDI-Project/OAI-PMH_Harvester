/**
 * Copyright © 2018 Tobias Weber (http://www.gerdi-project.de)
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
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.etls.transformers.constants.DataCiteConstants;
import de.gerdiproject.harvest.etls.transformers.constants.Iso19139Constants;
import de.gerdiproject.json.datacite.Creator;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Date;
import de.gerdiproject.json.datacite.Description;
import de.gerdiproject.json.datacite.GeoLocation;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.ResourceType;
import de.gerdiproject.json.datacite.Title;
import de.gerdiproject.json.datacite.abstr.AbstractDate;
import de.gerdiproject.json.datacite.enums.DateType;
import de.gerdiproject.json.datacite.enums.DescriptionType;
import de.gerdiproject.json.datacite.enums.ResourceTypeGeneral;
import de.gerdiproject.json.datacite.extension.generic.ResearchData;
import de.gerdiproject.json.geo.GeoJson;
import de.gerdiproject.json.geo.Point;

/**
 * A harvesting strategy for the ISO 19139 metadata standard.<br>
 *  https://www.iso.org/standard/32557.html
 *
 * The following DataCite and GeRDI generic extension fields are currently
 * NOT implemented:
 * - D7     Contributor
 * - D9     Language
 * - D11    AlternateIdentifier
 * - D12    RelatedIdentifier
 * - D13    Size
 * - D14    Format
 * - D15    Version
 * - D16    Rights
 * - D19    FundingReference
 * - E1     WebLink
 * - E4     ResearchDiscipline
 * For a possible mapping from ISO19139 to these fields
 * see https://wiki.gerdi-project.de/x/8YDmAQ
 * 
 * @author Tobias Weber
 */
public class Iso19139Transformer extends AbstractIteratorTransformer<Element, DataCiteJson>
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(Iso19139Transformer.class);


    @Override
    public DataCiteJson transformElement(Element record)
    {
        final boolean isRecordDeleted = record.children()
                                        .first()
                                        .attr(Iso19139Constants.RECORD_STATUS)
                                        .equals(Iso19139Constants.RECORD_STATUS_DEL);

        // check if entry/record is "deleted" from repository
        if (isRecordDeleted)
            return null;

        // retrieve record header and metadata
        final Element header = record.selectFirst(DataCiteConstants.RECORD_HEADER);
        final Element metadata = record.selectFirst(Iso19139Constants.RECORD_METADATA);

        // create document
        final DataCiteJson document = new DataCiteJson(parseDocumentId(header));
        
        // parse metadata that is used by more than one document field
        final List<AbstractDate> dateList = parseDates(metadata);
        final List<Title> titleList = parseTitles(metadata);

        // DataCite metadata
        document.setIdentifier(parseIdentifier(metadata));
        document.addCreators(parseCreators(metadata));
        document.addTitles(titleList);
        document.setPublisher(parsePublisher(metadata));
        document.addDates(dateList);
        document.setPublicationYear(parsePublicationYear(metadata, dateList));
        document.setResourceType(parseResourceType(metadata));
        document.addDescriptions(parseDescriptions(metadata));
        document.addGeoLocations(parseGeoLocations(metadata));
        
        // GeRDI Extension metadata
        document.addResearchData(parseResearchData(metadata, titleList));

        return document;
    }
    
    
    /**
     * Parses metadata for an identifier (D1) from an ISO19139 metadata record.
     *
     * @param metadata the metadata that is to be parsed
     * @todo ISO19139 does not guarantee a DOI, but a "unique and persistent identifier",
     *       up to now these are URNs - the following call will set the identifierType to DOI
     *       nevertheless
     *
     * @return the string representation of the identifier
     */
    private Identifier parseIdentifier(Element metadata)
    {
        final Element identifier = metadata.selectFirst(Iso19139Constants.IDENTIFIER);
        return new Identifier(identifier.text());
    }
    

    /**
     * Parses metadata for creators (D2) from an ISO19139 metadata record.
     * 
     * This field is not easily mappable from ISO19139 to DataCite.
     * The same value as for the Publisher is used.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of parsed creators
     */
    private List<Creator> parseCreators(Element metadata)
    {
        final List<Creator> creatorList = new LinkedList<>();
        final Element creator = metadata.selectFirst(Iso19139Constants.PUBLISHER);

        if (creator != null)
            creatorList.add(new Creator(creator.text()));
        
        return creatorList;
    }
    
    
    /**
     * Parses metadata for titles (D3) from an ISO19139 metadata record.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of parsed titles
     */
    private List<Title> parseTitles(Element metadata)
    {
        final List<Title> titleList = new LinkedList<>();
        final Element title = metadata.selectFirst(Iso19139Constants.TITLE);

        if (title != null)
            titleList.add(new Title(title.text()));
        
        return titleList;
    }

    
    /**
     * Parses metadata for a publisher (D4) from an ISO19139 metadata record.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return the parsed publisher name or null, 
     *          if the corresponding metadata is missing
     */
    private String parsePublisher(Element metadata)
    {
        final Element publisher = metadata.selectFirst(Iso19139Constants.PUBLISHER);

        return publisher != null
                ?publisher.text()
                : null;
    }
    
    
    /**
     * Parses metadata for the publication year (D5) from an ISO19139 metadata record
     * and from already parsed dates.<br>
     * 
     * This field is not easily mappable from ISO19139 to DataCite.
     * In a first attempt, the already parsed dates are searched for an "Issued" date.
     * If no such date exists, the datestamp of the metadata record is used 
     * since it is the best approximation:
     * "The date which specifies when the metadata record was created or updated."
     *
     * @param metadata the metadata that is to be parsed
     * @param dateList a list of parsed DataCite dates
     *
     * @return the publication year or null, if it could not be parsed
     */
    private Integer parsePublicationYear(Element metadata, List<AbstractDate> dateList)
    {
        Integer publicationYear = null;
            
        // first look for the publication year in already harvested dates
        for (AbstractDate date : dateList) {
            if (date.getType() == DateType.Issued) {
                Calendar cal = DatatypeConverter.parseDateTime(date.getValue());
                publicationYear = cal.get(Calendar.YEAR);
                break;
            }
        }
        
        // fallback: use the datestamp of the record
        if(publicationYear == null) {
            final Element datestamp = metadata.selectFirst(Iso19139Constants.DATESTAMP);
    
            if (datestamp != null) {
                try {
                    final Calendar cal = DatatypeConverter.parseDateTime(datestamp.text());
                    publicationYear = cal.get(Calendar.YEAR);
                    
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Datestamp does not seem to be a date: {}",
                                metadata.select(Iso19139Constants.DATESTAMP).text());
                }
            }
        }
        
        return publicationYear;
    }
    

    /**
     * Parses metadata for dates (D8) from an ISO19139 metadata record.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of parsed dates
     */
    private List<AbstractDate> parseDates(Element metadata)
    {
        List<AbstractDate> dateList = new LinkedList<>();
        Elements isoDates = metadata.select(Iso19139Constants.DATES);

        for (Element isoDate : isoDates) {
            final DateType dateType = Iso19139Constants.DATE_TYPE_MAP.get(
                                          isoDate.select(Iso19139Constants.DATE_TYPE).text());

            if (dateType != null) {
                dateList.add(
                    new Date(isoDate.select(Iso19139Constants.DATE).text(), dateType));
            }
        }

        return dateList;
    }


    /**
     * Parses metadata for a resource type (D10) from an ISO19139 metadata record.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return the parsed resource type
     */
    private ResourceType parseResourceType(Element metadata)
    {
        final Element resourceType = metadata.selectFirst(Iso19139Constants.RESOURCE_TYPE);
    
        return resourceType != null
            ? new ResourceType(resourceType.text(), ResourceTypeGeneral.Dataset)
            : null;
    }
    
    
    /**
     * Parses metadata for descriptions (D17) from an ISO19139 metadata record.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of parsed descriptions
     */
    private  List<Description> parseDescriptions(Element metadata)
    {
        final List<Description> descriptionList = new LinkedList<>();
        final Element description = metadata.selectFirst(Iso19139Constants.DESCRIPTIONS);

        if (description != null)
            descriptionList.add(new Description(description.text(), DescriptionType.Abstract));
        
        return descriptionList;
    }


    /**
     * Parses metadata for geolocations (D18) from an ISO19139 metadata record.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of geolocations which are given as a bounding box
     */
    private List<GeoLocation> parseGeoLocations(Element metadata)
    {
        List<GeoLocation> geoLocationList = new LinkedList<>();
        Elements isoGeoLocations = metadata.select(Iso19139Constants.GEOLOCS);

        for (Element isoGeoLocation : isoGeoLocations) {
            GeoLocation geoLocation = new GeoLocation();
            double west, east, south, north;

            try {
                west = Double.parseDouble(
                           isoGeoLocation.select(Iso19139Constants.GEOLOCS_WEST).text());
                east = Double.parseDouble(
                           isoGeoLocation.select(Iso19139Constants.GEOLOCS_EAST).text());
                south = Double.parseDouble(
                            isoGeoLocation.select(Iso19139Constants.GEOLOCS_SOUTH).text());
                north = Double.parseDouble(
                            isoGeoLocation.select(Iso19139Constants.GEOLOCS_NORTH).text());
            } catch (NullPointerException | NumberFormatException e) {
                LOGGER.info("Ignoring geolocation {} for document {}, has no valid coordinations",
                            parseIdentifier(metadata).getValue(),
                            isoGeoLocation.text());
                continue;
            }

            //is it a point or a polygon?
            if (west == east && south == north)
                geoLocation.setPoint(new GeoJson(new Point(west, south)));
            else
                geoLocation.setBox(west, east, south, north);

            geoLocationList.add(geoLocation);
        }

        return geoLocationList;
    }
    

    /**
     * Parses metadata for research data (E3) from an ISO19139 metadata record
     * and from previously parsed titles.
     *
     * @param metadata the metadata that is to be parsed
     * @param titleList the already parsed titles of the record
     *
     * @return a list of parsed research data
     */
    private List<ResearchData> parseResearchData(Element metadata, List<Title> titleList)
    {
        final List<ResearchData> researchDataList = new LinkedList<>();
        
        if(!titleList.isEmpty()) {
            final String researchTitle = titleList.get(0).getValue();

            final Element researchDataURL
                = metadata.selectFirst(Iso19139Constants.RESEARCH_DATA);
    
            if (researchDataURL != null) {
                // check whether URL is - wait for it - valid
                try {
                    new URL(researchDataURL.text());
    
                    researchDataList.add(new ResearchData(researchDataURL.text(), researchTitle));
                } catch (MalformedURLException e) {
                    LOGGER.warn("URL {} is not valid, skipping", researchDataURL);
                }
            }
        }
        
        return researchDataList;
    }
    
    
    /**
     * Parses an identifier that uniquely identifies this document.
     * 
     * @param header the record header
     * 
     * @return a unique identifier of the document
     */
    private String parseDocumentId(Element header)
    {
        return header.selectFirst(DataCiteConstants.IDENTIFIER).text();
    }
}
