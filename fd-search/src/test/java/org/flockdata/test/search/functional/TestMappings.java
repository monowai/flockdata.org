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

import junit.framework.TestCase;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Tag;
import org.flockdata.search.model.*;
import org.flockdata.test.engine.Helper;
import org.flockdata.test.engine.SimpleEntityTagRelationship;
import org.flockdata.test.engine.SimpleTag;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityTag;
import org.flockdata.track.model.GeoData;
import org.flockdata.track.model.SearchChange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

    @Test
    public void defaultTagQueryWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        String fortress = "fort";
        String company = "test";
        String doc = "doc";
        String user = "mike";

        Entity entity = Helper.getEntity(company, fortress, user, doc);

        EntitySearchChange change = new EntitySearchChange(new EntityBean(entity));
        change.setDescription("Test Description");
        change.setWhat(json);
        ArrayList<EntityTag> tags = new ArrayList<>();

        TagInputBean tagInput = new TagInputBean("myTag", "TheLabel", "rlxname");
        tagInput.setCode("my TAG");
        Tag tag = new SimpleTag(tagInput);

        tags.add(new SimpleEntityTagRelationship(entity, tag, "mytag", null));


        change.setTags(tags);


        deleteEsIndex(entity.getFortress().getIndexName());
        //searchRepo.ensureIndex(change.getIndexName(), change.getDocumentType());
        SearchResults searchResults = trackService.createSearchableChange(new EntitySearchChanges(change));
        SearchResult searchResult = searchResults.getSearchResults().iterator().next();
        Thread.sleep(1000);
        assertNotNull(searchResult);
        assertNotNull(searchResult.getSearchKey());
        entity.setSearchKey(searchResult.getSearchKey());
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
    public void count_CorrectSearchResults() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        String fortress = "fort";
        String company = "test";
        String doc = "doc";
        String user = "mike";

        Entity entity = Helper.getEntity(company, fortress, user, doc);
        Entity entityB = Helper.getEntity(company, fortress, user, doc);

        EntitySearchChange change = new EntitySearchChange(new EntityBean(entity));
        EntitySearchChange changeB = new EntitySearchChange(new EntityBean(entityB));
        change.setDescription("Test Description");
        change.setWhat(json);
        changeB.setWhat(json);
        ArrayList<EntityTag> tags = new ArrayList<>();

        TagInputBean tagInput = new TagInputBean("myTag", "TheLabel", "rlxname");
        tagInput.setCode("my TAG");
        Tag tag = new SimpleTag(tagInput);

        tags.add(new SimpleEntityTagRelationship(entity, tag, "mytag", null));
        change.setTags(tags);

        deleteEsIndex(entity.getFortress().getIndexName());

        Collection<SearchChange> changes = new ArrayList<>();
        changes.add(change);
        changes.add(changeB);
        EntitySearchChanges searchChanges = new EntitySearchChanges(changes);
        SearchResults searchResults = trackService.createSearchableChange(searchChanges);
        assertEquals("2 in 2 out", 2, searchResults.getSearchResults().size());
    }

    @Test
    public void completion_FindTagsByCodeAndDescription() throws Exception {

        String comp = "comp2";
        String fort = "fort2";
        String user = "mikey";
        Map<String, Object> what = Helper.getRandomMap();

        Entity entity = Helper.getEntity(comp, fort, user, fort, "AZERTY");
        deleteEsIndex(entity.getFortress().getIndexName());

        TagInputBean tagInputA = new TagInputBean("tagCode", "AutoComplete", "blah");

        TagInputBean tagInputB = new TagInputBean("myvalue", "AutoComplete", "blah");
        TagInputBean inst = new TagInputBean("Royal Marsden Free Hospital", "Institution", "inst");
        TagInputBean lead = new TagInputBean("Shepherd, JA", "Person", "lead");
        TagInputBean writer = new TagInputBean("Smith, JA", "Person", "lead");
        TagInputBean procedure = new TagInputBean("Surgical Procedures, Minimally Invasive", "Procedure", "involves");
        TagInputBean procedureB = new TagInputBean("Surgical Instruments", "Procedure", "involves");

        Collection<EntityTag> tags = new ArrayList<>();
        tags.add(Helper.getEntityTag(entity, tagInputA, "rlxname"));
        tags.add(Helper.getEntityTag(entity, tagInputB, "rlxname"));
        tags.add(Helper.getEntityTag(entity, inst, "abc"));
        tags.add(Helper.getEntityTag(entity, lead, "lead"));
        tags.add(Helper.getEntityTag(entity, writer, "writer"));
        tags.add(Helper.getEntityTag(entity, procedure, "proc"));
        tags.add(Helper.getEntityTag(entity, procedureB, "proc"));

        SearchChange change = new EntitySearchChange(new EntityBean(entity));
        change.setWhat(what);
        change.setTags(tags);

        searchRepo.ensureIndex(change.getIndexName(), change.getDocumentType());
        SearchChange searchResult = searchRepo.handle(change);

        assertNotNull(searchResult);
        Thread.sleep(2000);
        doQuery(entity.getFortress().getIndexName(), entity.getCallerRef(), 1);

        doCompletionQuery(entity.getFortress().getIndexName(), "tag", 1, "Completion failed");
        doCompletionQuery(entity.getFortress().getIndexName(), "tagc", 1, "Completion failed");
        doCompletionQuery(entity.getFortress().getIndexName(), "tagcod", 1, "Completion failed");

        doCompletionQuery(entity.getFortress().getIndexName(), "myv", 1, "Completion failed");
        // Only supports "start with"
//        doCompletionQuery(entity.getFortress().getIndexName(), "free", 1, "Completion failed");

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
        Entity entityA = Helper.getEntity("cust", "fort-tag-rlx", "anyuser", "fortdoc");

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

        TagInputBean tagInput = new TagInputBean("tagcode", "TagLabel", "tag-relationship");
        Tag tag = new SimpleTag(tagInput);


        ArrayList<EntityTag> tags = new ArrayList<>();

        HashMap<String, Object> tagProps = new HashMap<>();
        tagProps.put("num", 100d);
        tagProps.put("str", "hello");
        SimpleEntityTagRelationship entityTag = new SimpleEntityTagRelationship(entity, tag, "entity-relationship", tagProps);
        // DAT-442 Geo refactoring
        GeoData geoData = new GeoData();
        geoData.add("country", "NZ", "New Zealand", 174.0, -41.0);
        assertEquals("NZ", geoData.getProperties().get("country.code"));
        assertEquals("New Zealand", geoData.getProperties().get("country.name"));
        assertEquals("174.0,-41.0", geoData.getProperties().get("points.country"));
        //assertEquals(-41.0, Double.parseDouble(geoData.getProperties().get("country.lon").toString()));
        entityTag.setGeoData(geoData);
        tags.add(entityTag);


        EntitySearchChange change = new EntitySearchChange(new EntityBean(entity));

        change.setWhat(what);
        change.setTags(tags);


        searchRepo.ensureIndex(change.getIndexName().toLowerCase(), change.getDocumentType());
        SearchChange searchResult = searchRepo.handle(change);
        TestCase.assertNotNull(searchResult);
        Thread.sleep(2000);

        // ToDo: Assert shit
        doQuery(change.getIndexName().toLowerCase(), "*", 1);
    }

}
