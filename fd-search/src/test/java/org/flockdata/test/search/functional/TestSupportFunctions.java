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

package org.flockdata.test.search.functional;

import org.flockdata.model.Entity;
import org.flockdata.model.EntityTag;
import org.flockdata.model.EntityTagOut;
import org.flockdata.model.Tag;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.FdSearch;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.SearchChanges;
import org.flockdata.search.model.SearchResults;
import org.flockdata.test.helper.EntityContentHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author mholdsworth
 * @since 2/05/2015
 */

@RunWith(SpringRunner.class)
@SpringApplicationConfiguration(FdSearch.class)
public class TestSupportFunctions extends ESBase {
    @Test
    public void defaultTagQueryWorks() throws Exception {
        Map<String, Object> json = EntityContentHelper.getBigJsonText(20);

        String fortress = "fortress";
        String company = "company";
        String doc = "doc";
        String user = "mike";

        Entity entity = getEntity(company, fortress, user, doc);

        EntitySearchChange change = new EntitySearchChange(entity, indexManager.parseIndex(entity));
        change.setDescription("Test Description");
        change.setData(json);
        ArrayList<EntityTag> tags = new ArrayList<>();

        TagInputBean tagInput = new TagInputBean("myTag", "TheLabel", "rlxname").setCode("my TAG");
        Tag tag = new Tag(tagInput);
        tags.add(new EntityTagOut(entity, tag, "mytag", null));
        change.setStructuredTags(tags);

        SearchResults searchResults = esSearchWriter.createSearchableChange(new SearchChanges(change));
        Thread.sleep(1000);

        queryServiceEs.getTags(entity.getFortress().getRootIndex());

    }
}
