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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.flockdata.data.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.json.ExtractProfileDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Test;

/**
 * Handling columns from content profile
 *
 * @author mholdsworth
 * @since 27/01/2015
 */
public class TestCSVColumnParsing extends AbstractImport {

    @Test
    public void string_NoHeaderWithDelimiter() throws Exception {

        ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/column-parsing.json");
        ExtractProfile extractProfile = ExtractProfileDeserializer.getImportProfile("/import/csv-header-pipe-quote.json", contentModel);

        assertEquals(false, extractProfile.hasHeader());

        long rows = fileProcessor.processFile(extractProfile, "/data/pac.txt");
        assertEquals(1L, rows);
        List<TagInputBean> tagInputBeans = getTemplate().getTags();
        assertNotNull(tagInputBeans);
        assertEquals(4, tagInputBeans.size());
        boolean foundA = false, foundB = false, foundC = false, foundD = false;
        for (TagInputBean tagInputBean : tagInputBeans) {
            if (tagInputBean.getLabel().equals("OSCategory")) {
                foundA = true;
                assertEquals("E1140", tagInputBean.getCode());
            } else if (tagInputBean.getLabel().equals("Expenditure")) {
                foundB = true;
                assertEquals("D", tagInputBean.getCode());
                assertEquals("Direct", tagInputBean.getName());
            } else if (tagInputBean.getLabel().equals("InterestGroup")) {
                foundC = true;
                assertEquals("C00485250", tagInputBean.getCode());
            } else if (tagInputBean.getLabel().equals("Politician")) {
                foundD = true;
                assertEquals("N00031647", tagInputBean.getCode());
            }

        }
        assertTrue("Failed to find OS Category Tag", foundA);
        assertTrue("Failed to find Expenditure Tag", foundB);
        assertTrue("Failed to find InterestGroup Tag", foundC);
        assertTrue("Failed to find Politician Tag", foundD);
        for (EntityInputBean entityInputBean : fdTemplate.getEntities()) {
            assertEquals("4111320141231324700", entityInputBean.getCode());
        }
    }

    @Test
    public void segment_SetInPayloadFromSource() throws Exception {

        ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/column-parsing.json");
        ExtractProfile profile = new ExtractProfileHandler(contentModel, false);
        assertEquals(false, profile.hasHeader());
        profile.setQuoteCharacter("|");
        long rows = fileProcessor.processFile(profile, "/data/pac.txt");
        assertEquals(1L, rows);
        for (EntityInputBean entityInputBean : fdTemplate.getEntities()) {
            assertEquals("The segment was not set in to the EntityInput", "2014", entityInputBean.getSegment());
        }
    }


}
