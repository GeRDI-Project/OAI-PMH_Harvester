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

import java.util.List;

import org.jsoup.nodes.Element;

import de.gerdiproject.harvest.etls.transformers.constants.DataCiteConstants;
import de.gerdiproject.harvest.etls.transformers.constants.HtmlUtils;
import de.gerdiproject.json.datacite.Contributor;
import de.gerdiproject.json.datacite.Creator;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.FundingReference;
import de.gerdiproject.json.datacite.GeoLocation;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.RelatedIdentifier;
import de.gerdiproject.json.datacite.Rights;
import de.gerdiproject.json.datacite.Subject;
import de.gerdiproject.json.datacite.abstr.AbstractDate;
import de.gerdiproject.json.datacite.enums.ContributorType;
import de.gerdiproject.json.datacite.enums.FunderIdentifierType;
import de.gerdiproject.json.datacite.enums.NameType;
import de.gerdiproject.json.datacite.enums.ResourceTypeGeneral;
import de.gerdiproject.json.datacite.nested.AwardNumber;
import de.gerdiproject.json.datacite.nested.FunderIdentifier;
import de.gerdiproject.json.datacite.nested.NameIdentifier;
import de.gerdiproject.json.datacite.nested.PersonName;
import de.gerdiproject.json.geo.GeoJson;
import de.gerdiproject.json.geo.Point;
import de.gerdiproject.json.geo.Polygon;

/**
 * A transformer for records of the Datacite 4.1 metadata standard.<br>
 * https://schema.datacite.org/meta/kernel-4.1/
 *
 * @author Robin Weiss
 */
public class DataCite4Transformer extends DataCite3Transformer
{
    @Override
    @SuppressWarnings("CPD-START") // we want to keep duplicates here, because there will be slight changes in other transformers
    protected void setDocumentFieldsFromRecord(DataCiteJson document, Element record)
    {
        final Element metadata = getMetadata(record);

        final Identifier identifier = HtmlUtils.getObject(metadata, DataCiteConstants.IDENTIFIER, this::parseIdentifier);
        document.setIdentifier(identifier);

        final List<RelatedIdentifier> relatedIdentifiers = HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.RELATED_IDENTIFIERS, this::parseRelatedIdentifier);
        document.addRelatedIdentifiers(relatedIdentifiers);

        document.setPublisher(HtmlUtils.getString(metadata, DataCiteConstants.PUBLISHER));
        document.setLanguage(HtmlUtils.getString(metadata, DataCiteConstants.LANGUAGE));
        document.setVersion(HtmlUtils.getString(metadata, DataCiteConstants.VERSION));
        document.setPublicationYear(parsePublicationYear(metadata));
        document.addSizes(HtmlUtils.getStringsFromParent(metadata, DataCiteConstants.SIZES));
        document.addFormats(HtmlUtils.getStringsFromParent(metadata, DataCiteConstants.FORMATS));
        document.setResourceType(HtmlUtils.getObject(metadata, DataCiteConstants.RESOURCE_TYPE, this::parseResourceType));
        document.addCreators(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.CREATORS, this::parseCreator));
        document.addContributors(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.CONTRIBUTORS, this::parseContributor));
        document.addTitles(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.TITLES, this::parseTitle));
        document.addDescriptions(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.DESCRIPTIONS, this::parseDescription));
        document.addSubjects(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.SUBJECTS, this::parseSubject));
        document.addAlternateIdentifiers(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.ALTERNATE_IDENTIFIERS, this::parseAlternateIdentifier));
        document.addRights(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.RIGHTS_LIST, this::parseRights));
        document.addDates(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.DATES, this::parseDate));
        document.addGeoLocations(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.GEO_LOCATIONS, this::parseGeoLocation));
        document.addFundingReferences(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.FUNDING_REFERENCES, this::parseFundingReference));
        document.addWebLinks(createWebLinks(identifier, relatedIdentifiers));
    }


    @Override
    protected GeoLocation parseGeoLocation(Element ele)
    {
        final GeoLocation geoLocation = super.parseGeoLocation(ele);

        // in DataCite 4, polygons were added to GeoLocations
        final List<GeoJson> geoLocationPolygons = HtmlUtils.elementsToList(ele.select(DataCiteConstants.GEOLOCATION_POLYGON), this::parseGeoLocationPolygon);
        geoLocation.setPolygons(geoLocationPolygons);

        return geoLocation;
    }


    /**
     * Retrieves a {@linkplain GeoJson} {@linkplain Polygon} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain GeoJson} {@linkplain Polygon}
     *
     * @return the {@linkplain GeoJson} {@linkplain Polygon} represented by the specified HTML element
     */
    protected GeoJson parseGeoLocationPolygon(Element ele)
    {
        final List<Point> polygonPoints = HtmlUtils.elementsToList(ele.select(DataCiteConstants.POLYGON_POINT), this::parseGeoLocationPoint);

        final Polygon poly = new Polygon(polygonPoints);

        return new GeoJson(poly);
    }


    @Override
    protected Point parseGeoLocationPoint(Element ele)
    {
        // in DataCite 4.0, longitude and latitude are swapped
        try {
            final double longitude = Double.parseDouble(ele.selectFirst(DataCiteConstants.POINT_LONG).text());
            final double latitude = Double.parseDouble(ele.selectFirst(DataCiteConstants.POINT_LAT).text());

            return new Point(longitude, latitude);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    @Override
    protected double[] parseGeoLocationBox(Element ele)
    {
        // in DataCite 4.0, the order of the box parameters changes
        try {
            final double[] boxParameters = new double[4];

            boxParameters[0] = Double.parseDouble(ele.selectFirst(DataCiteConstants.BOX_WEST_LONG).text());
            boxParameters[1] = Double.parseDouble(ele.selectFirst(DataCiteConstants.BOX_EAST_LONG).text());
            boxParameters[2] = Double.parseDouble(ele.selectFirst(DataCiteConstants.BOX_SOUTH_LAT).text());
            boxParameters[3] = Double.parseDouble(ele.selectFirst(DataCiteConstants.BOX_NORTH_LAT).text());

            return boxParameters;
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }


    /**
     * Retrieves a {@linkplain FundingReference} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain FundingReference}
     *
     * @return the {@linkplain FundingReference} represented by the specified HTML element
     */
    @Override
    protected FundingReference parseFundingReference(Element ele)
    {
        // in DataCite 4.0, there are dedicated FundingReferences instead of funder-Contributors
        final String funderName = HtmlUtils.getString(ele, DataCiteConstants.FUNDER_NAME);
        final FunderIdentifier funderIdentifier = HtmlUtils.getObject(ele, DataCiteConstants.FUNDER_IDENTIFIER, this::parseFunderIdentifier);
        final AwardNumber awardNumber = HtmlUtils.getObject(ele, DataCiteConstants.AWARD_NUMBER, this::parseAwardNumber);
        final String awardTitle = HtmlUtils.getString(ele, DataCiteConstants.AWARD_TITLE);

        final FundingReference fundingReference = new FundingReference(funderName);
        fundingReference.setFunderIdentifier(funderIdentifier);
        fundingReference.setAwardNumber(awardNumber);
        fundingReference.setAwardTitle(awardTitle);

        return fundingReference;
    }


    /**
     * Retrieves a {@linkplain FunderIdentifier} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain FunderIdentifier}
     *
     * @return the {@linkplain FunderIdentifier} represented by the specified HTML element
     */
    protected FunderIdentifier parseFunderIdentifier(Element ele)
    {
        final String value = ele.text();
        final FunderIdentifierType funderIdentifierType = HtmlUtils.getEnumAttribute(ele, DataCiteConstants.FUNDER_IDENTIFIER_TYPE, FunderIdentifierType.class);

        final FunderIdentifier funderIdentifier = new FunderIdentifier(value, funderIdentifierType);
        return funderIdentifier;
    }


    /**
     * Retrieves a {@linkplain AwardNumber} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain AwardNumber}
     *
     * @return the {@linkplain AwardNumber} represented by the specified HTML element
     */
    protected AwardNumber parseAwardNumber(Element ele)
    {
        final String value = ele.text();
        final String awardURI = HtmlUtils.getAttribute(ele, DataCiteConstants.AWARD_URI);

        final AwardNumber awardNumber = new AwardNumber(value, awardURI);
        return awardNumber;
    }


    @Override
    protected Contributor parseContributor(Element ele)
    {
        // in DataCite 4.0, there are no "funder" Contributors,
        // so there is no need to check for them anymore

        final PersonName contributorName = parsePersonName(ele.selectFirst(DataCiteConstants.CONTRIBUTOR_NAME));
        final ContributorType contributorType = HtmlUtils.getEnumAttribute(ele, DataCiteConstants.CONTRIBUTOR_TYPE, ContributorType.class);
        final List<NameIdentifier> nameIdentifiers = HtmlUtils.elementsToList(ele.select(DataCiteConstants.NAME_IDENTIFIER), this::parseNameIdentifier);
        final List<String> affiliations = HtmlUtils.elementsToStringList(ele.select(DataCiteConstants.AFFILIATION));

        final Contributor contributor = new Contributor(contributorName, contributorType);
        contributor.addNameIdentifiers(nameIdentifiers);
        contributor.addAffiliations(affiliations);

        // in DataCite 4.0, givenName and familyName are added
        final String givenName = HtmlUtils.getString(ele, DataCiteConstants.GIVEN_NAME);
        final String familyName = HtmlUtils.getString(ele, DataCiteConstants.FAMILY_NAME);
        contributor.setGivenName(givenName);
        contributor.setFamilyName(familyName);

        return contributor;
    }


    @Override
    protected Creator parseCreator(Element ele)
    {
        final Creator creator = super.parseCreator(ele);

        // in DataCite 4.0, givenName and familyName are added
        final String givenName = HtmlUtils.getString(ele, DataCiteConstants.GIVEN_NAME);
        final String familyName = HtmlUtils.getString(ele, DataCiteConstants.FAMILY_NAME);
        creator.setGivenName(givenName);
        creator.setFamilyName(familyName);

        return creator;
    }


    @Override
    protected Subject parseSubject(Element ele)
    {
        final Subject subject = super.parseSubject(ele);

        // in DataCite 4.0, valueURI is added
        final String valueURI = HtmlUtils.getAttribute(ele, DataCiteConstants.VALUE_URI);
        subject.setValueURI(valueURI);

        return subject;
    }


    @Override
    protected AbstractDate parseDate(Element ele)
    {
        final AbstractDate date = super.parseDate(ele);

        // in DataCite 4.0, dateinformation is added
        final String dateInformation = HtmlUtils.getAttribute(ele, DataCiteConstants.DATE_INFORMATION);
        date.setDateInformation(dateInformation);

        return date;
    }


    @Override
    protected RelatedIdentifier parseRelatedIdentifier(Element ele)
    {
        final RelatedIdentifier relatedIdentifier = super.parseRelatedIdentifier(ele);

        // in DataCite 4.1, resourceTypeGeneral is added
        final ResourceTypeGeneral resourceTypeGeneral = parseResourceTypeGeneral(ele);
        relatedIdentifier.setResourceTypeGeneral(resourceTypeGeneral);

        return relatedIdentifier;
    }


    @Override
    protected PersonName parsePersonName(Element ele)
    {
        final String name = ele.text();

        // in DataCite 4.1, nameType is added
        final NameType nameType = HtmlUtils.getEnumAttribute(ele, DataCiteConstants.NAME_TYPE, NameType.class);

        return new PersonName(name, nameType);
    }


    @Override
    protected Rights parseRights(Element ele)
    {
        final Rights rights = super.parseRights(ele);

        // in DataCite 4.1, language is added
        final String language = HtmlUtils.getAttribute(ele, DataCiteConstants.LANGUAGE);
        rights.setLang(language);

        return rights;
    }
}
