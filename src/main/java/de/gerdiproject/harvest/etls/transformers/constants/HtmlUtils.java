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
package de.gerdiproject.harvest.etls.transformers.constants;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A collection of static methods for parsing HTML records.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HtmlUtils
{
    /**
     * Retrieves the text of the first occurrence of a specified tag
     * derived from a specified {@linkplain Element}.
     *
     * @param element the HTML {@linkplain Element} that contains the tag
     * @param tagName the name of the tag of which the text is to be retrieved
     *
     * @return the text inside the first occurrence of a specified tag,
     *          or null if the tag could not be found
     */
    public static String getString(Element element, String tagName)
    {
        final Element stringElement = element.selectFirst(tagName);
        return stringElement == null ? null : stringElement.text();
    }


    /**
     * Retrieves the texts of all children of a specified parent {@linkplain Element}.
     *
     * @param element the HTML {@linkplain Element} that contains the parent tag
     * @param parentTagName the name of the parent {@linkplain Element} of the child tags
     *
     * @return a {@linkplain List} of {@linkplain String}s
     *          or null if the tag could not be found
     */
    public static List<String> getStringsFromParent(Element element, String parentTagName)
    {
        final Element parent = element.selectFirst(parentTagName);
        return parent == null ? null : elementsToStringList(parent.children());
    }


    /**
     * Retrieves the texts of all occurrences of {@linkplain Element}s with a specified tag name.
     *
     * @param element the HTML {@linkplain Element} that contains the elements
     * @param tagName the tag name of the {@linkplain Element}s to be retrieved
     *
     * @return a {@linkplain List} of {@linkplain String}s
     *          or null if no tag could not be found
     */
    public static List<String> getStrings(Element element, String tagName)
    {
        return elementsToStringList(element.select(tagName));
    }


    /**
     * Retrieves the first occurrence of an {@linkplain Element} with a specified
     * tag and maps it to an object via a specified mapping function.
     *
     * @param element the HTML {@linkplain Element} that contains the requested tag
     * @param tagName the name of the requested tag
     * @param eleToObject a function that maps the found {@linkplain Element} to T
     * @param <T> the output type of the mapping function
     *
     * @return an object representation of the tag or null if it does not exist
     */
    public static <T> T getObject(Element element, String tagName, Function<Element, T> eleToObject)
    {
        final Element requestedTag = element.selectFirst(tagName);
        return requestedTag == null ? null : eleToObject.apply(requestedTag);
    }


    /**
     * Retrieves all occurrences of {@linkplain Element}s with specified tags
     * and maps them to a {@linkplain List} of objects via a specified mapping function.
     *
     * @param element the HTML {@linkplain Element} that contains the requested tags
     * @param tagName the name of the requested tag
     * @param eleToObject a function that maps the found {@linkplain Element}s to T
     * @param <T> the output type of the mapping function
     *
     * @return a {@linkplain List} of object representations of the tag,
     * or null if no matching tags were found
     */
    public static <T> List<T> getObjects(Element element, String tagName, Function<Element, T> eleToObject)
    {
        final Elements eles = element.select(tagName);
        return eles == null ? null : elementsToList(eles, eleToObject);
    }


    /**
     * Retrieves all child {@linkplain Element}s of a specified parent tag and
     * maps them to a {@linkplain List} of objects via a specified mapping function.
     *
     * @param element the HTML {@linkplain Element} that contains the parent tag
     * @param parentTagName the name of the parent tag
     * @param eleToObject a function that maps the child {@linkplain Element}s to T
     * @param <T> the output type of the mapping function
     *
     * @return a {@linkplain List} of objects of the tag or null if it does not exist
     */
    public static <T> List<T> getObjectsFromParent(Element element, String parentTagName, Function<Element, T> eleToObject)
    {
        final Element parent = element.selectFirst(parentTagName);
        return parent == null
               ? null
               : elementsToList(parent.children(), eleToObject);
    }


    /**
     * Retrieves an attribute value of an HTML {@linkplain Element}.
     *
     * @param element the HTML {@linkplain Element} from which the attribute is retrieved
     * @param attributeKey the key of the attribute
     *
     * @return the attribute value, or null if no such attribute exists
     */
    public static String getAttribute(Element element, String attributeKey)
    {
        final String attr = element.attr(attributeKey);
        return attr.isEmpty() ? null : attr;
    }


    /**
     * Retrieves an attribute value of an HTML {@linkplain Element}
     * and attempts to map it to an {@linkplain Enum}.
     *
     * @param element the HTML {@linkplain Element} from which the attribute is retrieved
     * @param attributeKey the key of the attribute
     * @param enumClass the {@linkplain Enum} class to which the attribute value must be mapped
     * @param <T> the type of the {@linkplain Enum}
     *
     * @return the enum representation of the attribute value,
     * or null if the attribute is missing or could not be mapped
     */
    public static <T extends Enum<T>> T getEnumAttribute(Element element, String attributeKey, Class<T> enumClass)
    {
        T returnValue = null;

        try {
            if (element.hasAttr(attributeKey))
                returnValue = Enum.valueOf(enumClass, element.attr(attributeKey).trim());
        } catch (IllegalArgumentException e) {
            returnValue = null;
        }

        return returnValue;
    }


    /**
     * Applies a specified mapping function to every {@linkplain Element} of a {@linkplain Collection},
     * to generate a {@linkplain List} of mapped objects.
     *
     * @param elements the {@linkplain Element} that are to be mapped to objects
     * @param eleToObject a function that maps the {@linkplain Element}s to T
     * @param <T> the output type of the mapping function
     *
     * @return a {@linkplain LinkedList} of objects that were mapped or null if no object could be mapped
     */
    public static <T> List<T> elementsToList(Collection<Element> elements, Function<Element, T> eleToObject)
    {
        if (elements == null || elements.isEmpty())
            return null;

        final List<T> list = new LinkedList<>();

        for (Element ele : elements) {
            final T obj = eleToObject.apply(ele);

            if (obj != null)
                list.add(obj);
        }

        return list.isEmpty() ? null : list;
    }


    /**
     * Maps a {@linkplain Collection} of {@linkplain Element}s to a {@linkplain List}
     * of {@linkplain String}s by retrieving the text of the {@linkplain Element}s.
     *
     * @param elements the {@linkplain Element}s that are to be converted to {@linkplain String}s
     *
     * @return a {@linkplain LinkedList} of {@linkplain String}s
     */
    public static List<String> elementsToStringList(Collection<Element> elements)
    {
        return elementsToList(elements, (Element ele) -> ele.text());
    }
}