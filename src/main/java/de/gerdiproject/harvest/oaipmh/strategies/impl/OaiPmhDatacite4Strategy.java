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

import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Element;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.oaipmh.constants.DataCiteStrategyConstants;
import de.gerdiproject.harvest.oaipmh.constants.OaiPmhConstants;
import de.gerdiproject.harvest.oaipmh.strategies.IStrategy;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.RelatedIdentifier;
import de.gerdiproject.json.datacite.extension.WebLink;
import de.gerdiproject.json.datacite.extension.enums.WebLinkType;

/**
 * A harvesting strategy for the Datacite 4.1 metadata standard.<br>
 * https://schema.datacite.org/meta/kernel-4.1/
 *
 * @author Robin Weiss
 *
 */
public class OaiPmhDatacite4Strategy implements IStrategy
{
    @Override
    public IDocument harvestRecord(Element record)
    {
        Boolean isRecordDeleted = record.children()
                                  .first()
                                  .attr(DataCiteStrategyConstants.RECORD_STATUS)
                                  .equals(DataCiteStrategyConstants.RECORD_STATUS_DEL);

        // check if entry/record is "deleted" from repository
        if (isRecordDeleted)
            return null;

        // get header and meta data for each record
        final Element header = record.select(DataCiteStrategyConstants.RECORD_HEADER).first();

        // get identifier and date stamp
        final String repositoryIdentifier = DataCite4ElementParser.getString(header, DataCiteStrategyConstants.IDENTIFIER);

        final DataCiteJson document = new DataCiteJson(repositoryIdentifier);
        document.setRepositoryIdentifier(repositoryIdentifier);

        Element metadata = record.select(DataCiteStrategyConstants.RECORD_METADATA).first();
        document.setPublisher(DataCite4ElementParser.getString(metadata, "publisher"));
        document.setLanguage(DataCite4ElementParser.getString(metadata, "language"));
        document.setVersion(DataCite4ElementParser.getString(metadata, "version"));
        document.setSizes(DataCite4ElementParser.getStrings(metadata, "sizes"));
        document.setFormats(DataCite4ElementParser.getStrings(metadata, "formats"));
        document.setCreators(DataCite4ElementParser.getObjects(metadata, "creators", DataCite4ElementParser::parseCreator));
        document.setContributors(DataCite4ElementParser.getObjects(metadata, "contributors", DataCite4ElementParser::parseContributor));
        document.setTitles(DataCite4ElementParser.getObjects(metadata, "titles", DataCite4ElementParser::parseTitle));
        document.setResourceType(DataCite4ElementParser.getObject(metadata, "resourceType", DataCite4ElementParser::parseResourceType));
        document.setDescriptions(DataCite4ElementParser.getObjects(metadata, "descriptions", DataCite4ElementParser::parseDescription));
        document.setSubjects(DataCite4ElementParser.getObjects(metadata, "subjects", DataCite4ElementParser::parseSubject));
        document.setAlternateIdentifiers(DataCite4ElementParser.getObjects(metadata, "alternateIdentifiers", DataCite4ElementParser::parseAlternateIdentifier));
        document.setRightsList(DataCite4ElementParser.getObjects(metadata, "rightsList", DataCite4ElementParser::parseRights));
        document.setDates(DataCite4ElementParser.getObjects(metadata, "dates", DataCite4ElementParser::parseDate));
        document.setGeoLocations(DataCite4ElementParser.getObjects(metadata, "geoLocations", DataCite4ElementParser::parseGeoLocation));
        document.setFundingReferences(DataCite4ElementParser.getObjects(metadata, "fundingReferences", DataCite4ElementParser::parseFundingReference));

        Identifier identifier = DataCite4ElementParser.getObject(metadata, "identifier", DataCite4ElementParser::parseIdentifier);
        document.setIdentifier(identifier);

        List<RelatedIdentifier> relatedIdentifiers = DataCite4ElementParser.getObjects(metadata, "relatedIdentifiers", DataCite4ElementParser::parseRelatedIdentifier);
        document.setRelatedIdentifiers(relatedIdentifiers);

        document.setWebLinks(createWebLinks(identifier, relatedIdentifiers));

        try {
            String publicationYear = DataCite4ElementParser.getString(metadata, "publicationYear");
            document.setPublicationYear(Short.parseShort(publicationYear));
        } catch (NumberFormatException | NullPointerException e) {
            document.setPublicationYear((short)0);
        }

        return document;
    }


    /**
     * Retrieves {@linkplain WebLink}s from DataCite identifiers.
     *
     * @param identifier the identifier of a {@linkplain DataCiteJson}
     * @param relatedIdentifiers related identifiers of a {@linkplain DataCiteJson}
     *
     * @return a list of {@linkplain WebLink}s
     */
    private List<WebLink> createWebLinks(Identifier identifier, List<RelatedIdentifier> relatedIdentifiers)
    {
        List<WebLink> webLinks = new LinkedList<>();

        // get related URLs
        for (RelatedIdentifier ri : relatedIdentifiers) {
            final String relatedUrl;

            switch (ri.getType()) {
                case DOI:
                    relatedUrl = ri.getValue().startsWith("http")
                                 ? ri.getValue()
                                 : String.format(OaiPmhConstants.DOI_URL, ri.getValue());
                    break;

                case URL:
                    relatedUrl = ri.getValue();
                    break;

                default:
                    relatedUrl = null;
            }

            if (relatedUrl != null) {
                final WebLink relatedLink = new WebLink(relatedUrl);
                relatedLink.setType(WebLinkType.Related);
                relatedLink.setName(ri.getRelationType().toString());
                webLinks.add(relatedLink);
            }
        }

        // convert identifier to view url
        final String identifierURL = identifier.getValue().startsWith("http")
                                     ? identifier.getValue()
                                     : String.format(OaiPmhConstants.DOI_URL, identifier.getValue());

        final WebLink viewLink = new WebLink(identifierURL);
        viewLink.setType(WebLinkType.ViewURL);
        viewLink.setName("Resource");
        webLinks.add(viewLink);

        return webLinks;
    }
}
