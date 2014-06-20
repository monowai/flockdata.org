package com.test.importer;

import com.auditbucket.client.Importer;
import com.auditbucket.client.common.CsvTrackMapper;
import com.auditbucket.client.common.DelimitedMappable;
import com.auditbucket.client.common.ImportParams;
import com.auditbucket.client.csv.CsvColumnHelper;
import com.auditbucket.client.rest.StaticDataResolver;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

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
public class CSVThing {
    @Test
    public void headerRow() throws Exception {
        ImportParams params = Importer.getImportParams("/csvtest.json", null);
        params.setStaticDataResolver(new StaticDataResolver() {
            @Override
            public String resolveCountryISOFromName(String name) throws DatagioException {
                return name;
            }
        });
        CsvTrackMapper mapper = new CsvTrackMapper(params);
        // @*, the column Header becomes the index for the tag and the Value becomes the name of the tag
        String[] headers= new String[]{"Title",     "Tag",     "TagVal", "ValTag", "Origin",      "Year", "Gold Medals" };
        String[] data = new String[]{  "TitleTests","TagName", "Gold",   "8",      "New Zealand", "2008", "12"};
        String json = mapper.setData(headers, data, params);
        assertNotNull (json);
        ObjectMapper om = new ObjectMapper();
        Map values = om.readValue(json, Map.class);
        assertTrue("Title Missing", values.containsKey("Title"));
        assertTrue("Tag Missing", values.containsKey("Tag"));
        assertTrue("Tag Value Missing", values.containsKey("TagVal"));
        assertTrue("Tag Value Missing", values.containsKey("ValTag"));

        assertEquals(data[0], mapper.getCallerRef());
        List<TagInputBean> tags = mapper.getTags();
        assertEquals(5, tags.size());
        //assertEquals(true, tags.contains("Gold Medals"));
        for (TagInputBean tag : tags) {
            switch (tag.getName()){
                case "Gold Medals":
                    Object o=tag.getMetaLinks().get("2008");
                    assertNotNull(o);
                    assertEquals(12,((Map)o).get("value") );
                    break;
                case "TagName":
                    assertEquals("TagName", tag.getCode());
                    break;
                case "Gold":
                    assertEquals(true, tag.isMustExist());
                    break;
                case "ValTag":
                    assertNotNull(tag.getMetaLinks().get("undefined"));
                    assertEquals(1, tag.getMetaLinks().size());
                    assertEquals("ValTag", tag.getName());
                    assertEquals("ValTag", tag.getIndex());
                    assertEquals(8, ((Map)tag.getMetaLinks().get("undefined")).get("value"));
                    break;
                case "New Zealand":
                    assertEquals("Country", tag.getIndex());

            }
        }
    }
    @Test
    public void columnHelperWorks() throws Exception{
        //
        String[] headers= new String[]{"Title", "Tag", "TagVal", "ValTag", "Origin", "Year", "Gold Medals"  };
        String[] data = new String[]{"TitleTests", "TagName", "Gold", "8", "New Zealand"  ,"2008", "12"};
        ImportParams params = Importer.getImportParams("/csvtest.json", null );
        CsvColumnHelper columnHelper = new CsvColumnHelper(headers[0], data[0], params.getColumnDef(headers[0]));
        assertTrue("CallerRef was wrong", columnHelper.isCallerRef());
        assertTrue("Title was wrong", columnHelper.isTitle());
        assertEquals("Title", columnHelper.getKey());
        assertEquals(data[0], columnHelper.getValue());
        assertFalse("Shouldn't be a tag", columnHelper.isTag());

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
        assertTrue("Should have an indirect lookup", columnHelper.getNameColumn()!=null);
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
        params.setStaticDataResolver(new StaticDataResolver() {
            @Override
            public String resolveCountryISOFromName(String name) throws DatagioException {
                return name;
            }
        });
        //CsvTrackMapper mapper = new CsvTrackMapper(params);
        String[]headers = {"Athlete","Age","Country","Year","Sport","Gold Medals","Silver Medals","Bronze Medals"};
        String[]values = { "Michael Phelps","23","United States","2008", "Swimming","8","0","0","8"};
        DelimitedMappable row = (DelimitedMappable) params.getMappable();
        MetaInputBean header = (MetaInputBean) row;
        row.setData(headers, values, params);
        assertEquals(values[0] + "." + values[3], header.getCallerRef());
        boolean goldTag=false, athleteTag=false, sportTag=false, countryTag=false;
        assertEquals("Silver and Bronze medal values are 0 so should not be included", 5, header.getTags().size());
        for (TagInputBean tagInputBean : header.getTags()) {
            if (tagInputBean.getName().equals("Gold Medals")){
                assertEquals("Gold Medals", tagInputBean.getIndex());
                Object o = tagInputBean.getMetaLinks().get("competed");
                assertNotNull("Custom relationship name not working", o);
                assertEquals(8, ((HashMap) o).get("value"));
                goldTag = true;
            }
            if ( tagInputBean.getName().equals("Michael Phelps")){
                assertNotNull("Custom relationship name not working", tagInputBean.getMetaLinks().containsKey("won"));
                assertEquals("Athlete", tagInputBean.getIndex());
                athleteTag = true;
            }
            if ( tagInputBean.getName().equals("Swimming")){
                assertNotNull("Default relationship name not working", tagInputBean.getMetaLinks().containsKey("undefined"));
                assertEquals("Sport", tagInputBean.getIndex());
                sportTag = true;
            }
            if ( tagInputBean.getName().equals("United States")){
                assertNotNull("Default relationship name not working", tagInputBean.getMetaLinks().containsKey("Country"));
                countryTag = true;
            }
            if ( tagInputBean.getName().equals("Sport")){
                assertEquals("No targets tag present", 1, tagInputBean.getTargets().size());
                TagInputBean athlete = tagInputBean.getTargets().get("competes-in").iterator().next();
                assertNotNull ( athlete);
                assertEquals("Michael Phelps", athlete.getName());
                assertEquals("Athlete", athlete.getIndex());
                assertTrue("Direction not reversed", athlete.isReverse());
            }

            if ( tagInputBean.getName().equals("23")){
                assertEquals("No targets tag present", 1, tagInputBean.getTargets().size());
                TagInputBean athlete = tagInputBean.getTargets().get("at-age").iterator().next();
                assertNotNull ( athlete);
                assertEquals("Michael Phelps", athlete.getName());
                assertEquals("Athlete", athlete.getIndex());
                assertFalse("Direction not defaulted", athlete.isReverse());
            }

        }
        assertTrue("Gold Tag not evaluated", goldTag);
        assertTrue("Athlete Tag not evaluated", athleteTag);
        assertTrue("Sport Tag not evaluated", sportTag);
        assertTrue("Country Tag not evaluated", countryTag);

    }
}
