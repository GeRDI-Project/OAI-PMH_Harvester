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
 * A static collection of constant parameters for configuring the Dublin Core strategy.
 *
 * @author Jan Frömberg
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DublinCoreConstants
{
    public static final String IDENTIFIERS = "dc|identifier";
    public static final String TITLES = "dc|title";
    public static final String CREATORS = "dc|creator";
    public static final String CONTRIBUTORS = "dc|contributor";
    public static final String DESCRIPTIONS = "dc|description";
    public static final String PUBLISHER = "dc|publisher";
    public static final String SUBJECTS = "dc|subject";
    public static final String DATES = "dc|date";
    public static final String LANG = "dc|language";
    public static final String RES_TYPE = "dc|type";
    public static final String FORMATS = "dc|format";
    public static final String RIGHTS = "dc|rights";

    public static final String VIEW_URL_TITLE = "View URL";
}
