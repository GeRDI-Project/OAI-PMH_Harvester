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
package de.gerdiproject.harvest.oaipmh.constants;

/**
 * A static collection of constant parameters regarding OAI-PMH.
 *
 * @author Robin Weiss
 */
public class OaiPmhConstants
{
    // QUERY
    public static final String DATE_FROM_QUERY = "&from=";
    public static final String DATE_TO_QUERY = "&until=";
    public static final String METADATA_PREFIX_QUERY = "&metadataPrefix=";

    // URLs
    public static final String LIST_RECORDS_URL = "%s?verb=ListRecords%s";
    public static final String IDENTIFY_URL = "%s?verb=Identify";
    public static final String RESUMPTION_URL =  "%s?verb=ListRecords&resumptionToken=%s";
    public static final String DOI_URL = "https://doi.org/%s";

    // Elements
    public static final String REPOSITORY_NAME_ELEMENT = "repositoryName";
    public static final String RECORD_ELEMENT = "record";
    public static final String RESUMPTION_TOKEN_ELEMENT = "resumptionToken";

    // Other
    public static final String DEFAULT_PROVIDER = "Unknown";


    /**
     * Private Constructor, because this is a static class.
     */
    private OaiPmhConstants()
    {
    }
}
