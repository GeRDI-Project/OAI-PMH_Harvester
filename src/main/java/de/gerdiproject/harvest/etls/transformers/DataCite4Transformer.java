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
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.RelatedIdentifier;
import de.gerdiproject.json.geo.Point;

/**
 * A transformer for records of the Datacite 4.1 metadata standard.<br>
 * https://schema.datacite.org/meta/kernel-4.1/
 *
 * @author Robin Weiss
 */
public class DataCite4Transformer extends AbstractDataCiteTransformer
{
    @Override
    protected void setDocumentFieldsFromRecord(DataCiteJson document, Element record)
    {
        final Element metadata = getMetadata(record);

        final Identifier identifier = getObject(metadata, DataCiteConstants.IDENTIFIER, this::parseIdentifier);
        document.setIdentifier(identifier);

        final List<RelatedIdentifier> relatedIdentifiers = getObjects(metadata, DataCiteConstants.RELATED_IDENTIFIERS, this::parseRelatedIdentifier);
        document.addRelatedIdentifiers(relatedIdentifiers);

        document.setPublisher(getString(metadata, DataCiteConstants.PUBLISHER));
        document.setLanguage(getString(metadata, DataCiteConstants.LANGUAGE));
        document.setVersion(getString(metadata, DataCiteConstants.VERSION));
        document.setPublicationYear(parsePublicationYear(metadata));
        document.addSizes(getStrings(metadata, DataCiteConstants.SIZES));
        document.addFormats(getStrings(metadata, DataCiteConstants.FORMATS));
        document.setResourceType(getObject(metadata, DataCiteConstants.RESOURCE_TYPE, this::parseResourceType));
        document.addCreators(getObjects(metadata, DataCiteConstants.CREATORS, this::parseCreator));
        document.addContributors(getObjects(metadata, DataCiteConstants.CONTRIBUTORS, this::parseContributor));
        document.addTitles(getObjects(metadata, DataCiteConstants.TITLES, this::parseTitle));
        document.addDescriptions(getObjects(metadata, DataCiteConstants.DESCRIPTIONS, this::parseDescription));
        document.addSubjects(getObjects(metadata, DataCiteConstants.SUBJECTS, this::parseSubject));
        document.addAlternateIdentifiers(getObjects(metadata, DataCiteConstants.ALTERNATE_IDENTIFIERS, this::parseAlternateIdentifier));
        document.addRights(getObjects(metadata, DataCiteConstants.RIGHTS_LIST, this::parseRights));
        document.addDates(getObjects(metadata, DataCiteConstants.DATES, this::parseDate));
        document.addGeoLocations(getObjects(metadata, DataCiteConstants.GEO_LOCATIONS, this::parseGeoLocation));
        document.addFundingReferences(getObjects(metadata, DataCiteConstants.FUNDING_REFERENCES, this::parseFundingReference));
        document.addWebLinks(createWebLinks(identifier, relatedIdentifiers));
    }


    @Override
    protected Point parseGeoLocationPoint(Element ele)
    {
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
}
