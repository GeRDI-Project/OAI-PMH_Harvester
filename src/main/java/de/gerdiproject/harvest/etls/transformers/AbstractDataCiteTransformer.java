/*
 *  Copyright © 2018 Robin Weiss (http://www.gerdi-project.de/)
 *
 *  Licensed under the Apache License, Version 2.0 (the DataCiteConstants.License);
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  DataCiteConstants.AS IS BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package de.gerdiproject.harvest.etls.transformers;

import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Element;

import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.etls.transformers.constants.DataCiteConstants;
import de.gerdiproject.json.datacite.AlternateIdentifier;
import de.gerdiproject.json.datacite.Contributor;
import de.gerdiproject.json.datacite.Creator;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Date;
import de.gerdiproject.json.datacite.DateRange;
import de.gerdiproject.json.datacite.Description;
import de.gerdiproject.json.datacite.FundingReference;
import de.gerdiproject.json.datacite.GeoLocation;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.RelatedIdentifier;
import de.gerdiproject.json.datacite.ResourceType;
import de.gerdiproject.json.datacite.Rights;
import de.gerdiproject.json.datacite.Subject;
import de.gerdiproject.json.datacite.Title;
import de.gerdiproject.json.datacite.abstr.AbstractDate;
import de.gerdiproject.json.datacite.constants.DataCiteDateConstants;
import de.gerdiproject.json.datacite.enums.ContributorType;
import de.gerdiproject.json.datacite.enums.DateType;
import de.gerdiproject.json.datacite.enums.DescriptionType;
import de.gerdiproject.json.datacite.enums.FunderIdentifierType;
import de.gerdiproject.json.datacite.enums.NameType;
import de.gerdiproject.json.datacite.enums.RelatedIdentifierType;
import de.gerdiproject.json.datacite.enums.RelationType;
import de.gerdiproject.json.datacite.enums.ResourceTypeGeneral;
import de.gerdiproject.json.datacite.enums.TitleType;
import de.gerdiproject.json.datacite.extension.generic.WebLink;
import de.gerdiproject.json.datacite.extension.generic.enums.WebLinkType;
import de.gerdiproject.json.datacite.nested.AwardNumber;
import de.gerdiproject.json.datacite.nested.FunderIdentifier;
import de.gerdiproject.json.datacite.nested.NameIdentifier;
import de.gerdiproject.json.datacite.nested.PersonName;
import de.gerdiproject.json.geo.GeoJson;
import de.gerdiproject.json.geo.Point;
import de.gerdiproject.json.geo.Polygon;

/**
 * This class offers methods for transforming DataCite records to {@linkplain DataCiteJson} objects.
 *
 * @author Robin Weiss
 */
public abstract class AbstractDataCiteTransformer extends AbstractOaiPmhRecordTransformer
{
    /**
     * Retrieves a {@linkplain Identifier} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Identifier}
     *
     * @return the {@linkplain Identifier} represented by the specified HTML element
     */
    protected Identifier parseIdentifier(Element ele)
    {
        final String value = ele.text();
        return new Identifier(value);
    }


    /**
     * Retrieves a {@linkplain Creator} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Creator}
     *
     * @return the {@linkplain Creator} represented by the specified HTML element
     */
    protected Creator parseCreator(Element ele)
    {
        final PersonName creatorName = parsePersonName(ele.selectFirst(DataCiteConstants.CREATOR_NAME));
        final String givenName = getString(ele, DataCiteConstants.GIVEN_NAME);
        final String familyName = getString(ele, DataCiteConstants.FAMILY_NAME);
        final List<String> affiliations = elementsToStringList(ele.select(DataCiteConstants.AFFILIATION));
        final List<NameIdentifier> nameIdentifiers = elementsToList(ele.select(DataCiteConstants.NAME_IDENTIFIER), this::parseNameIdentifier);

        final Creator creator = new Creator(creatorName);
        creator.setGivenName(givenName);
        creator.setFamilyName(familyName);
        creator.addAffiliations(affiliations);
        creator.addNameIdentifiers(nameIdentifiers);

        return creator;
    }


    /**
     * Retrieves a {@linkplain Contributor} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Contributor}
     *
     * @return the {@linkplain Contributor} represented by the specified HTML element
     */
    protected Contributor parseContributor(Element ele)
    {
        final PersonName contributorName = parsePersonName(ele.selectFirst(DataCiteConstants.CONTRIBUTOR_NAME));
        final ContributorType contributorType = getEnumAttribute(ele, DataCiteConstants.CONTRIBUTOR_TYPE, ContributorType.class);
        final String givenName = getString(ele, DataCiteConstants.GIVEN_NAME);
        final String familyName = getString(ele, DataCiteConstants.FAMILY_NAME);
        final List<String> affiliations = elementsToStringList(ele.select(DataCiteConstants.AFFILIATION));
        final List<NameIdentifier> nameIdentifiers = elementsToList(ele.select(DataCiteConstants.NAME_IDENTIFIER), this::parseNameIdentifier);

        final Contributor contributor = new Contributor(contributorName, contributorType);
        contributor.setGivenName(givenName);
        contributor.setFamilyName(familyName);
        contributor.addAffiliations(affiliations);
        contributor.addNameIdentifiers(nameIdentifiers);

        return contributor;
    }


    /**
     * Retrieves a {@linkplain Title} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Title}
     *
     * @return the {@linkplain Title} represented by the specified HTML element
     */
    protected Title parseTitle(Element ele)
    {
        final String value = ele.text();
        final String language = getAttribute(ele, OaiPmhConstants.LANGUAGE_ATTRIBUTE);
        final TitleType titleType = getEnumAttribute(ele, DataCiteConstants.TITLE_TYPE, TitleType.class);

        final Title title = new Title(value);
        title.setLang(language);
        title.setType(titleType);

        return title;
    }


    /**
     * Retrieves a {@linkplain ResourceType} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain ResourceType}
     *
     * @return the {@linkplain ResourceType} represented by the specified HTML element
     */
    protected ResourceType parseResourceType(Element ele)
    {
        final String value = ele.text();
        final ResourceTypeGeneral generalType = getEnumAttribute(ele, DataCiteConstants.RESOURCE_TYPE_GENERAL, ResourceTypeGeneral.class);
        final ResourceType resourceType = new ResourceType(value, generalType);
        return resourceType;
    }


    /**
     * Retrieves a {@linkplain Description} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Description}
     *
     * @return the {@linkplain Description} represented by the specified HTML element
     */
    protected Description parseDescription(Element ele)
    {
        final String value = ele.text();
        final DescriptionType descriptionType = getEnumAttribute(ele, DataCiteConstants.DESC_TYPE, DescriptionType.class);
        final String language = getAttribute(ele, OaiPmhConstants.LANGUAGE_ATTRIBUTE);

        final Description description = new Description(value, descriptionType);
        description.setLang(language);

        return description;
    }


    /**
     * Retrieves a {@linkplain Subject} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Subject}
     *
     * @return the {@linkplain Subject} represented by the specified HTML element
     */
    protected Subject parseSubject(Element ele)
    {
        final String value = ele.text();
        final String subjectScheme = getAttribute(ele, DataCiteConstants.SUBJECT_SCHEME);
        final String schemeURI = getAttribute(ele, DataCiteConstants.SCHEME_URI);
        final String valueURI = getAttribute(ele, DataCiteConstants.VALUE_URI);
        final String language = getAttribute(ele, OaiPmhConstants.LANGUAGE_ATTRIBUTE);

        final Subject subject = new Subject(value);
        subject.setSubjectScheme(subjectScheme);
        subject.setSchemeURI(schemeURI);
        subject.setValueURI(valueURI);
        subject.setLang(language);
        return subject;
    }


    /**
     * Retrieves a {@linkplain RelatedIdentifier} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain RelatedIdentifier}
     *
     * @return the {@linkplain RelatedIdentifier} represented by the specified HTML element
     */
    protected RelatedIdentifier parseRelatedIdentifier(Element ele)
    {
        final String value = ele.text();
        final RelatedIdentifierType relatedIdentifierType = getEnumAttribute(ele, DataCiteConstants.RELATED_IDENTIFIER_TYPE, RelatedIdentifierType.class);
        final ResourceTypeGeneral resourceTypeGeneral = getEnumAttribute(ele, DataCiteConstants.RESOURCE_TYPE_GENERAL, ResourceTypeGeneral.class);
        final RelationType relationType = getEnumAttribute(ele, DataCiteConstants.RELATION_TYPE, RelationType.class);
        final String relatedMetadataScheme = getAttribute(ele, DataCiteConstants.RELATED_METADATA_SCHEME);
        final String schemeURI = getAttribute(ele, DataCiteConstants.SCHEME_URI);
        final String schemeType = getAttribute(ele, DataCiteConstants.SCHEME_TYPE);

        final RelatedIdentifier relatedIdentifier = new RelatedIdentifier(value, relatedIdentifierType, relationType);
        relatedIdentifier.setResourceTypeGeneral(resourceTypeGeneral);
        relatedIdentifier.setRelatedMetadataScheme(relatedMetadataScheme);
        relatedIdentifier.setSchemeURI(schemeURI);
        relatedIdentifier.setSchemeType(schemeType);
        return relatedIdentifier;
    }


    /**
     * Retrieves an {@linkplain AlternateIdentifier} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain AlternateIdentifier}
     *
     * @return the {@linkplain AlternateIdentifier} represented by the specified HTML element
     */
    protected AlternateIdentifier parseAlternateIdentifier(Element ele)
    {
        final String value = ele.text();
        final String alternateIdentifierType = getAttribute(ele, DataCiteConstants.ALTERNATE_IDENTIFIER_TYPE);

        final AlternateIdentifier alternateIdentifier = new AlternateIdentifier(value, alternateIdentifierType);
        return alternateIdentifier;
    }


    /**
     * Retrieves a {@linkplain Rights} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Rights}
     *
     * @return the {@linkplain Rights} represented by the specified HTML element
     */
    protected Rights parseRights(Element ele)
    {
        final String value = ele.text();
        final String rightsURI = getAttribute(ele, DataCiteConstants.RIGHTS_URI);
        final String language = getAttribute(ele, DataCiteConstants.LANGUAGE);

        final Rights rights = new Rights(value);
        rights.setLang(language);
        rights.setUri(rightsURI);

        return rights;
    }


    /**
     * Retrieves an {@linkplain AbstractDate} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain AbstractDate}
     *
     * @return a {@linkplain Date} or {@linkplain DateRange} represented by the specified HTML element
     */
    protected AbstractDate parseDate(Element ele)
    {
        final String value = ele.text();
        final DateType dateType = getEnumAttribute(ele, DataCiteConstants.DATE_TYPE, DateType.class);
        final String dateInformation = getAttribute(ele, DataCiteConstants.DATE_INFORMATION);

        final AbstractDate date = value.contains(DataCiteDateConstants.DATE_RANGE_SPLITTER)
                                  ? new DateRange(value, dateType)
                                  : new Date(value, dateType);

        date.setDateInformation(dateInformation);
        return date;
    }


    /**
     * Retrieves a {@linkplain FundingReference} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain FundingReference}
     *
     * @return the {@linkplain FundingReference} represented by the specified HTML element
     */
    protected FundingReference parseFundingReference(Element ele)
    {
        final String funderName = getString(ele, DataCiteConstants.FUNDER_NAME);
        final FunderIdentifier funderIdentifier = getObject(ele, DataCiteConstants.FUNDER_IDENTIFIER, this::parseFunderIdentifier);
        final AwardNumber awardNumber = getObject(ele, DataCiteConstants.AWARD_NUMBER, this::parseAwardNumber);
        final String awardTitle = getString(ele, DataCiteConstants.AWARD_TITLE);

        final FundingReference fundingReference = new FundingReference(funderName);
        fundingReference.setFunderIdentifier(funderIdentifier);
        fundingReference.setAwardNumber(awardNumber);
        fundingReference.setAwardTitle(awardTitle);

        return fundingReference;
    }


    /**
     * Retrieves a {@linkplain GeoLocation} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain GeoLocation}
     *
     * @return the {@linkplain GeoLocation} represented by the specified HTML element
     */
    protected GeoLocation parseGeoLocation(Element ele)
    {
        final String geoLocationPlace = getString(ele, DataCiteConstants.GEOLOCATION_PLACE);
        final Point geoLocationPoint = getObject(ele, DataCiteConstants.GEOLOCATION_POINT, this::parseGeoLocationPoint);
        final List<GeoJson> geoLocationPolygons = elementsToList(ele.select(DataCiteConstants.GEOLOCATION_POLYGON), this::parseGeoLocationPolygon);
        final double[] geoLocationBox = getObject(ele, DataCiteConstants.GEOLOCATION_BOX, this::parseGeoLocationBox);

        final GeoLocation geoLocation = new GeoLocation();
        geoLocation.setPlace(geoLocationPlace);
        geoLocation.setPolygons(geoLocationPolygons);

        if (geoLocationPoint != null)
            geoLocation.setPoint(new GeoJson(geoLocationPoint));

        if (geoLocationBox != null)
            geoLocation.setBox(geoLocationBox[0], geoLocationBox[1], geoLocationBox[2], geoLocationBox[3]);

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
        List<Point> polygonPoints = elementsToList(ele.select(DataCiteConstants.POLYGON_POINT), this::parseGeoLocationPoint);

        final Polygon poly = new Polygon(polygonPoints);

        return new GeoJson(poly);
    }


    /**
     * Retrieves a {@linkplain Point} from the HTML representation of a GeoJson point.
     *
     * @param ele the HTML element that represents the {@linkplain Point}
     *
     * @return the {@linkplain Point}  represented by the specified HTML element
     */
    protected Point parseGeoLocationPoint(Element ele)
    {
        final String[] values = ele.text().split(" ");
        final double longitude = Double.parseDouble(values[0]);
        final double latitude = Double.parseDouble(values[1]);

        if (values.length == 3) {
            final double elevation = Double.parseDouble(values[2]);
            return new Point(longitude, latitude, elevation);
        } else
            return new Point(longitude, latitude);
    }


    /**
     * Retrieves a double array with four elements that represent the
     * west-bound longitude, east-bound longitude, south-bound latitude, and north-bound latitude
     * of a {@linkplain GeoJson} box.
     *
     * @param ele the HTML element that represents the GeoJson box
     *
     * @return a double array with four elements
     */
    protected double[] parseGeoLocationBox(Element ele)
    {
        final String[] values = ele.text().split(" ");
        final double[] boxParameters = new double[4];

        try {
            boxParameters[0] = Double.parseDouble(values[0]);
            boxParameters[1] = Double.parseDouble(values[1]);
            boxParameters[2] = Double.parseDouble(values[2]);
            boxParameters[3] = Double.parseDouble(values[3]);
        } catch (NumberFormatException e) {
            return null;
        }

        return boxParameters;
    }


    /**
     * Retrieves {@linkplain WebLink}s from DataCite identifiers.
     *
     * @param identifier the identifier of a {@linkplain DataCiteJson}
     * @param relatedIdentifiers related identifiers of a {@linkplain DataCiteJson}
     *
     * @return a list of {@linkplain WebLink}s
     */
    protected List<WebLink> createWebLinks(Identifier identifier, List<RelatedIdentifier> relatedIdentifiers)
    {
        final List<WebLink> webLinks = new LinkedList<>();

        // get related URLs
        if (relatedIdentifiers != null) {
            for (RelatedIdentifier ri : relatedIdentifiers) {
                final String relatedUrl;

                switch (ri.getType()) {
                    case DOI:
                        relatedUrl = ri.getValue().startsWith(DataCiteConstants.URL_PREFIX)
                                     ? ri.getValue()
                                     : String.format(OaiPmhConstants.DOI_URL, ri.getValue());
                        break;

                    case URL:
                        relatedUrl = ri.getValue();
                        break;

                    default:
                        relatedUrl = null;
                }

                if (relatedUrl != null) {
                    final WebLink relatedLink = new WebLink(relatedUrl);
                    relatedLink.setType(WebLinkType.Related);
                    relatedLink.setName(ri.getRelationType().toString());
                    webLinks.add(relatedLink);
                }
            }
        }

        // convert identifier to view url
        if (identifier != null) {
            final String identifierURL = identifier.getValue().startsWith(DataCiteConstants.URL_PREFIX)
                                         ? identifier.getValue()
                                         : String.format(OaiPmhConstants.DOI_URL, identifier.getValue());

            final WebLink viewLink = new WebLink(identifierURL);
            viewLink.setType(WebLinkType.ViewURL);
            viewLink.setName(DataCiteConstants.RESOURCE_LINK_NAME);
            webLinks.add(viewLink);
        }

        return webLinks;
    }


    /**
     * Attempts to parse the publication year from DataCite record metadata.
     *
     * @param metadata DataCite record metadata
     *
     * @return the publication year or null, if it does not exist
     */
    protected Integer parsePublicationYear(Element metadata)
    {
        try {
            final String publicationYear = getString(metadata, DataCiteConstants.PUBLICATION_YEAR);
            return Integer.parseInt(publicationYear);

        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
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
        final FunderIdentifierType funderIdentifierType = getEnumAttribute(ele, DataCiteConstants.FUNDER_IDENTIFIER_TYPE, FunderIdentifierType.class);

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
        final String awardURI = getAttribute(ele, DataCiteConstants.AWARD_URI);

        final AwardNumber awardNumber = new AwardNumber(value, awardURI);
        return awardNumber;
    }


    /**
     * Retrieves a {@linkplain NameIdentifier} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain NameIdentifier}
     *
     * @return the {@linkplain NameIdentifier} represented by the specified HTML element
     */
    protected NameIdentifier parseNameIdentifier(Element ele)
    {
        final String value = ele.text();
        final String nameIdentifierScheme = getAttribute(ele, DataCiteConstants.NAME_IDENTIFIER_SCHEME);
        final String schemeURI = getAttribute(ele, DataCiteConstants.SCHEME_URI);

        final NameIdentifier nameIdentifier = new NameIdentifier(value, nameIdentifierScheme);
        nameIdentifier.setSchemeURI(schemeURI);
        return nameIdentifier;
    }

    /**
     * Retrieves a {@linkplain PersonName} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain PersonName}
     *
     * @return the {@linkplain PersonName} represented by the specified HTML element
     */
    protected PersonName parsePersonName(Element ele)
    {
        final String name = ele.text();
        final NameType nameType = getEnumAttribute(ele, DataCiteConstants.NAME_TYPE, NameType.class);
        return new PersonName(name, nameType);
    }
}
