/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

import static junit.framework.TestCase.assertNotNull;

import java.util.ArrayList;
import java.util.Map;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchChanges;
import org.flockdata.search.SearchResults;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.test.helper.MockDataFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author mholdsworth
 * @since 2/05/2015
 */

@RunWith(SpringRunner.class)
public class TestSupportFunctions extends ESBase {
  @Test
  public void defaultTagQueryWorks() throws Exception {
    Map<String, Object> json = ContentDataHelper.getBigJsonText(20);

    String fortress = "fortress";
    String company = "company";
    String doc = "doc";
    String user = "mike";

    Entity entity = getEntity(company, fortress, user, doc);

    EntitySearchChange change = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity));
    change.setDescription("Test Description");
    change.setData(json);
    ArrayList<EntityTag> tags = new ArrayList<>();

    TagInputBean tagInput = new TagInputBean("myTag", "TheLabel", "rlxname").setCode("my TAG");
    tags.add(MockDataFactory.getEntityTag(entity, tagInput, "mytag"));
    change.setStructuredTags(tags);

    SearchResults searchResults = esSearchWriter.createSearchableChange(new SearchChanges(change));
    Thread.sleep(1000);
    assertNotNull(searchResults);
    queryServiceEs.getTags(entity.getFortress().getRootIndex());

  }
}
