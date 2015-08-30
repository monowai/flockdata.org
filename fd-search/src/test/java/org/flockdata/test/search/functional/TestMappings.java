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
import org.flockdata.model.Entity;
import org.flockdata.model.EntityTag;
import org.flockdata.model.EntityTagOut;
import org.flockdata.model.Tag;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.search.IndexHelper;
import org.flockdata.search.model.*;
import org.flockdata.test.engine.Helper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.GeoDataBean;
import org.flockdata.track.bean.GeoDataBeans;
import org.flockdata.track.bean.SearchChange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
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

    private Logger logger = LoggerFactory.getLogger(TestMappings.class);

    @Test
    public void defaultTagQueryWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);

        String fortress = "fort";
        String company = "test";
        String doc = "doc";
        String user = "mike";

        Entity entity = Helper.getEntity(company, fortress, user, doc);

        EntitySearchChange change = new EntitySearchChange(entity);
        change.setDescription("Test Description");
        change.setWhat(json);
        ArrayList<EntityTag> tags = new ArrayList<>();

        TagInputBean tagInput = new TagInputBean("myTag", "TheLabel", "rlxname").
                setCode("my TAG");

        Tag tag = new Tag(tagInput);
        tags.add(new EntityTagOut(entity, tag, "mytag", null));

        change.setTags(tags);

        deleteEsIndex(entity);
        //searchRepo.ensureIndex(change.getIndexName(), change.getType());
        SearchResults searchResults = trackService.createSearchableChange(new EntitySearchChanges(change));
        SearchResult searchResult = searchResults.getSearchResults().iterator().next();
        Thread.sleep(1000);
        assertNotNull(searchResult);
        assertNotNull(searchResult.getSearchKey());
        entity.setSearchKey(searchResult.getSearchKey());
        json = searchRepo.findOne(entity);

        doFacetQuery(IndexHelper.parseIndex(entity), "tag.mytag.thelabel.code.facet", "my TAG", 1, "Exact match of tag code is not working");
        doFieldQuery(entity, "tag.mytag.thelabel.code", "my tag", 1, "Gram match of un-faceted tag code is not working");
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

        EntitySearchChange change = new EntitySearchChange(entity);
        EntitySearchChange changeB = new EntitySearchChange(entityB);
        change.setDescription("Test Description");
        change.setWhat(json);
        changeB.setWhat(json);
        ArrayList<EntityTag> tags = new ArrayList<>();

        TagInputBean tagInput = new TagInputBean("myTag", "TheLabel", "rlxname");
        tagInput.setCode("my TAG");
        Tag tag = new Tag(tagInput);

        tags.add(new EntityTagOut(entity, tag, "mytag", null));
        change.setTags( tags);

        deleteEsIndex(entity);

        Collection<SearchChange> changes = new ArrayList<>();
        changes.add(change);
        changes.add(changeB);
        EntitySearchChanges searchChanges = new EntitySearchChanges(changes);
        SearchResults searchResults = trackService.createSearchableChange(searchChanges);
        assertEquals("2 in 2 out", 2, searchResults.getSearchResults().size());
    }



    @Test
    public void testCustomMappingWorks() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity entityA = Helper.getEntity("cust", "fort", "anyuser", "fort");
        Entity entityB = Helper.getEntity("cust", "fortb", "anyuser", "fortb");

        SearchChange changeA = new EntitySearchChange(entityA, new ContentInputBean(json));
        SearchChange changeB = new EntitySearchChange(entityB, new ContentInputBean(json));

        // FortB will have
        changeA.setDescription("Test Description");
        changeB.setDescription("Test Description");

        deleteEsIndex(entityA);
        deleteEsIndex(entityB);

        searchRepo.ensureIndex(changeA);
        searchRepo.ensureIndex(changeB);
        changeA = searchRepo.handle(changeA);
        changeB = searchRepo.handle(changeB);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeB);
        assertNotNull(changeA.getSearchKey());
        assertNotNull(changeB.getSearchKey());

        // by default we analyze the @description field
        doDefaultFieldQuery(entityA, "description", changeA.getDescription(), 1);

        // In fortb.json we don't analyze the description (overriding the default) so it shouldn't be found
        doDefaultFieldQuery(entityB, "description", changeB.getDescription(), 0);

    }

    @Test
    public void sameIndexDifferentDocumentsHaveMappingApplied() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity entityA = Helper.getEntity("cust", "fort", "anyuser", "fortdoc");
        Entity entityB = Helper.getEntity("cust", "fort", "anyuser", "doctype");


        SearchChange changeA = new EntitySearchChange(entityA, new ContentInputBean(json));
        SearchChange changeB = new EntitySearchChange(entityB, new ContentInputBean(json));

        Tag tag = new Tag(new TagInputBean("myTag", "TheLabel", "rlxname"));
        tag.setCode("my TAG");// we should be able to find this as lowercase
        assertEquals("my TAG", tag.getCode());
        ArrayList<EntityTag> tagsA = new ArrayList<>();
        tagsA.add(new EntityTagOut(entityA, tag, "mytag", null));

        ArrayList<EntityTag> tagsB = new ArrayList<>();
        tagsB.add(new EntityTagOut(entityB, tag, "mytag", null));

        changeA.setTags(tagsA);
        changeB.setTags(tagsB);

        deleteEsIndex(entityA);
        deleteEsIndex(entityB);

        searchRepo.ensureIndex(changeA);
        searchRepo.ensureIndex(changeB);
        changeA = searchRepo.handle(changeA);
        changeB = searchRepo.handle(changeB);
        Thread.sleep(1000);
        assertNotNull(changeA);
        assertNotNull(changeB);
        assertNotNull(changeA.getSearchKey());
        assertNotNull(changeB.getSearchKey());

        doFacetQuery(entityA, entityA.getType().toLowerCase(), "tag.mytag.thelabel.code.facet", tag.getCode(), 1);
        doFacetQuery(entityB, entityB.getType().toLowerCase(), "tag.mytag.thelabel.code.facet", tag.getCode(), 1);
        String index = IndexHelper.getIndexRoot(entityA.getFortress()) +"*";

        doFacetQuery(index, "tag.mytag.thelabel.code.facet", tag.getCode(), 2, "Not scanning across indexes");

    }

    @Test
    public void tagWithRelationshipNamesMatchingNodeNames() throws Exception {
        Map<String, Object> json = Helper.getBigJsonText(20);
        Entity entityA = Helper.getEntity("cust", "fort-tag-rlx", "anyuser", "fortdoc");

        SearchChange changeA = new EntitySearchChange(entityA, new ContentInputBean(json));

        Tag tag = new Tag(new TagInputBean("aValue", "myTag", "myTag"));
        tag.setName("myTag");// This will be used as the relationship name between the entity and the tag!

        ArrayList<EntityTag> tags = new ArrayList<>();
        tags.add(new EntityTagOut(entityA, tag, "mytag", null));
        changeA.setTags(tags);

        deleteEsIndex(entityA);

        searchRepo.ensureIndex(changeA);

        changeA = searchRepo.handle(changeA);

        assertNotNull(changeA);
        assertNotNull(changeA.getSearchKey());

        // DAT-328
        Thread.sleep(5000);
        doFacetQuery(entityA, entityA.getType().toLowerCase(), "tag.mytag.code.facet", tag.getCode(), 1);

    }

    @Test
    public void geo_Points() throws Exception {
        String comp = "geo_Points";
        String fort = "geo_Points";
        String user = "mikey";

        Entity entity = Helper.getEntity(comp, fort, user, fort);
        deleteEsIndex(entity);

        Map<String, Object> what = Helper.getSimpleMap(
                EntitySearchSchema.WHAT_CODE, "GEO");
        what.put(EntitySearchSchema.WHAT_NAME, "NameText");
        what.put(EntitySearchSchema.WHAT_DESCRIPTION, "This is a description");

        TagInputBean tagInput = new TagInputBean("tagcode", "TagLabel", "tag-relationship");
        Tag tag = new Tag(tagInput);


        ArrayList<EntityTag> tags = new ArrayList<>();

        HashMap<String, Object> tagProps = new HashMap<>();
        tagProps.put("num", 100d);
        tagProps.put("str", "hello");
        EntityTag entityTag = new EntityTagOut(entity, tag, "entity-relationship", tagProps);
        // DAT-442 Geo refactoring
        GeoDataBeans geoPayLoad = new GeoDataBeans();
        GeoDataBean geoData = new GeoDataBean();
        GeoDataBean streetData = new GeoDataBean();
        streetData.add("street", "abc", "123 Main Street", 168.0, -13.03);
        geoData.add("country", "NZ", "New Zealand", 174.0, -41.0);
        geoPayLoad.add("country", geoData);
        geoPayLoad.add("street", streetData);
        geoData = geoPayLoad.getGeoBeans().get("country");
        TestCase.assertNotNull(geoData);
        entityTag.setGeoData(geoPayLoad);
        tags.add(entityTag);

        EntitySearchChange change = new EntitySearchChange(entity);

        change.setWhat(what);
        change.setTags(tags);

        searchRepo.ensureIndex(change);
        SearchChange searchResult = searchRepo.handle(change);
        TestCase.assertNotNull(searchResult);
        Thread.sleep(2000);

        String result = doQuery(entity, "*", 1);
        logger.info(result);
        assertTrue("Couldn't find the country GeoPoints", result.contains("points\":{\"country\""));
        assertTrue("Should be two geo points", result.contains("\"street\""));
        assertTrue(result.contains("174"));
        assertTrue(result.contains("-41"));

        doCompletionQuery(entity, "nz", 1, "Couldn't autocomplete on geo tag for NZ. If there are results, then the field name may be in error");
        doCompletionQuery(entity, "new ze", 1, "Couldn't autocomplete on geo tag for New Zealand");
    }

}
