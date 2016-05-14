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

import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.TagHelper;
import org.flockdata.model.Alias;
import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.model.Tag;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.model.TagSearchChange;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by mike on 16/05/16.
 */
public class TestTagChanges {
    @Test
    public void serialization () throws Exception {
        TagSearchChange searchChange = getSimpleTagInput("testIndex", "TheCompany", "TheFortress", "TheCode", "TheLabel");

        String json = JsonUtils.toJson(searchChange);
        assertNotNull(json);
        TagSearchChange deserializedChange = JsonUtils.toObject(json.getBytes(), TagSearchChange.class);
        assertEquals (searchChange.getType(), deserializedChange.getType());
        assertEquals(searchChange.getAliases().size(), deserializedChange.getAliases().size());

    }
    public static TagSearchChange getSimpleTagInput (String indexName, String companyName, String fortressName, String code, String label){
        Company company = new Company(companyName);
        Fortress fortress = new Fortress(new FortressInputBean(fortressName), company);
        TagInputBean tagInputBean = new TagInputBean(code, label);
        Tag tag = new Tag(tagInputBean);
        String key = TagHelper.parseKey(code+"Alias");
        Alias alias = new Alias(tagInputBean.getLabel(), new AliasInputBean(code+"Alias", "someAliasDescription"),key, tag);
        tag.addAlias( alias);
        return new TagSearchChange(indexName, fortress, tag);
    }
}
