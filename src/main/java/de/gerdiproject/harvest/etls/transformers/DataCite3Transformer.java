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

import java.util.List;

import org.jsoup.nodes.Element;

import de.gerdiproject.harvest.etls.transformers.constants.DataCiteConstants;
import de.gerdiproject.json.datacite.Contributor;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.FundingReference;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.RelatedIdentifier;
import de.gerdiproject.json.datacite.enums.FunderIdentifierType;
import de.gerdiproject.json.datacite.nested.FunderIdentifier;
import de.gerdiproject.json.datacite.nested.NameIdentifier;
import de.gerdiproject.json.datacite.nested.PersonName;
import de.gerdiproject.json.geo.Point;

/**
 * A transformer for the Datacite3 metadata standard.<br>
 * https://schema.datacite.org/meta/kernel-3.0/doc/DataCite-MetadataKernel_v3.0.pdf
 *
 * @author Robin Weiss
 */
public class DataCite3Transformer extends AbstractDataCiteTransformer
{
    @Override
    @SuppressWarnings("CPD-START") // we want to keep duplicates here, because there will be slight changes in other transformers
    protected void setDocumentFieldsFromRecord(DataCiteJson document, Element record)
    {
        final Element metadata = getMetadata(record);

        final Identifier identifier = getObject(metadata, DataCiteConstants.IDENTIFIER, this::parseIdentifier);
        document.setIdentifier(identifier);

        final List<RelatedIdentifier> relatedIdentifiers = getObjects(metadata, DataCiteConstants.RELATED_IDENTIFIERS, this::parseRelatedIdentifier);
        document.addRelatedIdentifiers(relatedIdentifiers);

        document.setPublisher(getString(metadata, DataCiteConstants.PUBLISHER));
        document.setLanguage(getString(metadata, DataCiteConstants.LANGUAGE));
        document.setVersion(getString(metadata, DataCiteConstants.VERSION));
        document.setPublicationYear(parsePublicationYear(metadata));
        document.addSizes(getStrings(metadata, DataCiteConstants.SIZES));
        document.addFormats(getStrings(metadata, DataCiteConstants.FORMATS));
        document.setResourceType(getObject(metadata, DataCiteConstants.RESOURCE_TYPE, this::parseResourceType));
        document.addCreators(getObjects(metadata, DataCiteConstants.CREATORS, this::parseCreator));
        document.addContributors(getObjects(metadata, DataCiteConstants.CONTRIBUTORS, this::parseContributor));
        document.addTitles(getObjects(metadata, DataCiteConstants.TITLES, this::parseTitle));
        document.addDescriptions(getObjects(metadata, DataCiteConstants.DESCRIPTIONS, this::parseDescription));
        document.addSubjects(getObjects(metadata, DataCiteConstants.SUBJECTS, this::parseSubject));
        document.addAlternateIdentifiers(getObjects(metadata, DataCiteConstants.ALTERNATE_IDENTIFIERS, this::parseAlternateIdentifier));
        document.addRights(getObjects(metadata, DataCiteConstants.RIGHTS_LIST, this::parseRights));
        document.addDates(getObjects(metadata, DataCiteConstants.DATES, this::parseDate));
        document.addGeoLocations(getObjects(metadata, DataCiteConstants.GEO_LOCATIONS, this::parseGeoLocation));
        document.addWebLinks(createWebLinks(identifier, relatedIdentifiers));

        // in DataCite 3.0,fundingReferences are contributors with type "funder"
        document.addFundingReferences(getObjects(metadata, DataCiteConstants.CONTRIBUTORS, this::parseFundingReference));
    }


    @Override @SuppressWarnings("CPD-OFF")
    protected Point parseGeoLocationPoint(Element ele)
    {
        final String[] values = ele.text().split(" ");

        // since DataCite 4.1, longitude and latitude have switched
        final double latitude = Double.parseDouble(values[0]);
        final double longitude = Double.parseDouble(values[1]);

        if (values.length == 3) {
            final double elevation = Double.parseDouble(values[2]);
            return new Point(longitude, latitude, elevation);
        } else
            return new Point(longitude, latitude);
    }


    @Override
    protected double[] parseGeoLocationBox(Element ele)
    {
        final String[] values = ele.text().split(" ");
        final double[] boxParameters = new double[4];

        // since DataCite 4.1, the order of box parameters has changed
        try {
            boxParameters[0] = Double.parseDouble(values[1]);
            boxParameters[1] = Double.parseDouble(values[3]);
            boxParameters[2] = Double.parseDouble(values[0]);
            boxParameters[3] = Double.parseDouble(values[2]);
        } catch (NumberFormatException e) {
            return null;
        }

        return boxParameters;
    }


    @Override
    protected Contributor parseContributor(Element ele)
    {
        final String contributorType = getAttribute(ele, DataCiteConstants.CONTRIBUTOR_TYPE);

        // since DataCite 4.1, "funder" is no longer a contributor type!
        return contributorType.equals(DataCiteConstants.CONTRIBUTOR_TYPE_FUNDER)
               ? null
               : super.parseContributor(ele);
    }


    @Override
    protected FundingReference parseFundingReference(Element ele)
    {
        // in DataCite 3.0,fundingReferences are contributors with type "funder"
        final String contributorType = getAttribute(ele, DataCiteConstants.CONTRIBUTOR_TYPE);

        if (!contributorType.equals(DataCiteConstants.CONTRIBUTOR_TYPE_FUNDER))
            return null;

        final PersonName contributorName = parsePersonName(ele.selectFirst(DataCiteConstants.CONTRIBUTOR_NAME));
        final NameIdentifier nameIdentifier = parseNameIdentifier(ele.selectFirst(DataCiteConstants.NAME_IDENTIFIER));
        FunderIdentifier funderIdentifier = null;

        // convert nameIdentifier to funder identifier
        if (nameIdentifier != null) {
            FunderIdentifierType funderIdentifierType;

            try {
                funderIdentifierType = FunderIdentifierType.valueOf(nameIdentifier.getNameIdentifierScheme());
            } catch (IllegalArgumentException e) {
                funderIdentifierType = FunderIdentifierType.Other;
            }

            funderIdentifier = new FunderIdentifier(nameIdentifier.getValue(), funderIdentifierType);
        }

        final FundingReference fundingReference = new FundingReference(contributorName.getValue());
        fundingReference.setFunderIdentifier(funderIdentifier);
        return fundingReference;
    }
}
