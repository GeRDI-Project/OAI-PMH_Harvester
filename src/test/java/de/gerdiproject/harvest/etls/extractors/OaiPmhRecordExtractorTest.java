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
package de.gerdiproject.harvest.etls.extractors;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.jsoup.nodes.Element;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.gerdiproject.harvest.OaiPmhContextListener;
import de.gerdiproject.harvest.application.ContextListener;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;
import de.gerdiproject.harvest.etls.OaiPmhETL;
import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.utils.data.DiskIO;
import de.gerdiproject.harvest.utils.data.HttpRequesterUtils;
import de.gerdiproject.json.GsonUtils;
import de.gerdiproject.json.datacite.DataCiteJson;
import lombok.RequiredArgsConstructor;

/**
 * This class provides Unit Tests for the {@linkplain OaiPmhRecordExtractor}.
 *
 * @author Robin Weiss
 */
@RunWith(Parameterized.class) @RequiredArgsConstructor
public class OaiPmhRecordExtractorTest extends AbstractIteratorExtractorTest<Element>
{
    private static final String HTTP_RESOURCE_FOLDER = "mockedHttpResponses";
    private static final String CONFIG_RESOURCE = "config.json";
    private static final String RECORDS_URL_PREFIX = String.format(
                                                         OaiPmhConstants.LIST_RECORDS_URL,
                                                         "www.mo.ck/oai",
                                                         OaiPmhConstants.METADATA_PREFIX_QUERY);

    @Parameters(name = "metadataPrefix: {0}")
    public static Object[] getParameters()
    {
        return new Object[] {
                   "oai_dc",
                   "oai_datacite",
                   "datacite2",
                   "datacite3",
                   "datacite4",
                   "iso19139"
               };
    }


    private final DiskIO diskReader = new DiskIO(GsonUtils.createGerdiDocumentGsonBuilder().create(), StandardCharsets.UTF_8);
    private final String metadataPrefix;


    @Override
    protected AbstractIteratorExtractor<Element> setUpTestObjects()
    {
        // replace placeholder in config.json
        replacePlaceHoldersInFile(
            getResource(CONFIG_RESOURCE),
            getConfigFile(),
            "${metadataPrefix}",
            metadataPrefix);

        return super.setUpTestObjects();
    }


    private void replacePlaceHoldersInFile(final File inputFile, final File outputFile, final String placeHolder, final String replacement)
    {
        // replace placeholders in file content
        final String fileContent = diskReader
                                   .getString(inputFile)
                                   .replace(placeHolder, replacement);

        // write file
        diskReader.writeStringToFile(outputFile, fileContent);
    }


    @Override
    protected ContextListener getContextListener()
    {
        return new OaiPmhContextListener();
    }


    @Override
    protected AbstractIteratorETL<Element, DataCiteJson> getEtl()
    {
        return new OaiPmhETL();
    }


    @Override
    protected File getConfigFile()
    {
        return new File(getTemporaryTestDirectory(), CONFIG_RESOURCE);
    }


    @Override
    protected File getMockedHttpResponseFolder()
    {
        return getResource(HTTP_RESOURCE_FOLDER);
    }


    @Override
    protected Element getExpectedOutput()
    {
        final String recordsPath =
            HttpRequesterUtils.urlToFilePath(
                RECORDS_URL_PREFIX + metadataPrefix,
                getMockedHttpResponseFolder()).toString();

        return diskReader.getHtml(recordsPath).selectFirst(OaiPmhConstants.RECORD_ELEMENT);
    }


    @Override
    protected void assertExpectedOutput(final Element expectedOutput, final Element actualOutput)
    {
        assertTrue(String.format(WRONG_OBJECT_ERROR, testedObject.getClass().getSimpleName()),
                   expectedOutput.hasSameValue(actualOutput));
    }
}
