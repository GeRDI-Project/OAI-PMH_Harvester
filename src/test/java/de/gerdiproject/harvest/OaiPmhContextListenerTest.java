/**
 * Copyright © 2019 Robin Weiss (http://www.gerdi-project.de)
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
package de.gerdiproject.harvest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;

import org.jsoup.nodes.Element;
import org.junit.Test;

import de.gerdiproject.harvest.application.AbstractContextListenerTest;
import de.gerdiproject.harvest.application.ContextListenerTestWrapper;
import de.gerdiproject.harvest.application.MainContextUtils;
import de.gerdiproject.harvest.application.events.ServiceInitializedEvent;
import de.gerdiproject.harvest.etls.AbstractIteratorETL;
import de.gerdiproject.harvest.etls.OaiPmhETL;
import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.etls.events.GetRepositoryNameEvent;
import de.gerdiproject.harvest.event.EventSystem;
import de.gerdiproject.harvest.utils.data.constants.DataOperationConstants;
import de.gerdiproject.harvest.utils.file.FileUtils;

/**
 * This class provides Unit Tests for the {@linkplain OaiPmhContextListener}.
 *
 * @author Robin Weiss
 */
public class OaiPmhContextListenerTest extends AbstractContextListenerTest<OaiPmhContextListener>
{
    private static final int INIT_TIMEOUT = 5000;


    /**
     * Tests if initializing the OaiPmh-Harvester without a valid
     * host URL will return a fallback repository name, if requested.
     */
    @Test
    public void testUnassignedRepositoryName()
    {
        // initialize MainContext
        waitForEvent(ServiceInitializedEvent.class,
                     INIT_TIMEOUT,
                     () -> testedObject.contextInitialized(null));

        // retrieve fallback repository name
        final String repositoryName = EventSystem.sendSynchronousEvent(new GetRepositoryNameEvent());

        assertEquals("The GetRepositoryNameEvent should return: " + OaiPmhConstants.UNKNOWN_PROVIDER,
                     OaiPmhConstants.UNKNOWN_PROVIDER,
                     repositoryName
                    );
    }


    /**
     * Tests if initializing the OaiPmh-Harvester with a valid
     * host URL will return a repository name by parsing an Identify-request.
     */
    @Test
    public void testAssignedRepositoryName()
    {
        // set up mocked HTTP responses
        final File httpResourceFolder = getResource("mockedHttpResponses");

        if (httpResourceFolder != null) {
            final File httpCacheFolder = new File(
                MainContextUtils.getCacheDirectory(getClass()),
                DataOperationConstants.CACHE_FOLDER_PATH);

            FileUtils.copyFile(httpResourceFolder, httpCacheFolder);
        }

        // set up mocked configuration
        final ContextListenerTestWrapper<? extends AbstractIteratorETL<Element, ?>> contextInitializer =
            new ContextListenerTestWrapper<>(testedObject, () -> new OaiPmhETL());

        final File configFileResource = getResource("config.json");

        if (configFileResource != null && configFileResource.exists()) {
            final File configFile = contextInitializer.getConfigFile();
            FileUtils.copyFile(configFileResource, configFile);
        }

        // initialize MainContext
        waitForEvent(ServiceInitializedEvent.class,
                     INIT_TIMEOUT,
                     () -> testedObject.contextInitialized(null));

        // retrieve valid repository name
        final String repositoryName = EventSystem.sendSynchronousEvent(new GetRepositoryNameEvent());

        assertNotEquals("The GetRepositoryNameEvent should not return: " + OaiPmhConstants.UNKNOWN_PROVIDER,
                        OaiPmhConstants.UNKNOWN_PROVIDER,
                        repositoryName
                       );
    }
}
