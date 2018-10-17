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

/**
 * A static collection of constant parameters for configuring the OAI-PMH harvester.
 *
 * @author Robin Weiss
 */
public class OaiPmhParameterConstants
{
    public static final String METADATA_PREFIX_KEY = "metadataPrefix";
    public static final String METADATA_PREFIX_DEFAULT_VALUE = "datacite4";

    public static final String FROM_KEY = "from";
    public static final String FROM_DEFAULT_VALUE = "";

    public static final String UNTIL_KEY = "until";
    public static final String UNTIL_DEFAULT_VALUE = "";

    public static final String HOST_URL_KEY = "hostUrl";
    public static final String HOST_URL_DEFAULT_VALUE = "";


    /**
     * Private Constructor, because this is a static class.
     */
    private OaiPmhParameterConstants()
    {
    }
}
