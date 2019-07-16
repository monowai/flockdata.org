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

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import junit.framework.TestCase;
import org.flockdata.data.ContentModel;
import org.flockdata.helper.FlockException;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.model.ExtractProfileHandler;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 17/12/2015
 */
public class TestCalculatedColumns extends AbstractImport {
    @Test
    public void string_headerWithDelimiter() throws Exception {
        // DAT-527

        ContentModel params = ContentModelDeserializer.getContentModel("/model/calculatedcolumns.json");

        long rows = fileProcessor.processFile(new ExtractProfileHandler(params), "/data/calculatedcolumns.csv");
        int expectedRows = 1;
        assertEquals(expectedRows, rows);
        List<EntityInputBean> entityInputBeans = fdTemplate.getEntities();

        for (EntityInputBean entityInputBean : entityInputBeans) {
            //BulkHours,ScheduledHours,Hours
            TestCase.assertEquals(1d, entityInputBean.getContent().getData().get("BulkHours"));
            TestCase.assertEquals(8.5d, entityInputBean.getContent().getData().get("ScheduledHours"));
            TestCase.assertEquals(9d, entityInputBean.getContent().getData().get("Hours"));
            // VarianceHours is a dynamic column
            TestCase.assertNotNull("Calculated column should have been created", entityInputBean.getContent().getData().get("VarianceHours"));
            TestCase.assertEquals(.5d, entityInputBean.getContent().getData().get("VarianceHours"));

            TestCase.assertNotNull("Calculated column should have been created", entityInputBean.getContent().getData().get("WorkHours"));
            TestCase.assertEquals(10d, entityInputBean.getContent().getData().get("WorkHours"));
            TestCase.assertEquals("Value should have come from the calculated column", 10d, entityInputBean.getProperties().get("value"));
            TestCase.assertNotNull("Computed column, not in source data, was not added", entityInputBean.getContent().getData().get("computedOnly"));
            TestCase.assertEquals("Computed column, not in source data, was not computed", 100d, entityInputBean.getContent().getData().get("computedOnly"));
        }

        // Check that the payload will serialize
        ObjectMapper om = new ObjectMapper();
        try {
            om.writeValueAsString(entityInputBeans);
        } catch (Exception e) {
            throw new FlockException("Failed to serialize");
        }

    }

}
