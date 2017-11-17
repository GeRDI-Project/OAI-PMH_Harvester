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
package de.gerdiproject.harvest.oaipmh.strategies;

import de.gerdiproject.harvest.oaipmh.strategies.impl.OaiPmhDatacite3Strategy;
import de.gerdiproject.harvest.oaipmh.strategies.impl.OaiPmhDublinCoreStrategy;

/**
 * This static factory generates strategies for harvesting OAI-PMH records.
 *
 * @author Robin Weiss
 */
public class OaiPmhStrategyFactory
{
    /**
     * Private Constructor, because this is a static class.
     */
    private OaiPmhStrategyFactory()
    {

    }

    /**
     * Creates a strategy to harvest OAI-PMH with a specified metadataPrefix.
     *
     * @param metadataPrefix the type of the records that are to be harvested.
     *
     * @return a strategy to harvest OAI-PMH with a specified metadataPrefix,
     * or null if no strategy with the corresponding metadataPrefix exists
     */
    public static IStrategy createStrategy(String metadataPrefix)
    {
        String strategyName = metadataPrefix.toLowerCase();
        IStrategy strategy;

        switch (strategyName) {
            case "datacite3":
                strategy = new OaiPmhDatacite3Strategy();
                break;

            case "oai_dc":
                strategy = new OaiPmhDublinCoreStrategy();
                break;

            //case "ore":
            //case "mets":
            default:
                strategy = null;
        }

        return strategy;
    }
}
