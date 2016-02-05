/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.test.importer;

import junit.framework.TestCase;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.test.client.AbstractImport;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.flockdata.transform.Transformer;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Created by mike on 27/02/15.
 */
public class TestDataTypeConversion extends AbstractImport {
    @Test
    public void preserve_NumberValueAsString() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        String fileName = "/profile/data-types.json";
        ClientConfiguration configuration = getClientConfiguration(fileName);

        ContentProfileImpl profile = ProfileReader.getImportProfile(fileName);
        fileProcessor.processFile(profile, "/data/data-types.csv", getFdWriter(), null, configuration);
        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        assertEquals(2, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            if (tagInputBean.getLabel().equals("as-string"))
                assertEquals("00165", tagInputBean.getCode());
        }
        EntityInputBean entity = getFdWriter().getEntities().iterator().next();
        assertNotNull ( entity.getContent());
        assertEquals("The N/A string should have been set to the default of 0", 0, entity.getContent().getData().get("illegal-num"));
        assertEquals("The Blank string should have been set to the default of 0", 0, entity.getContent().getData().get("blank-num"));
    }

    @Test
    public void double_EntityProperty() throws Exception {
        // Tests that numeric values are converted to explicit data-type
        FileProcessor fileProcessor = new FileProcessor();
        String fileName = "/profile/entity-data-types.json";
        ClientConfiguration configuration = getClientConfiguration(fileName);

        ContentProfileImpl profile = ProfileReader.getImportProfile(fileName);
        fileProcessor.processFile(profile, "/data/entity-data-types.csv", getFdWriter(), null, configuration);
        List<EntityInputBean> entityInputBeans = getFdWriter().getEntities();
        assertEquals(2, entityInputBeans.size());
        for (EntityInputBean entityInputBean : entityInputBeans) {
            Object o = entityInputBean.getProperties().get("value");
            TestCase.assertTrue("Should have been cast to a Double but was " + o.getClass().getName(), o instanceof Double);
        }
    }

    @Test
    public void preserve_TagCodeAlwaysString() throws Exception {
        // Even though the source column can be treated as a number, it should be set as a String because
        // it drives a tag code.
        FileProcessor fileProcessor = new FileProcessor();
        String fileName = "/profile/data-types.json";
        ClientConfiguration configuration = getClientConfiguration(fileName);

        ContentProfileImpl profile = ProfileReader.getImportProfile(fileName);
        fileProcessor.processFile(profile, "/data/data-types.csv", getFdWriter(), null, configuration);
        List<TagInputBean> tagInputBeans = getFdWriter().getTags();
        assertEquals(2, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            if (tagInputBean.getLabel().equals("tag-code"))
                assertEquals("123", tagInputBean.getCode());
        }
        List<EntityInputBean> entities = getFdWriter().getEntities();
        for (EntityInputBean entity : entities) {
            Object whatString = entity.getContent().getData().get("tag-code");
            assertEquals(""+whatString.getClass(), true, whatString instanceof String);
        }

    }

    @Test
    public void number_Converts() throws Exception {
        // DAT-454
        String fileName = "/profile/data-types.json";
        ContentProfileImpl profile = ProfileReader.getImportProfile(fileName);
        String header[] = new String[]{"num"};
        String row[] = new String[]{"0045"};
        Map<String, Object> converted = Transformer.convertToMap(header, row, profile);
        assertEquals("45", converted.get("num").toString());

        row = new String[]{null};
        converted = Transformer.convertToMap(header, row, profile);
        assertEquals(null, converted.get("num"));

    }

    @Test
    public void number_ConvertsWithThousandSeparator() throws Exception {
        // DAT-454

        String fileName = "/profile/data-types.json";
        ContentProfileImpl profile = ProfileReader.getImportProfile(fileName);
        String header[] = new String[]{"num"};
        String row[] = new String[]{"50,000.99"}; // Not internatioalised
        Map<String, Object> converted = Transformer.convertToMap(header, row, profile);
        assertEquals("50000.99", converted.get("num").toString());


        row = new String[]{""};
        converted = Transformer.convertToMap(header, row, profile);
        assertEquals(null, converted.get("num"));

        row = new String[]{null};
        converted = Transformer.convertToMap(header, row, profile);
        assertEquals(null, converted.get("num"));

    }

    @Test
    public void title_Expression() throws Exception {
        // DAT-457
        FileProcessor fileProcessor = new FileProcessor();
        String fileName = "/profile/data-types.json";
        ContentProfileImpl profile = ProfileReader.getImportProfile(fileName);
        ClientConfiguration configuration = getClientConfiguration(fileName);
        fileProcessor.processFile(profile, "/data/data-types.csv", getFdWriter(), null, configuration);
        List<EntityInputBean> entities = getFdWriter().getEntities();
        assertEquals(1, entities.size());
        EntityInputBean entityInputBean = entities.iterator().next();
        assertEquals("Title expression did not evaluate", "00165-test", entityInputBean.getName());
    }

    @Test
    public void date_CreatedDateSets() throws Exception {
        // DAT-457
        FileProcessor fileProcessor = new FileProcessor();
        String fileName = "/profile/data-types.json";
        ContentProfileImpl profile = ProfileReader.getImportProfile(fileName);
        ClientConfiguration configuration = getClientConfiguration(fileName);
        fileProcessor.processFile(profile, "/data/data-types.csv", getFdWriter(), null, configuration);
        List<EntityInputBean> entities = getFdWriter().getEntities();
        assertEquals(1, entities.size());
        EntityInputBean entityInputBean = entities.iterator().next();

        Calendar calInstance = Calendar.getInstance();
        calInstance.setTime(entityInputBean.getWhen());
        assertEquals(2015, calInstance.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, calInstance.get(Calendar.MONTH));
        assertEquals(14, calInstance.get(Calendar.DATE));

        // DAT-523
        Object randomDate = entityInputBean.getContent().getData().get("randomDate");
        assertNotNull(randomDate);
        TestCase.assertTrue("", randomDate instanceof String);
        new DateTime(randomDate);
        TestCase.assertNull(entityInputBean.getContent().getData().get("nullDate"));


    }

    @Test
    public void date_LastChange() throws Exception {
        // Given 2 dates that could be the last change, check the most recent
        FileProcessor fileProcessor = new FileProcessor();
        String fileName = "/profile/data-types.json";
        ContentProfileImpl profile = ProfileReader.getImportProfile(fileName);
        ClientConfiguration configuration = getClientConfiguration(fileName);
        fileProcessor.processFile(profile, "/data/data-types.csv", getFdWriter(), null, configuration);
        List<EntityInputBean> entities = getFdWriter().getEntities();
        assertEquals(1, entities.size());
        EntityInputBean entityInputBean = entities.iterator().next();

        Calendar calInstance = Calendar.getInstance();
        calInstance.setTime(entityInputBean.getLastChange());
        assertEquals(2015, calInstance.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, calInstance.get(Calendar.MONTH));
        assertEquals(25, calInstance.get(Calendar.DATE));

    }


}