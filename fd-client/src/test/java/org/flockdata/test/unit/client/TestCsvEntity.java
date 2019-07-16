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
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.flockdata.data.ContentModel;
import org.flockdata.helper.JsonUtils;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.TransformationHelper;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.entity.EntityPayloadTransformer;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.json.ExtractProfileDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 8/05/2014
 */
public class TestCsvEntity extends AbstractImport {

    private static ContentModel getContentModel(String profile) throws IOException {
        return ContentModelDeserializer.getContentModel(profile);
    }

    @Test
    public void validate_ColumnHelper() throws Exception {
        String[] headers = new String[] {"Title", "Tag", "TagVal", "ValTag", "Origin", "Year", "Gold Medals"};
        ContentModel params = getContentModel("/model/csvtest.json");
        ColumnDefinition colDef = params.getColumnDef(headers[0]);

        assertTrue("CallerRef flag was wrong", colDef.isCallerRef());
        assertTrue("Tag flag was wrong", colDef.isTag());
        assertTrue("Title was wrong", colDef.isTitle());


        colDef = params.getColumnDef(headers[1]);
        assertTrue("Should be a tag", colDef.isTag());
        assertFalse("Shouldn't be a title", TransformationHelper.evaluate(colDef.isTitle()));
        assertFalse("Shouldn't be a callerRef", TransformationHelper.evaluate(colDef.isCallerRef()));

        colDef = params.getColumnDef(headers[2]);
        assertTrue("Should be a tag", colDef.isTag());

        assertFalse("Shouldn't be a title", TransformationHelper.evaluate(colDef.isTitle()));
        assertFalse("Shouldn't be a callerRef", TransformationHelper.evaluate(colDef.isCallerRef()));
        assertTrue("Should exist", TransformationHelper.evaluate(colDef.isMustExist()));

        colDef = params.getColumnDef(headers[3]);
        assertTrue("Should be a tag", colDef.isTag());
        assertFalse("Shouldn't be a title", TransformationHelper.evaluate(colDef.isTitle()));
        assertFalse("Shouldn't be a callerRef", TransformationHelper.evaluate(colDef.isCallerRef()));

        colDef = params.getColumnDef(headers[4]);
        assertTrue("Should be a tag", colDef.isTag());
        assertTrue("Should be a country", colDef.getLabel().equals("Country"));
        assertTrue("must exist", colDef.isMustExist());

        colDef = params.getColumnDef(headers[6]);
        assertTrue("Should be a tag", colDef.isTag());
        assertEquals("'Gold Medals'", colDef.getName()); // This has not been parsed by SPEL so it literal
        assertFalse("Shouldn't be a title", TransformationHelper.evaluate(colDef.isTitle()));
        assertFalse("Shouldn't be a callerRef", TransformationHelper.evaluate(colDef.isCallerRef()));
        assertFalse("Doesn't have to exist", TransformationHelper.evaluate(colDef.isMustExist()));

        Collection<EntityKeyBean> entityLinks = params.getColumnDef("xRef").getEntityLinks();
        Assert.assertEquals(3, entityLinks.size());


    }

    @Test
    public void complexCSVStructure() throws Exception {
        ContentModel contentModel = getContentModel("/model/complex-concept.json");


        String[] headers = {"Athlete", "Age", "Country", "Year", "Sport", "Gold Medals", "Silver Medals", "Bronze Medals"};
        String[] values = {"Michael Phelps", "23", "United States", "2008", "Swimming", "8", "0", "0", "8"};
        EntityInputBean entityInputBean = Transformer.toEntity(Transformer.convertToMap(headers, values, new ExtractProfileHandler(contentModel)), contentModel);

        assertEquals(values[0] + "." + values[3], entityInputBean.getCode());
        boolean goldTag = false, athleteTag = false, sportTag = false, countryTag = false;
//        assertEquals("Silver and Bronze medal values are 0 so should not be included", 5, header.getTags().size());
        for (TagInputBean tagInputBean : entityInputBean.getTags()) {
            if (tagInputBean.getCode().equals("Gold")) {
                assertEquals("Gold Medals", tagInputBean.getLabel());
                EntityTagRelationshipInput o = tagInputBean.getEntityTagLinks().get("competed");
                assertNotNull("Custom relationship name not working", o);
                assertEquals(8, o.getProperties().get("value"));
                goldTag = true;
            }
            if (tagInputBean.getCode().equals("Michael Phelps")) {
                assertNotNull("Custom relationship name not working", tagInputBean.getEntityTagLinks().containsKey("won"));
                assertEquals("Athlete", tagInputBean.getLabel());
                athleteTag = true;
            }
            if (tagInputBean.getCode().equals("Swimming")) {
                assertNotNull("Default relationship name not working", tagInputBean.getEntityTagLinks().containsKey("blah"));
                assertEquals("Sport", tagInputBean.getLabel());
                sportTag = true;
            }
            if (tagInputBean.getCode().equals("United States")) {
                assertNotNull("Relationship name not working", tagInputBean.getEntityTagLinks().containsKey("from"));
                countryTag = true;
            }
            if (tagInputBean.getCode().equals("Sport")) {
                assertEquals("No targets tag present", 1, tagInputBean.getTargets().size());
                TagInputBean athlete = tagInputBean.getTargets().get("competes-in").iterator().next();
                assertNotNull(athlete);
                assertEquals("Michael Phelps", athlete.getName());
                assertEquals("Athlete", athlete.getLabel());
                assertTrue("Direction not reversed", athlete.isReverse());
            }

            if (tagInputBean.getCode().equals("23")) {
                assertEquals("No targets tag present", 1, tagInputBean.getTargets().size());
                TagInputBean athlete = tagInputBean.getTargets().get("at-age").iterator().next();
                assertNotNull(athlete);
                assertEquals("Michael Phelps", athlete.getCode());
                assertEquals("Athlete", athlete.getLabel());
                assertFalse("Direction not defaulted", athlete.isReverse());
            }

        }
        assertTrue("Gold Tag not evaluated", goldTag);
        assertTrue("Athlete Tag not evaluated", athleteTag);
        assertTrue("Sport Tag not evaluated", sportTag);
        assertTrue("Country Tag not evaluated", countryTag);

    }

    @Test
    public void nestedTags() throws Exception {
        ContentModel con = getContentModel("/model/nestedTags.json");
        // @*, the column Header becomes the index for the tag and the Value becomes the name of the tag
        String[] headers = new String[] {"transaction_id", "zip", "state", "stateName", "city", "country"};
        String[] data = new String[] {"1", "123", "CA", "California", "San Francisco", "United States"};
        EntityInputBean entity = Transformer.toEntity(Transformer.convertToMap(headers, data, new ExtractProfileHandler(con)), con);

        assertNotNull(entity);
        List<TagInputBean> tags = entity.getTags();
        assertEquals(1, tags.size());

        TagInputBean zipTag = tags.iterator().next();
        assertEquals("Name is not set if it is the same as the code", null, zipTag.getName());
        assertEquals("ZipCode", zipTag.getLabel());

        Map<String, Collection<TagInputBean>> locatedTags = zipTag.getTargets();
        assertEquals(1, locatedTags.size());
        assertNotNull(locatedTags.get("located"));
        TagInputBean cityTag = locatedTags.get("located").iterator().next();
        assertNotNull(cityTag);
        assertEquals("San Francisco", cityTag.getCode());
        assertEquals("City", cityTag.getLabel());
        assertNotNull(cityTag.getDescription());
        assertEquals(cityTag.getCode(), cityTag.getDescription());

        Map<String, Collection<TagInputBean>> stateTags = cityTag.getTargets();
        assertEquals(1, stateTags.size());
        TagInputBean stateTag = stateTags.get("city").iterator().next();
        assertNotNull(stateTag);
        assertEquals("CA", stateTag.getCode());
        assertEquals("California", stateTag.getName());

        Map<String, Collection<TagInputBean>> countryTags = stateTag.getTargets();
        TagInputBean countryTag = countryTags.get("state").iterator().next();
        assertNotNull(countryTag);
        assertEquals("United States", countryTag.getCode());
        assertEquals(true, countryTag.isMustExist());

    }

    @Test
    public void csv_DelmitedTagsInColumn() throws Exception {
        //
        String[] headers = new String[] {"Title", "Tag"};
        String[] data = new String[] {"TitleTests", "TagA,TagB,TagC"};
        ContentModel params = getContentModel("/model/csv-entity-tags.json");
        EntityPayloadTransformer mapper = EntityPayloadTransformer.newInstance(params);
        mapper.transform(Transformer.convertToMap(headers, data, new ExtractProfileHandler(params)));

        ColumnDefinition colDef = params.getColumnDef(headers[0]);

        assertTrue("CallerRef was wrong", colDef.isCallerRef());
        assertTrue("Title was wrong", colDef.isTitle());

//        colDef = params.getColumnDef(headers[1]);
        Assert.assertEquals(3, mapper.getTags().size());
        for (TagInputBean tagInputBean : mapper.getTags()) {
            String name = tagInputBean.getCode();
            boolean tagA, tagB, tagC;
            tagA = name.equals("TagA");
            tagB = name.equals("TagB");
            tagC = name.equals("TagC");
            assertEquals(true, tagA || tagB || tagC);
        }
    }

    @Test
    public void csv_NumberParsesAsString() throws Exception {
        //
        String[] headers = new String[] {"Title", "TagValueAsNumber", "TagNumberAsString", "StringAsNumber", "created", "updated", "code"};
        String[] data = new String[] {"TitleTests", "123", "123", "123", "1235015570", "1235015805", "123"};
        ContentModel contentModel = getContentModel("/model/csv-entity-data-types.json");
//        assertTrue(contentModel.isEntityOnly());
        EntityPayloadTransformer mapper = EntityPayloadTransformer.newInstance(contentModel);

        Map<String, Object> json = mapper.transform(Transformer.convertToMap(headers, data, new ExtractProfileHandler(contentModel)));

        ColumnDefinition colDef = contentModel.getColumnDef(headers[0]);

        assertTrue("CallerRef was wrong", colDef.isCallerRef());
        assertTrue("Title was wrong", colDef.isTitle());

        Object o = json.get("TagNumberAsString");
        Assert.assertTrue("Could be converted to a string but it's a Tag so should be preserved", o instanceof String);

        o = json.get("TagValueAsNumber");
        Assert.assertTrue("Forced conversion to a number for a Tag (overriding default behaviour)", o instanceof Number);

        o = json.get("StringAsNumber");
        Assert.assertTrue("Should not have been converted to a number", o instanceof Number);

        colDef = contentModel.getColumnDef("created");
        assertTrue("Created Date Not Found", colDef.isCreateDate());
        assertTrue("Didn't resolve to epoc", colDef.isDateEpoc());

        assertNotNull("Code field was missing", json.get("code"));
        assertEquals("123", json.get("code"));

    }

    @Test
    public void null_EntityRow() throws Exception {
        ContentModel params = ContentModelDeserializer.getContentModel("/model/csvtest.json");
        EntityPayloadTransformer mapper = EntityPayloadTransformer.newInstance(params);
        // @*, the column Header becomes the index for the tag and the Value becomes the name of the tag
        String[] headers = new String[] {"Title", "Field", "Year"};
        String[] data = new String[] {"TitleTests", null, "2009"};
        Map<String, Object> jsonMap = mapper.transform(Transformer.convertToMap(headers, data, new ExtractProfileHandler(params)));
        assertNotNull(jsonMap);

        assertEquals(null, jsonMap.get("Field"));
        String json = JsonUtils.toJson(jsonMap);
        jsonMap = JsonUtils.toMap(json);
        assertNotNull(jsonMap);
        assertFalse(jsonMap.isEmpty());
        assertEquals(null, jsonMap.get("Field"));

    }

    @Test
    public void empty_ColumnWithASpace() throws Exception {
        ContentModel params = ContentModelDeserializer.getContentModel("/model/csvtest.json");
        EntityPayloadTransformer transformer = EntityPayloadTransformer.newInstance(params);
        // @*, the column Header becomes the index for the tag and the Value becomes the name of the tag
        String[] headers = new String[] {"Title", "Year"};
        String[] data = new String[] {" ", "2009"};
        Map<String, Object> jsonMap = transformer.transform(Transformer.convertToMap(headers, data, new ExtractProfileHandler(params)));
        assertNotNull(jsonMap);

        assertEquals("Expected 2 entityLinks", 2, transformer.getEntityLinks().size());
        // Exposed and Blah

        boolean blahFound = false, exposedFound = false;
        for (String relationship : transformer.getEntityLinks().keySet()) {
            if (relationship.equalsIgnoreCase("blah")) {
                blahFound = true;
                assertEquals(1, transformer.getEntityLinks().get(relationship).size());
            } else if (relationship.equalsIgnoreCase("exposed")) {
                exposedFound = true;
                assertEquals(2, transformer.getEntityLinks().get(relationship).size());
                for (EntityKeyBean entityKeyBean : transformer.getEntityLinks().get(relationship)) {
                    if (entityKeyBean.getDocumentType().equals("Celebration")) {
                        assertTrue(entityKeyBean.isParent());
                    } else {
                        assertFalse(entityKeyBean.isParent());
                    }
                }
                // 2 EntityKeyBean
            } else {
                throw new RuntimeException("Unexpected EntityLink Relationship" + relationship);
            }
        }

        assertTrue(blahFound);
        assertTrue(exposedFound);
        assertEquals("", jsonMap.get("Title"));
        String json = JsonUtils.toJson(jsonMap);
        jsonMap = JsonUtils.toMap(json);
        assertNotNull(jsonMap);
        assertFalse(jsonMap.isEmpty());
        assertEquals("", jsonMap.get("Title"));

    }

    @Test
    public void empty_ColumnWithASpaceIsIgnored() throws Exception {
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/csvtest-emptyisignored.json");
        ExtractProfile extractProfile = ExtractProfileDeserializer.getImportProfile("/import/header-ignore-empty.json", contentModel);
        assertTrue("isEmptyIgnored is not set", contentModel.isEmptyIgnored());

        EntityPayloadTransformer mapper = EntityPayloadTransformer.newInstance(contentModel);
        // @*, the column Header becomes the index for the tag and the Value becomes the name of the tag
        String[] headers = new String[] {"Title", "Year"};
        String[] data = new String[] {" ", "2009"};
        Map<String, Object> jsonMap = mapper.transform(Transformer.convertToMap(headers, data, extractProfile));
        assertNotNull(jsonMap);

        assertNull(jsonMap.get("Title"));
        String json = JsonUtils.toJson(jsonMap);
        jsonMap = JsonUtils.toMap(json);
        assertNotNull(jsonMap);
        assertFalse(jsonMap.isEmpty());
        assertNull(jsonMap.get("Title"));

    }

    @Test
    public void test_ignoredEntityRow() throws Exception {
        String fileName = "/model/entity-ignore-row.json";

        ContentModel contentModel = ContentModelDeserializer.getContentModel(fileName);

        ExtractProfile extractProfile = ExtractProfileDeserializer.getImportProfile(fileName, contentModel);

        assertEquals('|', extractProfile.getDelimiter());
        assertEquals(Boolean.TRUE, extractProfile.hasHeader());
        assertNotNull(contentModel.getCondition());

        fileProcessor.processFile(extractProfile, "/data/geo-address.txt");

        Collection<String> ids = new ArrayList<>();
        ids.add("56");
        ids.add("379232");
        ids.add("520");
        ids.add("724");

        List<EntityInputBean> entities = getTemplate().getEntities();
        TestCase.assertEquals(ids.size(), entities.size());
        for (EntityInputBean entity : entities) {
            switch (entity.getCode()) {
                case "56":
                    assertNotNull(entity.getName());
                    TestCase.assertEquals("SUITE 5", entity.getProperties().get("unitName"));
                    assertNotNull(entity.getTags().iterator().next());
                    TagInputBean address = entity.getTags().iterator().next();
                    assertEquals("1 RHONE AVENUE", address.getName());
                    assertEquals(1, address.getTargets().size());
                    TestCase.assertEquals(1, address.getProperties().size());
                    TestCase.assertEquals("SUITE 5", address.getProperties().get("unit"));
                    TagInputBean suburb = address.getTargets().get("address").iterator().next();
                    assertEquals("nz", suburb.getKeyPrefix());
                    assertEquals("Suburb name not set", "TE ATATU PENINSULA", suburb.getName());

                    TagInputBean postCode = suburb.getTargets().get("postcode").iterator().next();
                    assertEquals("0610", postCode.getCode());
                    assertEquals("nz", postCode.getKeyPrefix());
                    TagInputBean city = postCode.getTargets().get("towncity").iterator().next();
                    assertEquals("100004U", city.getCode());
                    assertEquals("AUCKLAND", city.getName());
                    assertEquals("nz", city.getKeyPrefix());
                    TagInputBean country = city.getTargets().get("country").iterator().next();
                    TestCase.assertEquals("NZ", country.getCode());
                    TestCase.assertEquals("Country", country.getLabel());
                    //assertTrue(entity.getProperties().size() == 0);
                    break;
                case "379232":
                    assertNotNull(entity.getName());
                    assertNotNull(entity.getTags().iterator().next());
                    address = entity.getTags().iterator().next();
                    assertEquals("15 HUIA ROAD", address.getName());
                    assertTrue(entity.getProperties().size() > 0);
                    TestCase.assertEquals("FLAT 3", entity.getProperties().get("unitName"));
                    break;
                case "520":
                    assertNotNull(entity.getName());
                    TestCase.assertEquals("Null property should not be stored", 1, entity.getProperties().size());
                    TestCase.assertEquals("RANUI PRIMARY SCHOOL", entity.getProperties().get("building"));
                    assertNotNull(entity.getTags().iterator().next());
                    address = entity.getTags().iterator().next();
                    TestCase.assertEquals("16A RANUI STATION ROAD", address.getName());

                    break;
                case "724":
                    assertNotNull(entity.getName());
                    TestCase.assertEquals(2, entity.getProperties().size());
                    address = entity.getTags().iterator().next();
                    TestCase.assertEquals(2, address.getProperties().size());
                    TestCase.assertEquals("SHOP 10A", address.getProperties().get("unit"));
                    TestCase.assertEquals("MERIDIAN MALL", address.getProperties().get("building"));
                    break;
                default:
                    throw new RuntimeException("Unexpected entity " + entity);

            }

        }
    }

}
