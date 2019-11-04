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
package de.gerdiproject.harvest.etls.transformers.constants;

import java.util.HashMap;
import java.util.Map;

import de.gerdiproject.json.datacite.enums.DateType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A static collection of constant parameters for configuring the ISO19139 strategy.
 *
 * @author Tobias Weber
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Iso19139Constants
{
    public static final String DATA_IDENTIFICATION =
        "identificationInfo > MD_DataIdentification,"
        + "*|identificationInfo > *|MD_DataIdentification";

    public static final String PUBLISHER =
        "CI_ResponsibleParty > organisationName > gco|CharacterString,"
        + "*|CI_ResponsibleParty > *|organisationName > gco|CharacterString";

    public static final String TITLE =
        "citation > CI_Citation > title,"
        + "*|citation > *|CI_Citation > *|title > gco|CharacterString";

    public static final String ALTERNATE_TITLE =
        "citation > CI_Citation > alternateTitle > gco|CharacterString,"
        + "*|citation > *|CI_Citation > *|alternateTitle > gco|CharacterString";

    public static final String KEYWORDS =
        " MD_Keywords > keyword > gco|CharacterString,"
        + "*|MD_Keywords > *|keyword > gco|CharacterString";

    public static final String DATESTAMP =
        "dateStamp > gco|DateTime,"
        + "*|dateStamp > gco|DateTime";

    public static final String RESEARCH_DATA =
        "transferOptions > MD_DigitalTransferOptions > onLine > CI_OnlineResource > linkage > URL,"
        + "*|transferOptions > *|MD_DigitalTransferOptions > *|onLine > *|CI_OnlineResource > *|linkage > *|URL";

    public static final String DATES =
        "citation > CI_citation > date > CI_Date,"
        + "*|citation > *|CI_citation > *|date > *|CI_Date";

    public static final String GEO_LOCATION_BOX =
        "extent > EX_Extent > geographicElement > EX_GeographicBoundingBox,"
        + "*|extent > *|EX_Extent > *|geographicElement > *|EX_GeographicBoundingBox";

    public static final String GEO_LOCATION_DESCRIPTION =
        "extent > EX_Extent > geographicElement > EX_GeographicDescription > geographicIdentifier > MD_Identifier > code > gco|CharacterString,"
        + "*|extent > *|EX_Extent > *|geographicElement > *|EX_GeographicDescription > *|geographicIdentifier > *|MD_Identifier > *|code > gco|CharacterString";


    public static final String GEO_LOCATION_WEST =
        "westBoundLongitude > gco|Decimal,"
        + "*|westBoundLongitude > gco|Decimal";

    public static final String GEO_LOCATION_EAST =
        "eastBoundLongitude > gco|Decimal,"
        + "*|eastBoundLongitude > gco|Decimal";

    public static final String GEO_LOCATION_SOUTH =
        "southBoundLatitude > gco|Decimal,"
        + "*|southBoundLatitude > gco|Decimal";

    public static final String GEO_LOCATION_NORTH =
        "northBoundLatitude > gco|Decimal,"
        + "*|northBoundLatitude > gco|Decimal";

    public static final String RESOURCE_TYPE = "MD_ScopeCode, *|MD_ScopeCode";
    public static final String DATE = "date > *, *|date > *";
    public static final String DATE_TYPE = "CI_DateTypeCode, *|CI_DateTypeCode";
    public static final String DESCRIPTIONS = "abstract, *|abstract";
    public static final String LANGUAGE = "language > LanguageCode, *|language > *|LanguageCode";
    public static final String CODE_LIST_VALUE = "codeListValue";

    public static final String DATE_PARSING_FAILED = "Datestamp is not a date: {}";
    public static final String SCHEMA_URL = "http://www.isotc211.org/2005/gmd/gmd.xsd";

    public static final Map<String, DateType> DATE_TYPE_MAP = createDateTypeMap();


    /**
     * Initializes the DateTypeMap
     */
    private static Map<String, DateType> createDateTypeMap()
    {
        final Map<String, DateType> map = new HashMap<>();
        map.put("publication", DateType.Issued);
        map.put("revision", DateType.Updated);
        map.put("creation", DateType.Created);
        return map;
    }
}
