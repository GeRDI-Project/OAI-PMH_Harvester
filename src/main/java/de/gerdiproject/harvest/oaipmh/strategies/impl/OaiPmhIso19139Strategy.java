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
package de.gerdiproject.harvest.oaipmh.strategies.impl;

import org.jsoup.nodes.Element;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.oaipmh.constants.Iso19139StrategyConstants;
import de.gerdiproject.harvest.oaipmh.strategies.IStrategy;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Identifier;

/**
 * A harvesting strategy for the ISO 19139 metadata standard.<br>
 *  https://www.iso.org/standard/32557.html
 *
 * @author Tobias Weber
 *
 */
public class OaiPmhIso19139Strategy implements IStrategy
{
    @Override
    public IDocument harvestRecord(Element record)
    {
        Boolean isRecordDeleted = record.children()
                                  .first()
                                  .attr(Iso19139StrategyConstants.RECORD_STATUS)
                                  .equals(Iso19139StrategyConstants.RECORD_STATUS_DEL);

        // check if entry/record is "deleted" from repository
        if (isRecordDeleted)
            return null;

        //@todo: delete?
        // get header and meta data for each record
        //final Element header = record.select(Iso19139StrategyConstants.RECORD_HEADER).first();

        // get identifier and date stamp
        final String repositoryIdentifier = "TODO";

        final DataCiteJson document = new DataCiteJson(repositoryIdentifier);
        document.setRepositoryIdentifier(repositoryIdentifier);

        Element metadata = record.select(Iso19139StrategyConstants.RECORD_METADATA).first();

        /* Category 0 - Minimal viable harvester */
        Identifier identifier = DataCite4ElementParser.getObject(metadata, "identifier", DataCite4ElementParser::parseIdentifier);
        document.setIdentifier(identifier);
        document.setTitles(DataCite4ElementParser.getObjects(metadata, "titles", DataCite4ElementParser::parseTitle));
        document.setPublisher(DataCite4ElementParser.getString(metadata, "publisher"));
        document.setResourceType(DataCite4ElementParser.getObject(metadata, "resourceType", DataCite4ElementParser::parseResourceType));
        document.setDescriptions(DataCite4ElementParser.getObjects(metadata, "descriptions", DataCite4ElementParser::parseDescription));
        document.setGeoLocations(DataCite4ElementParser.getObjects(metadata, "geoLocations", DataCite4ElementParser::parseGeoLocation));


        /* Category 1 - To be done until 0.4 finishes */
        document.setCreators(DataCite4ElementParser.getObjects(metadata, "creators", DataCite4ElementParser::parseCreator));

        try {
            String publicationYear = DataCite4ElementParser.getString(metadata, "publicationYear");
            document.setPublicationYear(Short.parseShort(publicationYear));
        } catch (NumberFormatException | NullPointerException e) {
            document.setPublicationYear((short)0);
        }

        document.setDates(DataCite4ElementParser.getObjects(metadata, "dates", DataCite4ElementParser::parseDate));
        //document.setWebLinks(createWebLinks(identifier, relatedIdentifiers));

        /* Category 2 Postpone until needed
        document.setLanguage(DataCite4ElementParser.getString(metadata, "language"));
        document.setVersion(DataCite4ElementParser.getString(metadata, "version"));
        document.setSizes(DataCite4ElementParser.getStrings(metadata, "sizes"));
        document.setFormats(DataCite4ElementParser.getStrings(metadata, "formats"));
        document.setContributors(DataCite4ElementParser.getObjects(metadata, "contributors", DataCite4ElementParser::parseContributor));
        document.setSubjects(DataCite4ElementParser.getObjects(metadata, "subjects", DataCite4ElementParser::parseSubject));
        document.setAlternateIdentifiers(DataCite4ElementParser.getObjects(metadata, "alternateIdentifiers", DataCite4ElementParser::parseAlternateIdentifier));
        document.setRightsList(DataCite4ElementParser.getObjects(metadata, "rightsList", DataCite4ElementParser::parseRights));
        document.setFundingReferences(DataCite4ElementParser.getObjects(metadata, "fundingReferences", DataCite4ElementParser::parseFundingReference));
        List<RelatedIdentifier> relatedIdentifiers = DataCite4ElementParser.getObjects(metadata, "relatedIdentifiers", DataCite4ElementParser::parseRelatedIdentifier);
        document.setRelatedIdentifiers(relatedIdentifiers);
        */

        return document;

    }
}
