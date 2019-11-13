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
import java.nio.charset.StandardCharsets;

import org.jsoup.nodes.Element;

import de.gerdiproject.harvest.OaiPmhContextListener;
import de.gerdiproject.harvest.application.ContextListener;
import de.gerdiproject.harvest.application.MainContextUtils;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;
import de.gerdiproject.harvest.etls.OaiPmhETL;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.harvest.utils.file.FileUtils;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class provides Unit Tests for an {@linkplain AbstractOaiPmhRecordTransformer} implementation.
 *
 * @author Robin Weiss
 */
public abstract class AbstractOaiPmhTransformerTest extends AbstractIteratorTransformerTest<Element, DataCiteJson>
{
    private static final String INPUT_RESOURCE = "input.html";
    private static final String OUTPUT_RESOURCE = "output.json";
    private static final String MOCKED_HTTP_RESPONSES_RESOURCE = "mockedHttpResponses";

    private final DiskIO diskReader = new DiskIO(GsonUtils.createGerdiDocumentGsonBuilder().create(), StandardCharsets.UTF_8);


    @Override
    protected ContextListener getContextListener()
    {
        return new OaiPmhContextListener();
    }


    @Override
    protected AbstractIteratorTransformer<Element, DataCiteJson> setUpTestObjects()
    {
        // copy mocked HTTP responses
        final File httpResourceFolder = getResource(MOCKED_HTTP_RESPONSES_RESOURCE);

        if (httpResourceFolder != null) {
            // copy mocked HTTP responses to the cache folder to drastically speed up the testing
            final File httpCacheFolder = new File(
                MainContextUtils.getCacheDirectory(getClass()),
                DataOperationConstants.CACHE_FOLDER_PATH);

            FileUtils.copyFile(httpResourceFolder, httpCacheFolder);
        }

        return super.setUpTestObjects();
    }


    @Override
    protected AbstractIteratorETL<Element, DataCiteJson> getEtl()
    {
        return new OaiPmhETL();
    }


    @Override
    protected Element getMockedInput()
    {
        return diskReader.getHtml(getResource(INPUT_RESOURCE).toString());
    }


    @Override
    protected DataCiteJson getExpectedOutput()
    {
        final File resource = getResource(OUTPUT_RESOURCE);
        return diskReader.getObject(resource, DataCiteJson.class);
    }
}
