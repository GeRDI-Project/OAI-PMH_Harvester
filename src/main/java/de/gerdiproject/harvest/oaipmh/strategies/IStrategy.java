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

import org.jsoup.nodes.Element;

import de.gerdiproject.harvest.IDocument;

/**
 * This interface represents a strategy for harvesting a document from an OAI-PMH record.
 *
 * @author Robin Weiss
 */
public interface IStrategy
{
	/**
	 * Reads an HTML record from an OAI-PMH record list and generates a single document
	 * out of it.
	 * 
	 * @param record the HTML element that represents the OAI-PMH record
	 * 
	 * @return a searchable document
	 */
    IDocument harvestRecord(Element record);
}
