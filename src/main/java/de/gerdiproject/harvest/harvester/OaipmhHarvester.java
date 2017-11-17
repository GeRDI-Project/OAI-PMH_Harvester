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
package de.gerdiproject.harvest.harvester;

import de.gerdiproject.harvest.IDocument;
import de.gerdiproject.harvest.oaipmh.constants.OaiPmhParameterConstants;
import de.gerdiproject.harvest.oaipmh.constants.OaiPmhUrlConstants;
import de.gerdiproject.json.datacite.*;
import de.gerdiproject.json.datacite.Date;
import de.gerdiproject.json.datacite.abstr.AbstractDate;
import de.gerdiproject.json.datacite.enums.ContributorType;
import de.gerdiproject.json.datacite.enums.DateType;
import de.gerdiproject.json.datacite.enums.DescriptionType;
import de.gerdiproject.json.datacite.enums.RelatedIdentifierType;
import de.gerdiproject.json.datacite.enums.RelationType;
//import de.gerdiproject.json.datacite.enums.ResourceTypeGeneral;
import de.gerdiproject.json.datacite.extension.WebLink;
import de.gerdiproject.json.datacite.extension.enums.WebLinkType;

//import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A harvester
 *
 * @author Jan Frömberg
 */
public class OaipmhHarvester extends AbstractListHarvester<Element>
{
    private static final String PROVIDER = "PANGAEA";
    //private static final String PROVIDER_URL = "https://depositonce.tu-berlin.de";

    //private static final List<String> FORMATS = Collections.unmodifiableList(Arrays.asList("application/pdf"));
    //private static final ResourceType RESOURCE_TYPE = createResourceType();

    //private static final String VIEW_URL = "https://depositonce.tu-berlin.de/handle/%s";
    //private static final String VIEW_URL_DOI = "http://dx.doi.org/10.14279/depositonce-%s";

    //private static final String DOWNLOAD_URL_FILE = "https://depositonce.tu-berlin.de/bitstream/11303/7055/5/mazoun_redha_de.pdf";

    private static final String LOGO_URL = "https://www.pangaea.de/assets/v.4af174c00225b228e260e809f8eff22b/layout-images/pangaea-logo.png";

    private final SimpleDateFormat dateFormat;


    /**
     * Default Constructor.
     *
     */
    public OaipmhHarvester()
    {
        // only one document is created per harvested entry
        super(1);

        dateFormat = new SimpleDateFormat("yyyy'-'MM'-'dd");
    }

    @Override
    public void setProperty(String key, String value)
    {
        super.setProperty(key, value);

        if (key.equals(OaiPmhParameterConstants.DATE_FROM_KEY) ||
            key.equals(OaiPmhParameterConstants.DATE_TO_KEY) ||
            key.equals(OaiPmhParameterConstants.METADATA_PREFIX_KEY))
            init();
    }


    /**
     * Grap stuff from URL
     * @return A collection of Elements
     */
    @Override
    protected Collection<Element> loadEntries()
    {
        Collection<Element> documentsList = new LinkedList<>();
        String url = assembleMainUrl();

        while (url != null) {
            logger.info("resumptionUrl: " + url);

            Document doc = httpRequester.getHtmlFromUrl(url);

            if (doc == null)
                break;

            // add records to list
            documentsList.addAll(doc.select("record"));

            // get next URL
            Element token = doc.select("resumptionToken").first();
            url = (token != null)
                  ? assembleResumptionUrl(token.text())
                  : null;
        }
        logger.info("Finished getting " + documentsList.size() + " entries!");
        return documentsList;
    }


    private String assembleMainUrl()
    {
        String mainUrl = null;
        String host = getProperty(OaiPmhParameterConstants.HOST_URL_KEY);

        if (host != null) {
            String from = getProperty(OaiPmhParameterConstants.DATE_FROM_KEY);
            String until = getProperty(OaiPmhParameterConstants.DATE_TO_KEY);
            String metadataPrefix = getProperty(OaiPmhParameterConstants.METADATA_PREFIX_KEY);

            StringBuilder queryBuilder = new StringBuilder();

            if (from != null && until != null) {
                queryBuilder.append(OaiPmhUrlConstants.DATE_FROM_QUERY).append(from);
                queryBuilder.append(OaiPmhUrlConstants.DATE_TO_QUERY).append(until);
            }

            if (metadataPrefix != null)
                queryBuilder.append(OaiPmhUrlConstants.METADATA_PREFIX_QUERY).append(metadataPrefix);

            mainUrl = String.format(OaiPmhUrlConstants.BASE_URL, host, queryBuilder.toString());
        }

        return mainUrl;
    }


    private String assembleResumptionUrl(String resumptionToken)
    {
        String host = getProperty(OaiPmhParameterConstants.HOST_URL_KEY);
        return String.format(OaiPmhUrlConstants.RESUMPTION_URL, host, resumptionToken);
    }

    /*private static ResourceType createResourceType() //TODO: how to deal with that and other meta data formats like ore, mets, etc
    {
        ResourceType resourceType = new ResourceType("Whatever Data", ResourceTypeGeneral.Dataset);

        return resourceType;
    }*/

    /**
     * Harvest the DepositOnce Repo
     * @param entry Each Entry of function loadEntries()
     * @return A list of DataCite-documents
     */
    @Override
    protected List<IDocument> harvestEntry(Element entry)
    {
        //each entry-node starts with record subelements are header and metadata
        //Example: https://www.cancerdata.org/oai?verb=ListRecords&from=2017-11-01&metadataPrefix=oai_dc
        DataCiteJson document = new DataCiteJson();
        // get header and meta data stuff for each record
        Elements children = entry.children();
        Elements headers = children.select("header");
        Boolean deleted = children.first().attr("status").equals("deleted") ? true : false;
        //logger.info("Identifier deleted?: " + deleted.toString() + " (" + children.first().attr("status") + ")");
        Elements metadata = children.select("metadata");

        List<WebLink> links = new LinkedList<>();
        List<RelatedIdentifier> relatedIdentifiers = new LinkedList<>();
        List<AbstractDate> dates = new LinkedList<>();
        List<Title> titles = new LinkedList<>();
        List<Description> descriptions = new LinkedList<>();
        List<Subject> subjects = new LinkedList<>();
        //List<ResourceType> rtypes = new LinkedList<>();
        List<Creator> creators = new LinkedList<>();
        List<Contributor> contributors = new LinkedList<>();
        List<String> dctype = new LinkedList<>();

        // set document overhead
        //Attributes attributes = entry.attributes();
        //String version = attributes.get("version");
        //document.setVersion(version);
        //document.setResourceType(RESOURCE_TYPE);
        document.setPublisher(PROVIDER);
        //document.setFormats(FORMATS);

        // get identifier and datestamp
        Element identifier = headers.select("identifier").first();
        //String identifier_handle = identifier.text().split(":")[2];
        //logger.info("Identifier Handle (Header): " + identifier_handle);
        Identifier mainIdentifier = new Identifier(identifier.text());

        // get source
        //Source source = new Source(String.format(VIEW_URL, identifier_handle), PROVIDER);
        //source.setProviderURI(PROVIDER_URL);
        //document.setSources(source);

        // get last updated
        String recorddate = headers.select("datestamp").first().text();
        Date updatedDate = new Date(recorddate, DateType.Updated);
        dates.add(updatedDate);

        //check if Entry is "deleted"
        if (deleted) {
            document.setVersion("deleted");
            document.setIdentifier(mainIdentifier);

            // add dates if there are any
            if (!dates.isEmpty())
                document.setDates(dates);

            return Arrays.asList(document);
        }


        // get publication date
        Calendar cal = Calendar.getInstance();
        Elements pubdate = metadata.select("dc|date");

        try {
            cal.setTime(dateFormat.parse(pubdate.first().text()));
            document.setPublicationYear((short) cal.get(Calendar.YEAR));

            Date publicationDate = new Date(pubdate.first().text(), DateType.Available);
            dates.add(publicationDate);
        } catch (ParseException e) { //NOPMD do nothing. just do not add the date if it does not exist
        }

        // get resource types
        Elements dctypes = metadata.select("dc|type");

        for (Element e : dctypes)
            dctype.add(e.text());

        document.setFormats(dctype);

        // get creators
        Elements creatorElements = metadata.select("dc|creator");

        for (Element e : creatorElements) {
            Creator creator = new Creator(e.text());
            creators.add(creator);
        }

        document.setCreators(creators);

        // get contributors
        Elements contribElements = metadata.select("dc|contributor");

        for (Element e : contribElements) {
            Contributor contrib = new Contributor(e.text(), ContributorType.ContactPerson);
            contributors.add(contrib);
        }

        document.setContributors(contributors);

        // get titles
        Elements dctitles = metadata.select("dc|title");

        for (Element title : dctitles) {
            Title dctitle = new Title(title.text());
            titles.add(dctitle);
        }

        document.setTitles(titles);

        // get descriptions
        Elements descriptionElements = metadata.select("dc|description");

        for (Element descElement : descriptionElements) {
            Description description = new Description(descElement.text(), DescriptionType.Abstract);
            descriptions.add(description);
        }

        document.setDescriptions(descriptions);

        // get web links
        WebLink logoLink = new WebLink(LOGO_URL);
        logoLink.setName("Logo");
        logoLink.setType(WebLinkType.ProviderLogoURL);
        links.add(logoLink);

        // get identifier URLs
        Elements identEles = metadata.select("dc|identifier");
        int numidents = identEles.size();

        for (Element identElement : identEles) {
            WebLink viewLink = new WebLink(identElement.text());
            viewLink.setName("Identifier" + numidents);
            viewLink.setType(WebLinkType.ViewURL);
            links.add(viewLink);
            numidents--;
        }

        // get keyword subjects
        Elements dcsubjects = metadata.select("dc|subject");

        for (Element subject : dcsubjects) {
            Subject dcsubject = new Subject(subject.text());
            subjects.add(dcsubject);
        }

        document.setSubjects(subjects);

        // parse references
        Elements referenceElements = metadata.select("DOI");

        for (Element doiRef : referenceElements) {
            relatedIdentifiers.add(new RelatedIdentifier(
                                       doiRef.text(),
                                       RelatedIdentifierType.DOI,
                                       RelationType.IsReferencedBy));
        }

        // compile a document
        document.setIdentifier(mainIdentifier);
        document.setWebLinks(links);

        // add dates if there are any
        if (!dates.isEmpty())
            document.setDates(dates);

        // add related identifiers if there are any
        if (!relatedIdentifiers.isEmpty())
            document.setRelatedIdentifiers(relatedIdentifiers);

        return Arrays.asList(document);
    }
}
