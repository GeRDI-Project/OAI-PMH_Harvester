/*
 *  Copyright © 2019 Robin Weiss (http://www.gerdi-project.de/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package de.gerdiproject.harvest.etls.transformers;

import java.io.File;

import org.jsoup.nodes.Element;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.gerdiproject.json.datacite.DataCiteJson;
import lombok.RequiredArgsConstructor;

/**
 * This class provides Unit Tests the {@linkplain DataCiteFlexTransformer}.
 * It uses example records based on varying DataCite schema versions to
 * see if the version differences appear as expected in the transformed documents.
 *
 * @author Robin Weiss
 */
@RunWith(Parameterized.class) @RequiredArgsConstructor
public class DataCiteFlexTransformerTest extends AbstractOaiPmhTransformerTest
{
    private static final String INPUT_RESOURCE = "%s_input.html";
    private static final String OUTPUT_RESOURCE = "%s_output.json";

    @Parameters(name = "recordType: {0}")
    public static Object[] getParameters()
    {
        return new Object[] {
                   "datacite2",
                   "datacite3",
                   "datacite4",
                   "invalid"
               };
    }

    private final String recordType;


    @Override
    protected Element getMockedInput()
    {
        final String resourcePath = getResource(String.format(INPUT_RESOURCE, recordType)).toString();
        return diskReader.getHtml(resourcePath);
    }


    @Override
    protected DataCiteJson getExpectedOutput()
    {
        final File resource = getResource(String.format(OUTPUT_RESOURCE, recordType));
        return diskReader.getObject(resource, DataCiteJson.class);
    }
}
