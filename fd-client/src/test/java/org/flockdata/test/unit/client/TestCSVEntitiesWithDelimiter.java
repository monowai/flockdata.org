/*
 *  Copyright 2012-2016 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

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

        ImportContentModel params = ProfileReader.getContentModel("/model/no-header-entities.json");
        //assertEquals('|', params.getDelimiter());
        assertEquals(false, params.hasHeader());
        long rows = fileProcessor.processFile(params, "/data/no-header.txt");
        int expectedRows = 6;
        assertEquals(expectedRows, rows);

        List<EntityInputBean>entities = fdBatcher.getEntities();
        for (EntityInputBean entity : entities) {
            assertNotNull ( "Remapping column name to target", entity.getContent().getData().get("institution"));
            // DAT-528
            assertNull("Column 11 is flagged as false for persistence", entity.getContent().getData().get("11"));

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
