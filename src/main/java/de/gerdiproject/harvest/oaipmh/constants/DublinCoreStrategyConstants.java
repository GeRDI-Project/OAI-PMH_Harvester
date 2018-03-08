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
 * A static collection of constant parameters for configuring the DataCite3 strategy.
 *
 * @author Jan Frömberg
 *
 */
public class DublinCoreStrategyConstants
{

    public static final String RECORD_STATUS = "status";
    public static final String RECORD_STATUS_DEL = "deleted";
    public static final String RECORD_HEADER = "header";
    public static final String RECORD_METADATA = "metadata";
    public static final String RECORD_DATESTAMP = "datestamp";

    public static final String IDENTIFIER = "identifier";
    public static final String METADATA_IDENTIFIER = "dc|identifier";

    public static final String DOC_TITLE = "dc|title";
    public static final String DOC_CREATORS = "dc|creator";
    public static final String DOC_CONTRIBUTORS = "dc|contributor";
    public static final String DOC_DESCRIPTIONS = "dc|description";

    public static final String PUBLISHER = "dc|publisher";
    public static final String SUBJECTS = "dc|subject";

    public static final String METADATA_DATE = "dc|date";

    public static final String LANG = "dc|language";
    public static final String RES_TYPE = "dc|type";

    public static final String METADATA_FORMATS = "dc|format";
    public static final String RIGHTS = "dc|rights";

    /**
     * Private Constructor, because this is a static class.
     */
    private DublinCoreStrategyConstants()
    {
    }
}
