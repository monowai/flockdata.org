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

package org.flockdata.test.helper;

import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.transform.ColumnDefinition;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * Created by mike on 12/07/16.
 */
public class TestContentModel {

    @Test
    public void serializeEntityTagRelationship() throws Exception{
        ContentModel model = ContentModelDeserializer.getContentModel("/model/entity-tag-relationship.json");
        assertNotNull ( model);
        ColumnDefinition jurisdiction = model.getContent().get("jurisdiction_description");
        assertNotNull ( jurisdiction);
        assertNotNull ( jurisdiction.getEntityTagLinks());
        assertEquals (1, jurisdiction.getEntityTagLinks().size());
    }

}
