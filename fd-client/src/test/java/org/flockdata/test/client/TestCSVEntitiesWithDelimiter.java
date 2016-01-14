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

package org.flockdata.test.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
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
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by mike on 27/01/15.
 */
public class TestCSVEntitiesWithDelimiter extends AbstractImport {

    @Test
    public void string_NoHeaderWithDelimiter() throws Exception {
        FileProcessor fileProcessor = new FileProcessor();
        File file = new File("/profile/no-header-entities.json");
        ClientConfiguration configuration = Configure.getConfiguration(file);
        assertNotNull(configuration);
        configuration.setDefaultUser("test");

        ImportProfile params = ProfileReader.getImportProfile("/profile/no-header-entities.json");
        //assertEquals('|', params.getDelimiter());
        assertEquals(false, params.hasHeader());
        long rows = fileProcessor.processFile(params, "/no-header.txt", getFdWriter(), null, configuration);
        int expectedRows = 6;
        assertEquals(expectedRows, rows);

        List<EntityInputBean>entities = getFdWriter().getEntities();
        for (EntityInputBean entity : entities) {
            assertNotNull ( "Remapping column name to target", entity.getContent().getWhat().get("institution"));
            // DAT-528
            assertNull("Column 11 is flagged as false for persistence", entity.getContent().getWhat().get("11"));

            assertEquals(3, entity.getTags().size());
            List<TagInputBean> tagInputBeans = entity.getTags();
            for (TagInputBean tagInputBean : tagInputBeans) {
                if( tagInputBean.getLabel().equals("Year")) {
                    assertEquals("2012", tagInputBean.getCode());
                } else  if ( tagInputBean.getLabel().equals("Institution"))  {
                    assertFalse(tagInputBean.getCode().contains("|"));
                    assertFalse(tagInputBean.getName().contains("|"));
                    assertEquals("Institution", tagInputBean.getLabel());
                    TestCase.assertEquals(1, tagInputBean.getTargets().size());
                    Collection<TagInputBean> targets = tagInputBean.getTargets().get("represents");
                    for (TagInputBean represents : targets) {
                        assertFalse(represents.getCode().contains("|"));
                        assertTrue(represents.isMustExist());
                    }
                } else if ( tagInputBean.getLabel().equals("ZipCode")){
                    assertEquals("Data type was not preserved as a string", "01", tagInputBean.getCode());
                }
            }

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
