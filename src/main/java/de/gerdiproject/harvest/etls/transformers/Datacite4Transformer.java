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
package de.gerdiproject.harvest.etls.transformers;

import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Element;

import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.etls.transformers.utils.DataCite4ElementParser;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.RelatedIdentifier;
import de.gerdiproject.json.datacite.extension.generic.WebLink;
import de.gerdiproject.json.datacite.extension.generic.enums.WebLinkType;

/**
 * A transformer for records of the Datacite 4.1 metadata standard.<br>
 * https://schema.datacite.org/meta/kernel-4.1/
 *
 * @author Robin Weiss
 */
public class Datacite4Transformer extends AbstractOaiPmhRecordTransformer
{

    @Override
    protected void setDocumentFieldsFromRecord(DataCiteJson document, Element record)
    {
        final Element metadata = getMetadata(record);

        document.setPublisher(DataCite4ElementParser.getString(metadata, "publisher"));
        document.setLanguage(DataCite4ElementParser.getString(metadata, "language"));
        document.setVersion(DataCite4ElementParser.getString(metadata, "version"));
        document.addSizes(DataCite4ElementParser.getStrings(metadata, "sizes"));
        document.addFormats(DataCite4ElementParser.getStrings(metadata, "formats"));
        document.addCreators(DataCite4ElementParser.getObjects(metadata, "creators", DataCite4ElementParser::parseCreator));
        document.addContributors(DataCite4ElementParser.getObjects(metadata, "contributors", DataCite4ElementParser::parseContributor));
        document.addTitles(DataCite4ElementParser.getObjects(metadata, "titles", DataCite4ElementParser::parseTitle));
        document.setResourceType(DataCite4ElementParser.getObject(metadata, "resourceType", DataCite4ElementParser::parseResourceType));
        document.addDescriptions(DataCite4ElementParser.getObjects(metadata, "descriptions", DataCite4ElementParser::parseDescription));
        document.addSubjects(DataCite4ElementParser.getObjects(metadata, "subjects", DataCite4ElementParser::parseSubject));
        document.addAlternateIdentifiers(DataCite4ElementParser.getObjects(metadata, "alternateIdentifiers", DataCite4ElementParser::parseAlternateIdentifier));
        document.addRights(DataCite4ElementParser.getObjects(metadata, "rightsList", DataCite4ElementParser::parseRights));
        document.addDates(DataCite4ElementParser.getObjects(metadata, "dates", DataCite4ElementParser::parseDate));
        document.addGeoLocations(DataCite4ElementParser.getObjects(metadata, "geoLocations", DataCite4ElementParser::parseGeoLocation));
        document.addFundingReferences(DataCite4ElementParser.getObjects(metadata, "fundingReferences", DataCite4ElementParser::parseFundingReference));

        final Identifier identifier = DataCite4ElementParser.getObject(metadata, "identifier", DataCite4ElementParser::parseIdentifier);
        document.setIdentifier(identifier);

        List<RelatedIdentifier> relatedIdentifiers = DataCite4ElementParser.getObjects(metadata, "relatedIdentifiers", DataCite4ElementParser::parseRelatedIdentifier);
        document.addRelatedIdentifiers(relatedIdentifiers);

        document.addWebLinks(createWebLinks(identifier, relatedIdentifiers));

        try {
            String publicationYear = DataCite4ElementParser.getString(metadata, "publicationYear");
            document.setPublicationYear(Integer.parseInt(publicationYear));
        } catch (NumberFormatException | NullPointerException e) {
            document.setPublicationYear(null);
        }
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
        final List<WebLink> webLinks = new LinkedList<>();

        // get related URLs
        if (relatedIdentifiers != null) {
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
        }

        // convert identifier to view url
        if (identifier != null) {
            final String identifierURL = identifier.getValue().startsWith("http")
                                         ? identifier.getValue()
                                         : String.format(OaiPmhConstants.DOI_URL, identifier.getValue());

            final WebLink viewLink = new WebLink(identifierURL);
            viewLink.setType(WebLinkType.ViewURL);
            viewLink.setName("Resource");
            webLinks.add(viewLink);
        }

        return webLinks;
    }
}
