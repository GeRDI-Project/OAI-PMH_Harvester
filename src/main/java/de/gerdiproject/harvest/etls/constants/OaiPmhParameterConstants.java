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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A static collection of constant parameters for configuring the OAI-PMH harvester.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OaiPmhParameterConstants
{
    //
    public static final String  DATACITE_3_METADATA_PREFIX = "datacite3";
    public static final String  DATACITE_3_METADATA_PREFIX_2 = "oai_datacite3";
    public static final String  DATACITE_3_METADATA_PREFIX_3 = "oai_datacite";

    public static final String  DATACITE_4_METADATA_PREFIX = "datacite4";
    public static final String  DATACITE_4_METADATA_PREFIX_2 = "oai_datacite4";
    public static final String  DATACITE_4_METADATA_PREFIX_3 = "datacite";

    public static final String  DUBLIN_CORE_METADATA_PREFIX = "oai_dc";

    public static final String  ISO_19139_METADATA_PREFIX = "iso19139";

    public static final List<String> METADATA_PREFIX_ALLOWED_VALUES =
        Collections.unmodifiableList(Arrays.asList(
                                         DATACITE_3_METADATA_PREFIX,
                                         DATACITE_3_METADATA_PREFIX_2,
                                         DATACITE_3_METADATA_PREFIX_3,
                                         DATACITE_4_METADATA_PREFIX,
                                         DATACITE_4_METADATA_PREFIX_2,
                                         DATACITE_4_METADATA_PREFIX_3,
                                         DUBLIN_CORE_METADATA_PREFIX,
                                         ISO_19139_METADATA_PREFIX
                                     ));

    public static final String METADATA_PREFIX_KEY = "metadataPrefix";
    public static final String METADATA_PREFIX_DEFAULT_VALUE = DUBLIN_CORE_METADATA_PREFIX;

    public static final String FROM_KEY = "from";
    public static final String FROM_DEFAULT_VALUE = "";

    public static final String UNTIL_KEY = "until";
    public static final String UNTIL_DEFAULT_VALUE = "";

    public static final String HOST_URL_KEY = "hostUrl";
    public static final String HOST_URL_DEFAULT_VALUE = "";
}
