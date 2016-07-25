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
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.Transformer;
import org.flockdata.transform.tags.TagMapper;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tag targets are handled
 * Created by mike on 27/01/15.
 */
public class TestTabTags {
    @Test
    public void string_NestedTags() throws Exception {
        ContentModel params = ContentModelDeserializer.getContentModel("/model/sectors.json");
        TagMapper tagMapper = new TagMapper();
        String[] headers = new String[]{"Catcode","Catname","Catorder","Industry","Sector","Sector Long"};
        String[] data = new String[]{"F2600","Private Equity & Investment Firms","F07","Securities & Investment","Finance/Insur/RealEst","Finance","Insurance & Real Estate"};

        Map<String, Object> json = tagMapper.setData(Transformer.convertToMap(headers, data, new ExtractProfileHandler(params)),params);
        assertEquals(1, tagMapper.getTags().size());
        TagInputBean tag = tagMapper.getTags().iterator().next();

        assertNotNull(json);
        assertNotNull(tagMapper);
        assertEquals("Code does not match", "F2600", tag.getCode());
        assertEquals("Name does not match", "Private Equity & Investment Firms", tag.getName());
        assertNotNull(tag.getProperties().get("order"));
        assertEquals(1, tag.getTargets().size());
        Collection<TagInputBean> targets = tag.getTargets().get("comprises");
        assertEquals(1, targets.size());
        tag = targets.iterator().next();
        assertNotNull(tag.getDescription());
    }

}
