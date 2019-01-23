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

import java.net.MalformedURLException;
import java.net.URL;

import org.jsoup.nodes.Element;

import de.gerdiproject.harvest.etls.transformers.constants.DublinCoreConstants;
import de.gerdiproject.harvest.utils.HtmlUtils;
import de.gerdiproject.json.datacite.Contributor;
import de.gerdiproject.json.datacite.Creator;
import de.gerdiproject.json.datacite.DataCiteJson;
import de.gerdiproject.json.datacite.Date;
import de.gerdiproject.json.datacite.Description;
import de.gerdiproject.json.datacite.Identifier;
import de.gerdiproject.json.datacite.Rights;
import de.gerdiproject.json.datacite.Subject;
import de.gerdiproject.json.datacite.Title;
import de.gerdiproject.json.datacite.enums.ContributorType;
import de.gerdiproject.json.datacite.enums.DateType;
import de.gerdiproject.json.datacite.enums.DescriptionType;
import de.gerdiproject.json.datacite.extension.generic.WebLink;
import de.gerdiproject.json.datacite.extension.generic.enums.WebLinkType;

/**
 * This class is a transformer for OAI-PMH DublinCore records.
 *
 * @author Jan Frömberg, Robin Weiss
 */
public class DublinCoreTransformer extends AbstractOaiPmhRecordTransformer
{
    @Override
    protected void setDocumentFieldsFromRecord(DataCiteJson document, Element record)
    {
        // get header and meta data for each record
        Element metadata = getMetadata(record);

        document.setPublisher(HtmlUtils.getString(metadata, DublinCoreConstants.PUBLISHER));
        document.setLanguage(HtmlUtils.getString(metadata, DublinCoreConstants.LANG));
        document.addFormats(HtmlUtils.getStrings(metadata, DublinCoreConstants.FORMATS));
        document.addFormats(HtmlUtils.getStrings(metadata, DublinCoreConstants.RES_TYPE));

        document.setIdentifier(HtmlUtils.getObject(metadata, DublinCoreConstants.IDENTIFIERS,
                                                   (Element e) -> new Identifier(e.text())));

        document.addDates(HtmlUtils.getObjects(metadata, DublinCoreConstants.DATES,
                                               (Element e) -> new Date(e.text(), DateType.Issued)));

        document.addCreators(HtmlUtils.getObjects(metadata, DublinCoreConstants.CREATORS,
                                                  (Element e) -> new Creator(e.text())));

        document.addContributors(HtmlUtils.getObjects(metadata, DublinCoreConstants.CONTRIBUTORS,
                                                      (Element e) -> new Contributor(e.text(), ContributorType.ContactPerson)));

        document.addTitles(HtmlUtils.getObjects(metadata, DublinCoreConstants.TITLES,
                                                (Element e) -> new Title(e.text())));

        document.addDescriptions(HtmlUtils.getObjects(metadata, DublinCoreConstants.DESCRIPTIONS,
                                                      (Element e) ->new Description(e.text(), DescriptionType.Abstract)));

        document.addSubjects(HtmlUtils.getObjects(metadata, DublinCoreConstants.SUBJECTS,
                                                  (Element e) -> new Subject(e.text())));

        document.addRights(HtmlUtils.getObjects(metadata, DublinCoreConstants.RIGHTS,
                                                (Element e) -> new Rights(e.text())));

        document.addWebLinks(HtmlUtils.getObjects(metadata, DublinCoreConstants.IDENTIFIERS, this::identifierToWebLink));

        document.setPublicationYear(parsePublicationYearFromDates(document.getDates()));
    }


    /**
     * Parses a {@linkplain WebLink} from a DublinCore identifier.
     *
     * @param identifier the identifier that is to be parsed
     *
     * @return a {@linkplain WebLink}
     */
    private WebLink identifierToWebLink(Element identifier)
    {
        WebLink viewLink;

        try {
            // check if URL is valid
            new URL(identifier.text());

            viewLink =  new WebLink(identifier.text(), DublinCoreConstants.VIEW_URL_TITLE, WebLinkType.ViewURL);

        } catch (MalformedURLException ex) {
            viewLink = null;
        }

        return viewLink;
    }
}
