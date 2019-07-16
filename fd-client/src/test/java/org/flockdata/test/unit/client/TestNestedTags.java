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

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.flockdata.data.ContentModel;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.flockdata.transform.json.ExtractProfileDeserializer;
import org.flockdata.transform.model.ExtractProfile;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 1/03/2015
 */
public class TestNestedTags extends AbstractImport {

    @Test
    public void label_missingColumnDoesNotCreateTargetTag() throws Exception {
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/interest-groups.json");
        ExtractProfile extractProfile = ExtractProfileDeserializer.getImportProfile("/import/csv-header-pipe-quote.json", contentModel);

        fileProcessor.processFile(extractProfile, "/data/tags-inputs.csv");

        List<TagInputBean> tagInputBeans = getTemplate().getTags();
        // The profile defines a nested tag but the value is missing in the source

        assertEquals(1, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            assertFalse("The target tag should not exist as the source value was missing", tagInputBean.hasTargets());
        }
    }
}
