/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
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
import org.flockdata.search.model.SearchChanges;
import org.flockdata.search.model.TagSearchChange;
import org.flockdata.track.bean.SearchChange;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author mholdsworth
 * @since 16/05/2016
 */
public class TestTagChanges {
    public static TagSearchChange getSimpleTagInput(String indexName, String companyName, String fortressName, String code, String label) {
        Company company = new Company(companyName);
        Fortress fortress = new Fortress(new FortressInputBean(fortressName), company);
        TagInputBean tagInputBean = new TagInputBean(code, label);
        Tag tag = new Tag(tagInputBean);
        String key = TagHelper.parseKey(code + "Alias");
        Alias alias = new Alias(tagInputBean.getLabel(), new AliasInputBean(code + "Alias", "someAliasDescription"), key, tag);
        tag.addAlias(alias);
        return new TagSearchChange(indexName, tag);
    }

    @Test
    public void serializationTagSearchChange () throws Exception {
        TagSearchChange searchChange = getSimpleTagInput("testIndex", "TheCompany", "TheFortress", "TheCode", "TheLabel");

        String json = JsonUtils.toJson(searchChange);
        assertNotNull(json);
        Collection<SearchChange>changes = new ArrayList<>();
        TagSearchChange deserializedChange = JsonUtils.toObject(json.getBytes(), TagSearchChange.class);
        assertEquals (searchChange.getType(), deserializedChange.getType());
        assertEquals(searchChange.getAliases().size(), deserializedChange.getAliases().size());

        changes.add(searchChange);
        SearchChanges searchChanges = new SearchChanges(changes);
        json = JsonUtils.toJson(searchChanges);

        SearchChanges deserializedChanges =JsonUtils.toObject(json.getBytes(), SearchChanges.class);
        assertNotNull( deserializedChanges);
        assertEquals(1, deserializedChanges.getChanges().size());
        assertTrue (deserializedChanges.getChanges().iterator().next() instanceof TagSearchChange);


    }
}
