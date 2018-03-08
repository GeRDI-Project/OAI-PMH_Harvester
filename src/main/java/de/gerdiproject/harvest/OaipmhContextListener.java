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

import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.harvester.OaipmhHarvester;
import de.gerdiproject.harvest.oaipmh.constants.OaiPmhParameterConstants;

import javax.servlet.annotation.WebListener;

import java.util.Arrays;
import java.util.List;

/**
 * This class initializes the OAI-PMH harvester and a logger.
 *
 * @author Jan Frömberg
 */
@WebListener
public class OaipmhContextListener extends ContextListener<OaipmhHarvester>
{
    @Override
    protected List<AbstractParameter<?>> getHarvesterSpecificParameters()
    {
        StringParameter propertyFrom = new StringParameter(
            OaiPmhParameterConstants.DATE_FROM_KEY,
            OaiPmhParameterConstants.DATE_FROM_DEFAULT);

        StringParameter propertyTo = new StringParameter(
            OaiPmhParameterConstants.DATE_TO_KEY,
            OaiPmhParameterConstants.DATE_TO_DEFAULT);

        StringParameter propertyHostUrl = new StringParameter(
            OaiPmhParameterConstants.HOST_URL_KEY,
            OaiPmhParameterConstants.HOST_URL_DEFAULT);

        StringParameter propertyMetadataPrefix = new StringParameter(
            OaiPmhParameterConstants.METADATA_PREFIX_KEY,
            OaiPmhParameterConstants.METADATA_PREFIX_DEFAULT);

        return Arrays.asList(
                   propertyFrom,
                   propertyTo,
                   propertyHostUrl,
                   propertyMetadataPrefix);
    }
}
