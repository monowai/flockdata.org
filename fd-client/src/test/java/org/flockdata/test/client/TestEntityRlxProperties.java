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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.client.Configure;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
import org.flockdata.transform.FileProcessor;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.*;

/**
 * Created by mike on 28/01/15.
 */
public class TestEntityRlxProperties extends AbstractImport{
    @Test
    public void string_NoHeaderWithDelimiter() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        File file = new File("/properties-rlx.json");
        ClientConfiguration configuration = Configure.getConfiguration(file);
        assertNotNull(configuration);
        configuration.setDefaultUser("test");

        ImportProfile params = ProfileReader.getImportProfile("/properties-rlx.json");
        assertEquals(',', params.getDelimiter());
        assertEquals(false, params.hasHeader());
        long rows = fileProcessor.processFile(params, "/properties-rlx.txt", getFdWriter(), null, configuration);
        assertEquals(4, rows);
        List<EntityInputBean> entityBatch = getFdWriter().getEntities();
        assertEquals(4, entityBatch.size());
        for (EntityInputBean entityInputBean : entityBatch) {
            assertFalse("Expression not parsed for callerRef",entityInputBean.getCode().contains("|"));
            assertTrue("Caller ref appears invalid", entityInputBean.getCode().length() > 4);
            assertTrue("Tag not set", entityInputBean.getTags().size() == 3);
            TagInputBean politician= null;
            for (TagInputBean tagInputBean : entityInputBean.getTags()) {
                assertFalse("Expression not parsed for code", tagInputBean.getCode().contains("|"));
                assertNull("Name should be null if it equals the code", tagInputBean.getName());
                if ( tagInputBean.getLabel().equals("Politician"))
                    politician= tagInputBean;
                if ( tagInputBean.getLabel().equals("InterestGroup")){
                    assertEquals("direct", tagInputBean.getEntityLinks().keySet().iterator().next());
                }
            }
            assertNotNull(politician);
            HashMap link = (HashMap) politician.getEntityLinks().get("receives");
            assertNotNull(link);
            assertNotNull(link.get("amount"));
            assertTrue("Amount not calculated as a value", Integer.parseInt(link.get("amount").toString()) >0);

        }
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValueAsString(entityBatch);
        } catch (Exception e) {
            throw new FlockException("Failed to serialize");
        }

    }



}
