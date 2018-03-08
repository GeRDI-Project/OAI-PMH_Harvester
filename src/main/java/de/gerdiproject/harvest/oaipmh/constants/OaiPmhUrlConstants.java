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
 * A static collection of constant parameters for assembling OAI-PMH URLs.
 *
 * @author Robin Weiss
 */
public class OaiPmhUrlConstants
{
    // QUERY
    public static final String DATE_FROM_QUERY = "&from=";
    public static final String DATE_TO_QUERY = "&until=";
    public static final String METADATA_PREFIX_QUERY = "&metadataPrefix=";

    // URLs
    public static final String BASE_URL = "%s?verb=ListRecords%s";
    public static final String RESUMPTION_URL =  "%s?verb=ListRecords&resumptionToken=%s";

    /**
     * Private Constructor, because this is a static class.
     */
    private OaiPmhUrlConstants()
    {
    }
}
