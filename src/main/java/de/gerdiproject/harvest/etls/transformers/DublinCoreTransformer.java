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
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

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
import de.gerdiproject.json.datacite.nested.Publisher;

/**
 * This class is a transformer for OAI-PMH DublinCore records.
 *
 * @author Jan Frömberg, Robin Weiss
 */
public class DublinCoreTransformer extends AbstractOaiPmhRecordTransformer
{
    @Override
    protected void setDocumentFieldsFromRecord(final DataCiteJson document, final Element record)
    {
        // get header and meta data for each record
        final Element metadata = getMetadata(record);

        document.setPublisher(new Publisher(HtmlUtils.getString(metadata, DublinCoreConstants.PUBLISHER)));
        document.setLanguage(HtmlUtils.getString(metadata, DublinCoreConstants.LANG));
        document.addFormats(HtmlUtils.getStrings(metadata, DublinCoreConstants.FORMATS));
        document.addFormats(HtmlUtils.getStrings(metadata, DublinCoreConstants.RES_TYPE));

        document.addSubjects(parseSeparatedTextElements(
                                 metadata,
                                 DublinCoreConstants.SUBJECTS,
                                 (String s) -> new Subject(s)));

        document.setIdentifier(HtmlUtils.getObject(
                                   metadata,
                                   DublinCoreConstants.IDENTIFIERS,
                                   (final Element e) -> new Identifier(e.text())));

        document.addDates(HtmlUtils.getObjects(
                              metadata,
                              DublinCoreConstants.DATES,
                              (final Element e) -> new Date(e.text(), DateType.Issued)));

        document.addCreators(HtmlUtils.getObjects(
                                 metadata,
                                 DublinCoreConstants.CREATORS,
                                 (final Element e) -> new Creator(e.text())));

        document.addContributors(HtmlUtils.getObjects(
                                     metadata,
                                     DublinCoreConstants.CONTRIBUTORS,
                                     (final Element e) -> new Contributor(e.text(), ContributorType.ContactPerson)));

        document.addTitles(HtmlUtils.getObjects(
                               metadata,
                               DublinCoreConstants.TITLES,
                               (final Element e) -> new Title(e.text())));

        document.addDescriptions(HtmlUtils.getObjects(
                                     metadata,
                                     DublinCoreConstants.DESCRIPTIONS,
                                     (final Element e) ->new Description(e.text(), DescriptionType.Abstract)));

        document.addRights(HtmlUtils.getObjects(
                               metadata,
                               DublinCoreConstants.RIGHTS,
                               (final Element e) -> new Rights(e.text())));

        document.addWebLinks(HtmlUtils.getObjects(
                                 metadata,
                                 DublinCoreConstants.IDENTIFIERS, this::identifierToWebLink));

        document.setPublicationYear(parsePublicationYearFromDates(document.getDates()));
    }


    /**
     * Parses a {@linkplain WebLink} from a DublinCore identifier.
     *
     * @param identifier the identifier that is to be parsed
     *
     * @return a {@linkplain WebLink}
     */
    private WebLink identifierToWebLink(final Element identifier)
    {
        WebLink viewLink;

        try {
            // check if URL is valid
            new URL(identifier.text());

            viewLink =  new WebLink(identifier.text(), DublinCoreConstants.VIEW_URL_TITLE, WebLinkType.ViewURL);

        } catch (final MalformedURLException ex) {
            viewLink = null;
        }

        return viewLink;
    }


    @Override
    public void clear()
    {
        // nothing to clean up
    }


    /**
     * This helper function retrieves all HTML elements of a specified name,
     * splits the text content by commas and semicolons and converts each
     * split item to specified objects, all of which are returned in a {@linkplain List}.
     *
     * @param metadata the HTML element that contains the tags
     * @param tag the name of the tags that are parsed
     * @param mappingFunction a function that maps text to the specified type
     * @param <T> the type of the {@linkplain List} that is to be returned
     *
     * @return a {@linkplain List} or null, if nothing was retrieved
     */
    private static <T> List<T> parseSeparatedTextElements(final Element metadata, final String tag, final Function<String, T> mappingFunction)
    {
        final List<T> outputList = new LinkedList<>();

        final List<Element> textElements = metadata.select(tag);

        for (final Element ele : textElements)
            outputList.addAll(stringToList(ele.text(), mappingFunction));

        return outputList.isEmpty() ? null : outputList;
    }


    /**
     * This helper function maps a String that is separated by commas
     * and/or semicolons to a {@linkplain List} of a specified type.
     * Spaces are trimmed off of each element that is extracted in that way.
     *
     * @param inputString the raw input string
     * @param mappingFunction a function that maps each separated element to a specified type
     * @param <T> the type of the returned {@linkplain List}
     *
     * @return a {@linkplain List} of all extracted elements
     */
    private static <T> List<T> stringToList(final String inputString, final Function<String, T> mappingFunction)
    {
        final List<T> list = new LinkedList<>();

        if (inputString != null && !inputString.isEmpty()) {
            final int len = inputString.length();
            int lastIndex = 0;

            while (lastIndex < len) {
                final int commaIndex = inputString.indexOf(',', lastIndex);
                final int semicolonIndex = inputString.indexOf(';', lastIndex);
                final int nextIndex;

                if (commaIndex == -1 && semicolonIndex == -1)
                    nextIndex = len;
                else if (commaIndex == -1)
                    nextIndex = semicolonIndex;
                else if (semicolonIndex == -1)
                    nextIndex = commaIndex;
                else
                    nextIndex = semicolonIndex < commaIndex ? semicolonIndex : commaIndex;

                final String subString = inputString.substring(lastIndex, nextIndex).trim();

                if (!subString.isEmpty())
                    list.add(mappingFunction.apply(subString));

                lastIndex = nextIndex + 1;
            }
        }

        return list;
    }
}
