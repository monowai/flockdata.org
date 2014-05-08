package com.test.importer;

import com.auditbucket.client.CSVColumnHelper;
import com.auditbucket.client.ImportParams;
import com.auditbucket.client.StaticDataResolver;
import com.auditbucket.client.TrackMapper;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.TagInputBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * User: mike
 * Date: 8/05/14
 * Time: 11:29 AM
 */
public class CSVRow {
    @Test
    public void headerRow() throws Exception {
        ImportParams params = new ImportParams();
        TrackMapper mapper = new TrackMapper(params);
        // @*, the column Header becomes the index for the tag and the Value becomes the name of the tag
        String[] headers= new String[]{"$#Title",   "@Tag",    "@!TagVal", "@*ValTag", "%SomeCountry", "Year", "@*Gold Medals [Year]" };
        String[] data = new String[]{  "TitleTests","TagName", "Gold",     "8",        "New Zealand",  "2008", "12"};
        String json = mapper.setData(headers, data, new StaticDataResolver() {
            @Override
            public String resolveCountryISOFromName(String name) throws DatagioException {
                return name;
            }
        });
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
        assertEquals(true, tags.contains("Gold Medals"));
        for (TagInputBean tag : tags) {
            switch (tag.getName()){
                case "Gold Medals":
                    Object o=tag.getMetaLinks().get("2008");
                    assertNotNull(o);
                    assertEquals("12",((Map)o).get("value") );
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
                    assertEquals("8", ((Map)tag.getMetaLinks().get("undefined")).get("value"));
                    break;
                case "New Zealand":
                    assertEquals("Country", tag.getIndex());

            }
        }
    }
    @Test
    public void columnHelperWorks(){
        String[] headers= new String[]{"$#Title","@Tag", "@!TagVal", "@*TagVal", "%Origin","Year", "@*Gold Medals [Year]"  };
        String[] data = new String[]{"TitleTests", "TagName", "Gold", "8", "New Zealand"  ,"2008", "12"};

        CSVColumnHelper columnHelper = new CSVColumnHelper(headers[0], data[0]);
        assertTrue("CallerRef was wrong", columnHelper.isCallerRef());
        assertTrue("Title was wrong", columnHelper.isTitle());
        assertEquals("Title", columnHelper.getKey());
        assertEquals(data[0], columnHelper.getValue());
        assertFalse("Shouldn't be a tag", columnHelper.isTagName());

        columnHelper = new CSVColumnHelper(headers[1], data[1]);
        assertTrue("Should be a tag", columnHelper.isTagName());
        assertEquals(data[1], columnHelper.getValue());
        assertFalse("Shouldn't be a title", columnHelper.isTitle());
        assertFalse("Shouldn't be a callerRef", columnHelper.isCallerRef());

        columnHelper = new CSVColumnHelper(headers[2], data[2]);
        assertTrue("Should be a tag", columnHelper.isTagName());
        assertEquals(data[2], columnHelper.getValue());
        assertFalse("Shouldn't be a title", columnHelper.isTitle());
        assertFalse("Shouldn't be a callerRef", columnHelper.isCallerRef());
        assertTrue("Should exist", columnHelper.isMustExist());

        columnHelper = new CSVColumnHelper(headers[3], data[3]);
        assertTrue("Should be a tag", columnHelper.isTagName());
        assertTrue("Tag to value", columnHelper.isTagToValue());
        assertEquals(data[3], columnHelper.getValue());
        assertFalse("Shouldn't be a title", columnHelper.isTitle());
        assertFalse("Shouldn't be a callerRef", columnHelper.isCallerRef());
        assertFalse("Doesn't have to exist", columnHelper.isMustExist());

        columnHelper = new CSVColumnHelper(headers[4], data[4]);
        assertTrue("Should be a tag", columnHelper.isTagName());
        assertTrue("Should be a country", columnHelper.isCountry());
        assertTrue("must exist", columnHelper.isMustExist());

        columnHelper = new CSVColumnHelper(headers[6], data[6]);
        assertTrue("Should be a tag", columnHelper.isTagName());
        assertEquals("Gold Medals", columnHelper.getKey());
        assertTrue("Should have an indirect lookup", columnHelper.getIndirectColumn()!=null);
        assertEquals("Year", columnHelper.getIndirectColumn());
        assertTrue("Tag to value", columnHelper.isTagToValue());
        assertEquals(data[6], columnHelper.getValue());
        assertFalse("Shouldn't be a title", columnHelper.isTitle());
        assertFalse("Shouldn't be a callerRef", columnHelper.isCallerRef());
        assertFalse("Doesn't have to exist", columnHelper.isMustExist());
        //assertEquals("Country index is always Country", "Country", columnHelper.getKey());

    }
}
