package com.test.importer;

import com.auditbucket.client.Importer;
import com.auditbucket.client.common.CsvEntityMapper;
import com.auditbucket.client.common.DelimitedMappable;
import com.auditbucket.client.common.ImportParams;
import com.auditbucket.client.csv.CsvColumnHelper;
import com.auditbucket.client.rest.IStaticDataResolver;
import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.track.bean.EntityInputBean;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * User: mike
 * Date: 8/05/14
 * Time: 11:29 AM
 */
public class CsvEntity {
    @Test
    public void entityRow() throws Exception {
        ImportParams params = Importer.getImportParams("/csvtest.json", null);
        params.setStaticDataResolver(new IStaticDataResolver() {
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
        String[] headers = new String[]{"Title", "Tag", "TagVal", "ValTag", "Origin", "Year", "Gold Medals"};
        String[] data = new String[]{"TitleTests", "TagName", "Gold", "8", "New Zealand", "2008", "12"};
        Map<String, Object> json = mapper.setData(headers, data, params);
        assertNotNull(json);
        //ObjectMapper om = new ObjectMapper();
        //Map values = om.readValue(json, Map.class);
        assertTrue("Title Missing", json.containsKey("Title"));
        assertTrue("Tag Missing", json.containsKey("Tag"));
        assertTrue("Tag Value Missing", json.containsKey("TagVal"));
        assertTrue("Tag Value Missing", json.containsKey("ValTag"));

        assertEquals(data[0], mapper.getCallerRef());
        List<TagInputBean> tags = mapper.getTags();
        int tagsFound = 0;
        boolean callerRefFoundAsATag = false;
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

            }
        }
        assertTrue("The callerRef was flagged as a tag but not found", callerRefFoundAsATag);
        assertSame(tags.size(), tagsFound);
    }

    @Test
    public void columnHelperWorks() throws Exception {
        //
        String[] headers = new String[]{"Title", "Tag", "TagVal", "ValTag", "Origin", "Year", "Gold Medals"};
        String[] data = new String[]{"TitleTests", "TagName", "Gold", "8", "New Zealand", "2008", "12"};
        ImportParams params = Importer.getImportParams("/csvtest.json", null);
        CsvColumnHelper columnHelper = new CsvColumnHelper(headers[0], data[0], params.getColumnDef(headers[0]));
        assertTrue("CallerRef was wrong", columnHelper.isCallerRef());
        assertTrue("CallerRef was wrong", columnHelper.isTag());
        assertTrue("Title was wrong", columnHelper.isTitle());
        assertEquals("Title", columnHelper.getKey());
        assertEquals(data[0], columnHelper.getValue());


        columnHelper = new CsvColumnHelper(headers[1], data[1], params.getColumnDef(headers[1]));
        assertTrue("Should be a tag", columnHelper.isTag());
        assertEquals(data[1], columnHelper.getValue());
        assertFalse("Shouldn't be a title", columnHelper.isTitle());
        assertFalse("Shouldn't be a callerRef", columnHelper.isCallerRef());

        columnHelper = new CsvColumnHelper(headers[2], data[2], params.getColumnDef(headers[2]));
        assertTrue("Should be a tag", columnHelper.isTag());
        assertEquals(data[2], columnHelper.getValue());
        assertFalse("Shouldn't be a title", columnHelper.isTitle());
        assertFalse("Shouldn't be a callerRef", columnHelper.isCallerRef());
        assertTrue("Should exist", columnHelper.isMustExist());

        columnHelper = new CsvColumnHelper(headers[3], data[3], params.getColumnDef(headers[3]));
        assertTrue("Should be a tag", columnHelper.isTag());
        assertTrue("Tag to value", columnHelper.isValueAsProperty());
        assertEquals(data[3], columnHelper.getValue());
        assertFalse("Shouldn't be a title", columnHelper.isTitle());
        assertFalse("Shouldn't be a callerRef", columnHelper.isCallerRef());
        assertFalse("Doesn't have to exist", columnHelper.isMustExist());

        columnHelper = new CsvColumnHelper(headers[4], data[4], params.getColumnDef(headers[4]));
        assertTrue("Should be a tag", columnHelper.isTag());
        assertTrue("Should be a country", columnHelper.isCountry());
        assertTrue("must exist", columnHelper.isMustExist());

        columnHelper = new CsvColumnHelper(headers[6], data[6], params.getColumnDef(headers[6]));
        assertTrue("Should be a tag", columnHelper.isTag());
        assertEquals("Gold Medals", columnHelper.getKey());
        assertTrue("Should have an indirect lookup", columnHelper.getNameColumn() != null);
        assertEquals("Year", columnHelper.getNameColumn());
        assertTrue("Tag to value", columnHelper.isValueAsProperty());
        assertEquals(data[6], columnHelper.getValue());
        assertFalse("Shouldn't be a title", columnHelper.isTitle());
        assertFalse("Shouldn't be a callerRef", columnHelper.isCallerRef());
        assertFalse("Doesn't have to exist", columnHelper.isMustExist());
        //assertEquals("Country index is always Country", "Country", columnHelper.getKey());

    }

    @Test
    public void complexCSVStructure() throws Exception {
        ImportParams params = Importer.getImportParams("/complex-concept.json", null);
        params.setStaticDataResolver(new IStaticDataResolver() {
            @Override
            public String resolveCountryISOFromName(String name) throws FlockException {
                return name;
            }

            @Override
            public String resolve(String type, Map<String, Object> args) {
                return null;
            }
        });
        //CsvTrackMapper mapper = new CsvTrackMapper(params);
        String[] headers = {"Athlete", "Age", "Country", "Year", "Sport", "Gold Medals", "Silver Medals", "Bronze Medals"};
        String[] values = {"Michael Phelps", "23", "United States", "2008", "Swimming", "8", "0", "0", "8"};
        DelimitedMappable row = (DelimitedMappable) params.getMappable();
        EntityInputBean header = (EntityInputBean) row;
        row.setData(headers, values, params);
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
        ImportParams params = Importer.getImportParams("/nestedTags.json", null);
        params.setStaticDataResolver(new IStaticDataResolver() {
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
        Map<String, Object> json = mapper.setData(headers, data, params);
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
}
