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

package org.flockdata.test.engine.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityLog;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Log;
import org.flockdata.data.SystemUser;
import org.flockdata.data.Tag;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.track.service.SearchHandler;
import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchResult;
import org.flockdata.search.SearchResults;
import org.flockdata.search.SearchSchema;
import org.flockdata.search.SearchTag;
import org.flockdata.store.Store;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.EntityTagInputBean;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.flockdata.track.bean.EntityTagResult;
import org.flockdata.track.bean.SearchChange;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author mholdsworth
 * @tag Test, Entity, Tag
 * @since 29/06/2013
 */
public class TestEntityTags extends EngineBase {

    @Autowired
    SearchHandler searchHandler;

    @Override
    @Before
    public void cleanUpGraph() {
        // DAT-348
        super.cleanUpGraph();
    }

    @Test
    public void tags_entityTagsUpdatedForExistingEntity() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("tags_MetaTagsUpdatedForExistingEntity", mike_admin);
        assertNotNull(su);

        FortressInputBean fib = new FortressInputBean("ABC")
            .setStoreEnabled(true)
            .setSearchEnabled(false);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);
        assertNotNull(fortress);

        TagInputBean firstTag = new TagInputBean("firstTag", null, "demo");

        EntityInputBean entityBean = new EntityInputBean(fortress, "mtest", "aTest", new DateTime(), "abc");
        entityBean.addTag(firstTag);
        entityBean.setArchiveTags(false);
        ContentInputBean contentBean = new ContentInputBean();
        contentBean.setEvent("Test");
        Map<String, Object> jsonMap = ContentDataHelper.getRandomMap();
        contentBean.setData(jsonMap);
        entityBean.setContent(contentBean);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());

        Assert.assertEquals(1, entityService.getLogCount(su.getCompany(), entity.getKey()));
        Assert.assertEquals(1, entityTagService.findEntityTagResults(entity).size());

        // Scenario 1: We send in the header with no content
        entityBean.setContent(null);
        mediationFacade.trackEntity(su.getCompany(), entityBean);
        Assert.assertEquals(1, entityService.getLogCount(su.getCompany(), entity.getKey()));
        Assert.assertEquals(1, entityTagService.findEntityTagResults(entity).size());

        // Scenario 2: We have an existing entity with content logged - it has one existing tag
        //           we now have a second tag added but no content.
        TagInputBean secondTag = new TagInputBean("secondTag", null, "demo");
        entityBean.addTag(secondTag);
        entity = mediationFacade.trackEntity(su.getCompany(), entityBean).getEntity();

        Assert.assertEquals(1, entityService.getLogCount(su.getCompany(), entity.getKey()));
        Assert.assertEquals(2, entityTagService.findEntityTagResults(entity).size());

        EntityLog lastLog = logService.getLastLog(entity);
        assertNotNull(lastLog);
        assertFalse(lastLog.getLog().isMocked());
        Map<String, Object> values = mediationFacade.getLogContent((EntityNode) entity, lastLog.getId());
        assertFalse(values.isEmpty());
        assertEquals(jsonMap.get("Key").toString(), values.get("Key").toString());

    }

    @Test
    public void tagsRemovedFromExistingEntityOnUpdate() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("tagsRemovedFromExistingEntityOnUpdate", mike_admin);
        assertNotNull(su);

        FortressInputBean fib = new FortressInputBean("ABC", true);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);
        assertNotNull(fortress);

        TagInputBean firstTag = new TagInputBean("firstTag", null, "demo");

        EntityInputBean entityBean = new EntityInputBean(fortress, "mtest", "aTest", new DateTime(), "abc");
        entityBean.addTag(firstTag);
        entityBean.setArchiveTags(false);
        ContentInputBean contentBean = new ContentInputBean();
        Map<String, Object> jsonMap = ContentDataHelper.getRandomMap();
        contentBean.setData(jsonMap);
        entityBean.setContent(contentBean);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());

        Assert.assertEquals(1, entityService.getLogCount(su.getCompany(), entity.getKey()));
        Assert.assertEquals(1, entityTagService.findEntityTagResults(entity).size());

        // Scenario 2: We have an existing entity with content logged - it has one existing tag
        //           we now have a second tag added but no content.
        TagInputBean replaceTag = new TagInputBean("secondTag", null, "demo");
        entityBean.addTag(replaceTag);
        entityBean.setReplaceExistingTags(true);

        entity = mediationFacade.trackEntity(su.getCompany(), entityBean).getEntity();

        Assert.assertEquals(1, entityTagService.findEntityTagResults(entity).size());


    }


    @Test
    public void simpleTagAgainstEntity() throws Exception {
        SystemUser su = registerSystemUser("simpleTagAgainstEntity", mike_admin);
        assertNotNull(su);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        TagInputBean flopTag = new TagInputBean("FLOP");
        tagService.createTag(su.getCompany(), flopTag);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());

        EntityTagInputBean entityTag = new EntityTagInputBean(resultBean.getEntity().getKey(), null, "!!!");
        exception.expect(IllegalArgumentException.class);
        entityTagService.processTag(entity, entityTag);
        // First entityTag created
        entityTag = new EntityTagInputBean(entity.getKey(), flopTag.getCode(), "ABC");

        entityTagService.processTag(entity, entityTag);

        Boolean tagRlxExists = entityTagService.relationshipExists(entity, flopTag.getCode(), "ABC");
        assertTrue("Tag not found " + flopTag.getName(), tagRlxExists);

        entityTagService.processTag(entity, entityTag);
        // Behaviour - Can't add the same tagValue twice for the same combo
        tagRlxExists = entityTagService.relationshipExists(entity, flopTag.getCode(), "ABC");
        assertTrue(tagRlxExists);
    }

    @Test
    public void dates_TagRelationshipSinceTests() throws Exception {
        SystemUser su = registerSystemUser("dates_SinceRecorded", mike_admin);
        assertNotNull(su);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("dates_SinceRecorded", true));
        assertNotNull(fortress);

        TagInputBean flopTag = new TagInputBean("FLOP");
        tagService.createTag(su.getCompany(), flopTag);

        DateTime fCreated = new DateTime().minus(10000);
        EntityInputBean entityBean = new EntityInputBean(fortress, "anyone", "aTest", fCreated, "abc");
        TrackResultBean resultBean = mediationFacade.trackEntity(fortress.getDefaultSegment(), entityBean);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());

        assertEquals(fCreated.getMillis(), entity.getFortressCreatedTz().getMillis());

        EntityTagInputBean tagA = new EntityTagInputBean(entity.getKey(), flopTag.getCode(), "ABC").
            setSince(true);
        entityTagService.processTag(entity, tagA);

        Boolean tagRlxExists = entityTagService.relationshipExists(entity, flopTag.getCode(), "ABC");
        assertTrue("Tag not found " + flopTag.getName(), tagRlxExists);

        Collection<EntityTag> entityTags = entityTagService.findEntityTags(entity);
        for (EntityTag entityTag : entityTags) {
            assertEquals("Date did not correspond to the Fortress created date", entity.getFortressCreatedTz().getMillis(), Long.parseLong(entityTag.getProperties().get(EntityTag.SINCE).toString()));
        }

        // Creating some content and adding a new Tag to the entity
        DateTime fUpdated = new DateTime().minus(10000);
        ContentInputBean contentInputBean = new ContentInputBean("harry", fUpdated, ContentDataHelper.getRandomMap());

        entityBean.addTag(new TagInputBean("Tag2", null, "second").
            setSince(true));

        entityBean.setArchiveTags(false);// We don't have a reference to the original tag in the Input
        // as we assigned it in a secondary step, so will accumulate tags and stop them being archived
        entityBean.setContent(contentInputBean);
        mediationFacade.trackEntity(fortress.getDefaultSegment(), entityBean);
        entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        assertEquals(fCreated, entity.getFortressCreatedTz());
        assertEquals(fUpdated.getMillis(), entity.getFortressUpdatedTz().getMillis());
        entityTags = entityTagService.findEntityTags(entity);
        assertEquals(2, entityTags.size());
        for (EntityTag tag : entityTags) {
            if (tag.getTag().getCode().equalsIgnoreCase(flopTag.getCode())) {
                assertEquals("Date did not correspond to the Fortress created date", entity.getFortressCreatedTz().getMillis(), Long.parseLong(tag.getProperties().get(EntityTag.SINCE).toString()));
            } else {
                assertEquals("Date did not correspond to the Fortress updated date", entity.getFortressUpdatedTz().getMillis(), Long.parseLong(tag.getProperties().get(EntityTag.SINCE).toString()));
            }
        }

    }

    @Test
    public void DAT386() throws Exception {
        SystemUser su = registerSystemUser("DAT386", mike_admin);
        FortressInputBean fib = new FortressInputBean("DAT386", true);
        fib.setStoreEnabled(false);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);

        TagInputBean tagInput = new TagInputBean("DAT386");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "DAT386", "DAT386", new DateTime(), "abc");
        entityInput.addTag(new TagInputBean("TagA", null, "aaaa"));
        entityInput.addTag(new TagInputBean("TagB", null, "bbbb"));

        mediationFacade.trackEntity(su.getCompany(), entityInput);
        ContentInputBean contentInputBean = new ContentInputBean(ContentDataHelper.getRandomMap());
        entityInput = new EntityInputBean(fortress, "DAT386", "DAT386", new DateTime(), "abc");
        entityInput.addTag(new TagInputBean("TagA", null, "aaaa"));
        entityInput.setContent(contentInputBean);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertNotNull(result.getEntity());
        assertEquals(1, entityTagService.findEntityTagResults(result.getEntity()).size());
    }

    @Test
    public void renameRelationship() throws Exception {

        SystemUser su = registerSystemUser("renameRelationship", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        entityInput.addTag(new TagInputBean("TagA", null, "AAAA"));
        entityInput.addTag(new TagInputBean("TagB", null, "BBBB"));
        entityInput.addTag(new TagInputBean("TagC", null, "CCCC"));
        entityInput.addTag(new TagInputBean("TagD", null, "DDDD"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(entity);

        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());
        assertFalse(entityTagService.relationshipExists(entity, "TagC", "!!Twee!!"));//
        // Remove a single tag
        for (EntityTag value : tagSet) {
            if (value.getTag().getCode().equals("TagC")) {
                entityTagService.changeType(entity, value, "!!Twee!!");
            }
        }

        assertTrue(entityTagService.relationshipExists(entity, "TagC", "!!Twee!!"));
    }

    @Test
    public void createAndDeleteEntityTags() throws Exception {

        SystemUser su = registerSystemUser("createAndDeleteEntityTags", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");

        entityInput.addTag(new TagInputBean("TagA", null, "AAAA"));
        entityInput.addTag(new TagInputBean("TagB", null, "BBBB"));
        entityInput.addTag(new TagInputBean("TagC", null, "CCCC"));
        entityInput.addTag(new TagInputBean("TagD", null, "DDDD"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(entity);

        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());
        // Remove a single tag
        for (EntityTag value : tagSet) {
            if (value.getTag().getCode().equals("TagB")) {
                entityTagService.deleteEntityTags(entity, value);
            }
        }
        tagSet = entityTagService.findEntityTags(entity);
        assertNotNull(tagSet);
        assertEquals(3, tagSet.size());
        // Ensure that the deleted tag is not in the results
        for (EntityTag entityTag : tagSet) {
            assertFalse(entityTag.getTag().getCode().equals("TagB"));
        }
    }

    @Test
    public void nullTagValueCRUD() throws Exception {
        SystemUser su = registerSystemUser("nullTagValueCRUD", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        entityInput.addTag(new TagInputBean("TagA", "TestTag", "rlx"));
        entityInput.addTag(new TagInputBean("TagB", "TestTag", "rlx"));
        entityInput.addTag(new TagInputBean("TagC", "TestTag", "rlx"));
        entityInput.addTag(new TagInputBean("TagD", "TestTag", "rlx"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(entity);
        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());

        entityService.updateEntity(entity);
        entity = entityService.getEntity(su.getCompany(), entity.getKey());
        EntitySummaryBean summaryBean = entityService.getEntitySummary(null, entity.getKey());
        assertNotNull(summaryBean.getTags());
        Set<Entity> entities = entityTagService.findEntityTagResults(su.getCompany(), "TagA");
        assertNotNull(entities);
        assertNotSame(entities.size() + " Entities returned!", 0, entities.size());

        assertEquals(entity.getKey(), entities.iterator().next().getKey());
        entities = entityTagService.findEntityTagResults(su.getCompany(), "TagC");
        assertNotNull(entities);
        assertEquals(entity.getKey(), entities.iterator().next().getKey());
        entities = entityTagService.findEntityTagResults(su.getCompany(), "TagD");
        assertNotNull(entities);
        assertEquals(entity.getKey(), entities.iterator().next().getKey());
    }

    @Test
    public void nullCodeValue() throws Exception {
        SystemUser su = registerSystemUser("nullCodeValue", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        // In this scenario, the Tag name is the key if the value is null
        TagInputBean tag = new TagInputBean("TagD", null, "DDDD");
        tag.setName(null);
        entityInput.addTag(tag);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(entity);
        assertNotNull(tagSet);
        assertEquals(1, tagSet.size());

    }

    @Test
    public void duplicateTagNotCreated() throws Exception {
        SystemUser su = registerSystemUser("duplicateTagNotCreated", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        entityInput.addTag(new TagInputBean("TagA", null, "camel"));
        entityInput.addTag(new TagInputBean("taga", null, "lower"));
        entityInput.addTag(new TagInputBean("tAgA", null, "mixed"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Tag tag = tagService.findTag(su.getCompany(), null, "Taga");
        assertNotNull(tag);
        Collection<EntityTag> entityTags = entityTagService.findEntityTags(entity);
        for (EntityTag entityTag : entityTags) {
            Assert.assertEquals("Expected same tag for each relationship", tag.getId(), entityTag.getTag().getId());
        }
        assertEquals("Expected 3 relationships for the same tag", 3, entityTags.size());

    }

    @Test
    public void trackSupressed_EntityTagsAreStillReturned() throws Exception {
        SystemUser su = registerSystemUser("noEntityTagsAreReturned", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        EntityInputBean entity = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        entity.setTrackSuppressed(true);
        // This should create the same Tag object, but return one row for each relationships
        entity.addTag(new TagInputBean("TagA", null, "camel"));
        entity.addTag(new TagInputBean("taga", null, "lower"));
        entity.addTag(new TagInputBean("tAgA", null, "mixed"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entity);

        assertEquals(3, resultBean.getTags().size());
        Long id = null;
        for (EntityTag entityTag : resultBean.getTags()) {
            if (id == null) {
                id = entityTag.getTag().getId();
            }
            Assert.assertEquals(id, entityTag.getTag().getId());
        }
        assertNull(resultBean.getEntity().getKey());

    }

    @Test
    public void createLogForInvalidEntity() throws Exception {
        SystemUser su = registerSystemUser("createLogForInvalidEntity", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        EntityInputBean entity = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        mediationFacade.trackEntity(su.getCompany(), entity);
        ContentInputBean contentInputBean = new ContentInputBean("Harry", "InvalidKey", new DateTime(), ContentDataHelper.getRandomMap());
        exception.expect(NotFoundException.class);
        mediationFacade.trackLog(su.getCompany(), contentInputBean);

    }

    @Test
    public void createLogForValidEntityWithNoContent() throws Exception {
        SystemUser su = registerSystemUser("createLogForValidEntityWithNoContent", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        EntityInputBean entity = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TrackResultBean rb = mediationFacade.trackEntity(su.getCompany(), entity);
        ContentInputBean contentInput = new ContentInputBean("Harry", rb.getEntity().getKey(), new DateTime(), null);
        assertNotNull(mediationFacade.trackLog(su.getCompany(), contentInput));
    }

    @Test
    public void differentTagTypeSameTagName() throws Exception {
        SystemUser su = registerSystemUser("differentTagTypeSameTagName", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);

        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TagInputBean tag = new TagInputBean("TagA");
        tag.addEntityTagLink("Type1");
        tag.addEntityTagLink("Type2");
        tag.addEntityTagLink("Type3");
        entityInput.addTag(tag);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(entity);
        assertNotNull(tagSet);
        assertEquals(3, tagSet.size());

        EntitySummaryBean summaryBean = entityService.getEntitySummary(su.getCompany(), entity.getKey());
        assertNotNull(summaryBean);
        assertEquals(3, summaryBean.getTags().size());

    }

    @Test
    public void tagListAndSingular() throws Exception {
        SystemUser su = registerSystemUser("tagListAndSingular", mike_admin);
        assertNotNull(su);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@flockdata.com", "Email", "email-to");
        tagA.addEntityTagLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@flockdata.com", "Email", "email-cc");
        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey(), true);
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(entity);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        EntitySummaryBean summaryBean = entityService.getEntitySummary(null, entity.getKey());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithNullProperties() throws Exception {
        SystemUser su = registerSystemUser("mapRelationshipsWithNullProperties", mike_admin);
        assertNotNull(su);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean tagA = new TagInputBean("mike@flockdata.com", null, "email-to");
        tagA.addEntityTagLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@flockdata.com", null, "email-cc");
        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey(), true);
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(entity);
        EntitySummaryBean summaryBean = entityService.getEntitySummary(null, entity.getKey());
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithProperties() throws Exception {
        SystemUser su = registerSystemUser("mapRelationshipsWithProperties", mike_admin);
        assertNotNull(su);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        Map<String, Object> propA = new HashMap<>();
        Map<String, Object> propB = new HashMap<>();
        propA.put("myValue", 10);
        propB.put("myValue", 20);

        TagInputBean tagA = new TagInputBean("mike@flockdata.com", "Email", new EntityTagRelationshipInput("email-to", propA)).setLabel("Email");
        tagA.addEntityTagLink(new EntityTagRelationshipInput("email-cc", propB));
        TagInputBean tagB = new TagInputBean("np@flockdata.com", "Email", "email-cc");

        inputBean.addTag(tagA);
        inputBean.addTag(tagB);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey(), true);
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(entity);
        EntitySummaryBean summaryBean = entityService.getEntitySummary(su.getCompany(), entity.getKey());
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void duplicateRLXTypesNotStored() throws Exception {
        SystemUser su = registerSystemUser("duplicateRLXTypesNotStored", mike_admin);
        assertNotNull(su);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagInputBean = new TagInputBean("mike@flockdata.com", null, "email-to");
        tagInputBean.addEntityTagLink("email-to");
        tagInputBean.addEntityTagLink("email-to");

        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey(), true);
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(entity);
        assertEquals("One for the Generic tag and one for exploration", 1, tagResults.size());
    }

    @Test
    public void directedEntityTagsWork() throws Exception {
        SystemUser su = registerSystemUser("directedEntityTagsWork", mike_admin);
        assertNotNull(su);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagInputBean = new TagInputBean("mike@flockdata.com", null)
            .addEntityTagLink(new EntityTagRelationshipInput("email-to")
            );// Entity->Tag

        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        // By default, tags are inbound to the Entity. This asserts the reverse also works
        Collection<EntityTagResult> tagResults = entityTagService.findOutboundTagResults(entity);
        assertEquals("No tag heading out from the Entity could be found", 1, tagResults.size());

    }

    @Test
    public void tagsAndValuesWithSpaces() throws Exception {

        SystemUser su = registerSystemUser("tagsAndValuesWithSpaces", mike_admin);
        assertNotNull(su);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@flockdata.com", null, "email-to");
        tagA.addEntityTagLink("email cc");
        TagInputBean tagB = new TagInputBean("np@flockdata.com", null, "email-cc");

        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(entity);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        EntitySummaryBean summaryBean = entityService.getEntitySummary(null, entity.getKey());
        assertEquals(3, summaryBean.getTags().size());

        String json = JsonUtils.toJson(summaryBean);
        assertNotNull("Error serializing to json", json);
    }

    @Test
    public void entity_nestedTagStructure() throws Exception {

        SecurityContextHolder.getContext().setAuthentication(authDefault);
        SystemUser su = registerSystemUser("entity_nestedTagStructure", mike_admin);
        assertNotNull(su);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean country = new TagInputBean("New Zealand");
        TagInputBean wellington = new TagInputBean("Wellington");
        TagInputBean auckland = new TagInputBean("Auckland");
        country.setTargets("capital-city", wellington);
        country.setTargets("city", auckland);
        TagInputBean section = new TagInputBean("Thorndon");
        wellington.setTargets("section", section);
        TagInputBean building = new TagInputBean("ABC House");
        section.setTargets("houses", building);

        entityInput.addTag(country);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertNotNull(resultBean);
        // Tags are not associated with the entity rather the structure is enforced while importing
        Tag countryTag = tagService.findTag(su.getCompany(), null, "New Zealand");
        Tag cityTag = tagService.findTag(su.getCompany(), null, "Wellington");
        Tag sectionTag = tagService.findTag(su.getCompany(), null, "Thorndon");
        Tag houseTag = tagService.findTag(su.getCompany(), null, "ABC House");

        assertNotNull(countryTag);
        Assert.assertEquals(2, tagService.findDirectedTags(countryTag).size());  // Country has 2 cities
        assertNotNull(cityTag);
        Assert.assertEquals(1, tagService.findDirectedTags(cityTag).size());
        assertNotNull(sectionTag);
        Assert.assertEquals(1, tagService.findDirectedTags(sectionTag).size());
        assertNotNull(houseTag);
        Assert.assertEquals(0, tagService.findDirectedTags(houseTag).size());
    }

    @Test
    public void usGeographyStructure() throws Exception {
        SystemUser su = registerSystemUser("usGeographyStructure", mike_admin);
        assertNotNull(su);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country);
        TagInputBean cityInputTag = new TagInputBean(city);
        TagInputBean stateInputTag = new TagInputBean("CA");

        TagInputBean institutionTag = new TagInputBean("mikecorp", null, "owns");
        // Institution is in a city
        institutionTag.setTargets("located", cityInputTag);
        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);
        entityInput.addTag(institutionTag);

        // Institution<-city<-state<-country

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertNotNull(resultBean);
        Collection<EntityTag> tags = entityTagService.findEntityTags(resultBean.getEntity());
        assertFalse(tags.isEmpty());

        for (EntityTag tag : tags) {
            Assert.assertEquals("mikecorp", tag.getTag().getCode());
            Collection<Tag> cities = tagService.findDirectedTags(tag.getTag());
            org.junit.Assert.assertFalse(cities.isEmpty());
            Tag cityTag = cities.iterator().next();
            assertEquals(cityInputTag.getName(), cityTag.getName());
            Collection<Tag> states = tagService.findDirectedTags(cityTag);
            assertFalse(states.isEmpty());
            Tag stateTag = states.iterator().next();
            assertEquals(stateInputTag.getName(), stateTag.getName());
        }

    }

    @Test
    public void tagsInSearchDoc() throws Exception {
        SystemUser su = registerSystemUser("tagsInSearchDoc", mike_admin);
        assertNotNull(su);
        engineConfig.setTestMode(true);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country, "Country");
        TagInputBean cityInputTag = new TagInputBean(city, "City");
        TagInputBean stateInputTag = new TagInputBean("CA", "State");

        TagInputBean institutionTag = new TagInputBean("mikecorp", null, "works");
        // Institution is in a city
        institutionTag.setTargets("located", cityInputTag);
        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);
        inputBean.addTag(institutionTag);
        inputBean.setContent(new ContentInputBean(ContentDataHelper.getRandomMap()));

        // Institution<-city<-state<-country

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(resultBean);
        Collection<EntityTag> tags = entityTagService.findEntityTags(resultBean.getEntity());
        assertFalse(tags.isEmpty());

        EntitySearchChange searchChange = searchService.getEntityChange(resultBean);
        assertNotNull(searchChange);
        assertNotNull(searchChange.getTagValues());
    }

    @Test
    public void entity_LocatedGeoData() throws Exception {
        // DAT-452
        SystemUser su = registerSystemUser("entity_LocatedGeoData", mike_admin);
        assertNotNull(su);
        engineConfig.setTestMode(true);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entity_LocatedGeoData", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "geoTracTest", "anyvalue", new DateTime(), "abc");
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country, "Country");
        TagInputBean stateInputTag = new TagInputBean("CA", "State");
        TagInputBean cityInputTag = new TagInputBean(city, "City");

        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);

        // Institution is in a city
        inputBean.addTag(cityInputTag.addEntityTagLink(new EntityTagRelationshipInput("geodata", true)));

        inputBean.setContent(new ContentInputBean(ContentDataHelper.getRandomMap()));

        // Institution<-city<-state<-country
        assertEquals(1, inputBean.getTags().iterator().next().getEntityTagLinks().size());
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(resultBean);

        Iterable<EntityTag> tags = entityTagService.findEntityTagsWithGeo(resultBean.getEntity());
        for (EntityTag tag : tags) {
            logger.info(tag.toString());
            assertEquals("geodata", tag.getRelationship());
            assertTrue(tag.isGeoRelationship());
            assertNotNull("geo data block not found for a geo flagged relationship connected to an entity",
                tag.getGeoData());
        }

        EntitySearchChange searchChange = searchService.getEntityChange(resultBean);
        assertNotNull(searchChange);
        assertNotNull(searchChange.getTagValues());
    }

    @Test
    public void entityTag_SimpleRelationship() throws Exception {
        String name = "Doctor John";
        TagInputBean authorTag = new TagInputBean(name + ":person");
        authorTag.addEntityTagLink("writer");
        authorTag.addEntityTagLink("lead");

        SystemUser su = registerSystemUser("targetTagWithAuditRelationship", mike_admin);
        assertNotNull(su);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean countryTag = new TagInputBean("New Zealand");
        TagInputBean cityTag = new TagInputBean("Auckland");
        TagInputBean institution = new TagInputBean("Auckland University:Institution");
        cityTag.setTargets("jurisdiction", countryTag); // Auckland located in NZ

        institution.addEntityTagLink("located");
        cityTag.addEntityTagLink("city");

        inputBean.addTag(cityTag); // Not attached to entity
        inputBean.addTag(countryTag);
        inputBean.addTag(institution);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(result);
        Collection<EntityTag> tags = entityTagService.findEntityTags(result.getEntity());
        assertEquals(2, tags.size());
        for (EntityTag tag : tags) {
            assertTrue(tag.getTag().getCode().equals(institution.getCode()) || tag.getTag().getCode().equals(cityTag.getCode()));
        }

    }

    @Test
    public void geoTag() throws Exception {
        SystemUser su = registerSystemUser("geoTag", mike_admin);
        assertNotNull(su);

        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean entityInput = new EntityInputBean(fortress, "geoTest", "geoTest", new DateTime(), "abc");
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country, "Country", "");
        TagInputBean cityInputTag = new TagInputBean("LA", "City", "").setName(city);
        TagInputBean stateInputTag = new TagInputBean("CA", "State", "");

        TagInputBean institutionTag = new TagInputBean("mikecorp", "Institution");
        institutionTag.addEntityTagLink(new EntityTagRelationshipInput("owns", true));
        // Institution is in a city
        institutionTag.setTargets("located", cityInputTag);
        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);
        entityInput.addTag(institutionTag);

        // Institution<-city<-state<-country

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertNotNull(resultBean);
        assertNotNull(tagService.findTag(fortress.getCompany(), "Country", null, "USA"));

        Iterable<EntityTag> tags = entityTagService.findEntityTagsWithGeo(resultBean.getEntity());
        //assertFalse(tags.isEmpty());

        for (EntityTag tag : tags) {
            assertEquals("mikecorp", tag.getTag().getCode());
            assertNotNull(tag.getGeoData());
            assertEquals(stateInputTag.getCode(), tag.getGeoData().getGeoBeans().get("state").getCode());
            assertEquals(cityInputTag.getCode(), tag.getGeoData().getGeoBeans().get("city").getCode());
            Collection<Tag> cities = tagService.findDirectedTags(tag.getTag());
            org.junit.Assert.assertFalse(cities.isEmpty());
            Tag cityTag = cities.iterator().next();
            assertEquals(cityInputTag.getName(), cityTag.getName());
            Collection<Tag> states = tagService.findDirectedTags(cityTag);
            assertFalse(states.isEmpty());
            Tag stateTag = states.iterator().next();
            assertEquals(stateInputTag.getName(), stateTag.getName());
        }

    }

    @Test
    public void tagsAreUpdatedOnEntityUpdate() throws Exception {
        SystemUser su = registerSystemUser("tagsAreUpdatedOnEntityUpdate", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("TEST-CREATE", null, "rlx-test");

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(), ContentDataHelper.getRandomMap());
        inputBean.setContent(logBean);

        inputBean.addTag(tagInput);
        TrackResultBean entityResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        EntityNode created = entityService.getEntity(su.getCompany(), entityResult.getEntity().getKey());
        Log firstLog = entityService.getLastEntityLog(su.getCompany(), created.getKey()).getLog();
        assertNotNull(created);

        // Test that a tag is removed
        EntityInputBean updatedEntity = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        // Force a change to be detected
        ContentInputBean alb = new ContentInputBean("mike", new DateTime(), ContentDataHelper.getRandomMap());
        updatedEntity.setContent(alb);
        // Updating an existing Entity but the tagCollection is minus TEST-CREATE tag
        // The create call should create a new Tag - TEST-UPDATE - and then remove the TEST-CREATE
        updatedEntity.addTag(new TagInputBean("TEST-UPDATE", null, "camel"));
        entityResult = mediationFacade.trackEntity(su.getCompany(), updatedEntity);
        EntityNode entity = entityService.getEntity(su.getCompany(), entityResult.getEntity().getKey());
        assertNotNull(entity);

        // Should only be one tag
        validateTag(entity, null, 1);
        // It should be the update tag
        validateTag(entity, "TEST-UPDATE", 1);
        // The create tag should not be against the entity but against the log
        validateTag(entity, "TEST-CREATE", 0);

        Collection<EntityTag> results = entityService.getLastLogTags(su.getCompany(), entity.getKey());
        assertEquals(0, results.size()); // No tags against the last change log - tags are against the entity

        results = entityService.getLogTags(su.getCompany(), firstLog.getEntityLog());
        assertEquals(1, results.size());
        Assert.assertEquals("TEST-CREATE", results.iterator().next().getTag().getCode());

        // Make sure when we pass NO tags, i.e. just running an update, we don't change ANY tags

        alb = new ContentInputBean("mike", new DateTime(), ContentDataHelper.getRandomMap());
        updatedEntity.setContent(alb);
        updatedEntity.getTags().clear();
        entityResult = mediationFacade.trackEntity(su.getCompany(), updatedEntity);
        entity = entityService.getEntity(su.getCompany(), entityResult.getEntity().getKey());

        // 3 logs
        Assert.assertEquals(3, entityService.getLogCount(su.getCompany(), entity.getKey()));
        // Update tag should still be against the entity
        validateTag(entity, "TEST-UPDATE", 1);

        // Here we will cancel the last two logs getting us back to the initial state
        // should be one tag of TEST-CREATE logged

        entityService.cancelLastLog(su.getCompany(), entity);
        entityService.cancelLastLog(su.getCompany(), entity);
        entity = entityService.getEntity(su.getCompany(), entity.getKey());
        //ToDo: We are only adding back tags that were removed If tag was added by the cancelled log then it should
        // also be removed The answer here should be 1

        validateTag(entity, null, 1);
        validateTag(entity, "TEST-CREATE", 1);
        Collection<EntityTag> entityTags = entityTagService.findEntityTags(entity);
        for (EntityTag entityTag : entityTags) {
            assertEquals("Archived relationship name was not restored", "rlx-test", entityTag.getRelationship());
        }
    }

    @Test
    public void oneTagRemovedFromASetOfTwo() throws Exception {
        SystemUser su = registerSystemUser("oneTagRemovedFromASetOfTwo", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("TAG-FIRST", null, "rlx-test");

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(), ContentDataHelper.getRandomMap());
        inputBean.setContent(logBean);

        inputBean.addTag(tagInput);
        tagInput = new TagInputBean("TAG-SECOND", null, "rlxb-test");
        inputBean.addTag(tagInput);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        EntityNode created = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        entityService.getLastEntityLog(su.getCompany(), created.getKey()).getLog();
        assertNotNull(created);
        validateTag(created, null, 2);

        // Test that a tag is removed
        EntityInputBean updatedEntity = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        // Force a change to be detected
        ContentInputBean alb = new ContentInputBean("mike", new DateTime(), ContentDataHelper.getRandomMap());
        updatedEntity.setContent(alb);
        // we are updating an existing entity with two tags and telling it that only one is now valid
        updatedEntity.addTag(new TagInputBean("TAG-FIRST", null, "rlx-test"));
        resultBean = mediationFacade.trackEntity(su.getCompany(), updatedEntity);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        assertNotNull(entity);

        // Should be one tag
        validateTag(entity, null, 1);
        // It should be the update tag
        validateTag(entity, "TAG-FIRST", 1);
        // The create tag should not be against the entity but against the log
        validateTag(entity, "TEST-SECOND", 0);

        Collection<EntityTag> results = entityService.getLastLogTags(su.getCompany(), entity.getKey());
        // No tags removed for the last tag
        assertEquals(0, results.size()); // No tags against the logs
        Assert.assertEquals(1, entityTagService.findEntityTagResults(entity).size());

    }

    @Test
    public void addNewTagToExistingEntity() throws Exception {
        SystemUser su = registerSystemUser("addNewTagToExistingEntity", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        //assertNotNull(result);
        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(), ContentDataHelper.getRandomMap());
        inputBean.setContent(logBean);
        // This should create the same Tag object
        inputBean.addTag(new TagInputBean("TagA", "TestTag", "camel"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);

        assertNotNull(resultBean);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        assertNotNull(entity);

        validateTag(entity, "TagA", 1);

        //Adding a second tag (the first is already in the entity
        inputBean.addTag(new TagInputBean("TagB", "TestTag", "horse"));
        mediationFacade.trackEntity(su.getCompany(), inputBean);
        validateTag(entity, "TagB", 2);

    }

    @Test
    public void add_SameTagTwiceToSameEntity() throws Exception {
        SystemUser su = registerSystemUser("addNewTagToExistingEntity", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        //assertNotNull(result);
        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(), ContentDataHelper.getRandomMap());
        inputBean.setContent(logBean);
        // This should create the same Tag object
        inputBean.addTag(new TagInputBean("TagA", "TestTag", "camel"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);

        assertNotNull(resultBean);

        //Adding a second tag (the first is already in the entity
        inputBean.addTag(new TagInputBean("TagA", "TestTag", "camel"));
        mediationFacade.trackEntity(su.getCompany(), inputBean);
        validateTag(resultBean.getEntity(), "TagA", 1);

    }

    @Test
    public void directionalTagsAndRelationshipPropertiesPreserved() throws Exception {
        SystemUser su = registerSystemUser("directionalTagsAndRelationshipPropertiesPreserved", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(), ContentDataHelper.getRandomMap());
        inputBean.setContent(logBean);

        Map<String, Object> rlxProperties = new HashMap<>();
        rlxProperties.put("stringTest", "blah");
        rlxProperties.put("doubleTest", 100d);
        rlxProperties.put("weight", 99);
        rlxProperties.put("abAdded", "z");

        TagInputBean inBound = new TagInputBean("TAG-IN", "DefTag", new EntityTagRelationshipInput("rlx-test", rlxProperties).setReverse(true));
        inputBean.addTag(inBound);

        TagInputBean outBound = new TagInputBean("TAG-OUT", "DefTest", new EntityTagRelationshipInput("rlxb-test", rlxProperties));
        inputBean.addTag(outBound);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity created = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        entityService.getLastEntityLog(su.getCompany(), created.getKey()).getLog();

        // Total of two tags
        validateTag(created, null, 2);

        Collection<EntityTagResult> inboundTags = entityTagService.findInboundTagResults(created);
        assertEquals("One tag should be inbound", 1, inboundTags.size());
        EntityTagResult trackOut = inboundTags.iterator().next();
        Assert.assertEquals("TAG-IN", trackOut.getTag().getCode());
        assertEquals("blah", trackOut.getProperties().get("stringTest"));
        assertEquals(100d, trackOut.getProperties().get("doubleTest"));
        assertEquals(99, trackOut.getWeight().intValue());
        Long currentWhen = (Long) trackOut.getProperties().get(EntityTag.FD_WHEN);
        assertTrue(currentWhen > 0);

        logBean = new ContentInputBean("mike", new DateTime(), ContentDataHelper.getRandomMap());
        inputBean.getTags().clear();
        inputBean.addTag(outBound);
        inputBean.setContent(logBean);

        // Removing the inbound tag
        mediationFacade.trackEntity(su.getCompany(), inputBean);
        validateTag(created, null, 1);
        Collection<EntityTagResult> outboundTags = entityTagService.findOutboundTagResults(su.getCompany(), created);

        // One remains and is reversed
        assertEquals(1, outboundTags.size());

        // Cancelling last change should restore the inbound tag
        mediationFacade.cancelLastLog(su.getCompany(), created);
        // Total of two tags
        validateTag(created, null, 1);
        // One of which is outbound and the other inbound
        assertEquals(1, outboundTags.size());

        // Check that we still have our custom properties
        Collection<EntityTag> entityTags = entityTagService.findEntityTags(created);
        EntityTag entityTag = entityTags.iterator().next();
        Assert.assertEquals("TAG-IN", entityTag.getTag().getCode());
        assertEquals("blah", entityTag.getProperties().get("stringTest"));
        assertEquals(100d, entityTag.getProperties().get("doubleTest"));
        assertEquals(99, entityTag.getWeight().intValue());
        assertEquals(currentWhen, entityTag.getProperties().get(EntityTag.FD_WHEN));
    }

    @Test
    public void addNewTagToExistingEntityWithNoLog() throws Exception {
        SystemUser su = registerSystemUser("addNewTagToExistingEntityWithNoLog", mike_admin);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        //assertNotNull(result);
        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        // This should create the same Tag object
        inputBean.addTag(new TagInputBean("TagA", null, "camel"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);

        assertNotNull(resultBean);
        EntityNode entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        assertNotNull(entity);

        validateTag(entity, "TagA", 1);

        // Updating the entity with a second tag in the input will add it to the entity.
        inputBean.addTag(new TagInputBean("TagB", null, "horse"));
        mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        validateTag(entity, "TagB", 2);

    }

    @Test
    public void cancel_TagsAreArchived() throws Exception {
        SystemUser su = registerSystemUser("search", mike_admin);
        FortressNode fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("cancelLogTag", true));
        EntityInputBean inputBean = new EntityInputBean(fo, "wally", "CancelDoc", new DateTime(), "ABC123");
        ContentInputBean log = new ContentInputBean("wally", new DateTime(), ContentDataHelper.getRandomMap());
        inputBean.addTag(new TagInputBean("Happy").addEntityTagLink("testinga"));
        inputBean.addTag(new TagInputBean("Happy Days").addEntityTagLink("testingb"));
        inputBean.setContent(log);
        TrackResultBean result;
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        // We now have 1 log with tags validated in ES

        // Add another Log - replacing the two existing Tags with two new ones
        log = new ContentInputBean("wally", new DateTime(), ContentDataHelper.getRandomMap());
        inputBean.getTags().clear();
        inputBean.addTag(new TagInputBean("Sad Days").addEntityTagLink("testingb"));
        inputBean.addTag(new TagInputBean("Days Bay").addEntityTagLink("testingc"));
        inputBean.setContent(log);
        result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        // We now have 2 logs, sad tags, no happy tags

        // Cancel Log - this will remove the sad tags and leave us with happy tags
        mediationFacade.cancelLastLog(su.getCompany(), (EntityNode) result.getEntity());
        Collection<EntityTag> tags = entityTagService.findEntityTags(result.getEntity());
        assertEquals(2, tags.size());

    }

    @Test
    public void track_IgnoreGraphAndCheckSearch() throws Exception {
        // Validates that you can still get a SearchChange to index from an entity with no log
        logger.info("## track_IgnoreGraphAndCheckSearch started");
        SystemUser su = registerSystemUser("Isabella");
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("TrackGraph", true));

        EntityInputBean entityInput = new EntityInputBean(fortress, "wally", "ignoreGraph", new DateTime(), "ABC123");
        entityInput.setTrackSuppressed(true);
        entityInput.setEntityOnly(true); // If true, the entity will be indexed
        // Track suppressed but search is enabled
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertNotNull(searchService.getEntityChange(result));

    }

    @Test
    public void search_separateLogEventUpdatesSameSearchObject() throws Exception {
        logger.info("## search_nGramDefaults");
        SystemUser su = registerSystemUser("Romeo");
        FortressNode iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ngram", true));
        EntityInputBean inputBean = new EntityInputBean(iFortress, "olivia@sunnybell.com", "CompanyNode", new DateTime());
        inputBean.setDescription("This is a description");

        boolean oldValue = engineConfig.isSearchRequiredToConfirm();
        engineConfig.setSearchRequiredToConfirm(true);
        try {

            TrackResultBean trackResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
            SearchChange searchChange = searchService.getEntityChange(trackResult);

            searchChange.setSearchKey("SearchKey"); // any value

            SearchResults searchResults = getSearchResults(searchChange);
            searchHandler.handleResults(searchResults);

            assertNotNull(searchChange);

            Map<String, Object> what = ContentDataHelper.getSimpleMap(SearchSchema.WHAT_CODE, "AZERTY");
            what.put(SearchSchema.WHAT_NAME, "NameText");
            // Logging after the entity has been created
            trackResult = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", trackResult.getEntity().getKey(), new DateTime(), what));
            assertEquals("Search count should at least be 1", new Integer(1), trackResult.getEntity().getSearch());
            assertEquals("Search code value should be set if the platform requires it", searchChange.getSearchKey(), trackResult.getEntity().getSearchKey());
            SearchChange searchChangeB = searchService.getEntityChange(trackResult);
            assertEquals(searchChange.getId(), searchChangeB.getId());
            assertEquals("The log should be using the same search identifier", searchChange.getSearchKey(), searchChangeB.getSearchKey());
        } finally {
            engineConfig.setSearchRequiredToConfirm(oldValue);
        }

    }

    @Test
    public void count_NoExistingTagsFullTrackRequest() throws Exception {
        Boolean storeEnabled = engineConfig.storeEnabled();
        Store existing = engineConfig.store();
        // Emulates the default PostMan json track call.
        try {
            engineConfig.setStore(Store.MEMORY);
            engineConfig.setStoreEnabled(true);
            logger.info("## count_NoExistingTagsFullTrackRequest");
            SystemUser su = registerSystemUser("Blah");
            FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("count_NoExistingTagsFullTrackRequest", true));
            EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", new DateTime());
            inputBean.setDescription("This is a description");
            ContentInputBean cib = new ContentInputBean(ContentDataHelper.getRandomMap());
            inputBean.addTag(new TagInputBean("Samsung").setLabel("Law").addEntityTagLink("plaintiff"));
            inputBean.addTag(new TagInputBean("Apple").setLabel("Law").addEntityTagLink("defendant"));
            inputBean.setContent(cib);

            TrackResultBean trackResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
            assertNotNull(trackResult);
            Collection<EntityTag> tags = entityTagService.findEntityTags(trackResult.getEntity());
            assertEquals(2, tags.size());
        } finally {
            engineConfig.setStoreEnabled(storeEnabled);
            engineConfig.setStore(existing);

        }
    }

    // Use this to mock the search service result
    private SearchResults getSearchResults(SearchChange searchChange) {
        SearchResult searchResult = Mockito.mock(SearchResult.class);
        when(searchResult.getCode()).thenReturn(searchChange.getCode());
        when(searchResult.getCode()).thenReturn(searchChange.getCode());
        when(searchResult.getEntityId()).thenReturn(searchChange.getId());
        when(searchResult.getSearchKey()).thenReturn(searchChange.getSearchKey());
        Collection<SearchResult> results = new ArrayList<>();
        results.add(searchResult);
        SearchResults searchResults = new SearchResults(results);
        return searchResults;
    }

    @Test
    public void undefined_Tag() throws Exception {
        // DAT-411
        SystemUser su = registerSystemUser("undefined_Tag", mike_admin);
        FortressInputBean fib = new FortressInputBean("undefined_Tag", true);
        fib.setStoreEnabled(false);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);

        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "DAT386", "DAT386", new DateTime(), "abc");
        TagInputBean tagInput = new TagInputBean("MissingTag", "TestUndefined", new EntityTagRelationshipInput("rlx"))
            .setMustExist(true)
            .setNotFoundCode("Unknown");
        entityInput.addTag(tagInput);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);

        result = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertNotNull(result.getEntity());
        Collection<EntityTag> tags = entityTagService.findEntityTags(result.getEntity());
        assertNotNull(tags);
        assertEquals(1, tags.size());
        assertEquals(tagInput.getNotFoundCode(), tags.iterator().next().getTag().getCode());

        Tag byAlias = tagService.findTag(su.getCompany(), tagInput.getLabel(), null, tagInput.getCode());
        assertNotNull("Found tag should have resolved as mustExist code was set to Unknown", byAlias);
        assertEquals("Unknown", byAlias.getCode());
    }

    @Test
    public void custom_TagPath() throws Exception {
        SystemUser su = registerSystemUser("custom_TagPath", mike_admin);
        FortressInputBean fib = new FortressInputBean("custom_TagPath", true);
        fib.setStoreEnabled(false);
        FortressNode fortress = fortressService.registerFortress(su.getCompany(), fib);

        DocumentNode documentType = new DocumentNode(fortress, "DAT-498");
        documentType.setTagStructure(EntityTag.TAG_STRUCTURE.TAXONOMY);
        conceptService.save(documentType);
        documentType = conceptService.findDocumentType(fortress, documentType.getName());
        assertNotNull(documentType);
        assertNull(documentType.getGeoQuery());
        assertNotNull(documentType.getTagStructure());

        // Establish a multi path hierarchy
        TagInputBean interest = new TagInputBean("Motor", "Interest");
        TagInputBean category = new TagInputBean("cars", "Category");
        TagInputBean luxury = new TagInputBean("luxury cars", "Division");
        TagInputBean brands = new TagInputBean("brands", "Division");
        TagInputBean audi = new TagInputBean("audi", "Division");
        TagInputBean term = new TagInputBean("audi a3", "Term");

        category.setTargets("is", interest);
        luxury.setTargets("typed", category);
        brands.setTargets("typed", category);
        audi.setTargets("sub", brands);
        term.setTargets("classifying", audi);
        term.setTargets("classifying", luxury);
        mediationFacade.createTag(su.getCompany(), interest);

        EntityInputBean entityInputBean = new EntityInputBean(fortress, "blah", documentType.getName(), new DateTime());
        entityInputBean.setEntityOnly(true);
        term.addEntityTagLink("references");
        entityInputBean.addTag(term); // Terms are connected to entities
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), entityInputBean);
        assertNotNull(trackResultBean.getEntity());
        EntityTag.TAG_STRUCTURE etFinder = fortressService.getTagStructureFinder(trackResultBean.getEntity());
        assertNotNull("Unable to create the requested EntityTagFinder implementation ", etFinder);
        assertNotEquals(EntityTag.TAG_STRUCTURE.DEFAULT, etFinder);

        Collection<EntityTag> entityTags = entityTagService.findEntityTags(trackResultBean.getEntity());
        assertFalse("The custom EntityTag path should have been used to find the tags", entityTags.isEmpty());
        EntitySearchChange searchChange = searchService.getEntityChange(trackResultBean);
        assertNotNull(searchChange);
        assertFalse(searchChange.getTagValues().isEmpty());

        boolean divisonFound = false;
        boolean categoryFound = false;
        boolean interestFound = false;
        for (String s : searchChange.getTagValues().keySet()) {
            Map<String, ArrayList<SearchTag>> stringArrayListMap = searchChange.getTagValues().get(s);
            for (String labelType : stringArrayListMap.keySet()) {
                switch (labelType) {
                    case "term":
                        assertEquals(1, stringArrayListMap.get(labelType).size());
                        SearchTag theTerm = stringArrayListMap.get(labelType).iterator().next();
                        assertEquals("audi a3", theTerm.getCode());
                        assertEquals(3, theTerm.getParent().size());
                        divisonFound = theTerm.getParent().get("division") != null;
                        interestFound = theTerm.getParent().get("interest") != null;
                        categoryFound = theTerm.getParent().get("category") != null;
                        break;
                    default:
                        throw new RuntimeException("Unexpected entry " + labelType);
                }
            }
        }
        assertTrue("Division not found", divisonFound);
        assertTrue("Category not found", categoryFound);
        assertTrue("Interest not found", interestFound);

        String json = JsonUtils.toJson(searchChange);
        EntitySearchChange deserialized = JsonUtils.toObject(json.getBytes(), EntitySearchChange.class);
        assertNotNull(deserialized);
        assertEquals(searchChange.getTagValues().size(), deserialized.getTagValues().size());
        assertEquals(EntityTag.TAG_STRUCTURE.TAXONOMY, searchChange.getTagStructure());
    }

    @Test
    public void tagsInOut() throws Exception {
        SystemUser su = registerSystemUser("tagsInOut", mike_admin);
        String json = "[{\"code\":\"10000001\",\"fortress\":{\"name\":\"icij.org\",\"timeZone\":\"Pacific/Auckland\",\"enabled\":true,\"system\":false,\"code\":\"icij.org\"},\"documentType\":{\"name\":\"LegalEntity\",\"code\":\"LegalEntity\",\"versionStrategy\":\"FORTRESS\",\"tagStructure\":\"DEFAULT\"},\"when\":1143028800000,\"lastChange\":1361098800000,\"content\":{\"version\":1.0,\"when\":1143028800000,\"data\":{\"address\":\"ORION HOUSE SERVICES (HK) LIMITED ROOM 1401; 14/F.; WORLD COMMERCE  CENTRE; HARBOUR CITY; 7-11 CANTON ROAD; TSIM SHA TSUI; KOWLOON; HONG KONG\",\"internal_id\":1001256,\"jurisdiction\":\"SAM\",\"struck_off_date\":\"2013-02-15T00:00:00.000+13:00\",\"dorm_date\":null,\"service_provider\":\"Mossack Fonseca\",\"jurisdiction_description\":\"Samoa\",\"ibcRUC\":\"25221\",\"original_name\":\"TIANSHENG INDUSTRY AND TRADING CO., LTD.\",\"name\":\"TIANSHENG INDUSTRY AND TRADING CO., LTD.\",\"inactivation_date\":\"2013-02-18T00:00:00.000+13:00\",\"country_codes\":\"HKG\",\"incorporation_date\":\"2006-03-23T00:00:00.000+12:00\",\"status\":\"Defaulted\",\"node_id\":10000001},\"forceReindex\":false,\"contentType\":\"json\",\"transactional\":false},\"tags\":[{\"code\":\"SAM\",\"reverse\":false,\"label\":\"Jurisdiction\",\"entityTagLinks\":{\"jurisdiction\":{\"geo\":true,\"relationshipName\":\"jurisdiction\"}},\"mustExist\":false,\"aliases\":[],\"since\":false,\"merge\":false},{\"code\":\"Mossack Fonseca\",\"reverse\":true,\"label\":\"ServiceProvider\",\"entityTagLinks\":{\"manages\":{\"relationshipName\":\"manages\"}},\"mustExist\":false,\"since\":false,\"merge\":true},{\"code\":\"25221\",\"reverse\":false,\"label\":\"RUC\",\"entityTagLinks\":{\"ibc\":{\"relationshipName\":\"ibc\"}},\"mustExist\":false,\"since\":false,\"merge\":true},{\"code\":\"HKG\",\"reverse\":false,\"label\":\"Country\",\"entityTagLinks\":{\"located\":{\"geo\":true,\"relationshipName\":\"located\"}},\"mustExist\":false,\"aliases\":[],\"since\":false,\"merge\":false}],\"entityLinks\":{},\"properties\":{},\"description\":\"TIANSHENG INDUSTRY AND TRADING CO., LTD.\",\"name\":\"TIANSHENG INDUSTRY AND TRADING CO., LTD.\",\"searchSuppressed\":false,\"trackSuppressed\":false,\"entityOnly\":false,\"timezone\":\"Pacific/Auckland\",\"archiveTags\":false}]";
        Collection<EntityInputBean> eib = JsonUtils.toCollection(json.getBytes(), EntityInputBean.class);
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), eib.iterator().next());
        assertNotNull(result);
        Iterable<EntityTag> tags = entityTagService.findEntityTagsWithGeo(result.getEntity());
        assertNotNull(tags);
        assertTrue(tags.iterator().hasNext());
        int count = 0;

        for (EntityTag tag : tags) {
            count++;
        }
        assertEquals(4, count);

    }

    private void validateTag(Entity entity, String tagName, int totalExpected) {
        Collection<EntityTag> entityTags;
        entityTags = entityTagService.findEntityTags(entity);
        if (tagName == null) {
            assertEquals("Total Expected Tags is incorrect", totalExpected, entityTags.size());
            return;
        }

        boolean found = false;
        for (EntityTag entityTag : entityTags) {
            if (entityTag.getTag().getCode().equals(tagName)) {
                found = true;
                break;
            }
        }
        if (totalExpected == 0 && !found) {
            return;
        }
        if (totalExpected == 0) {
            fail("The expected tag [" + tagName + "] was found when it was not expected to exist");
            return;
        }
        assertTrue("The expected tag [" + tagName + "] was not found", found);
    }

}
