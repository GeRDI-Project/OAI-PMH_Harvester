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
    public static final String TITLE = "gmd|citation gmd|CI_Citation gmd|title";
    public static final String PUBLISHER = "gmd|pointOfContact gmd|CI_ResponsibleParty gmd|organisationName";
    public static final String DATESTAMP = "gmd|dateStamp gco|DateTime";
    public static final String RESOURCE_TYPE = "gmd|hierarchyLevel gmd|MD_ScopeCode";

    public static final String RESEARCH_DATA = "gmd|transferOptions gmd|MD_DigitalTransferOptions gmd|onLine gmd|CI_OnlineResource gmd|linkage gmd|URL";

    public static final String DATES = "gmd|citation gmd|CI_citation gmd|date";
    public static final String DATE = "gmd|date gco|Date";
    public static final String DATE_TYPE = "gmd|CI_DateTypeCode";
    public static final String DESCRIPTIONS = "gmd|abstract";
    public static final String GEOLOCS = "gmd|extent gmd|EX_Extent gmd|geographicElement gmd|EX_GeographicBoundingBox";
    public static final String GEOLOCS_WEST = "gmd|westBoundLongitude gco|Decimal";
    public static final String GEOLOCS_EAST = "gmd|eastBoundLongitude gco|Decimal";
    public static final String GEOLOCS_SOUTH = "gmd|southBoundLatitude gco|Decimal";
    public static final String GEOLOCS_NORTH = "gmd|northBoundLatitude gco|Decimal";

    public static final String DATE_PARSING_FAILED = "Datestamp is not a date: {}";

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
