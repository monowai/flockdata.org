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

import junit.framework.TestCase;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ContentModelHandler;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ColumnDefinition;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.*;

/**
 * Basic deserialization checks
 * Created by mike on 26/01/16.
 */
public class TestContentModel {

    @Test
    public void contentModelDeserializes() throws Exception {
        String fileName = "/model/document-type.json";
        ContentModel model = ContentModelDeserializer.getContentModel(fileName);
        assertNotNull(model.getDocumentType());
        TestCase.assertEquals("PAC", model.getDocumentType().getName());
    }

    @Test
    public void mergeColumnDefinitions() throws Exception{
        ContentModel contentModel = new ContentModelHandler();
        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setDataType("String");
        Map<String,ColumnDefinition> columns = new HashMap<>();
        columns.put("Existing", columnDefinition);
        contentModel.setContent(columns);

        columns = new HashMap<>(); // Create new payload to merge
        columns.put("Second", columnDefinition);

        assertEquals("Existing model did not have 1 ColDef", 1, contentModel.getContent().size());

        contentModel.setContent(columns);
        assertEquals("Second ColDef did not merge with the existing one", 2, contentModel.getContent().size());

    }

    @Test
    public void suppressionFlags() throws Exception {
        ContentModel model = ContentModelDeserializer.getContentModel("/model/track-suppression.json");
        Map<String,Object> row = new HashMap<>();
        row.put("blah", 10);
        EntityInputBean result = org.flockdata.transform.Transformer.toEntity(row, model);
        assertNotNull (result);
        assertTrue(result.isSearchSuppressed());
        assertTrue(result.isTrackSuppressed());
    }

}
