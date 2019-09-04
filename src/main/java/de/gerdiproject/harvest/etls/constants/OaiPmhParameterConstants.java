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
package de.gerdiproject.harvest.etls.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jsoup.nodes.Element;

import de.gerdiproject.harvest.etls.transformers.AbstractIteratorTransformer;
import de.gerdiproject.harvest.etls.transformers.DataCite2Transformer;
import de.gerdiproject.harvest.etls.transformers.DataCite3Transformer;
import de.gerdiproject.harvest.etls.transformers.DataCite4Transformer;
import de.gerdiproject.harvest.etls.transformers.DataCiteFlexTransformer;
import de.gerdiproject.harvest.etls.transformers.DublinCoreTransformer;
import de.gerdiproject.harvest.etls.transformers.Iso19139Transformer;
import de.gerdiproject.harvest.etls.transformers.constants.DataCiteConstants;
import de.gerdiproject.harvest.etls.transformers.constants.DublinCoreConstants;
import de.gerdiproject.harvest.etls.transformers.constants.Iso19139Constants;
import de.gerdiproject.json.datacite.DataCiteJson;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A static collection of constant parameters for configuring the OAI-PMH harvester.
 *
 * @author Robin Weiss
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OaiPmhParameterConstants
{
    public static final Map<String, Supplier<AbstractIteratorTransformer<Element, DataCiteJson>>> METADATA_SCHEMA_MAP =
        createMetaDataSchemaMap();

    public static final String METADATA_PREFIX_KEY = "metadataPrefix";
    public static final String METADATA_PREFIX_DEFAULT_VALUE = "oai_dc";

    public static final String FROM_KEY = "from";
    public static final String FROM_DEFAULT_VALUE = "";

    public static final String UNTIL_KEY = "until";
    public static final String UNTIL_DEFAULT_VALUE = "";

    public static final String HOST_URL_KEY = "hostUrl";
    public static final String HOST_URL_DEFAULT_VALUE = "";

    public static final String LOGO_URL_KEY = "logoUrl";
    public static final String LOGO_URL_DEFAULT_VALUE = "";

    public static final String VIEW_URL_KEY = "viewUrl";
    public static final String VIEW_URL_DEFAULT_VALUE = "";

    public static final String SET_KEY = "set";
    public static final String SET_DEFAULT_VALUE = "";

    /**
     * Creates a map for assigning {@linkplain AbstractIteratorTransformer} constructor calls to
     * metadata schema URLs as they appear in the ListMetadataFormats query.
     *
     * @return a map of metadata schema URLs to {@linkplain AbstractIteratorTransformer} constructor calls
     */
    private static Map<String, Supplier<AbstractIteratorTransformer<Element, DataCiteJson>>> createMetaDataSchemaMap()
    {
        final Map<String, Supplier<AbstractIteratorTransformer<Element, DataCiteJson>>> map = new HashMap<>();

        // add DataCite transformers
        final DataCite2Transformer dataCite2Transformer = new DataCite2Transformer();
        final DataCite2Transformer dataCite3Transformer = new DataCite3Transformer();
        final DataCite2Transformer dataCite4Transformer = new DataCite4Transformer();
        final DataCiteFlexTransformer dataCiteTransformer = new DataCiteFlexTransformer();

        map.put(DataCiteConstants.SCHEMA_2_URL, () -> dataCite2Transformer);
        map.put(DataCiteConstants.SCHEMA_2_0_URL, () -> dataCite2Transformer);
        map.put(DataCiteConstants.SCHEMA_2_1_URL, () -> dataCite2Transformer);
        map.put(DataCiteConstants.SCHEMA_2_2_URL, () -> dataCite2Transformer);
        map.put(DataCiteConstants.SCHEMA_3_URL, () -> dataCite3Transformer);
        map.put(DataCiteConstants.SCHEMA_3_0_URL, () -> dataCite3Transformer);
        map.put(DataCiteConstants.SCHEMA_3_1_URL, () -> dataCite3Transformer);
        map.put(DataCiteConstants.SCHEMA_4_URL, () -> dataCite4Transformer);
        map.put(DataCiteConstants.SCHEMA_4_0_URL, () -> dataCite4Transformer);
        map.put(DataCiteConstants.SCHEMA_4_1_URL, () -> dataCite4Transformer);
        map.put(DataCiteConstants.SCHEMA_4_2_URL, () -> dataCite4Transformer);
        map.put(DataCiteConstants.SCHEMA_4_3_URL, () -> dataCite4Transformer);

        map.put(DataCiteConstants.OAI_SCHEMA_1_0_URL, () -> dataCiteTransformer);
        map.put(DataCiteConstants.OAI_SCHEMA_1_1_URL, () -> dataCiteTransformer);
        map.put(DataCiteConstants.NO_SCHEMA_URL, () -> dataCiteTransformer);

        // add other transformers
        map.put(DublinCoreConstants.SCHEMA_URL, () -> new DublinCoreTransformer());
        map.put(Iso19139Constants.SCHEMA_URL, () -> new Iso19139Transformer());

        // NOT IMPLEMENTED:
        //map.put("http://www.openarchives.org/OAI/2.0/rdf.xsd", () -> new RdfTransformer());
        //map.put("https://www.openaire.eu/cerif_schema/cerif-1.6-2_openaire-1.0.xsd", () -> new CerifTransformer());
        //map.put("https://api.figshare.com/v2/static/figshare-oai-qdc.xsd", () -> new QdcTransformer());
        //map.put("http://www.loc.gov/standards/mets/mets.xsd", () -> new MetsTransformer());
        //map.put("http://ws.pangaea.de/schemas/pangaea/MetaData.xsd", () -> new PanTransformer());
        //map.put("http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/dif_v9.4.xsd", () -> new DifTransformer());

        return Collections.unmodifiableMap(map);
    }
}
