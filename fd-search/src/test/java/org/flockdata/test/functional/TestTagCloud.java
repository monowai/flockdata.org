/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.test.functional;

import org.flockdata.engine.repo.neo4j.model.*;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.dao.neo4j.model.CompanyNode;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.FortressUser;
import org.flockdata.search.dao.QueryDaoES;
import org.flockdata.search.endpoint.ElasticSearchEP;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.TagCloud;
import org.flockdata.search.model.TagCloudParams;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityTag;
import org.flockdata.track.model.SearchChange;
import org.flockdata.track.model.TrackSearchDao;
import org.joda.time.DateTime;
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
    ElasticSearchEP searchEP;

    @Autowired
    QueryDaoES queryDaoES;

    @Test
    public void defaultTagQueryWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        // These are the minimum objects necessary to create Entity data
        Fortress fortress = new FortressNode(new FortressInputBean("fort1", false), new CompanyNode("comp")) ;
        FortressUser user = new FortressUserNode(fortress, "mikey");
        DocumentTypeNode doc = new DocumentTypeNode(fortress, fortress.getName());

        DateTime now = new DateTime();
        EntityInputBean mib = getEntityInputBean(doc, user, "zzaa99", now);

        Entity entity = new EntityNode("zzUnique", fortress, mib, doc, user);

        SearchChange change = new EntitySearchChange(entity);
        change.setDescription("Test Description");
        change.setWhat(json);
        ArrayList<EntityTag> tags = new ArrayList<>();

        TagNode tag = new TagNode(new TagInputBean("myTag", "TheLabel", "rlxname"));
        tag.setCode("my TAG");// we should be able to find this as lowercase
        tags.add(new EntityTagRelationship(66l, tag));
        change.setTags(tags);

        deleteEsIndex(entity.getIndexName());

        trackRepo.update(change);
        Thread.sleep(1000);
        TagCloudParams tagCloudParams = new TagCloudParams();
        tagCloudParams.setCompany(entity.getFortress().getCompany().getName());
        tagCloudParams.setFortress(entity.getFortress().getName());
        tagCloudParams.setType(entity.getDocumentType());

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
