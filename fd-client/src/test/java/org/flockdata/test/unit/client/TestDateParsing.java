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

import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ExtractProfileHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ExtractProfile;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * Date helper functional tests
 *
 * @author mholdsworth
 * @since 27/06/2016
 */
public class TestDateParsing extends AbstractImport {

    @Test
    public void testSegmentsEvaluate() throws Exception{
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/date-calculation.json");
        ExtractProfile importProfile = new ExtractProfileHandler(contentModel, true)
                .setDelimiter("\t");

        fileProcessor.processFile(importProfile, "/data/date-calculation.txt");
        List<EntityInputBean> entities = fdWriter.getEntities();

        assertEquals(1, entities.size());
         // Asserts that the Dates helper class is doing its thing
        for (EntityInputBean entity : entities) {
            assertNotNull( entity.getSegment());
            //2015-04-28
            assertEquals("2015-04-28-02", entity.getSegment());
        }

    }
}
