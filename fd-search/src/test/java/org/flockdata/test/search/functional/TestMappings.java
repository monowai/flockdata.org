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

package org.flockdata.test.search.functional;

import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Tag;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.test.engine.Helper;
import org.flockdata.test.engine.SimpleEntityTagRelationship;
import org.flockdata.test.engine.SimpleTag;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.model.*;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void defaultTagQueryWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        String fortress = "fort";
        String company = "test";
        String doc = "doc";
        String user = "mike";

        Entity entity = Helper.getEntity(company, fortress, user, doc);

        SearchChange change = new EntitySearchChange(new EntityBean(entity));
        change.setDescription("Test Description");
        change.setWhat(json);
        ArrayList<EntityTag> tags = new ArrayList<>();

        TagInputBean tagInput = new TagInputBean("myTag", "TheLabel", "rlxname");
        tagInput.setCode("my TAG");
        Tag tag = new SimpleTag(tagInput);

        tags.add(new SimpleEntityTagRelationship(entity, tag, "mytag", null));


        change.setTags(tags);


        deleteEsIndex(entity.getFortress().getIndexName());
        searchRepo.ensureIndex(change.getIndexName(), change.getDocumentType());
        change = searchRepo.handle(change);
        Thread.sleep(1000);
        assertNotNull(change);
        assertNotNull(change.getSearchKey());
        entity.setSearchKey(change.getSearchKey());
        json = searchRepo.findOne(entity);

        // In this test, @tag.*.code is NOT_ANALYZED so it should find the value with a space in it
        // We also expect the code to be lower case
        doTermQuery(entity.getFortress().getIndexName(), "tag.mytag.thelabel.code.facet", "my TAG", 1, "Full text match of tag codes is not working");
//        doTermQuery(entity.getFortress().getIndexName(), "tag.mytag.code", "my tag", 1, "Case insensitive text match of tag codes is not working");
        //doTermQuery(entity.getFortress().getIndexName(), "tag.mytag.code", "my", 1, "Keyword search of tag codes is not working");
//        doTermQuery(entity.getFortress().getIndexName(), "tag.mytag.code.analyzed", "my tag", 1, "Case insensitive search of tag codes is not working");
        assertNotNull(json);

    }

    @Test
    public void testWhatIndexingDefaultAttributeWithNGram() throws Exception {
        String comp = "comp2";
        String fort = "fort2";
        String user = "mikey";
        String doc = fort;

        DateTime now = new DateTime();

        Entity entity = Helper.getEntity(comp, fort, user, doc);

        deleteEsIndex(entity.getFortress().getIndexName());

        Map<String, Object> what = Helper.getSimpleMap(
                EntitySearchSchema.WHAT_CODE, "AZERTY");
        what.put(EntitySearchSchema.WHAT_NAME, "NameText");
        what.put(EntitySearchSchema.WHAT_DESCRIPTION, "This is a description");

        SearchChange change = new EntitySearchChange(new EntityBean(entity));
        change.setDescription("This is a description");
        change.setWhat(what);

        searchRepo.ensureIndex(change.getIndexName(), change.getDocumentType());
        SearchChange searchResult = searchRepo.handle(change);
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
        Entity entityA = Helper.getEntity("cust", "fort", "anyuser", "fort");
        Entity entityB = Helper.getEntity("cust", "fortb", "anyuser", "fortb");

        SearchChange changeA = new EntitySearchChange(new EntityBean(entityA), new ContentInputBean(json));
        SearchChange changeB = new EntitySearchChange(new EntityBean(entityB), new ContentInputBean(json));

        // FortB will have
        changeA.setDescription("Test Description");
        changeB.setDescription("Test Description");

        deleteEsIndex(entityA.getFortress().getIndexName());
        deleteEsIndex(entityB.getFortress().getIndexName());

        searchRepo.ensureIndex(changeA.getIndexName(), changeA.getDocumentType());
        searchRepo.ensureIndex(changeB.getIndexName(), changeB.getDocumentType());
        changeA = searchRepo.handle(changeA);
        changeB = searchRepo.handle(changeB);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeB);
        assertNotNull(changeA.getSearchKey());
        assertNotNull(changeB.getSearchKey());

        // by default we analyze the @description field
        doDefaultFieldQuery(entityA.getFortress().getIndexName(), "description", changeA.getDescription(), 1);

        // In fortb.json we don't analyze the description (overriding the default) so it shouldn't be found
        doDefaultFieldQuery(entityB.getFortress().getIndexName(), "description", changeB.getDescription(), 0);

    }

    @Test
    public void sameIndexDifferentDocumentsHaveMappingApplied() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity entityA = Helper.getEntity("cust", "fort", "anyuser", "fortdoc");
        Entity entityB = Helper.getEntity("cust", "fort", "anyuser", "doctype");


        SearchChange changeA = new EntitySearchChange(new EntityBean(entityA), new ContentInputBean(json));
        SearchChange changeB = new EntitySearchChange(new EntityBean(entityB), new ContentInputBean(json));

        SimpleTag tag = new SimpleTag(new TagInputBean("myTag", "TheLabel", "rlxname"));
        tag.setCode("my TAG");// we should be able to find this as lowercase
        assertEquals("my TAG", tag.getCode());
        ArrayList<EntityTag> tagsA = new ArrayList<>();
        tagsA.add(new SimpleEntityTagRelationship(entityA, tag, "mytag", null));

        ArrayList<EntityTag> tagsB = new ArrayList<>();
        tagsB.add(new SimpleEntityTagRelationship(entityB, tag, "mytag", null));

        changeA.setTags(tagsA);
        changeB.setTags(tagsB);

        deleteEsIndex(entityA.getFortress().getIndexName());
        deleteEsIndex(entityB.getFortress().getIndexName());

        searchRepo.ensureIndex(changeA.getIndexName(), changeA.getDocumentType());
        searchRepo.ensureIndex(changeB.getIndexName(), changeB.getDocumentType());
        changeA = searchRepo.handle(changeA);
        changeB = searchRepo.handle(changeB);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeB);
        assertNotNull(changeA.getSearchKey());
        assertNotNull(changeB.getSearchKey());

        doTermQuery(entityA.getFortress().getIndexName(), entityA.getDocumentType().toLowerCase(), "tag.mytag.thelabel.code.facet", tag.getCode(), 1);
        doTermQuery(entityB.getFortress().getIndexName(), entityB.getDocumentType().toLowerCase(), "tag.mytag.thelabel.code.facet", tag.getCode(), 1);
        doTermQuery(entityB.getFortress().getIndexName(), "tag.mytag.thelabel.code.facet", tag.getCode(), 2);

    }

    @Test
    public void tagWithRelationshipNamesMatchingNodeNames() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity entityA = Helper.getEntity("cust", "fort", "anyuser", "fortdoc");

        SearchChange changeA = new EntitySearchChange(new EntityBean(entityA), new ContentInputBean(json));

        Tag tag = new SimpleTag(new TagInputBean("aValue", "myTag", "myTag"));
        tag.setName("myTag");// This will be used as the relationship name between the entity and the tag!

        ArrayList<EntityTag> tags = new ArrayList<>();
        tags.add(new SimpleEntityTagRelationship(entityA, tag, "mytag", null));
        changeA.setTags(tags);

        deleteEsIndex(entityA.getFortress().getIndexName());

        searchRepo.ensureIndex(changeA.getIndexName(), changeA.getDocumentType());

        changeA = searchRepo.handle(changeA);

        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeA.getSearchKey());

        // DAT-328
        doTermQuery(entityA.getFortress().getIndexName(), entityA.getDocumentType().toLowerCase(), "tag.mytag.code.facet", tag.getCode(), 1);

    }

    @Test
    public void geo_Points() throws Exception {
        String comp = "geo_Points";
        String fort = "geo_Points";
        String user = "mikey";
        String doc = fort;

        Entity entity = Helper.getEntity(comp, fort, user, doc);
        deleteEsIndex(entity.getFortress().getIndexName());

        Map<String, Object> what = Helper.getSimpleMap(
                EntitySearchSchema.WHAT_CODE, "GEO");
        what.put(EntitySearchSchema.WHAT_NAME, "NameText");
        what.put(EntitySearchSchema.WHAT_DESCRIPTION, "This is a description");

        EntitySearchChange change = new EntitySearchChange(new EntityBean(entity));
        change.setWhat(what);
        ArrayList<EntityTag> tags = new ArrayList<>();

        TagInputBean tagInput = new TagInputBean("myTag", "TheLabel", "rlxname");
        Tag tag = new SimpleTag(tagInput);

        tags.add(new SimpleEntityTagRelationship(entity, tag, "mytag", null));


        SimpleEntityTagRelationship entityTag = new SimpleEntityTagRelationship(entity, tag, "mytag", null);
        GeoData geoData = new GeoData("NZ", "New Zealand", "Wellington", null);
        geoData.setLatLong(174.0, -41.0);
        entityTag.setGeoData(geoData);

        change.setTags(tags);

        searchRepo.ensureIndex(change.getIndexName(), change.getDocumentType());
        SearchChange searchResult = searchRepo.handle(change);

    }

}
