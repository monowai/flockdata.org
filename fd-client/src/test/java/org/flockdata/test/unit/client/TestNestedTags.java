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
import org.flockdata.profile.ExtractProfileDeserializer;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.model.ExtractProfile;
import org.flockdata.registration.TagInputBean;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 *
 * Created by mike on 1/03/15.
 */
public class TestNestedTags extends AbstractImport {

    @Test
    public void label_missingColumnDoesNotCreateTargetTag() throws Exception {
        ContentModel contentModel = ContentModelDeserializer.getContentModel("/model/interest-groups.json");
        ExtractProfile extractProfile = ExtractProfileDeserializer.getImportProfile("/import/csv-header-pipe-quote.json", contentModel);

        fileProcessor.processFile(extractProfile, "/data/tags-inputs.csv");

        List<TagInputBean> tagInputBeans = getFdBatcher().getTags();
        // The profile defines a nested tag but the value is missing in the source

        assertEquals(1, tagInputBeans.size());
        for (TagInputBean tagInputBean : tagInputBeans) {
            System.out.println(tagInputBean);
            assertFalse("The target tag should not exist as the source value was missing", tagInputBean.hasTargets());
        }
    }
}
