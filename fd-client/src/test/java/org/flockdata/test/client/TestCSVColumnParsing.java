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

package org.flockdata.test.client;

import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ProfileReader;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Handling columns from content profile
 * Created by mike on 27/01/15.
 */
public class TestCSVColumnParsing extends AbstractImport {

    @Test
    public void string_NoHeaderWithDelimiter() throws Exception {

        ContentProfileImpl params = ProfileReader.getImportProfile("/profile/column-parsing.json");
        assertEquals(false, params.hasHeader());

        long rows = fileProcessor.processFile(params, "/data/pac.txt");
        assertEquals(1L, rows);
        List<TagInputBean> tagInputBeans = getFdBatcher().getTags();
        assertNotNull ( tagInputBeans);
        assertEquals(4, tagInputBeans.size());
        boolean foundA = false, foundB= false,foundC= false, foundD= false;
        for (TagInputBean tagInputBean : tagInputBeans) {
            if ( tagInputBean.getLabel().equals("OSCategory")){
                foundA = true;
                assertEquals("E1140", tagInputBean.getCode());
            } else if ( tagInputBean.getLabel().equals("Expenditure")){
                foundB = true;
                assertEquals("D", tagInputBean.getCode());
                assertEquals("Direct", tagInputBean.getName());
            }  else if ( tagInputBean.getLabel().equals("InterestGroup")){
                foundC = true;
                assertEquals("C00485250", tagInputBean.getCode());
            }  else if ( tagInputBean.getLabel().equals("Politician")){
                foundD = true;
                assertEquals("N00031647", tagInputBean.getCode());
            }

        }
        assertTrue("Failed to find OS Category Tag", foundA);
        assertTrue("Failed to find Expenditure Tag", foundB);
        assertTrue("Failed to find InterestGroup Tag", foundC);
        assertTrue("Failed to find Politician Tag", foundD);
        for (EntityInputBean entityInputBean : fdBatcher.getEntities()) {
            assertEquals("4111320141231324700", entityInputBean.getCode());
        }
    }

    @Test
    public void segment_SetInPayloadFromSource() throws Exception {
        File file = new File("/profile/column-parsing.json");

        ContentProfileImpl params = ProfileReader.getImportProfile("/profile/column-parsing.json");
        assertEquals(false, params.hasHeader());

        long rows = fileProcessor.processFile(params, "/data/pac.txt");
        assertEquals(1L, rows);
        for (EntityInputBean entityInputBean : fdBatcher.getEntities()) {
            assertEquals("The segment was not set in to the EntityInput", "2014", entityInputBean.getSegment());
        }
    }



}
