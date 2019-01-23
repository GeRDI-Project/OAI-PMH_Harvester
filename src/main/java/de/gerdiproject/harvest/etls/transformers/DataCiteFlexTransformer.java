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
    private final Map<String, AbstractOaiPmhRecordTransformer> transformerMap;


    /**
     * Constructor that initializes one transformer for each
     * DataCite kernel.
     */
    public DataCiteFlexTransformer()
    {
        super();

        final DataCite2Transformer dataCite2Transformer = new DataCite2Transformer();
        final DataCite3Transformer dataCite3Transformer = new DataCite3Transformer();
        final DataCite4Transformer dataCite4Transformer = new DataCite4Transformer();

        this.transformerMap = new HashMap<>();
        transformerMap.put(DataCiteConstants.SCHEMA_2_URL, dataCite2Transformer);
        transformerMap.put(DataCiteConstants.SCHEMA_2_0_URL, dataCite2Transformer);
        transformerMap.put(DataCiteConstants.SCHEMA_2_1_URL, dataCite2Transformer);
        transformerMap.put(DataCiteConstants.SCHEMA_2_2_URL, dataCite2Transformer);
        transformerMap.put(DataCiteConstants.SCHEMA_3_URL, dataCite3Transformer);
        transformerMap.put(DataCiteConstants.SCHEMA_3_0_URL, dataCite3Transformer);
        transformerMap.put(DataCiteConstants.SCHEMA_3_1_URL, dataCite3Transformer);
        transformerMap.put(DataCiteConstants.SCHEMA_4_URL, dataCite4Transformer);
        transformerMap.put(DataCiteConstants.SCHEMA_4_0_URL, dataCite4Transformer);
        transformerMap.put(DataCiteConstants.SCHEMA_4_1_URL, dataCite4Transformer);
    }


    @Override
    public void init(AbstractETL<?, ?> etl)
    {
        for (AbstractOaiPmhRecordTransformer transformer : transformerMap.values())
            transformer.init(etl);
    }


    @Override
    public void clear()
    {
        for (AbstractOaiPmhRecordTransformer transformer : transformerMap.values())
            transformer.clear();
    }


    @Override
    protected void setDocumentFieldsFromRecord(DataCiteJson document, Element record)
    {
        // try to find a fitting transformer for the record
        final String schemaUrl = getSchemaUrl(record);
        final AbstractOaiPmhRecordTransformer transformer = transformerMap.get(schemaUrl);

        // log error if the schema is unknown
        if (transformer == null)
            LOGGER.error(String.format(
                             DataCiteConstants.UNKNOWN_SCHEMA_ERROR,
                             schemaUrl));
        else
            transformer.setDocumentFieldsFromRecord(document, record);
    }


    /**
     * Retrieves the DataCite schema URL of the record.
     *
     * @param record the record that is to be harvested
     *
     * @return a URL pointing towards a metadata schema
     */
    private String getSchemaUrl(Element record)
    {
        final Element resource = record.selectFirst(DataCiteConstants.RESOURCE_ELEMENT);
        String schemaLocation = HtmlUtils.getAttribute(resource, DataCiteConstants.SCHEMA_LOCATION_ATTRIBUTE);

        // try alternative schema attribute
        if (schemaLocation == null)
            schemaLocation = HtmlUtils.getAttribute(resource, DataCiteConstants.NO_SCHEMA_LOCATION_ATTRIBUTE);

        // if still there is no attribute, abort
        if (schemaLocation == null)
            return null;

        // if multiple, space-separated schema URLs exist, choose the last one
        final String schemaUrl = schemaLocation.substring(schemaLocation.lastIndexOf(' ') + 1);
        return schemaUrl;
    }
}
