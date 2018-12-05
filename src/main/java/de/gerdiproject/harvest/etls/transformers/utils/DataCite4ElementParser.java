/*
 *  Copyright © 2018 Robin Weiss (http://www.gerdi-project.de/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package de.gerdiproject.harvest.etls.transformers.utils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.json.datacite.AlternateIdentifier;
import de.gerdiproject.json.datacite.Contributor;
import de.gerdiproject.json.datacite.Creator;
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
import de.gerdiproject.json.datacite.nested.AwardNumber;
import de.gerdiproject.json.datacite.nested.FunderIdentifier;
import de.gerdiproject.json.datacite.nested.NameIdentifier;
import de.gerdiproject.json.datacite.nested.PersonName;
import de.gerdiproject.json.geo.GeoJson;
import de.gerdiproject.json.geo.Point;
import de.gerdiproject.json.geo.Polygon;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This class offers static helper methods for parsing {@linkplain Element}s 
 * of DataCite 4.1 HTML records.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access=AccessLevel.PRIVATE)
public class DataCite4ElementParser
{
    /**
     * Retrieves the text of the first occurrence of a specified tag.
     *
     * @param ele the HTML element that is to be parsed
     * @param tagName the tag of which the text is to be retrieved
     *
     * @return the text inside the first occurrence of a specified tag,
     *          or null if the tag could not be found
     */
    public static String getString(Element ele, String tagName)
    {
        final Elements stringElements = ele.select(tagName);

        if (stringElements == null || stringElements.isEmpty())
            return null;

        final Element stringElement = stringElements.first();
        return stringElement == null ? null : stringElement.text();
    }


    /**
     * Retrieves the texts of all child tags of an {@linkplain Element}.
     *
     * @param ele the HTML {@linkplain Element} that contains the parent tag
     * @param tagName the name of the parent {@linkplain Element} of the child tags
     *
     * @return a {@linkplain List} of {@linkplain String}s
     *          or null if the tag could not be found
     */
    public static List<String> getStrings(Element ele, String tagName)
    {
        final Elements allElements = ele.select(tagName);

        if (allElements == null || allElements.isEmpty())
            return null;

        final Element parent = allElements.first();

        if (parent == null)
            return null;

        return elementsToStringList(parent.children());
    }


    /**
     * Retrieves the first occurrence of a specified tag and maps it to a specified class.
     *
     * @param ele the HTML {@linkplain Element} that contains the requested tag
     * @param tagName the name of the requested tag
     * @param eleToObject a mapping function that generates the requested class
     * @param <T> the requested type of the converted tag
     *
     * @return an object representation of the tag or null if it does not exist
     */
    public static <T> T getObject(Element ele, String tagName, Function<Element, T> eleToObject)
    {
        final Element requestedTag = ele.select(tagName).first();
        return requestedTag == null ? null : eleToObject.apply(requestedTag);
    }


    /**
     * Retrieves all child tags of a specified tag and maps them to a {@linkplain List} of a specified class.
     *
     * @param ele the HTML {@linkplain Element} that contains the parent tag
     * @param tagName the name of the parent tag
     * @param eleToObject a mapping function that maps a single child to the specified class
     * @param <T> the requested type of the converted tag
     *
     * @return a {@linkplain List} of objects of the tag or null if it does not exist
     */
    public static <T> List<T> getObjects(Element ele, String tagName, Function<Element, T> eleToObject)
    {
        final Element parent = ele.select(tagName).first();

        if (parent == null)
            return null;

        return elementsToList(parent.children(), eleToObject);
    }


    /**
     * Retrieves a {@linkplain Identifier} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Identifier}
     *
     * @return the {@linkplain Identifier} represented by the specified HTML element
     */
    public static Identifier parseIdentifier(Element ele)
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
    public static Creator parseCreator(Element ele)
    {
        final PersonName creatorName = parsePersonName(ele.selectFirst("creatorName"));
        final String givenName = getString(ele, "givenName");
        final String familyName = getString(ele, "familyName");
        final List<String> affiliations = elementsToStringList(ele.select("affiliation"));
        final List<NameIdentifier> nameIdentifiers = elementsToList(ele.select("nameIdentifier"), DataCite4ElementParser::parseNameIdentifier);

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
    public static Contributor parseContributor(Element ele)
    {
        final PersonName contributorName = parsePersonName(ele.selectFirst("contributorName"));
        final ContributorType contributorType = getEnumAttribute(ele, "contributorType", ContributorType.class);
        final String givenName = getString(ele, "givenName");
        final String familyName = getString(ele, "familyName");
        final List<String> affiliations = elementsToStringList(ele.select("affiliation"));
        final List<NameIdentifier> nameIdentifiers = elementsToList(ele.select("nameIdentifier"), DataCite4ElementParser::parseNameIdentifier);

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
    public static Title parseTitle(Element ele)
    {
        final String value = ele.text();
        final String language = getAttribute(ele, OaiPmhConstants.LANGUAGE_ATTRIBUTE);
        final TitleType titleType = getEnumAttribute(ele, "titleType", TitleType.class);

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
    public static ResourceType parseResourceType(Element ele)
    {
        final String value = ele.text();
        final ResourceTypeGeneral generalType = getEnumAttribute(ele, "resourceTypeGeneral", ResourceTypeGeneral.class);
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
    public static Description parseDescription(Element ele)
    {
        final String value = ele.text();
        final DescriptionType descriptionType = getEnumAttribute(ele, "descriptionType", DescriptionType.class);
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
    public static Subject parseSubject(Element ele)
    {
        final String value = ele.text();
        final String subjectScheme = getAttribute(ele, "subjectScheme");
        final String schemeURI = getAttribute(ele, "schemeURI");
        final String valueURI = getAttribute(ele, "valueURI");
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
    public static RelatedIdentifier parseRelatedIdentifier(Element ele)
    {
        final String value = ele.text();
        final RelatedIdentifierType relatedIdentifierType = getEnumAttribute(ele, "relatedIdentifierType", RelatedIdentifierType.class);
        final ResourceTypeGeneral resourceTypeGeneral = getEnumAttribute(ele, "resourceTypeGeneral", ResourceTypeGeneral.class);
        final RelationType relationType = getEnumAttribute(ele, "relationType", RelationType.class);
        final String relatedMetadataScheme = getAttribute(ele, "relatedMetadataScheme");
        final String schemeURI = getAttribute(ele, "schemeURI");
        final String schemeType = getAttribute(ele, "schemeType");

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
    public static AlternateIdentifier parseAlternateIdentifier(Element ele)
    {
        final String value = ele.text();
        final String alternateIdentifierType = getAttribute(ele, "alternateIdentifierType");

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
    public static Rights parseRights(Element ele)
    {
        final String value = ele.text();
        final String rightsURI = getAttribute(ele, "rightsURI");
        final String language = getAttribute(ele, OaiPmhConstants.LANGUAGE_ATTRIBUTE);

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
    public static AbstractDate parseDate(Element ele)
    {
        final String value = ele.text();
        final DateType dateType = getEnumAttribute(ele, "dateType", DateType.class);
        final String dateInformation = getAttribute(ele, "dateInformation");

        final AbstractDate date = value.contains(DataCiteDateConstants.DATE_RANGE_SPLITTER)
                                  ? new DateRange(value, dateType)
                                  : new Date(value, dateType);

        date.setDateInformation(dateInformation);
        return date;
    }


    /**
     * Retrieves a {@linkplain GeoLocation} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain GeoLocation}
     *
     * @return the {@linkplain GeoLocation} represented by the specified HTML element
     */
    public static GeoLocation parseGeoLocation(Element ele)
    {
        final String geoLocationPlace = getString(ele, "geoLocationPlace");
        final Point geoLocationPoint = getObject(ele, "geoLocationPoint", DataCite4ElementParser::parseGeoLocationPoint);
        final List<GeoJson> geoLocationPolygons = elementsToList(ele.select("geoLocationPolygon"), DataCite4ElementParser::parseGeoLocationPolygon);
        final double[] geoLocationBox = getObject(ele, "geoLocationBox", DataCite4ElementParser::parseGeoLocationBox);

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
     * Retrieves a {@linkplain FundingReference} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain FundingReference}
     *
     * @return the {@linkplain FundingReference} represented by the specified HTML element
     */
    public static FundingReference parseFundingReference(Element ele)
    {
        final String funderName = getString(ele, "funderName");
        final FunderIdentifier funderIdentifier = getObject(ele, "funderIdentifier", DataCite4ElementParser::parseFunderIdentifier);
        final AwardNumber awardNumber = getObject(ele, "awardNumber", DataCite4ElementParser::parseAwardNumber);
        final String awardTitle = getString(ele, "awardTitle");

        final FundingReference fundingReference = new FundingReference(funderName);
        fundingReference.setFunderIdentifier(funderIdentifier);
        fundingReference.setAwardNumber(awardNumber);
        fundingReference.setAwardTitle(awardTitle);

        return fundingReference;
    }


    /**
     * Retrieves a {@linkplain GeoJson} {@linkplain Polygon} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain GeoJson} {@linkplain Polygon}
     *
     * @return the {@linkplain GeoJson} {@linkplain Polygon} represented by the specified HTML element
     */
    private static GeoJson parseGeoLocationPolygon(Element ele)
    {
        List<Point> polygonPoints = elementsToList(ele.select("polygonPoint"), DataCite4ElementParser::parseGeoLocationPoint);

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
    private static Point parseGeoLocationPoint(Element ele)
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
    private static double[] parseGeoLocationBox(Element ele)
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
     * Retrieves a {@linkplain FunderIdentifier} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain FunderIdentifier}
     *
     * @return the {@linkplain FunderIdentifier} represented by the specified HTML element
     */
    private static FunderIdentifier parseFunderIdentifier(Element ele)
    {
        final String value = ele.text();
        final FunderIdentifierType funderIdentifierType = getEnumAttribute(ele, "funderIdentifierType", FunderIdentifierType.class);

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
    private static AwardNumber parseAwardNumber(Element ele)
    {
        final String value = ele.text();
        final String awardURI = getAttribute(ele, "awardURI");

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
    private static NameIdentifier parseNameIdentifier(Element ele)
    {
        final String value = ele.text();
        final String nameIdentifierScheme = getAttribute(ele, "nameIdentifierScheme");
        final String schemeURI = getAttribute(ele, "schemeURI");

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
    private static PersonName parsePersonName(Element ele)
    {
        final String name = ele.text();
        final NameType nameType = getEnumAttribute(ele, "nameType", NameType.class);
        return new PersonName(name, nameType);
    }


    /**
     * Retrieves the value of a HTML attribute.
     *
     * @param ele the HTML element that possibly has the attribute
     * @param attributeKey the key of the attribute
     *
     * @return the attribute value, or null if no such attribute exists
     */
    private static String getAttribute(Element ele, String attributeKey)
    {
        if (ele.hasAttr(attributeKey))
            return ele.attr(attributeKey);
        else
            return null;
    }


    /**
     * Retrieves the value of a HTML attribute and attempts to map it to an {@linkplain Enum}.
     *
     * @param ele the HTML element that possibly has the attribute
     * @param attributeKey the key of the attribute
     * @param enumClass the class to which the attribute value must be mapped
     * @param <T> the type of the {@linkplain Enum}
     *
     * @return the enum representation of the attribute value, or null if no such attribute exists or could not be mapped
     */
    private static <T extends Enum<T>> T getEnumAttribute(Element ele, String attributeKey, Class<T> enumClass)
    {
        T returnValue = null;

        try {
            if (ele.hasAttr(attributeKey))
                returnValue = Enum.valueOf(enumClass, ele.attr(attributeKey));
        } catch (IllegalArgumentException e) {
            returnValue = null;
        }

        return returnValue;
    }


    /**
     * Applies a mapping function to a {@linkplain Collection} of {@linkplain Element}s,
     * generating a {@linkplain List} of specified objects.
     *
     * @param elements the elements that are to be mapped
     * @param eleToObject the mapping function
     * @param <T> the type to which the elements are to be mapped
     *
     * @return a list of objects that were mapped or null if no object could be mapped
     */
    private static <T> List<T> elementsToList(Collection<Element> elements, Function<Element, T> eleToObject)
    {
        if (elements == null || elements.isEmpty())
            return null;

        final List<T> list = new LinkedList<>();

        for (Element ele : elements) {
            final T obj = eleToObject.apply(ele);

            if (obj != null)
                list.add(obj);
        }

        return list.isEmpty() ? null : list;
    }


    /**
     * Maps a {@linkplain Collection} of {@linkplain Element}s to a {@linkplain List} of {@linkplain String}s
     * by retrieving the text of the tag elements.
     *
     * @param elements the elements that are to be converted to strings
     *
     * @return a {@linkplain List} of {@linkplain String}s
     */
    private static List<String> elementsToStringList(Collection<Element> elements)
    {
        return elementsToList(elements, (Element ele) -> ele.text());
    }
}
