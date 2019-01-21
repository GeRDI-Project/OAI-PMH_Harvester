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
     * Retrieves the text of the first occurrence of a specified tag.
     *
     * @param ele the HTML element that is to be parsed
     * @param tagName the tag of which the text is to be retrieved
     *
     * @return the text inside the first occurrence of a specified tag,
     *          or null if the tag could not be found
     */
    public static String getString(Element ele, String tagName)
    {
        final Element stringElement = ele.selectFirst(tagName);
        return stringElement == null ? null : stringElement.text();
    }


    /**
     * Retrieves the texts of all child tags of an {@linkplain Element}.
     *
     * @param ele the HTML {@linkplain Element} that contains the parent tag
     * @param parentTagName the name of the parent {@linkplain Element} of the child tags
     *
     * @return a {@linkplain List} of {@linkplain String}s
     *          or null if the tag could not be found
     */
    public static List<String> getStringsFromParent(Element ele, String parentTagName)
    {
        final Element parent = ele.selectFirst(parentTagName);
        return parent == null ? null : elementsToStringList(parent.children());
    }


    /**
     * Retrieves the texts of all occurrences of {@linkplain Element}s with a specified tag name.
     *
     * @param ele the HTML {@linkplain Element} that contains the elements
     * @param tagName the tag name of the {@linkplain Element}s to be retrieved
     *
     * @return a {@linkplain List} of {@linkplain String}s
     *          or null if no tag could not be found
     */
    public static List<String> getStrings(Element ele, String tagName)
    {
        final Elements eles = ele.select(tagName);
        return eles == null ? null : elementsToStringList(eles);
    }


    /**
     * Retrieves the first occurrence of a specified tag and maps it to a specified class.
     *
     * @param ele the HTML {@linkplain Element} that contains the requested tag
     * @param tagName the name of the requested tag
     * @param eleToObject a mapping function that generates the requested class
     * @param <T> the requested type of the converted tag
     *
     * @return an object representation of the tag or null if it does not exist
     */
    public static <T> T getObject(Element ele, String tagName, Function<Element, T> eleToObject)
    {
        final Element requestedTag = ele.selectFirst(tagName);
        return requestedTag == null ? null : eleToObject.apply(requestedTag);
    }


    /**
     * Retrieves all occurrence of a specified tag and maps it to a List of a specified class.
     *
     * @param ele the HTML {@linkplain Element} that contains the requested tag
     * @param tagName the name of the requested tag
     * @param eleToObject a mapping function that generates the requested class
     * @param <T> the requested type of the converted list
     *
     * @return a {@linkplain List} of objects of the tag or null if it does not exist
     */
    public static <T> List<T> getObjects(Element ele, String tagName, Function<Element, T> eleToObject)
    {
        final Elements eles = ele.select(tagName);
        return eles == null ? null : elementsToList(eles, eleToObject);
    }


    /**
     * Retrieves all child tags of a specified tag and maps them to a {@linkplain List} of a specified class.
     *
     * @param ele the HTML {@linkplain Element} that contains the parent tag
     * @param tagName the name of the parent tag
     * @param eleToObject a mapping function that maps a single child to the specified class
     * @param <T> the requested type of the converted tag
     *
     * @return a {@linkplain List} of objects of the tag or null if it does not exist
     */
    public static <T> List<T> getObjectsFromParent(Element ele, String tagName, Function<Element, T> eleToObject)
    {
        final Element parent = ele.selectFirst(tagName);
        return parent == null
               ? null
               : elementsToList(parent.children(), eleToObject);
    }


    /**
     * Retrieves the value of a HTML attribute.
     *
     * @param ele the HTML element that possibly has the attribute
     * @param attributeKey the key of the attribute
     *
     * @return the attribute value, or null if no such attribute exists
     */
    public static String getAttribute(Element ele, String attributeKey)
    {
        final String attr = ele.attr(attributeKey);
        return attr.isEmpty() ? null : attr;
    }


    /**
     * Retrieves the value of a HTML attribute and attempts to map it to an {@linkplain Enum}.
     *
     * @param ele the HTML element that possibly has the attribute
     * @param attributeKey the key of the attribute
     * @param enumClass the class to which the attribute value must be mapped
     * @param <T> the type of the {@linkplain Enum}
     *
     * @return the enum representation of the attribute value, or null if no such attribute exists or could not be mapped
     */
    public static <T extends Enum<T>> T getEnumAttribute(Element ele, String attributeKey, Class<T> enumClass)
    {
        T returnValue = null;

        try {
            if (ele.hasAttr(attributeKey))
                returnValue = Enum.valueOf(enumClass, ele.attr(attributeKey).trim());
        } catch (IllegalArgumentException e) {
            returnValue = null;
        }

        return returnValue;
    }


    /**
     * Applies a mapping function to a {@linkplain Collection} of {@linkplain Element}s,
     * generating a {@linkplain List} of specified objects.
     *
     * @param elements the elements that are to be mapped
     * @param eleToObject the mapping function
     * @param <T> the type to which the elements are to be mapped
     *
     * @return a list of objects that were mapped or null if no object could be mapped
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
     * Maps a {@linkplain Collection} of {@linkplain Element}s to a {@linkplain List} of {@linkplain String}s
     * by retrieving the text of the tag elements.
     *
     * @param elements the elements that are to be converted to strings
     *
     * @return a {@linkplain List} of {@linkplain String}s
     */
    public static List<String> elementsToStringList(Collection<Element> elements)
    {
        return elementsToList(elements, (Element ele) -> ele.text());
    }
}