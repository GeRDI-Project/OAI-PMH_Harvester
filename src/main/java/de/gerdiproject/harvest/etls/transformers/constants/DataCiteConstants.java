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
package de.gerdiproject.harvest.etls.transformers.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A static collection of constant parameters used by DataCite transformations.
 *
 * @author Jan Frömberg
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataCiteConstants
{
    public static final String IDENTIFIER = "identifier";
    public static final String VERSION = "version";
    public static final String PUBLICATION_YEAR = "publicationYear";
    public static final String SIZES = "sizes";
    public static final String FORMATS = "formats";

    public static final String TITLES = "titles";
    public static final String TITLE_TYPE = "titleType";

    public static final String CONTRIBUTORS = "contributors";
    public static final String CONTRIBUTOR_NAME = "contributorName";
    public static final String CONTRIBUTOR_TYPE = "contributorType";

    public static final String CREATORS = "creators";
    public static final String CREATOR_NAME = "creatorName";

    public static final String GIVEN_NAME = "givenName";
    public static final String FAMILY_NAME = "familyName";
    public static final String AFFILIATION = "affiliation";

    public static final String NAME_IDENTIFIER = "nameIdentifier";
    public static final String NAME_IDENTIFIER_SCHEME = "nameIdentifierScheme";
    public static final String NAME_TYPE = "nameType";

    public static final String SUBJECTS = "subjects";
    public static final String SUBJECT_SCHEME = "subjectScheme";

    public static final String SCHEME_URI = "schemeURI";
    public static final String SCHEME_TYPE = "schemeType";
    public static final String VALUE_URI = "valueURI";

    public static final String DATES = "dates";
    public static final String DATE_TYPE = "dateType";
    public static final String DATE_INFORMATION = "dateInformation";

    public static final String PUBLISHER = "publisher";
    public static final String LANGUAGE = "language";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final String RESOURCE_TYPE_GENERAL = "resourceTypeGeneral";

    public static final String RELATED_IDENTIFIERS = "relatedIdentifiers";
    public static final String RELATED_IDENTIFIER_TYPE = "relatedIdentifierType";
    public static final String RELATION_TYPE = "relationType";
    public static final String RELATED_METADATA_SCHEME = "relatedMetadataScheme";

    public static final String RIGHTS_LIST = "rightsList";
    public static final String RIGHTS_URI = "rightsURI";

    public static final String DESCRIPTIONS = "descriptions";
    public static final String DESC_TYPE = "descriptionType";

    public static final String GEO_LOCATIONS = "geoLocations";
    public static final String GEOLOCATION_BOX = "geoLocationBox";
    public static final String GEOLOCATION_POINT = "geoLocationPoint";
    public static final String GEOLOCATION_PLACE = "geoLocationPlace";
    public static final String GEOLOCATION_POLYGON = "geoLocationPolygon";
    public static final String POLYGON_POINT = "polygonPoint";
    public static final String POINT_LONG  = "pointLongitude";
    public static final String POINT_LAT  = "pointLatitude";
    public static final String BOX_WEST_LONG  = "westBoundLongitude";
    public static final String BOX_EAST_LONG  = "eastBoundLongitude";
    public static final String BOX_SOUTH_LAT  = "southBoundLatitude";
    public static final String  BOX_NORTH_LAT = "northBoundLatitude";

    public static final String ALTERNATE_IDENTIFIERS = "alternateIdentifiers";
    public static final String ALTERNATE_IDENTIFIER_TYPE = "alternateIdentifierType";

    // DATACITE 2
    public static final Object RESOURCE_TYPE_GENERAL_FILM = "film";
    public static final String RIGHTS = "rights";

    // DATACITE 3
    public static final String CONTRIBUTOR_TYPE_FUNDER = "funder";

    // DATACITE 4
    public static final String FUNDING_REFERENCES = "fundingReferences";
    public static final String FUNDER_NAME = "funderName";
    public static final String FUNDER_IDENTIFIER = "funderIdentifier";
    public static final String FUNDER_IDENTIFIER_TYPE = "funderIdentifierType";
    public static final String AWARD_NUMBER = "awardNumber";
    public static final String AWARD_TITLE = "awardTitle";
    public static final String AWARD_URI = "awardURI";

    // Misc
    public static final String URL_PREFIX = "http";
    public static final String RESOURCE_LINK_NAME = "Resource";
    public static final String RESOURCE_ELEMENT = "resource";
    public static final String SCHEMA_LOCATION_ATTRIBUTE = "xsi:schemalocation";
    public static final String NO_SCHEMA_LOCATION_ATTRIBUTE = "xsi:nonamespaceschemalocation";

    // Schema URLs
    public static final String SCHEMA_2_URL = "http://schema.datacite.org/meta/kernel-2/metadata.xsd";
    public static final String SCHEMA_2_0_URL = "http://schema.datacite.org/meta/kernel-2.0/metadata.xsd";
    public static final String SCHEMA_2_1_URL = "http://schema.datacite.org/meta/kernel-2.1/metadata.xsd";
    public static final String SCHEMA_2_2_URL = "http://schema.datacite.org/meta/kernel-2.2/metadata.xsd";
    public static final String SCHEMA_3_URL = "http://schema.datacite.org/meta/kernel-3/metadata.xsd";
    public static final String SCHEMA_3_0_URL = "http://schema.datacite.org/meta/kernel-3.0/metadata.xsd";
    public static final String SCHEMA_3_1_URL = "http://schema.datacite.org/meta/kernel-3.1/metadata.xsd";
    public static final String SCHEMA_4_URL = "http://schema.datacite.org/meta/kernel-4/metadata.xsd";
    public static final String SCHEMA_4_0_URL = "http://schema.datacite.org/meta/kernel-4.0/metadata.xsd";
    public static final String SCHEMA_4_1_URL = "http://schema.datacite.org/meta/kernel-4.1/metadata.xsd";
    public static final String OAI_SCHEMA_1_0_URL = "http://schema.datacite.org/oai/oai-1.0/oai.xsd";
    public static final String OAI_SCHEMA_1_1_URL = "http://schema.datacite.org/oai/oai-1.1/oai.xsd";
    public static final String NO_SCHEMA_URL = "http://schema.datacite.org/meta/nonexistant/nonexistant.xsd";

    // Errors
    public static final String UNKNOWN_SCHEMA_ERROR = "Cannot harvest record! Unknown DataCite schema: %s";

}