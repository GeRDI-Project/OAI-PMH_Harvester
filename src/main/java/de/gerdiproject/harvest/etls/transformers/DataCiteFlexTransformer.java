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

import java.util.HashMap;
import java.util.Map;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gerdiproject.harvest.etls.AbstractETL;
import de.gerdiproject.harvest.etls.transformers.constants.DataCiteConstants;
import de.gerdiproject.harvest.utils.HtmlUtils;
import de.gerdiproject.json.datacite.DataCiteJson;

/**
 * This class is a transformer for DataCite records, where each record defines
 * its DataCite version.
 *
 * @author Robin Weiss
 */
public class DataCiteFlexTransformer extends AbstractOaiPmhRecordTransformer
{
    private final static Logger LOGGER = LoggerFactory.getLogger(DataCiteFlexTransformer.class);
    private final Map<Integer, AbstractOaiPmhRecordTransformer> transformerMap;


    /**
     * Constructor that initializes one transformer for each
     * DataCite kernel.
     */
    public DataCiteFlexTransformer()
    {
        super();

        this.transformerMap = new HashMap<>();
        transformerMap.put(2, new DataCite2Transformer());
        transformerMap.put(3, new DataCite3Transformer());
        transformerMap.put(4, new DataCite4Transformer());
    }


    @Override
    public void init(final AbstractETL<?, ?> etl)
    {
        super.init(etl);

        for (final AbstractOaiPmhRecordTransformer transformer : transformerMap.values())
            transformer.init(etl);
    }


    @Override
    public void clear()
    {
        for (final AbstractOaiPmhRecordTransformer transformer : transformerMap.values())
            transformer.clear();
    }


    @Override
    protected void setDocumentFieldsFromRecord(final DataCiteJson document, final Element record)
    {
        // retrieve the schema location attribute which denotes the DataCite schema
        final String schemaLocation = getSchemaLocation(record);

        // edge case: abort if schema location is not specified at all
        if (schemaLocation == null) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(
                    getErrorPrefix(record)
                    + DataCiteConstants.MISSING_SCHEMA_ERROR_SUFFIX);
            }

            return;
        }

        // parse schema major version from schema location
        final int schemaVersion = getSchemaVersion(schemaLocation);

        // try to find a fitting transformer for the record
        final AbstractOaiPmhRecordTransformer transformer = transformerMap.get(schemaVersion);

        // abort if the no transformer exists for the major version
        if (transformer == null) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(
                    getErrorPrefix(record)
                    + String.format(DataCiteConstants.UNKNOWN_SCHEMA_ERROR_SUFFIX, schemaLocation));
            }

            return;
        }

        // transform record with the corresponding strategy
        transformer.setDocumentFieldsFromRecord(document, record);
    }


    /**
     * Retrieves the DataCite schema location of the record.
     *
     * @param record the record that is to be harvested
     *
     * @return the schema location attribute of the record
     */
    private static String getSchemaLocation(final Element record)
    {
        final Element resource = record.selectFirst(DataCiteConstants.RESOURCE_ELEMENT);

        // try to get the xsi:schemaLocation attribute
        String schemaLocation = HtmlUtils.getAttribute(resource, DataCiteConstants.SCHEMA_LOCATION_ATTRIBUTE);

        // fallback: try to get any attribute that contains the keyword "schemalocation"
        if (schemaLocation == null) {
            for (final Attribute a : resource.attributes()) {
                if (a.getKey().contains(DataCiteConstants.SCHEMA_LOCATION)) {
                    schemaLocation = a.getValue();
                    break;
                }
            }
        }

        return schemaLocation;
    }


    /**
     * Retrieves the DataCite schema major version of a specified schema location.
     *
     * @param schemaLocation the schemaLocation attribute of the record
     *
     * @return the major version of the DataCite schema, or -1 if it could not be retrieved
     */
    private static int getSchemaVersion(final String schemaLocation)
    {
        // retrieve the index of "kernel-" wich is followed by the major version
        final int kernelIndex = schemaLocation.lastIndexOf(DataCiteConstants.SCHEMA_KERNEL_SUBSTRING)
                                + DataCiteConstants.SCHEMA_KERNEL_SUBSTRING.length();

        int schemaVersion;

        try {
            schemaVersion = Integer.parseInt(schemaLocation.substring(kernelIndex, kernelIndex + 1));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            schemaVersion = -1;
        }

        return schemaVersion;
    }
}
