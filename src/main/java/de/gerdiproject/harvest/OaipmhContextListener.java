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
package de.gerdiproject.harvest;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebListener;

import de.gerdiproject.harvest.application.ContextListener;
import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.OaiPmhETL;
import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;


/**
 * This class initializes the OAI-PMH harvester service.
 *
 * @author Robin Weiss
 */
@WebListener
public class OaipmhContextListener extends ContextListener
{
    @Override
    protected List<? extends AbstractETL<?, ?>> createETLs()
    {
        return Arrays.asList(new OaiPmhETL());
    }


    @Override
    protected String getRepositoryName()
    {
        // this will be overridden by the OAI-PMH ETL that knows which repository to harvest
        // by checking its parameters
        return OaiPmhConstants.UNINITIALIZED_PROVIDER;
    }
}
