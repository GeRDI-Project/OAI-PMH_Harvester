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
package de.gerdiproject.harvest.etls.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A static collection of constant parameters regarding OAI-PMH.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OaiPmhConstants
{
    // QUERY
    public static final String DATE_FROM_QUERY = "&from=";
    public static final String DATE_TO_QUERY = "&until=";
    public static final String METADATA_PREFIX_QUERY = "&metadataPrefix=";
    public static final String SET_QUERY = "&set=";

    // URLs
    public static final String LIST_RECORDS_URL = "%s?verb=ListRecords%s";
    public static final String IDENTIFY_URL = "%s?verb=Identify";
    public static final String METADATA_FORMATS_URL = "%s?verb=ListMetadataFormats";
    public static final String RESUMPTION_URL =  "%s?verb=ListRecords&resumptionToken=%%s";
    public static final String DOI_URL = "https://doi.org/%s";

    // Elements and Attributes
    public static final String REPOSITORY_NAME_ELEMENT = "repositoryName";
    public static final String RECORD_ELEMENT = "record";
    public static final String RESUMPTION_TOKEN_ELEMENT = "resumptionToken";

    public static final String RECORD_HEADER = "header";
    public static final String RECORD_METADATA = "metadata";

    public static final String HEADER_IDENTIFIER = "identifier";
    public static final String HEADER_DATESTAMP = "datestamp";
    public static final String HEADER_SET_SPEC = "setSpec";
    public static final String HEADER_STATUS_ATTRIBUTE = "status";
    public static final String HEADER_STATUS_ATTRIBUTE_DELETED = "deleted";

    // Other
    public static final String UNINITIALIZED_PROVIDER = "OaiPmh";
    public static final String UNKNOWN_PROVIDER = "<not set>";
    public static final String LANGUAGE_ATTRIBUTE = "xml:lang";
    public static final String LIST_SIZE_ATTRIBUTE = "completeListSize";
    public static final String ALL_METADATA_PREFIXES_SELECTION = "metadataFormat";
    public static final String METADATA_PREFIX_SELECTION = "metadataPrefix";
    public static final String METADATA_SCHEMA_SELECTION = "schema";
    public static final String LOGO_URL_TITLE = "logo";
    public static final String VIEW_URL_NAME = "Browse Repository";

    // Errors
    public static final String CANNOT_CREATE_TRANSFORMER = "Cannot create transformer!";
    public static final String CANNOT_GET_METADATA_SCHEMAS_ERROR = "Cannot retrieve list of viable metadata schemas!";
    public static final String REPOSITORY_UNSUPPORTED_METADATA_PREFIX_ERROR = "The '" + OaiPmhParameterConstants.METADATA_PREFIX_KEY + "'-parameter '%s' is not supported by this repository!";
    public static final String HARVESTER_UNSUPPORTED_METADATA_PREFIX_ERROR = "The '" + OaiPmhParameterConstants.METADATA_PREFIX_KEY + "'-parameter '%s' is not supported by the OAI-PMH harvester!";
    public static final String NO_METADATA_PREFIX_ERROR = "You must set the '" + OaiPmhParameterConstants.METADATA_PREFIX_KEY + "'-parameter in the config!";
    public static final String NO_HOST_URL_ERROR = "You must set the '" + OaiPmhParameterConstants.HOST_URL_KEY + "'-parameter in the config!";
    public static final String NO_RECORDS_ERROR = "The URL '%s' did not yield any harvestable records! A possible reason for this can be an outage of the harvested repository, or an incorrectly configured harvester.";
    public static final String NO_RECORDS_RESUMED_ERROR = "The resumption URL '%s' did not yield any harvestable records! This can be caused by an outage of the harvested repository.";
    public static final String CANNOT_CREATE_EXTRACTOR = "Cannot initialize the extractor, because a mandatory parameter was not set. Ignore this message if you set multiple parameters in a single request.";
    public static final String LAST_DATE_INFO =
        "The harvest stopped at date stamp: %s%nYou may try to harvest the remaining records by setting the '%s."
        + OaiPmhParameterConstants.FROM_KEY
        + "' parameter accordingly!";
    public static final String FALLBACK_URL_INFO = "The resumption URL '%s' did not yield any harvestable records! Attempting to continue the harvest via the fallback URL '%s'.";
}
