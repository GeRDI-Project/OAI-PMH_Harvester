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
package de.gerdiproject.harvest.etls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Iterator;

import org.jsoup.nodes.Element;
import org.junit.Test;

import de.gerdiproject.harvest.AbstractObjectUnitTest;
import de.gerdiproject.harvest.OaiPmhContextListener;
import de.gerdiproject.harvest.application.ContextListenerTestWrapper;
import de.gerdiproject.harvest.application.MainContext;
import de.gerdiproject.harvest.application.MainContextUtils;
import de.gerdiproject.harvest.application.events.ServiceInitializedEvent;
import de.gerdiproject.harvest.config.events.GetConfigurationEvent;
import de.gerdiproject.harvest.config.parameters.constants.ParameterConstants;
import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.etls.constants.OaiPmhParameterConstants;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.utils.HtmlUtils;
import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.harvest.utils.file.FileUtils;

/**
 * This class provides Unit Tests for the {@linkplain OaiPmhETL}.
 *
 * @author Robin Weiss
 */
public class OaiPmhETLTest extends AbstractObjectUnitTest<OaiPmhETL>
{
    private static final String HTTP_RESOURCE_FOLDER = "mockedHttpResponses";
    private static final String CONFIG_RESOURCE = "config.json";
    private static final String RESUMED_ELEMENT_ID = "SecondElement";
    private static final String FALLBACK_ELEMENT_ID = "ThirdElement";
    private static final String FROM_PARAMETER_VALUE = "2000-02-02";
    private static final String UNTIL_PARAMETER_VALUE = "4000-04-04";
    private static final String SET_PARAMETER_VALUE = "mocked-set";

    private ContextListenerTestWrapper<OaiPmhETL> contextInitializer;


    @Override
    protected OaiPmhETL setUpTestObjects()
    {
        // copy mocked HTTP responses to the cache folder to drastically speed up the testing
        final File httpCacheFolder = new File(
            MainContextUtils.getCacheDirectory(getClass()),
            DataOperationConstants.CACHE_FOLDER_PATH);
        FileUtils.copyFile(
            getResource(HTTP_RESOURCE_FOLDER),
            httpCacheFolder);

        this.contextInitializer =
            new ContextListenerTestWrapper<>(
            new OaiPmhContextListener(),
            () -> new OaiPmhETL());

        // copy over configuration file
        FileUtils.copyFile(
            getResource(CONFIG_RESOURCE),
            this.contextInitializer.getConfigFile());

        return this.contextInitializer.getEtl();
    }


    /**
     * Initializes the {@linkplain MainContext} and all components that belong to
     * it, including the ETL.
     */
    private void initializeContext()
    {
        waitForEvent(
            ServiceInitializedEvent.class,
            5000,
            () -> contextInitializer.initializeContext());

        this.config = EventSystem.sendSynchronousEvent(new GetConfigurationEvent());
    }


    /**
     * Tests if the {@linkplain OaiPmhETL#getMaxNumberOfDocuments} method correctly returns
     * the value of the "completeListSize" attribute specified in the resumptionToken HTML
     * element of the records response.
     */
    @Test
    public void testListSize()
    {
        initializeContext();

        final int size = testedObject.getMaxNumberOfDocuments();

        assertEquals("Expected the number returned by getMaxNumberOfDocuments() to be the same as in the records response.",
                     3,
                     size);
    }


    /**
     * Tests if the resumption token is correctly consumed to make use of OAI-PMH defined
     * pagination capabilities.
     */
    @Test
    public void testResumptionToken()
    {
        initializeContext();

        final Iterator<Element> extractorIter = testedObject.extractor.extract();

        // retrieve first element, memorize the resumptionToken
        extractorIter.next();

        // retrieve the second element using the resumptionToken
        final Element resumedElement = extractorIter.next();
        final String resumedElementId = HtmlUtils.getString(resumedElement, OaiPmhConstants.HEADER_IDENTIFIER);

        assertEquals(
            "Expected at least two elements to be extractable due to following the resumption token defined in the first element.",
            RESUMED_ELEMENT_ID,
            resumedElementId);
    }


    /**
     * Tests if the datestamp of the most recently harvested record is used to create a fallback URL
     * if the resumption token points to an erroneous response.
     */
    @Test
    public void testResumptionTokenFallback()
    {
        initializeContext();

        final Iterator<Element> extractorIter = testedObject.extractor.extract();

        // retrieve first element, memorize the resumptionToken
        extractorIter.next();

        // retrieve the second element using the resumptionToken
        extractorIter.next();

        // retrieve a third element using the datestamp of the second one as a fallback
        final Element fallbackElement = extractorIter.next();
        final String fallbackElementId = HtmlUtils.getString(fallbackElement, OaiPmhConstants.HEADER_IDENTIFIER);

        assertEquals(
            "Expected at least three elements to be extractable using a fallback URL.",
            FALLBACK_ELEMENT_ID,
            fallbackElementId);
    }


    /**
     * Tests if setting the "from" parameter results in extracting
     * a different element.
     */
    @Test
    public void testFromQueryParameter()
    {
        initializeContext();

        // set the "from" parameter
        final String fromParamKey = String.format(
                                        ParameterConstants.COMPOSITE_KEY,
                                        testedObject.getName(),
                                        OaiPmhParameterConstants.FROM_KEY);
        config.setParameter(fromParamKey, FROM_PARAMETER_VALUE);

        // apply changes
        testedObject.extractor.init(testedObject);

        // extract element
        final Iterator<Element> extractorIter = testedObject.extractor.extract();
        final Element fromElement = extractorIter.next();

        assertNotNull("Expected an element to be retrievable after setting the 'from' parameter.",
                      fromElement);
    }


    /**
     * Tests if setting the "until" parameter results in extracting
     * a different element.
     */
    @Test
    public void testUntilQueryParameter()
    {
        initializeContext();

        // set the "until" parameter
        final String untilParamKey = String.format(
                                         ParameterConstants.COMPOSITE_KEY,
                                         testedObject.getName(),
                                         OaiPmhParameterConstants.UNTIL_KEY);
        config.setParameter(untilParamKey, UNTIL_PARAMETER_VALUE);

        // apply changes
        testedObject.extractor.init(testedObject);

        // extract element
        final Iterator<Element> extractorIter = testedObject.extractor.extract();
        final Element fromElement = extractorIter.next();

        assertNotNull("Expected an element to be retrievable after setting the 'until' parameter.",
                      fromElement);
    }


    /**
     * Tests if setting the "from" and "until" parameters results in extracting
     * a different element.
     */
    @Test
    public void testFromAndUntilQueryParameter()
    {
        initializeContext();

        // set the "from" parameter
        final String fromParamKey = String.format(
                                        ParameterConstants.COMPOSITE_KEY,
                                        testedObject.getName(),
                                        OaiPmhParameterConstants.FROM_KEY);
        config.setParameter(fromParamKey, FROM_PARAMETER_VALUE);

        // set the "until" parameter
        final String untilParameterKey = String.format(
                                             ParameterConstants.COMPOSITE_KEY,
                                             testedObject.getName(),
                                             OaiPmhParameterConstants.UNTIL_KEY);
        config.setParameter(untilParameterKey, UNTIL_PARAMETER_VALUE);

        // apply changes
        testedObject.extractor.init(testedObject);

        // extract element
        final Iterator<Element> extractorIter = testedObject.extractor.extract();
        final Element fromElement = extractorIter.next();

        assertNotNull("Expected an element to be retrievable after setting both the 'until'- and 'from' parameters.",
                      fromElement);
    }


    /**
     * Tests if setting the "set" parameter results in extracting
     * a different element.
     */
    @Test
    public void testSetQueryParameter()
    {
        initializeContext();

        // set the "until" parameter
        final String setParamKey = String.format(
                                       ParameterConstants.COMPOSITE_KEY,
                                       testedObject.getName(),
                                       OaiPmhParameterConstants.SET_KEY);
        config.setParameter(setParamKey, SET_PARAMETER_VALUE);

        // apply changes
        testedObject.extractor.init(testedObject);

        // extract element
        final Iterator<Element> extractorIter = testedObject.extractor.extract();
        final Element fromElement = extractorIter.next();

        assertNotNull("Expected an element to be retrievable after setting the 'set' parameter.",
                      fromElement);
    }
}
