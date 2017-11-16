/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
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
package de.gerdiproject.harvest;

import de.gerdiproject.harvest.config.parameters.AbstractParameter;
import de.gerdiproject.harvest.config.parameters.StringParameter;
import de.gerdiproject.harvest.harvester.OaipmhHarvester;

import javax.servlet.annotation.WebListener;

import java.util.Arrays;
import java.util.List;

/**
 * This class initializes the ENA harvester and a logger
 *
 * @author Jan Frömberg
 */
@WebListener
public class OaipmhContextListener extends ContextListener<OaipmhHarvester>
{
	private final static String ENTRY_DEFAULT_FROM = "&from=2017-01-01";
    private final static String ENTRY_DEFAULT_TO = "&until=2017-12-31";
    public final static String PROPERTY_FROM = "from"; //e.g. 2017-11-01
    public final static String PROPERTY_TO = "until";
	
    @Override
    protected List<AbstractParameter<?>> getHarvesterSpecificParameters()
    {
        //StringParameter versionParam = new StringParameter("version", "v1");
        //StringParameter languageParam = new StringParameter("language", "de");
        StringParameter propertyFrom = new StringParameter(PROPERTY_FROM, ENTRY_DEFAULT_FROM);
        StringParameter propertyTo = new StringParameter(PROPERTY_TO, ENTRY_DEFAULT_TO);

        return Arrays.asList(propertyFrom, propertyTo);
    }
}
