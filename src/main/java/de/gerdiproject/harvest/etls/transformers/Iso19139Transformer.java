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

import java.util.Arrays;
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
import de.gerdiproject.json.datacite.Subject;
import de.gerdiproject.json.datacite.Title;
import de.gerdiproject.json.datacite.abstr.AbstractDate;
import de.gerdiproject.json.datacite.enums.DateType;
import de.gerdiproject.json.datacite.enums.DescriptionType;
import de.gerdiproject.json.datacite.enums.ResourceTypeGeneral;
import de.gerdiproject.json.datacite.enums.TitleType;
import de.gerdiproject.json.datacite.extension.generic.ResearchData;
import de.gerdiproject.json.datacite.nested.Publisher;

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
 * @author Robin Weiss
 */
public class Iso19139Transformer extends AbstractOaiPmhRecordTransformer
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(Iso19139Transformer.class);


    @Override
    protected void setDocumentFieldsFromRecord(final DataCiteJson document, final Element record)
    {
        final Element metadata = getMetadata(record);

        final Title mainTitle = HtmlUtils.getObject(
                                    metadata,
                                    Iso19139Constants.TITLE,
                                    (final Element e) -> new Title(e.text()));

        document.addTitles(Arrays.asList(mainTitle));

        document.addCreators(HtmlUtils.getObjects(metadata, Iso19139Constants.PUBLISHER,
                                                  (final Element e) -> new Creator(e.text())));
        document.setPublisher(new Publisher(HtmlUtils.getString(metadata, Iso19139Constants.PUBLISHER)));
        document.addSubjects(HtmlUtils.getObjects(metadata, Iso19139Constants.KEYWORDS,
                                                  (final Element e) -> new Subject(e.text())));
        document.addDates(HtmlUtils.getObjects(metadata, Iso19139Constants.DATES, this::parseDate));
        document.setPublicationYear(parsePublicationYear(metadata, document.getDates()));
        document.setResourceType(HtmlUtils.getObject(metadata, Iso19139Constants.RESOURCE_TYPE,
                                                     (final Element e) -> new ResourceType(e.text(), ResourceTypeGeneral.Dataset)));
        document.addDescriptions(HtmlUtils.getObjects(metadata, Iso19139Constants.DESCRIPTIONS,
                                                      (final Element e) -> new Description(e.text(), DescriptionType.Abstract)));
        document.addTitles(HtmlUtils.getObjects(metadata, Iso19139Constants.ALTERNATE_TITLE,
                                                (final Element e) -> new Title(e.text(), TitleType.AlternativeTitle, null)));

        document.addGeoLocations(parseGeoLocations(metadata));
        document.addResearchData(parseResearchData(metadata, mainTitle));
        document.setLanguage(parseLanguage(metadata));
    }


    /**
     * Parses alternative titles from the ISO19139 metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of parsed titles
     */
    private List<Title> parseAlternateTitles(final Element metadata)
    {
        return HtmlUtils.getObjects(
                   metadata,
                   Iso19139Constants.ALTERNATE_TITLE,
                   (final Element e) -> new Title(e.text(), TitleType.AlternativeTitle, null));
    }


    /**
     * Parses the main- and alternative titles from the ISO19139 metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return a list of parsed titles
     */
    private List<GeoLocation> parseGeoLocations(final Element metadata)
    {
        final List<GeoLocation> geoLocations = new LinkedList<>();

        for (final Element dataSet : metadata.select(Iso19139Constants.DATA_IDENTIFICATION)) {
            final String geoDescription = HtmlUtils.getString(dataSet, Iso19139Constants.GEO_LOCATION_DESCRIPTION);
            final Element geoBox = dataSet.selectFirst(Iso19139Constants.GEO_LOCATION_BOX);

            // skip this metadata if it contains nothing related to geography
            if (geoDescription == null && geoBox == null)
                continue;

            // add metadata to the geo location
            final GeoLocation geo = new GeoLocation();
            geo.setPlace(geoDescription);
            parseGeoLocationBox(geoBox, geo);
            geoLocations.add(geo);
        }

        return geoLocations;
    }


    /**
     * Retrieves the language from ISO19139 metadata.
     *
     * @param metadata the metadata that is to be parsed
     *
     * @return the language code if such an element exists, or null
     */
    private String parseLanguage(final Element metadata)
    {
        final Element langElement = metadata.selectFirst(Iso19139Constants.LANGUAGE);

        return langElement == null
               ? null
               : HtmlUtils.getAttribute(langElement, Iso19139Constants.CODE_LIST_VALUE);
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
    private Integer parsePublicationYear(final Element metadata, final Collection<AbstractDate> dateList)
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

                } catch (final IllegalArgumentException e) {
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
    private AbstractDate parseDate(final Element isoDate)
    {
        final DateType dateType = Iso19139Constants.DATE_TYPE_MAP.get(
                                      isoDate.selectFirst(Iso19139Constants.DATE_TYPE).text());

        return dateType == null
               ? null
               : new Date(isoDate.selectFirst(Iso19139Constants.DATE).text(), dateType);
    }


    /**
     * Parses box coordinates for a specified {@linkplain GeoLocation} from an ISO19139 extent.
     *
     * @param isoBox the box coordinates element that is to be parsed
     */
    private void parseGeoLocationBox(final Element isoBox, final GeoLocation geoLocation)
    {
        if (isoBox == null)
            return;

        try {
            final double west = Double.parseDouble(HtmlUtils.getString(isoBox, Iso19139Constants.GEO_LOCATION_WEST));
            final double east = Double.parseDouble(HtmlUtils.getString(isoBox, Iso19139Constants.GEO_LOCATION_EAST));
            final double south = Double.parseDouble(HtmlUtils.getString(isoBox, Iso19139Constants.GEO_LOCATION_SOUTH));
            final double north = Double.parseDouble(HtmlUtils.getString(isoBox, Iso19139Constants.GEO_LOCATION_NORTH));

            // is it a point or a polygon?
            if (west == east && south == north)
                geoLocation.setPoint(west, south);
            else
                geoLocation.setBox(west, east, south, north);

        } catch (NullPointerException | NumberFormatException e) { // NOPMD NPE is highly unlikely and an edge case
            // do nothing
        }
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
    private List<ResearchData> parseResearchData(final Element metadata, final Title mainTitle)
    {
        final List<ResearchData> researchDataList = new LinkedList<>();

        if (mainTitle != null) {
            final String researchTitle = mainTitle.getValue();
            final String researchDataURL = HtmlUtils.getString(metadata, Iso19139Constants.RESEARCH_DATA);

            if (researchDataURL != null)
                researchDataList.add(new ResearchData(researchDataURL, researchTitle));
        }

        return researchDataList;
    }


    @Override
    public void clear()
    {
        // nothing to clean up
    }
}
