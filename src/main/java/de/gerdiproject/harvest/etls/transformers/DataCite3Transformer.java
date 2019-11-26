/**
 * Copyright © 2018 Robin Weiss (http://www.gerdi-project.de)
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.etls.transformers.constants.DataCiteConstants;
import de.gerdiproject.harvest.utils.HtmlUtils;
import de.gerdiproject.json.DateUtils;
import de.gerdiproject.json.datacite.Contributor;
import de.gerdiproject.json.datacite.Creator;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Description;
import de.gerdiproject.json.datacite.GeoLocation;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.RelatedIdentifier;
import de.gerdiproject.json.datacite.Rights;
import de.gerdiproject.json.datacite.Subject;
import de.gerdiproject.json.datacite.Title;
import de.gerdiproject.json.datacite.abstr.AbstractDate;
import de.gerdiproject.json.datacite.enums.DateType;
import de.gerdiproject.json.datacite.enums.ResourceTypeGeneral;
import de.gerdiproject.json.datacite.nested.Affiliation;
import de.gerdiproject.json.datacite.nested.NameIdentifier;

/**
 * A transformer for the DataCite 3 metadata standard.<br>
 * https://schema.datacite.org/meta/kernel-3.0/doc/DataCite-MetadataKernel_v3.0.pdf
 *
 * @author Robin Weiss
 */
public class DataCite3Transformer extends DataCite2Transformer
{
    protected final GeometryFactory geometryFactory = new GeometryFactory();

    @Override
    @SuppressWarnings("CPD-START") // we want to keep duplicates here, because there will be slight changes in other transformers
    protected void setDocumentFieldsFromRecord(final DataCiteJson document, final Element record)
    {
        final Element metadata = getMetadata(record);

        // overwrite the identifier parsed from the header
        final Identifier identifier = HtmlUtils.getObject(metadata, DataCiteConstants.IDENTIFIER, this::parseIdentifier);

        if (identifier != null)
            document.setIdentifier(identifier);

        // parse and cache related identifiers, they are reused in weblink creation
        final List<RelatedIdentifier> relatedIdentifiers =
            HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.RELATED_IDENTIFIERS, this::parseRelatedIdentifier);
        document.addRelatedIdentifiers(relatedIdentifiers);

        document.setPublisher(parsePublisher(metadata));
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
        document.addDates(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.DATES, this::parseDate));
        document.addGeoLocations(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.GEO_LOCATIONS, this::parseGeoLocation));
        document.addWebLinks(createWebLinks(identifier, relatedIdentifiers));

        // to be compliant to DC 4.1, convert contributors with type "funder" to fundingReferences
        document.addFundingReferences(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.CONTRIBUTORS, this::parseFundingReference));

        // in DataCite 3.0, rights is a reapeatable Element of rightsList
        document.addRights(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.RIGHTS_LIST, this::parseRights));
    }


    /**
     * Retrieves a {@linkplain GeoLocation} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain GeoLocation}
     *
     * @return the {@linkplain GeoLocation} represented by the specified HTML element
     */
    protected GeoLocation parseGeoLocation(final Element ele)
    {
        final String geoLocationPlace = HtmlUtils.getString(ele, DataCiteConstants.GEOLOCATION_PLACE);
        final Point geoLocationPoint = HtmlUtils.getObject(ele, DataCiteConstants.GEOLOCATION_POINT, this::parseGeoLocationPoint);
        final double[] geoLocationBox = HtmlUtils.getObject(ele, DataCiteConstants.GEOLOCATION_BOX, this::parseGeoLocationBox);

        final GeoLocation geoLocation = new GeoLocation();
        geoLocation.setPlace(geoLocationPlace);

        if (geoLocationPoint != null)
            geoLocation.setPoint(geoLocationPoint);

        if (geoLocationBox != null)
            geoLocation.setBox(geoLocationBox[0], geoLocationBox[1], geoLocationBox[2], geoLocationBox[3]);

        return geoLocation;
    }


    /**
     * Retrieves a {@linkplain Point} from the HTML representation of a GeoJson point.
     *
     * @param ele the HTML element that represents the {@linkplain Point}
     *
     * @return the {@linkplain Point}  represented by the specified HTML element
     */
    protected Point parseGeoLocationPoint(final Element ele)
    {
        final String[] values = ele.text().split(" ");

        final double latitude = Double.parseDouble(values[0]);
        final double longitude = Double.parseDouble(values[1]);

        final Coordinate coord;

        if (values.length == 3) {
            final double elevation = Double.parseDouble(values[2]);
            coord = new Coordinate(longitude, latitude, elevation);
        } else
            coord = new Coordinate(longitude, latitude);

        return geometryFactory.createPoint(coord);
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
    protected double[] parseGeoLocationBox(final Element ele)
    {
        final String[] values = ele.text().split(" ");
        final double[] boxParameters = new double[4];

        try {
            boxParameters[0] = Double.parseDouble(values[1]);
            boxParameters[1] = Double.parseDouble(values[3]);
            boxParameters[2] = Double.parseDouble(values[0]);
            boxParameters[3] = Double.parseDouble(values[2]);
        } catch (final NumberFormatException e) {
            return null;
        }

        return boxParameters;
    }


    @Override
    protected ResourceTypeGeneral parseResourceTypeGeneral(final Element ele)
    {
        // in DataCite 3.0, the resource type "film" is removed,
        // so there is no need to check for it anymore
        return HtmlUtils.getEnumAttribute(
                   ele,
                   DataCiteConstants.RESOURCE_TYPE_GENERAL,
                   ResourceTypeGeneral.class);
    }


    @Override
    protected Contributor parseContributor(final Element ele)
    {
        final Contributor contributor = super.parseContributor(ele);

        // in DataCite 3.1, affiliations are added
        if (contributor != null) {
            final List<Affiliation> affiliations = HtmlUtils.getObjects(ele, DataCiteConstants.AFFILIATION, this::parseAffiliation);
            contributor.addAffiliations(affiliations);
        }

        return contributor;
    }


    @Override
    protected Creator parseCreator(final Element ele)
    {
        final Creator creator = super.parseCreator(ele);

        // in DataCite 3.1, affiliations are added
        final List<Affiliation> affiliations = HtmlUtils.getObjects(ele, DataCiteConstants.AFFILIATION, this::parseAffiliation);
        creator.addAffiliations(affiliations);

        return creator;
    }


    /**
     * Parses an {@linkplain Affiliation} from its HTML representation.
     * @param ele an affiliation element
     *
     * @return an {@linkplain Affiliation}
     */
    protected Affiliation parseAffiliation(final Element ele)
    {
        return new Affiliation(ele.text());
    }


    @Override
    protected Subject parseSubject(final Element ele)
    {
        final Subject subject = super.parseSubject(ele);

        // In DataCite 3.0, language and schemeURI are added
        final String schemeURI = HtmlUtils.getAttribute(ele, DataCiteConstants.SCHEME_URI);
        final String language = HtmlUtils.getAttribute(ele, OaiPmhConstants.LANGUAGE_ATTRIBUTE);

        subject.setSchemeURI(schemeURI);
        subject.setLang(language);

        return subject;
    }


    @Override
    protected NameIdentifier parseNameIdentifier(final Element ele)
    {
        final NameIdentifier nameIdentifier = super.parseNameIdentifier(ele);

        // In DataCite 3.0, schemeURI is added
        final String schemeURI = HtmlUtils.getAttribute(ele, DataCiteConstants.SCHEME_URI);
        nameIdentifier.setSchemeURI(schemeURI);

        return nameIdentifier;
    }


    @Override
    protected Title parseTitle(final Element ele)
    {
        final Title title = super.parseTitle(ele);

        // In DataCite 3.0, language is added
        final String language = HtmlUtils.getAttribute(ele, OaiPmhConstants.LANGUAGE_ATTRIBUTE);
        title.setLang(language);

        return title;
    }


    @Override
    protected Description parseDescription(final Element ele)
    {
        final Description description = super.parseDescription(ele);

        // In DataCite 3.0, language is added
        final String language = HtmlUtils.getAttribute(ele, OaiPmhConstants.LANGUAGE_ATTRIBUTE);
        description.setLang(language);

        return description;
    }


    @Override
    protected Rights parseRights(final Element ele)
    {
        final Rights rights = super.parseRights(ele);

        // In DataCite 3.0, rightsURI is added
        final String rightsURI = HtmlUtils.getAttribute(ele, DataCiteConstants.RIGHTS_URI);
        rights.setUri(rightsURI);

        return rights;
    }


    @Override
    protected AbstractDate parseDate(final Element ele)
    {
        final String dateString = ele.text();
        final DateType dateType = HtmlUtils.getEnumAttribute(ele, DataCiteConstants.DATE_TYPE, DateType.class);

        return DateUtils.parseAbstractDate(dateString, dateType);
    }
}
