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
package de.gerdiproject.harvest.oaipmh.constants;

/**
 * A static collection of constant parameters for configuring the DataCite3 strategy.
 * 
 * @author Jan Frömberg
 *
 */
public class DataCiteStrategyConstants {
	
	public static final String RECORD_STATUS = "status";
	public static final String RECORD_STATUS_DEL = "deleted";
	public static final String RECORD_HEADER = "header";
	public static final String RECORD_METADATA = "metadata";
	
	public static final String IDENTIFIER = "identifier";
	public static final String DATESTAMP = "datestamp";
	public static final String PUB_YEAR = "publicationYear";
	public static final String DOC_TITLE = "title";
	public static final String DOC_CREATORS = "creators";
	public static final String DOC_CREATORNAME = "creatorName";
	
	public static final String CONTRIBUTORS = "contributors";
	public static final String CONTRIB_TYPE = "contributorType";
	public static final String PUBLISHER = "publisher";
	public static final String SUBJECT = "subject";
	public static final String SUBJECT_SCHEME = "subjectScheme";
	public static final String METADATA_DATE = "date";
	public static final String METADATA_DATETYPE = "dateType";
	public static final String LANG = "language";
	public static final String RES_TYPE = "resourceType";
	public static final String RES_TYPE_GENERAL = "resourceTypeGeneral";
	
	public static final String REL_IDENTIFIERS = "relatedIdentifiers";
	public static final String REL_IDENT_TYPE = "relatedIdentifierType";
	public static final String REL_TYPE = "relationType";
	
	public static final String SIZE = "size";
	public static final String METADATA_FORMATS = "formats";
	public static final String RIGHTS_LIST = "rightsList";
	public static final String RIGHTS_URI = "rightsURI";
	public static final String DESCRIPTIONS = "descriptions";
	public static final String DESC_TYPE = "descriptionsType";
	
	public static final String GEOLOCS = "geoLocations";
	public static final String GEOLOC_BOX = "geolocationbox";
	public static final String GEOLOC_POINT = "geolocationpoint";
	public static final String GEOLOC_PLACE = "geolocationplace";
	
    /**
     * Private Constructor, because this is a static class.
     */
    private DataCiteStrategyConstants()
    {
    }
}
