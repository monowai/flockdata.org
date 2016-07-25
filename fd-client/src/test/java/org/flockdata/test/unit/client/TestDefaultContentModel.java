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

import org.flockdata.profile.ContentModelHandler;
import org.flockdata.profile.ContentValidationRequest;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.TransformationHelper;
import org.flockdata.transform.Transformer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

/**
 * Hook to test default ContentModel functionality
 * Created by mike on 26/07/16.
 */
public class TestDefaultContentModel {

    @Test
    public void defaultModel() throws Exception{

        Map<String,Object> row = new HashMap<>();
        Collection<Map<String,Object>>rows = new ArrayList<>();

        row.put("byteArray", "0x0000000001EC3C0F");
        row.put("nullValue", null);
        rows.add(row);

        ContentValidationRequest contentRequest = new ContentValidationRequest(rows);
        ContentModel result = contentRequest.getContentModel();

        if (result == null)
            result = new ContentModelHandler();

        result.setContent(Transformer.fromMapToModel(contentRequest.getRows()));

        assertEquals("nullValue should not get a Column Definition", row.size()-1, result.getContent().size());

        for (String key: result.getContent().keySet()) {
            ColumnDefinition columnDefinition = result.getContent().get(key);
            if (key.equalsIgnoreCase("byteArray")) {
                assertEquals("string", columnDefinition.getDataType());
                assertEquals(row.get(key), TransformationHelper.getObject(row, row.get("byteArray"),columnDefinition, key));
            }   else
                throw new Exception("Unexpected ColDef "+ columnDefinition);
        }



    }
}
