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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.flockdata.data.ContentModel;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Test;

/**
 * nested tags
 *
 * @author mholdsworth
 * @since 6/05/2015
 */
public class TestEntityProperties extends AbstractImport {
    @Test
    public void process_individualData() throws Exception {
        String contentModelFile = "/model/entity-properties.json";

        ContentModel contentModel = ContentModelDeserializer.getContentModel(contentModelFile);
        ExtractProfile extractProfile = new ExtractProfileHandler(contentModel, false);
        assertEquals(Boolean.FALSE, extractProfile.hasHeader());
        extractProfile.setQuoteCharacter("|");

        fileProcessor.processFile(extractProfile, "/data/entity-properties.txt");
        List<EntityInputBean> entities = fdTemplate.getEntities();

        for (EntityInputBean entity : entities) {
            assertNotEquals("One org and one candidate", 0, entity.getTags().size());
            assertNotNull(entity.getWhen());
            for (TagInputBean tagInputBean : entity.getTags()) {
                switch (tagInputBean.getLabel()) {
                    case "Year":
                        assertEquals("2014", tagInputBean.getCode());
                        break;
                    case "Contributor":
                        assertEquals("j10013521891", tagInputBean.getCode());
                        assertNotNull(tagInputBean.getName());
                        assertNotSame(tagInputBean.getName(), tagInputBean.getCode());
                        Map<String, EntityTagRelationshipInput> igRlx = tagInputBean.getEntityTagLinks();
                        assertFalse(igRlx.isEmpty());
                        EntityTagRelationshipInput valueMap = igRlx.get("contributed");
                        assertTrue(valueMap.getProperties().containsKey("value"));
                        assertTrue(Double.parseDouble(valueMap.getProperties().get("value").toString()) == 500);

                        break;
                    case "OSCategory":
                        assertEquals("G6400", tagInputBean.getCode());
                        break;
                    case "Politician":
                        Map<String, EntityTagRelationshipInput> rlx = tagInputBean.getEntityTagLinks();
                        assertFalse(rlx.isEmpty());
                        EntityTagRelationshipInput valMap = rlx.get("received");
                        assertTrue(valMap.getProperties().containsKey("value"));
                        assertTrue(Double.parseDouble(valMap.getProperties().get("value").toString()) == 500);
                        break;
                    case "ZipCode":
                        assertEquals("Zip code should not be turned to a number. Should be preserved as a string", "07450", tagInputBean.getCode());

                        assertNotNull(tagInputBean.getTargets().get("located"));
                        TagInputBean city = tagInputBean.getTargets().get("located").iterator().next();
                        assertNotNull(city);
                        assertNotNull(city.getTargets().get("city"));
                        TagInputBean state = city.getTargets().get("city").iterator().next();
                        assertEquals("US-NJ", state.getCode());
                        break;
                    default:
                        throw new Exception("Unexpected tag " + tagInputBean);

                }
            }
//            TagInputBean contributor = entity.getTags().get("contributed");
            assertNotNull(entity.getProperties());
            assertTrue(entity.getProperties().get("value") != null);

            // Neo4j will not store NULL values
            assertFalse("Building had a null value so should not have been set", entity.getProperties().containsKey("building"));
            Object value = entity.getProperties().get("value");
            assertTrue(value instanceof Number);
            assertEquals(500, Integer.parseInt(value.toString()));

            // Assert that we get the user defined value to compute
            assertTrue(entity.getProperties().get("valueDefault") != null);
            value = entity.getProperties().get("valueDefault");
            assertEquals("Userdefined value of 0 was not set", 0, Integer.parseInt(value.toString()));

            // Neo4j complains if you persist a null property value
            assertFalse("Should not be setting null property values", entity.getProperties().containsKey("valueNull"));

            assertTrue(entity.getProperties().get("valueCalc") != null);
            value = entity.getProperties().get("valueCalc");
            assertEquals("Column lookup expression did not evaluate", 2014, Integer.parseInt(value.toString()));

        }

        // Check that the payload will serialize
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValueAsString(entities);
        } catch (Exception e) {
            throw new FlockException("Failed to serialize");
        }


    }
}
