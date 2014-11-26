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

import org.flockdata.company.model.CompanyNode;
import org.flockdata.company.model.FortressNode;
import org.flockdata.company.model.FortressUserNode;
import org.flockdata.engine.schema.model.DocumentTypeNode;
import org.flockdata.engine.tag.model.TagNode;
import org.flockdata.engine.track.model.EntityNode;
import org.flockdata.engine.track.model.EntityTagRelationship;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.FortressUser;
import org.flockdata.search.endpoint.ElasticSearchEP;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityBean;
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

import static org.junit.Assert.assertNotNull;

/**
 * User: mike
 * Date: 15/08/14
 * Time: 12:53 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:root-context.xml"})
public class TestMappings extends ESBase {
    @Autowired
    TrackSearchDao searchRepo;

    @Autowired
    ElasticSearchEP searchEP;

    @Test
    public void defaultTagQueryWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        // These are the minimum objects necessary to create Entity data
        Fortress fortress = new FortressNode(new FortressInputBean("fort", false), new CompanyNode("comp")) ;
        FortressUser user = new FortressUserNode(fortress, "mikey");
        DocumentTypeNode doc = new DocumentTypeNode(fortress, fortress.getName());

        DateTime now = new DateTime();
        EntityInputBean mib = getEntityInputBean(doc, user, "zzaa99", now);

        Entity entity = new EntityNode("zzUnique", fortress, mib, doc, user);

        SearchChange change = new EntitySearchChange(new EntityBean(entity));
        change.setDescription("Test Description");
        change.setWhat(json);
        ArrayList<EntityTag> tags = new ArrayList<>();

        TagNode tag = new TagNode(new TagInputBean("myTag", "TheLabel", "rlxname"));
        tag.setCode("my TAG");// we should be able to find this as lowercase
        tags.add(new EntityTagRelationship(66l, tag));
        change.setTags(tags);


        deleteEsIndex(entity.getFortress().getIndexName());

        change = searchRepo.update(change);
        Thread.sleep(1000);
        assertNotNull(change);
        assertNotNull(change.getSearchKey());
        entity.setSearchKey(change.getSearchKey());
        json = searchRepo.findOne(entity);

        // In this test, @tag.*.code is NOT_ANALYZED so it should find the value with a space in it
        // We also expect the code to be lower case
        doTermQuery(entity.getFortress().getIndexName(), "@tag.mytag.code", "my tag", 1, "Case insensitive search of tag codes is not working");
        assertNotNull(json);

    }

    @Test
    public void testWhatIndexingDefaultAttributeWithNGram() throws Exception {
        Fortress fortress = new FortressNode(new FortressInputBean("fort2", false), new CompanyNode("comp2")) ;
        FortressUser user = new FortressUserNode(fortress, "mikey");
        DocumentTypeNode doc = new DocumentTypeNode(fortress, fortress.getName());

        DateTime now = new DateTime();
        EntityInputBean mib = getEntityInputBean(doc, user, now.toString(), now);
        mib.setDescription("This is a description");

        Entity entity = new EntityNode(Long.toString(now.getMillis()), fortress, mib, doc, user);

        deleteEsIndex(entity.getFortress().getIndexName());

        Map<String, Object> what = Helper.getSimpleMap(
                  EntitySearchSchema.WHAT_CODE, "AZERTY");
        what.put( EntitySearchSchema.WHAT_NAME, "NameText");
        what.put( EntitySearchSchema.WHAT_DESCRIPTION, "This is a description");
        ContentInputBean log = new ContentInputBean(user.getCode(), now, what);
        mib.setContent(log);
        SearchChange change = new EntitySearchChange(new EntityBean(entity));
        change.setWhat(what);

        SearchChange searchResult = searchRepo.update(change);
        assertNotNull(searchResult);
        Thread.sleep(2000);
        doQuery(entity.getFortress().getIndexName(), "AZERTY", 1);

        doTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_DESCRIPTION, "des", 1);
        doTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.DESCRIPTION, "des", 1);
        doTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_DESCRIPTION, "de", 0);
        doTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_DESCRIPTION, "descripti", 1);
        doTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_DESCRIPTION, "descriptio", 1);
        // ToDo: Figure out ngram mappings
//        doEsTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_DESCRIPTION, "is is a de", 1);

        doTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_NAME, "name", 1);
        doTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_NAME, "nam", 1);
        doTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_NAME, "nametext", 1);

        doTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_CODE, "az", 1);
        doTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_CODE, "azer", 1);
        doTermQuery(entity.getFortress().getIndexName(), EntitySearchSchema.WHAT + "." + EntitySearchSchema.WHAT_CODE, "azerty", 0);

    }


    @Test
    public void testCustomMappingWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity entityA = getEntity("cust", "fort", "anyuser");
        Entity entityB = getEntity("cust", "fortb", "anyuser");

        SearchChange changeA = new EntitySearchChange(new EntityBean(entityA), new ContentInputBean(json));
        SearchChange changeB = new EntitySearchChange(new EntityBean(entityB), new ContentInputBean(json));

        // FortB will have
        changeA.setDescription("Test Description");
        changeB.setDescription("Test Description");

        deleteEsIndex(entityA.getFortress().getIndexName());
        deleteEsIndex(entityB.getFortress().getIndexName());

        changeA = searchRepo.update(changeA);
        changeB = searchRepo.update(changeB);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeB);
        assertNotNull(changeA.getSearchKey());
        assertNotNull(changeB.getSearchKey());

        // by default we analyze the @description field
        doDefaultFieldQuery(entityA.getFortress().getIndexName(), "@description", changeA.getDescription(), 1);

        // In fortb.json we don't analyze the description (overriding the default) so it shouldn't be found
        doDefaultFieldQuery(entityB.getFortress().getIndexName(), "@description", changeB.getDescription(), 0);

    }

    @Test
    public void sameIndexDifferentDocumentsHaveMappingApplied() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity entityA = getEntity("cust", "fort", "anyuser", "fortdoc");
        Entity entityB = getEntity("cust", "fort", "anyuser", "doctype");


        SearchChange changeA = new EntitySearchChange(new EntityBean(entityA), new ContentInputBean(json));
        SearchChange changeB = new EntitySearchChange(new EntityBean(entityB), new ContentInputBean(json));

        TagNode tag = new TagNode(new TagInputBean("myTag", "TheLabel", "rlxname"));
        tag.setCode("my TAG");// we should be able to find this as lowercase
        ArrayList<EntityTag> tags = new ArrayList<>();
        tags.add(new EntityTagRelationship(66l, tag));
        changeA.setTags(tags);
        changeB.setTags(tags);

        deleteEsIndex(entityA.getFortress().getIndexName());
        deleteEsIndex(entityB.getFortress().getIndexName());

        changeA = searchRepo.update(changeA);
        changeB = searchRepo.update(changeB);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeB);
        assertNotNull(changeA.getSearchKey());
        assertNotNull(changeB.getSearchKey());

        doFieldQuery(entityA.getFortress().getIndexName(), entityA.getDocumentType().toLowerCase(), "@tag.mytag.code", "my tag", 1);
        doFieldQuery(entityB.getFortress().getIndexName(), entityB.getDocumentType().toLowerCase(), "@tag.mytag.code", "my tag", 1);
        doTermQuery(entityB.getFortress().getIndexName(), "@tag.mytag.code", "my tag", 2);

    }


}
