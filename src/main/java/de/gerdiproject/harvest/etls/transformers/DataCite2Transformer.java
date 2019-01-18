/**
 * Copyright © 2019 Robin Weiss (http://www.gerdi-project.de)
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

import java.util.Arrays;
import java.util.List;

import org.jsoup.nodes.Element;

import de.gerdiproject.harvest.etls.transformers.constants.DataCiteConstants;
import de.gerdiproject.json.datacite.Contributor;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.FundingReference;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.RelatedIdentifier;
import de.gerdiproject.json.datacite.enums.FunderIdentifierType;
import de.gerdiproject.json.datacite.enums.ResourceTypeGeneral;
import de.gerdiproject.json.datacite.nested.FunderIdentifier;
import de.gerdiproject.json.datacite.nested.NameIdentifier;
import de.gerdiproject.json.geo.Point;

/**
 * A transformer for the Datacite2 metadata standard.<br>
 * https://schema.datacite.org/meta/kernel-2.2/doc/DataCite-MetadataKernel_v2.2.pdf
 *
 * @author Robin Weiss
 */
public class DataCite2Transformer extends AbstractDataCiteTransformer
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
        document.addDates(getObjects(metadata, DataCiteConstants.DATES, this::parseDate));
        document.addWebLinks(createWebLinks(identifier, relatedIdentifiers));

        // in DataCite 2 and earlier, there is only one "rights" object
        document.addRights(Arrays.asList(getObject(metadata, DataCiteConstants.RIGHTS, this::parseRights)));

        // in DataCite 3 and earlier, fundingReferences are contributors with type "funder"
        document.addFundingReferences(getObjects(metadata, DataCiteConstants.CONTRIBUTORS, this::parseFundingReference));
    }


    @Override @SuppressWarnings("CPD-OFF")
    protected ResourceTypeGeneral parseResourceTypeGeneral(Element ele)
    {
        final String rawResType = getAttribute(ele, DataCiteConstants.RESOURCE_TYPE_GENERAL);

        if (rawResType != null && rawResType.equals(DataCiteConstants.RESOURCE_TYPE_GENERAL_FILM))
            return ResourceTypeGeneral.Audiovisual;
        else
            return super.parseResourceTypeGeneral(ele);
    }


    @Override
    protected Contributor parseContributor(Element ele)
    {
        final String contributorType = getAttribute(ele, DataCiteConstants.CONTRIBUTOR_TYPE);

        // since DataCite 4, "funder" is no longer a contributor type!
        return contributorType.equals(DataCiteConstants.CONTRIBUTOR_TYPE_FUNDER)
               ? null
               : super.parseContributor(ele);
    }


    @Override
    protected FundingReference parseFundingReference(Element ele)
    {
        // in DataCite 3 and earlier versions, fundingReferences are contributors with type "funder"
        final String contributorType = getAttribute(ele, DataCiteConstants.CONTRIBUTOR_TYPE);

        FundingReference funder = null;

        if (contributorType.equals(DataCiteConstants.CONTRIBUTOR_TYPE_FUNDER))
            funder = contributorToFunder(parseContributor(ele));

        return funder;
    }


    @Override
    protected Point parseGeoLocationPoint(Element ele)
    {
        // this function is never called, because there are no GeoLocations
        // in DataCite 2 and earlier versions
        return null;
    }


    @Override
    protected double[] parseGeoLocationBox(Element ele)
    {
        // this function is never called, because there are no GeoLocations
        // in DataCite 2 and earlier versions
        return null;
    }


    /**
     * This static method converts a contributor to a {@linkplain FundingReference}.
     * It is used to make DataCite 3 and earlier versions compatible with DataCite 4.
     *
     * @param funderContributor a contributor with type "funder"
     *
     * @return a makeshift {@linkplain FundingReference}
     */
    public static FundingReference contributorToFunder(Contributor funderContributor)
    {
        FunderIdentifier funderIdentifier = null;

        // convert nameIdentifier to funder identifier
        if (funderContributor.getNameIdentifiers() != null) {
            final NameIdentifier nameIdentifier = funderContributor.getNameIdentifiers().iterator().next();
            FunderIdentifierType funderIdentifierType;

            try {
                funderIdentifierType = FunderIdentifierType.valueOf(nameIdentifier.getNameIdentifierScheme());
            } catch (IllegalArgumentException e) {
                funderIdentifierType = FunderIdentifierType.Other;
            }

            funderIdentifier = new FunderIdentifier(nameIdentifier.getValue(), funderIdentifierType);
        }

        final FundingReference fundingReference = new FundingReference(funderContributor.getName().getValue());
        fundingReference.setFunderIdentifier(funderIdentifier);

        return fundingReference;
    }
}
