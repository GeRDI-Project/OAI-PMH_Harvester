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

import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.etls.transformers.constants.Iso19139Constants;
import de.gerdiproject.harvest.utils.HtmlUtils;
import de.gerdiproject.json.datacite.Creator;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Date;
import de.gerdiproject.json.datacite.Description;
import de.gerdiproject.json.datacite.GeoLocation;
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
 * A transformer for the ISO 19139 metadata standard.<br>
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
 *
 * For a possible mapping from ISO19139 to these fields
 * see https://wiki.gerdi-project.de/x/8YDmAQ
 *
 * @author Tobias Weber
 */
public class Iso19139Transformer extends AbstractOaiPmhRecordTransformer
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(Iso19139Transformer.class);


    @Override
    protected void setDocumentFieldsFromRecord(DataCiteJson document, Element record)
    {
        final Element metadata = getMetadata(record);

        // creators (D2) : use publisher metadata
        document.addCreators(HtmlUtils.getObjectsFromParent(metadata, Iso19139Constants.PUBLISHER,
                                                            (Element e) -> new Creator(e.text())));

        // titles (D3)
        document.addTitles(HtmlUtils.getObjectsFromParent(metadata, Iso19139Constants.TITLE,
                                                          (Element e) -> new Title(e.text())));

        // publisher (D4)
        document.setPublisher(HtmlUtils.getString(metadata, Iso19139Constants.PUBLISHER));

        // dates (D8)
        document.addDates(HtmlUtils.getObjectsFromParent(metadata, Iso19139Constants.DATES, this::parseDate));

        // publication year (D5)
        document.setPublicationYear(parsePublicationYear(metadata, document.getDates()));

        // resource type (D10)
        document.setResourceType(HtmlUtils.getObject(metadata, Iso19139Constants.RESOURCE_TYPE,
                                                     (Element e) -> new ResourceType(e.text(), ResourceTypeGeneral.Dataset)));

        // descriptions (D17)
        document.addDescriptions(HtmlUtils.getObjectsFromParent(metadata, Iso19139Constants.DESCRIPTIONS,
                                                                (Element e) -> new Description(e.text(), DescriptionType.Abstract)));

        // geolocations (D18)
        document.addGeoLocations(HtmlUtils.getObjectsFromParent(metadata, Iso19139Constants.GEOLOCS, this::parseGeoLocation));

        // research data (E3)
        document.addResearchData(parseResearchData(metadata, document.getTitles()));
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
    private Integer parsePublicationYear(Element metadata, Collection<AbstractDate> dateList)
    {
        // first look for the publication year in already harvested dates
        Integer publicationYear = parsePublicationYearFromDates(dateList);

        // fallback: use the datestamp of the record
        if (publicationYear == null) {
            final Element datestamp = metadata.selectFirst(Iso19139Constants.DATESTAMP);

            if (datestamp != null) {
                try {
                    final Calendar cal = DatatypeConverter.parseDateTime(datestamp.text());
                    publicationYear = cal.get(Calendar.YEAR);

                } catch (IllegalArgumentException e) {
                    LOGGER.debug(Iso19139Constants.DATE_PARSING_FAILED, datestamp.text());
                }
            }
        }

        return publicationYear;
    }


    /**
     * Parses a date (D8) from an ISO19139 record date.
     *
     * @param isoDate the date element that is to be parsed
     *
     * @return a date
     */
    private AbstractDate parseDate(Element isoDate)
    {
        final DateType dateType = Iso19139Constants.DATE_TYPE_MAP.get(
                                      isoDate.select(Iso19139Constants.DATE_TYPE).text());

        return dateType != null
               ? new Date(isoDate.select(Iso19139Constants.DATE).text(), dateType)
               : null;
    }


    /**
     * Parses a geolocation (D18) from an ISO19139 record geolocation.
     *
     * @param isoGeoLocation the geolocation element that is to be parsed
     *
     * @return a geolocation
     */
    private GeoLocation parseGeoLocation(Element isoGeoLocation)
    {
        GeoLocation geoLocation;

        try {
            double west = Double.parseDouble(HtmlUtils.getString(isoGeoLocation, Iso19139Constants.GEOLOCS_WEST));
            double east = Double.parseDouble(HtmlUtils.getString(isoGeoLocation, Iso19139Constants.GEOLOCS_EAST));
            double south = Double.parseDouble(HtmlUtils.getString(isoGeoLocation, Iso19139Constants.GEOLOCS_SOUTH));
            double north = Double.parseDouble(HtmlUtils.getString(isoGeoLocation, Iso19139Constants.GEOLOCS_NORTH));
            geoLocation = new GeoLocation();

            // is it a point or a polygon?
            if (west == east && south == north)
                geoLocation.setPoint(new GeoJson(new Point(west, south)));
            else
                geoLocation.setBox(west, east, south, north);

        } catch (NullPointerException | NumberFormatException e) {
            geoLocation = null;
        }

        return geoLocation;
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
    private List<ResearchData> parseResearchData(Element metadata, Collection<Title> titleList)
    {
        final List<ResearchData> researchDataList = new LinkedList<>();

        if (titleList != null && !titleList.isEmpty()) {
            final String researchTitle = titleList.iterator().next().getValue();
            final String researchDataURL = HtmlUtils.getString(metadata, Iso19139Constants.RESEARCH_DATA);

            if (researchDataURL != null)
                researchDataList.add(new ResearchData(researchDataURL, researchTitle));
        }

        return researchDataList;
    }
}
