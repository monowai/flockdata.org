/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.search.functional;

import org.flockdata.model.Entity;
import org.flockdata.model.EntityTag;
import org.flockdata.model.EntityTagOut;
import org.flockdata.model.Tag;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.search.dao.QueryDaoES;
import org.flockdata.search.endpoint.FdQueryEP;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.EntitySearchChanges;
import org.flockdata.search.model.TagCloud;
import org.flockdata.search.model.TagCloudParams;
import org.flockdata.search.service.TrackSearchDao;
import org.flockdata.test.engine.Helper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 15/08/14
 * Time: 12:53 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class TestTagCloud extends ESBase {
    @Autowired
    TrackSearchDao trackRepo;

    @Autowired
    FdQueryEP searchEP;

    @Autowired
    QueryDaoES queryDaoES;

    @Test
    public void defaultTagQueryWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        String fort = "defaultTagQueryWorks";
        String comp = "comp";
        String user = "user";
        String doc = fort;

        Entity entity = getEntity(comp, fort, user, doc);

        EntitySearchChange change = new EntitySearchChange(entity, indexHelper.parseIndex(entity));
        change.setDescription("Test Description");
        change.setData(json);
        ArrayList<EntityTag> tags = new ArrayList<>();

        Tag tag = new Tag(new TagInputBean("myTag", "TheLabel", "rlxname"));
        tag.setCode("my TAG");// we should be able to find this as lowercase
        tags.add(new EntityTagOut(entity, tag, "rlxname", null));
        change.setStructuredTags(null, tags);

        //deleteEsIndex(entity);

        trackService.createSearchableChange(new EntitySearchChanges(change));
        Thread.sleep(1000);
        TagCloudParams tagCloudParams = new TagCloudParams();
        tagCloudParams.setCompany(entity.getSegment().getCompany().getName());
        tagCloudParams.setFortress(entity.getFortress().getName());
        tagCloudParams.addType(entity.getType());
        ArrayList<String>rlxs = new ArrayList<>();
        rlxs.add("rlxname");
        tagCloudParams.setRelationships(rlxs);

        TagCloud tagCloud = queryDaoES.getCloudTag(tagCloudParams);
        // ToDo: Fix this
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
    public void pojo_TagCloud(){
        int count = 10;
        TagCloud tagCloud = new TagCloud(count);

        long max =100l;
        // 100 random keys and values
        for (long lValue=max; lValue > 0; lValue--) {
            tagCloud.addTerm("key" +lValue, lValue);
        }
        tagCloud.scale();
        assertEquals("Unexpected term count", count, tagCloud.getTerms().size());

        max =100l;
        tagCloud = new TagCloud(count);

        // Checking that the same value for different keys still results in a map
        // populated to capacity
        for (long lValue=max; lValue > 0; lValue--) {
            tagCloud.addTerm("keyA" +lValue, lValue);
            tagCloud.addTerm("keyB" +lValue, lValue);
            tagCloud.addTerm("keyC" +lValue, lValue);
            tagCloud.addTerm("keyD" +lValue, lValue);
            tagCloud.addTerm("keyE" +lValue, lValue);
        }
        tagCloud.scale();
        assertEquals(count, tagCloud.getTerms().size());

        max =100l;
        tagCloud = new TagCloud(10);

        for (long lValue=max; lValue > 0; lValue--) {
            tagCloud.addTerm("keyA"+lValue, 10);
        }
        tagCloud.scale();
        assertEquals("Adding the same value to different key should result in a full populated map", 10, tagCloud.getTerms().size());

    }


}
