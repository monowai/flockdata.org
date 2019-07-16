/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.unit.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Map;
import org.flockdata.data.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.flockdata.transform.tag.TagPayloadTransformer;

/**
 * @author mholdsworth
 * @since 20/06/2014
 */
public class TestCSVConcepts {
    @org.junit.Test
    public void csvTags() throws Exception {
        ContentModel model = ContentModelDeserializer.getContentModel("/model/csv-tag-import.json");
        String[] headers = new String[] {"company_name", "device_name", "device_code", "type", "city", "ram", "tags"};
        String[] data = new String[] {"Samsoon", "Palaxy", "PX", "Mobile Phone", "Auckland", "32mb", "phone,thing,other"};

        TagPayloadTransformer tagTransformer = TagPayloadTransformer.newInstance(model);
        Map<String, Object> json = tagTransformer.transform(Transformer.convertToMap(headers, data, new ExtractProfileHandler(model)));
        assertEquals(1, tagTransformer.getTags().size());
        TagInputBean tag = tagTransformer.getTags().iterator().next();

        assertNotNull(json);
        Map<String, Collection<TagInputBean>> allTargets = tag.getTargets();
        assertNotNull(allTargets);
        assertEquals(3, allTargets.size());
        assertEquals("Should have overridden the column name of device_name", "Device", tag.getLabel());
        assertEquals("Name value should be that of the defined column", "Palaxy", tag.getName());
        assertEquals("PX", tag.getCode());
        assertEquals("Device", tag.getLabel());
        assertNotNull(tag.getProperties().get("RAM"));

        TagInputBean makes = allTargets.get("makes").iterator().next();
        assertEquals("Manufacturer", makes.getLabel());
        assertEquals("Nested City tag not found", 1, makes.getTargets().size());
        TagInputBean city = makes.getTargets().get("located").iterator().next();
        assertEquals("Auckland", city.getCode());


        assertEquals("Samsoon", makes.getCode());
        assertEquals("Should be using the column name", "type", allTargets.get("of-type").iterator().next().getLabel());
        assertEquals(3, allTargets.get("mentions").size());

    }
}
