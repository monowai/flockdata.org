/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.client;

import org.flockdata.client.ClientConfiguration;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.profile.ImportProfile;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.DelimitedMappable;
import org.flockdata.transform.FdReader;
import org.flockdata.transform.csv.CsvEntityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * User: mike
 * Date: 8/05/14
 * Time: 11:29 AM
 */
public class TestCsvEntity {
    FdReader reader = new FdReader() {
        @Override
        public String resolveCountryISOFromName(String name) throws FlockException {
            return name;
        }

        @Override
        public String resolve(String type, Map<String, Object> args) {
            return null;
        }
    };
    @Test
    public void entityRow() throws Exception {
        ImportProfile params = ClientConfiguration.getImportParams("/csvtest.json");
        CsvEntityMapper mapper = new CsvEntityMapper(params);
        // @*, the column Header becomes the index for the tag and the Value becomes the name of the tag
        String[] headers = new String[]{"Title", "Tag", "TagVal", "ValTag", "Origin", "Year", "Gold Medals", "Category"};
        // Category column is intentionally null
        String[] data = new String[]{"TitleTests", "TagName", "Gold", "8", "New Zealand", "2008", "12", null };
        Map<String, Object> json = mapper.setData(headers, data, params, reader);
        assertNotNull(json);

        assertTrue("Title Missing", json.containsKey("Title"));
        assertTrue("Tag Missing", json.containsKey("Tag"));
        assertTrue("Tag Value Missing", json.containsKey("TagVal"));
        assertTrue("Tag Value Missing", json.containsKey("ValTag"));

        Assert.assertEquals(data[0], mapper.getCallerRef());
        List<TagInputBean> tags = mapper.getTags();
        int tagsFound = 0;
        boolean callerRefFoundAsATag = false;
        boolean nullCategoryFound = false;
        for (TagInputBean tag : tags) {

            switch (tag.getName()) {
                case "Gold Medals":
                    Object o = tag.getEntityLinks().get("2008");
                    assertNotNull(o);
                    assertEquals(12, ((Map) o).get("value"));
                    tagsFound ++;
                    break;
                case "TagName":
                    assertEquals("TagName", tag.getCode());
                    tagsFound ++;
                    break;
                case "Gold":
                    assertEquals(true, tag.isMustExist());
                    tagsFound ++;
                    break;
                case "ValTag":
                    assertNotNull(tag.getEntityLinks().get("undefined"));
                    assertEquals(1, tag.getEntityLinks().size());
                    assertEquals("ValTag", tag.getName());
                    assertEquals("ValTag", tag.getLabel());
                    assertEquals(8, ((Map) tag.getEntityLinks().get("undefined")).get("value"));
                    tagsFound ++;
                    break;
                case "New Zealand":
                    assertEquals("Country", tag.getLabel());
                    tagsFound ++;
                    break;
                case "TitleTests":
                    callerRefFoundAsATag = true;
                    assertEquals("TitleTests", tag.getName());
                    tagsFound ++;
                    break;
                case "Undefined":
                    nullCategoryFound = true;
                    assertEquals("Undefined", tag.getCode());
                    assertEquals("Category", tag.getLabel());
                    tagsFound++;
                    break;

            }
        }
        assertTrue("The callerRef was flagged as a tag but not found", callerRefFoundAsATag);
        assertTrue("The undefined category column was not found ", nullCategoryFound);
        assertSame(tags.size(), tagsFound);
    }

    @Test
    public void validate_ColumnHelper() throws Exception {
        String[] headers = new String[]{"Title", "Tag", "TagVal", "ValTag", "Origin", "Year", "Gold Medals"};
        ImportProfile params = getImportParams("/csvtest.json");
        ColumnDefinition colDef = params.getColumnDef(headers[0]);

        assertTrue("CallerRef was wrong", colDef.isCallerRef());
        assertTrue("CallerRef was wrong", colDef.isTag());
        assertTrue("Title was wrong", colDef.isTitle());


        colDef = params.getColumnDef(headers[1]);
        assertTrue("Should be a tag", colDef.isTag());
        assertFalse("Shouldn't be a title", colDef.isTitle());
        assertFalse("Shouldn't be a callerRef", colDef.isCallerRef());

        colDef = params.getColumnDef(headers[2]);
        assertTrue("Should be a tag", colDef.isTag());

        assertFalse("Shouldn't be a title", colDef.isTitle());
        assertFalse("Shouldn't be a callerRef", colDef.isCallerRef());
        assertTrue("Should exist", colDef.isMustExist());

        colDef = params.getColumnDef(headers[3]);
        assertTrue("Should be a tag", colDef.isTag());
        assertTrue("Tag to value", colDef.isValueAsProperty());
        assertFalse("Shouldn't be a title", colDef.isTitle());
        assertFalse("Shouldn't be a callerRef", colDef.isCallerRef());
        assertFalse("Doesn't have to exist", colDef.isMustExist());

        colDef = params.getColumnDef(headers[4]);
        assertTrue("Should be a tag", colDef.isTag());
        assertTrue("Should be a country", colDef.isCountry());
        assertTrue("must exist", colDef.isMustExist());

        colDef = params.getColumnDef(headers[6]);
        assertTrue("Should be a tag", colDef.isTag());
        assertEquals("Year", colDef.getNameColumn());
        assertTrue("Tag to value", colDef.isValueAsProperty());
        assertFalse("Shouldn't be a title", colDef.isTitle());
        assertFalse("Shouldn't be a callerRef", colDef.isCallerRef());
        assertFalse("Doesn't have to exist", colDef.isMustExist());

    }

    @Test
    public void complexCSVStructure() throws Exception {
        ImportProfile params = getImportParams("/complex-concept.json");


        String[] headers = {"Athlete", "Age", "Country", "Year", "Sport", "Gold Medals", "Silver Medals", "Bronze Medals"};
        String[] values = {"Michael Phelps", "23", "United States", "2008", "Swimming", "8", "0", "0", "8"};
        DelimitedMappable row = (DelimitedMappable) params.getMappable();
        EntityInputBean header = (EntityInputBean) row;
        row.setData(headers, values, params, reader);
        assertEquals(values[0] + "." + values[3], header.getCallerRef());
        boolean goldTag = false, athleteTag = false, sportTag = false, countryTag = false;
        assertEquals("Silver and Bronze medal values are 0 so should not be included", 5, header.getTags().size());
        for (TagInputBean tagInputBean : header.getTags()) {
            if (tagInputBean.getName().equals("Gold Medals")) {
                assertEquals("Gold Medals", tagInputBean.getLabel());
                Object o = tagInputBean.getEntityLinks().get("competed");
                assertNotNull("Custom relationship name not working", o);
                assertEquals(8, ((HashMap) o).get("value"));
                goldTag = true;
            }
            if (tagInputBean.getName().equals("Michael Phelps")) {
                assertNotNull("Custom relationship name not working", tagInputBean.getEntityLinks().containsKey("won"));
                assertEquals("Athlete", tagInputBean.getLabel());
                athleteTag = true;
            }
            if (tagInputBean.getName().equals("Swimming")) {
                assertNotNull("Default relationship name not working", tagInputBean.getEntityLinks().containsKey("undefined"));
                assertEquals("Sport", tagInputBean.getLabel());
                sportTag = true;
            }
            if (tagInputBean.getName().equals("United States")) {
                assertNotNull("Default relationship name not working", tagInputBean.getEntityLinks().containsKey("Country"));
                countryTag = true;
            }
            if (tagInputBean.getName().equals("Sport")) {
                assertEquals("No targets tag present", 1, tagInputBean.getTargets().size());
                TagInputBean athlete = tagInputBean.getTargets().get("competes-in").iterator().next();
                assertNotNull(athlete);
                assertEquals("Michael Phelps", athlete.getName());
                assertEquals("Athlete", athlete.getLabel());
                assertTrue("Direction not reversed", athlete.isReverse());
            }

            if (tagInputBean.getName().equals("23")) {
                assertEquals("No targets tag present", 1, tagInputBean.getTargets().size());
                TagInputBean athlete = tagInputBean.getTargets().get("at-age").iterator().next();
                assertNotNull(athlete);
                assertEquals("Michael Phelps", athlete.getName());
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
        ImportProfile params = getImportParams("/nestedTags.json");
        params.setStaticDataResolver(new FdReader() {
            @Override
            public String resolveCountryISOFromName(String name) throws FlockException {
                return name;
            }

            @Override
            public String resolve(String type, Map<String, Object> args) {
                return null;
            }
        });
        CsvEntityMapper mapper = new CsvEntityMapper(params);
        // @*, the column Header becomes the index for the tag and the Value becomes the name of the tag
        String[] headers = new String[]{"transaction_id", "zip", "state", "city", "country"};
        String[] data = new String[]{"1", "123", "CA", "San Francisco", "United States"};
        Map<String, Object> json = mapper.setData(headers, data, params, reader);
        assertNotNull(json);

        List<TagInputBean> tags = mapper.getTags();
        assertEquals(1, tags.size());

        TagInputBean zipTag = tags.iterator().next();
        assertEquals("123", zipTag.getName());
        assertEquals("ZipCode", zipTag.getLabel());

        Map<String, Collection<TagInputBean>> locatedTags = zipTag.getTargets();
        assertEquals(1, locatedTags.size());
        assertNotNull( locatedTags.get("located"));
        TagInputBean cityTag = locatedTags.get("located").iterator().next();
        assertNotNull(cityTag);
        assertEquals("San Francisco", cityTag.getName());
        assertEquals("City", cityTag.getLabel());

        Map<String, Collection<TagInputBean>> stateTags = cityTag.getTargets();
        assertEquals(1, stateTags.size());
        TagInputBean stateTag = stateTags.get("city").iterator().next();
        assertNotNull(stateTag);
        assertEquals("CA", stateTag.getName());

        Map<String, Collection<TagInputBean>> countryTags = stateTag.getTargets();
        TagInputBean countryTag = countryTags.get("state").iterator().next();
        assertNotNull(countryTag);
        assertEquals("United States", countryTag.getName());
        assertEquals(true, countryTag.isMustExist());

    }
    @Test
    public void csv_DelmitedTagsInColumn() throws Exception {
        //
        String[] headers = new String[]{"Title", "Tag"};
        String[] data = new String[]{"TitleTests", "TagA,TagB,TagC"};
        ImportProfile params = getImportParams("/csv-entity-tags.json");
        CsvEntityMapper mapper = new CsvEntityMapper(params);
        mapper.setData(headers, data, params, reader);

        ColumnDefinition colDef = params.getColumnDef(headers[0]);

        assertTrue("CallerRef was wrong", colDef.isCallerRef());
        assertTrue("Title was wrong", colDef.isTitle());

//        colDef = params.getColumnDef(headers[1]);
        Assert.assertEquals(3, mapper.getTags().size());
        for (TagInputBean tagInputBean : mapper.getTags()) {
            String name = tagInputBean.getName();
            boolean tagA, tagB, tagC;
            tagA = name.equals("TagA");
            tagB = name.equals("TagB");
            tagC = name.equals("TagC");
            assertEquals(true, tagA || tagB ||tagC);
        }
    }

    @Test
    public void csv_NumberParsesAsString() throws Exception {
        //
        String[] headers = new String[]{"Title", "NumberAsString"};
        String[] data = new String[]{"TitleTests", "123"};
        ImportProfile params = getImportParams("/csv-entity-data-types.json");
        CsvEntityMapper mapper = new CsvEntityMapper(params);

        Map<String,Object> json = mapper.setData(headers, data, params, reader);

        ColumnDefinition colDef = params.getColumnDef(headers[0]);

        assertTrue("CallerRef was wrong", colDef.isCallerRef());
        assertTrue("Title was wrong", colDef.isTitle());

        Object o = json.get("NumberAsString");
        Assert.assertTrue(o instanceof String);

    }
    public static ImportProfile getImportParams(String profile) throws IOException {
        ImportProfile importProfile;
        ObjectMapper om = FlockDataJsonFactory.getObjectMapper();

        File fileIO = new File(profile);
        if (fileIO.exists()) {
            importProfile = om.readValue(fileIO, ImportProfile.class);

        } else {
            InputStream stream = ClassLoader.class.getResourceAsStream(profile);
            if (stream != null) {
                importProfile = om.readValue(stream, ImportProfile.class);
            } else
                // Defaults??
                importProfile = new ImportProfile();
        }
        //importParams.setWriter(restClient);
        return importProfile;
    }

    @Test
    public void null_EntityRow() throws Exception {
        ImportProfile params = ClientConfiguration.getImportParams("/csvtest.json");
        CsvEntityMapper mapper = new CsvEntityMapper(params);
        // @*, the column Header becomes the index for the tag and the Value becomes the name of the tag
        String[] headers = new String[]{"Title",  "Field"};
        String[] data = new String[]{"TitleTests", null };
        Map<String, Object> jsonMap = mapper.setData(headers, data, params, reader);
        assertNotNull(jsonMap);

        assertEquals(null, jsonMap.get("Field"));
        String json = JsonUtils.getJSON(jsonMap);
        jsonMap = JsonUtils.getAsMap(json);
        assertNotNull(jsonMap);
        assertFalse (jsonMap.isEmpty());
        assertEquals(null, jsonMap.get("Field"));

    }

}
