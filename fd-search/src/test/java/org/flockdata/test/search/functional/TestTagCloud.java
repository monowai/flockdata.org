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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Map;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchChanges;
import org.flockdata.search.TagCloud;
import org.flockdata.search.TagCloudParams;
import org.flockdata.search.dao.QueryDaoES;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.test.helper.MockDataFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author mholdsworth
 * @since 15/08/2014
 */
@RunWith(SpringRunner.class)
public class TestTagCloud extends ESBase {
  @Autowired
  QueryDaoES queryDaoES;

  @Test
  public void defaultTagQueryWorks() throws Exception {
    Map<String, Object> json = ContentDataHelper.getBigJsonText(20);

    String fort = "defaultTagQueryWorks";
    String comp = "comp";
    String user = "user";
    String doc = fort;

    Entity entity = getEntity(comp, fort, user, doc);

    EntitySearchChange change = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity));
    change.setDescription("Test Description");
    change.setData(json);
    ArrayList<EntityTag> tags = new ArrayList<>();

    TagInputBean tag = new TagInputBean("my TAG", "TheLabel", "rlxname");
    assertEquals("my TAG", tag.getCode());// we should be able to find this as lowercase
    tags.add(MockDataFactory.getEntityTag(entity, tag, "rlxname"));
    change.setStructuredTags(null, tags);

    //deleteEsIndex(entity);

    esSearchWriter.createSearchableChange(new SearchChanges(change));
    Thread.sleep(1000);
    TagCloudParams tagCloudParams = new TagCloudParams();
    tagCloudParams.setCompany(entity.getSegment().getCompany().getName());
    tagCloudParams.setFortress(entity.getFortress().getName());
    tagCloudParams.addType(entity.getType());
    ArrayList<String> rlxs = new ArrayList<>();
    rlxs.add("rlxname");
    tagCloudParams.setRelationships(rlxs);

    // ToDo: Fix this
//        TagCloud tagCloud = queryDaoES.getCloudTag(tagCloudParams);
//        assertEquals(20, tagCloud.getTerms().get("now").intValue());
//        assertEquals(20, tagCloud.getTerms().get("is").intValue());
//        assertEquals(20, tagCloud.getTerms().get("time").intValue());
//        assertEquals(20, tagCloud.getTerms().get("for").intValue());
//        assertEquals(20, tagCloud.getTerms().get("all").intValue());
//        assertEquals(20, tagCloud.getTerms().get("good").intValue());
//        assertEquals(20, tagCloud.getTerms().get("men").intValue());
//        assertEquals(20, tagCloud.getTerms().get("come").intValue());
//        assertEquals(20, tagCloud.getTerms().get("aid").intValue());
//        assertEquals(20, tagCloud.getTerms().get("of").intValue());
//        assertEquals(20, tagCloud.getTerms().get("party").intValue());
//        assertEquals(1, tagCloud.getTerms().get("my").intValue());
//        assertEquals(1, tagCloud.getTerms().get("tag").intValue());

    // TODO to get the tag cloud working this asserts must wrok and be uncommented
    //assertEquals(60, tagCloud.getTerms().get("the").intValue());
    //assertEquals(40, tagCloud.getTerms().get("to").intValue());

  }

  @Test
  public void pojo_TagCloud() {
    int count = 10;
    TagCloud tagCloud = new TagCloud(count);

    long max = 100l;
    // 100 random keys and values
    for (long lValue = max; lValue > 0; lValue--) {
      tagCloud.addTerm("key" + lValue, lValue);
    }
    tagCloud.scale();
    assertEquals("Unexpected term count", count, tagCloud.getTerms().size());

    max = 100l;
    tagCloud = new TagCloud(count);

    // Checking that the same value for different keys still results in a map
    // populated to capacity
    for (long lValue = max; lValue > 0; lValue--) {
      tagCloud.addTerm("keyA" + lValue, lValue);
      tagCloud.addTerm("keyB" + lValue, lValue);
      tagCloud.addTerm("keyC" + lValue, lValue);
      tagCloud.addTerm("keyD" + lValue, lValue);
      tagCloud.addTerm("keyE" + lValue, lValue);
    }
    tagCloud.scale();
    assertEquals(count, tagCloud.getTerms().size());

    max = 100l;
    tagCloud = new TagCloud(10);

    for (long lValue = max; lValue > 0; lValue--) {
      tagCloud.addTerm("keyA" + lValue, 10);
    }
    tagCloud.scale();
    assertEquals("Adding the same value to different key should result in a full populated map", 10, tagCloud.getTerms().size());

  }


}
