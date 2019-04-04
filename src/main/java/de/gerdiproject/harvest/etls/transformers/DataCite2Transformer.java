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
import java.util.LinkedList;
import java.util.List;

import org.jsoup.nodes.Element;

import de.gerdiproject.harvest.etls.constants.OaiPmhConstants;
import de.gerdiproject.harvest.etls.transformers.constants.DataCiteConstants;
import de.gerdiproject.harvest.utils.HtmlUtils;
import de.gerdiproject.json.datacite.AlternateIdentifier;
import de.gerdiproject.json.datacite.Contributor;
import de.gerdiproject.json.datacite.Creator;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Date;
import de.gerdiproject.json.datacite.DateRange;
import de.gerdiproject.json.datacite.Description;
import de.gerdiproject.json.datacite.FundingReference;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.RelatedIdentifier;
import de.gerdiproject.json.datacite.ResourceType;
import de.gerdiproject.json.datacite.Rights;
import de.gerdiproject.json.datacite.Subject;
import de.gerdiproject.json.datacite.Title;
import de.gerdiproject.json.datacite.abstr.AbstractDate;
import de.gerdiproject.json.datacite.constants.DataCiteDateConstants;
import de.gerdiproject.json.datacite.enums.ContributorType;
import de.gerdiproject.json.datacite.enums.DateType;
import de.gerdiproject.json.datacite.enums.DescriptionType;
import de.gerdiproject.json.datacite.enums.FunderIdentifierType;
import de.gerdiproject.json.datacite.enums.RelatedIdentifierType;
import de.gerdiproject.json.datacite.enums.RelationType;
import de.gerdiproject.json.datacite.enums.ResourceTypeGeneral;
import de.gerdiproject.json.datacite.enums.TitleType;
import de.gerdiproject.json.datacite.extension.generic.WebLink;
import de.gerdiproject.json.datacite.extension.generic.enums.WebLinkType;
import de.gerdiproject.json.datacite.nested.FunderIdentifier;
import de.gerdiproject.json.datacite.nested.NameIdentifier;
import de.gerdiproject.json.datacite.nested.PersonName;

/**
 * A transformer for the Datacite2 metadata standard.<br>
 * https://schema.datacite.org/meta/kernel-2.2/doc/DataCite-MetadataKernel_v2.2.pdf
 *
 * @author Robin Weiss
 */
public class DataCite2Transformer extends AbstractOaiPmhRecordTransformer
{
    @Override
    //@SuppressWarnings("CPD-START") // we want to keep duplicates here, because there will be slight changes in other transformers
    protected void setDocumentFieldsFromRecord(DataCiteJson document, Element record)
    {
        final Element metadata = getMetadata(record);

        final Identifier identifier = HtmlUtils.getObject(metadata, DataCiteConstants.IDENTIFIER, this::parseIdentifier);
        document.setIdentifier(identifier);

        final List<RelatedIdentifier> relatedIdentifiers = HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.RELATED_IDENTIFIERS, this::parseRelatedIdentifier);
        document.addRelatedIdentifiers(relatedIdentifiers);

        document.setPublisher(HtmlUtils.getString(metadata, DataCiteConstants.PUBLISHER));
        document.setLanguage(HtmlUtils.getString(metadata, DataCiteConstants.LANGUAGE));
        document.setVersion(HtmlUtils.getString(metadata, DataCiteConstants.VERSION));
        document.setPublicationYear(parsePublicationYear(metadata));
        document.addSizes(HtmlUtils.getStringsFromParent(metadata, DataCiteConstants.SIZES));
        document.addFormats(HtmlUtils.getStringsFromParent(metadata, DataCiteConstants.FORMATS));
        document.setResourceType(HtmlUtils.getObject(metadata, DataCiteConstants.RESOURCE_TYPE, this::parseResourceType));
        document.addCreators(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.CREATORS, this::parseCreator));
        document.addContributors(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.CONTRIBUTORS, this::parseContributor));
        document.addTitles(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.TITLES, this::parseTitle));
        document.addDescriptions(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.DESCRIPTIONS, this::parseDescription));
        document.addSubjects(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.SUBJECTS, this::parseSubject));
        document.addAlternateIdentifiers(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.ALTERNATE_IDENTIFIERS, this::parseAlternateIdentifier));
        document.addDates(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.DATES, this::parseDate));
        document.addWebLinks(createWebLinks(identifier, relatedIdentifiers));

        // to be compliant to DC 4.1, convert the single rights-object to a rightsList
        document.addRights(Arrays.asList(HtmlUtils.getObject(metadata, DataCiteConstants.RIGHTS, this::parseRights)));

        // to be compliant to DC 4.1, convert contributors with type "funder" to fundingReferences
        document.addFundingReferences(HtmlUtils.getObjectsFromParent(metadata, DataCiteConstants.CONTRIBUTORS, this::parseFundingReference));
    }


    /**
     * Retrieves a {@linkplain Identifier} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Identifier}
     *
     * @return the {@linkplain Identifier} represented by the specified HTML element
     */
    protected Identifier parseIdentifier(Element ele)
    {
        final String value = ele.text();
        return new Identifier(value);
    }


    /**
     * Retrieves a {@linkplain Creator} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Creator}
     *
     * @return the {@linkplain Creator} represented by the specified HTML element
     */
    protected Creator parseCreator(Element ele)
    {
        final PersonName creatorName = parsePersonName(ele.selectFirst(DataCiteConstants.CREATOR_NAME));
        final List<NameIdentifier> nameIdentifiers = HtmlUtils.elementsToList(ele.select(DataCiteConstants.NAME_IDENTIFIER), this::parseNameIdentifier);

        final Creator creator = new Creator(creatorName);
        creator.addNameIdentifiers(nameIdentifiers);

        return creator;
    }


    /**
     * Retrieves a {@linkplain Contributor} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Contributor}
     *
     * @return the {@linkplain Contributor} represented by the specified HTML element
     */
    protected Contributor parseContributor(Element ele)
    {
        final String contributorTypeString = HtmlUtils.getAttribute(ele, DataCiteConstants.CONTRIBUTOR_TYPE);

        // to be compliant to DataCite 4.1, the type "funder" must be skipped
        if (contributorTypeString.equals(DataCiteConstants.CONTRIBUTOR_TYPE_FUNDER))
            return null;

        final PersonName contributorName = parsePersonName(ele.selectFirst(DataCiteConstants.CONTRIBUTOR_NAME));
        final ContributorType contributorType = HtmlUtils.getEnumAttribute(ele, DataCiteConstants.CONTRIBUTOR_TYPE, ContributorType.class);
        final List<NameIdentifier> nameIdentifiers = HtmlUtils.elementsToList(ele.select(DataCiteConstants.NAME_IDENTIFIER), this::parseNameIdentifier);

        final Contributor contributor = new Contributor(contributorName, contributorType);
        contributor.addNameIdentifiers(nameIdentifiers);

        return contributor;
    }


    /**
     * Retrieves a {@linkplain Title} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Title}
     *
     * @return the {@linkplain Title} represented by the specified HTML element
     */
    protected Title parseTitle(Element ele)
    {
        final String value = ele.text();
        final TitleType titleType = HtmlUtils.getEnumAttribute(ele, DataCiteConstants.TITLE_TYPE, TitleType.class);

        final Title title = new Title(value);
        title.setType(titleType);

        return title;
    }


    /**
     * Retrieves a {@linkplain ResourceType} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain ResourceType}
     *
     * @return the {@linkplain ResourceType} represented by the specified HTML element
     */
    protected ResourceType parseResourceType(Element ele)
    {
        final String value = ele.text();
        final ResourceTypeGeneral generalType = parseResourceTypeGeneral(ele);
        final ResourceType resourceType = new ResourceType(value, generalType);
        return resourceType;
    }


    /**
     * Retrieves a {@linkplain ResourceTypeGeneral} from an HTML element.
     *
     * @param ele an HTML element that has the resourceTypeGeneral attribute
     *
     * @return the {@linkplain ResourceTypeGeneral} of the HTML element
     */
    protected ResourceTypeGeneral parseResourceTypeGeneral(Element ele)
    {
        final String rawResType = HtmlUtils.getAttribute(ele, DataCiteConstants.RESOURCE_TYPE_GENERAL);

        if (DataCiteConstants.RESOURCE_TYPE_GENERAL_FILM.equals(rawResType))
            return ResourceTypeGeneral.Audiovisual;

        else
            return HtmlUtils.getEnumAttribute(
                       ele,
                       DataCiteConstants.RESOURCE_TYPE_GENERAL,
                       ResourceTypeGeneral.class);
    }


    /**
     * Retrieves a {@linkplain Description} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Description}
     *
     * @return the {@linkplain Description} represented by the specified HTML element
     */
    protected Description parseDescription(Element ele)
    {
        final String value = ele.text();
        final DescriptionType descriptionType = HtmlUtils.getEnumAttribute(ele, DataCiteConstants.DESC_TYPE, DescriptionType.class);
        return new Description(value, descriptionType);
    }


    /**
     * Retrieves a {@linkplain Subject} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Subject}
     *
     * @return the {@linkplain Subject} represented by the specified HTML element
     */
    protected Subject parseSubject(Element ele)
    {
        final String value = ele.text();
        final String subjectScheme = HtmlUtils.getAttribute(ele, DataCiteConstants.SUBJECT_SCHEME);

        final Subject subject = new Subject(value);
        subject.setSubjectScheme(subjectScheme);
        return subject;
    }


    /**
     * Retrieves a {@linkplain RelatedIdentifier} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain RelatedIdentifier}
     *
     * @return the {@linkplain RelatedIdentifier} represented by the specified HTML element
     */
    protected RelatedIdentifier parseRelatedIdentifier(Element ele)
    {
        final String value = ele.text();
        final RelatedIdentifierType relatedIdentifierType = HtmlUtils.getEnumAttribute(ele, DataCiteConstants.RELATED_IDENTIFIER_TYPE, RelatedIdentifierType.class);

        final RelationType relationType = HtmlUtils.getEnumAttribute(ele, DataCiteConstants.RELATION_TYPE, RelationType.class);
        final String relatedMetadataScheme = HtmlUtils.getAttribute(ele, DataCiteConstants.RELATED_METADATA_SCHEME);
        final String schemeURI = HtmlUtils.getAttribute(ele, DataCiteConstants.SCHEME_URI);
        final String schemeType = HtmlUtils.getAttribute(ele, DataCiteConstants.SCHEME_TYPE);

        final RelatedIdentifier relatedIdentifier = new RelatedIdentifier(value, relatedIdentifierType, relationType);
        relatedIdentifier.setRelatedMetadataScheme(relatedMetadataScheme);
        relatedIdentifier.setSchemeURI(schemeURI);
        relatedIdentifier.setSchemeType(schemeType);
        return relatedIdentifier;
    }



    /**
     * Retrieves an {@linkplain AlternateIdentifier} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain AlternateIdentifier}
     *
     * @return the {@linkplain AlternateIdentifier} represented by the specified HTML element
     */
    protected AlternateIdentifier parseAlternateIdentifier(Element ele)
    {
        final String value = ele.text();
        final String alternateIdentifierType = HtmlUtils.getAttribute(ele, DataCiteConstants.ALTERNATE_IDENTIFIER_TYPE);

        final AlternateIdentifier alternateIdentifier = new AlternateIdentifier(value, alternateIdentifierType);
        return alternateIdentifier;
    }


    /**
     * Retrieves a {@linkplain Rights} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain Rights}
     *
     * @return the {@linkplain Rights} represented by the specified HTML element
     */
    protected Rights parseRights(Element ele)
    {
        return new Rights(ele.text());
    }


    /**
     * Retrieves an {@linkplain AbstractDate} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain AbstractDate}
     *
     * @return a {@linkplain Date} or {@linkplain DateRange} represented by the specified HTML element
     */
    protected AbstractDate parseDate(Element ele)
    {
        final String value = ele.text();
        final DateType dateType = HtmlUtils.getEnumAttribute(ele, DataCiteConstants.DATE_TYPE, DateType.class);

        return value.contains(DataCiteDateConstants.DATE_RANGE_SPLITTER)
               ? new DateRange(value, dateType)
               : new Date(value, dateType);
    }


    /**
     * Retrieves {@linkplain WebLink}s from DataCite identifiers.
     *
     * @param identifier the identifier of a {@linkplain DataCiteJson}
     * @param relatedIdentifiers related identifiers of a {@linkplain DataCiteJson}
     *
     * @return a list of {@linkplain WebLink}s
     */
    protected List<WebLink> createWebLinks(Identifier identifier, List<RelatedIdentifier> relatedIdentifiers)
    {
        final List<WebLink> webLinks = new LinkedList<>();

        // get related URLs
        if (relatedIdentifiers != null) {
            for (RelatedIdentifier ri : relatedIdentifiers) {
                final String relatedUrl;

                switch (ri.getType()) {
                    case DOI:
                        relatedUrl = ri.getValue().startsWith(DataCiteConstants.URL_PREFIX)
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
            final String identifierURL = identifier.getValue().startsWith(DataCiteConstants.URL_PREFIX)
                                         ? identifier.getValue()
                                         : String.format(OaiPmhConstants.DOI_URL, identifier.getValue());

            final WebLink viewLink = new WebLink(identifierURL);
            viewLink.setType(WebLinkType.ViewURL);
            viewLink.setName(DataCiteConstants.RESOURCE_LINK_NAME);
            webLinks.add(viewLink);
        }

        return webLinks;
    }


    /**
     * Attempts to parse the publication year from DataCite record metadata.
     *
     * @param metadata DataCite record metadata
     *
     * @return the publication year or null, if it does not exist
     */
    protected Integer parsePublicationYear(Element metadata)
    {
        try {
            final String publicationYear = HtmlUtils.getString(metadata, DataCiteConstants.PUBLICATION_YEAR);
            return Integer.parseInt(publicationYear);

        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }


    /**
     * Parses a {@linkplain Contributor} from the HTML representation thereof, verifies
     * that it has the type "funder", and converts it to a {@linkplain FundingReference}.
     *
     * @param ele the HTML element that represents a {@linkplain Contributor}
     *
     * @return the {@linkplain FundingReference} represented by the specified HTML element, or null
     * if the contributor is not a funder or cannot be parsed
     */
    protected FundingReference parseFundingReference(Element ele)
    {
        final String contributorType = HtmlUtils.getAttribute(ele, DataCiteConstants.CONTRIBUTOR_TYPE);

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


    /**
     * Retrieves a {@linkplain NameIdentifier} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain NameIdentifier}
     *
     * @return the {@linkplain NameIdentifier} represented by the specified HTML element
     */
    protected NameIdentifier parseNameIdentifier(Element ele)
    {
        final String value = ele.text();
        final String nameIdentifierScheme = HtmlUtils.getAttribute(ele, DataCiteConstants.NAME_IDENTIFIER_SCHEME);

        final NameIdentifier nameIdentifier = new NameIdentifier(value, nameIdentifierScheme);
        return nameIdentifier;
    }


    /**
     * Retrieves a {@linkplain PersonName} from the HTML representation thereof.
     *
     * @param ele the HTML element that represents the {@linkplain PersonName}
     *
     * @return the {@linkplain PersonName} represented by the specified HTML element
     */
    protected PersonName parsePersonName(Element ele)
    {
        return new PersonName(ele.text());
    }
}
