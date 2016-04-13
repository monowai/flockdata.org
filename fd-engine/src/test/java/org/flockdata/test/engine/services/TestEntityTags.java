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

package org.flockdata.test.engine.services;

/**
 * User: Mike Holdsworth
 * Date: 27/06/13
 * Time: 4:49 PM
 */

import org.flockdata.dao.EntityTagDao;
import org.flockdata.engine.track.service.SearchHandler;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.*;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.model.*;
import org.flockdata.store.Store;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.*;
import org.flockdata.track.service.EntityService;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.Assert.*;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:11 AM
 */
public class TestEntityTags extends EngineBase {

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

        FortressInputBean fib = new FortressInputBean("ABC", true);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);
        assertNotNull(fortress);

        TagInputBean firstTag = new TagInputBean("firstTag", null, "demo");

        EntityInputBean entityBean = new EntityInputBean(fortress, "mtest", "aTest", new DateTime(), "abc");
        entityBean.addTag(firstTag);
        entityBean.setArchiveTags(false);
        ContentInputBean contentBean = new ContentInputBean();
        contentBean.setEvent("Test");
        Map<String, Object> jsonMap = EntityContentHelper.getRandomMap();
        contentBean.setData(jsonMap);
        entityBean.setContent(contentBean);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());

        Assert.assertEquals(1, entityService.getLogCount(su.getCompany(), entity.getKey()));
        Assert.assertEquals(1, entityTagService.getEntityTags(entity).size());

        // Scenario 1: We send in the header with no content
        entityBean.setContent(null);
        mediationFacade.trackEntity(su.getCompany(), entityBean);
        Assert.assertEquals(1, entityService.getLogCount(su.getCompany(), entity.getKey()));
        Assert.assertEquals(1, entityTagService.getEntityTags(entity).size());

        // Scenario 2: We have an existing entity with content logged - it has one existing tag
        //           we now have a second tag added but no content.
        TagInputBean secondTag = new TagInputBean("secondTag", null, "demo");
        entityBean.addTag(secondTag);
        entity = mediationFacade.trackEntity(su.getCompany(), entityBean).getEntity();

        Assert.assertEquals(1, entityService.getLogCount(su.getCompany(), entity.getKey()));
        Assert.assertEquals(2, entityTagService.getEntityTags(entity).size());

        EntityLog lastLog = logService.getLastLog(entity);
        assertNotNull(lastLog);
        Map<String, Object> values = mediationFacade.getLogContent(entity, lastLog.getId());
        assertFalse(values.isEmpty());
        assertEquals(jsonMap.get("Key").toString(), values.get("Key").toString());

    }

    @Test
    public void simpleTagAgainstEntity() throws Exception {
        SystemUser su = registerSystemUser("simpleTagAgainstEntity", mike_admin);
        assertNotNull(su);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        TagInputBean flopTag = new TagInputBean("FLOP");
        tagService.createTag(su.getCompany(), flopTag);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());

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
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("dates_SinceRecorded", true));
        assertNotNull(fortress);

        TagInputBean flopTag = new TagInputBean("FLOP");
        tagService.createTag(su.getCompany(), flopTag);

        DateTime fCreated = new DateTime().minus(10000);
        EntityInputBean entityBean = new EntityInputBean(fortress, "anyone", "aTest", fCreated, "abc");
        TrackResultBean resultBean = mediationFacade.trackEntity(fortress.getDefaultSegment(), entityBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());

        assertEquals(fCreated.getMillis(), entity.getFortressCreatedTz().getMillis());

        EntityTagInputBean tagA = new EntityTagInputBean(entity.getKey(), flopTag.getCode(), "ABC").
                setSince(true);
        entityTagService.processTag(entity, tagA);

        Boolean tagRlxExists = entityTagService.relationshipExists(entity, flopTag.getCode(), "ABC");
        assertTrue("Tag not found " + flopTag.getName(), tagRlxExists);

        Collection<EntityTag> entityTags = entityTagService.getEntityTags(entity);
        for (EntityTag entityTag : entityTags) {
            assertEquals("Date did not correspond to the Fortress created date", entity.getFortressCreatedTz().getMillis(), Long.parseLong(entityTag.getProperties().get(EntityTag.SINCE).toString()));
        }

        // Creating some content and adding a new Tag to the entity
        DateTime fUpdated = new DateTime().minus(10000);
        ContentInputBean contentInputBean = new ContentInputBean("harry", fUpdated, EntityContentHelper.getRandomMap());

        entityBean.addTag(new TagInputBean("Tag2", null, "second").
                setSince(true));

        entityBean.setArchiveTags(false);// We don't have a reference to the original tag in the Input
        // as we assigned it in a secondary step, so will accumulate tags and stop them being archived
        entityBean.setContent(contentInputBean);
        mediationFacade.trackEntity(fortress.getDefaultSegment(), entityBean);
        entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        assertEquals(fCreated, entity.getFortressCreatedTz());
        assertEquals(fUpdated.getMillis(), entity.getFortressUpdatedTz().getMillis());
        entityTags = entityTagService.getEntityTags(entity);
        assertEquals(2, entityTags.size());
        for (EntityTag tag : entityTags) {
            if (tag.getTag().getCode().equalsIgnoreCase(flopTag.getCode()))
                assertEquals("Date did not correspond to the Fortress created date", entity.getFortressCreatedTz().getMillis(), Long.parseLong(tag.getProperties().get(EntityTag.SINCE).toString()));
            else
                assertEquals("Date did not correspond to the Fortress updated date", entity.getFortressUpdatedTz().getMillis(), Long.parseLong(tag.getProperties().get(EntityTag.SINCE).toString()));
        }

    }

    @Test
    public void DAT386() throws Exception {
        SystemUser su = registerSystemUser("DAT386", mike_admin);
        FortressInputBean fib = new FortressInputBean("DAT386", true);
        fib.setStoreActive(false);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        TagInputBean tagInput = new TagInputBean("DAT386");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "DAT386", "DAT386", new DateTime(), "abc");
        entityInput.addTag(new TagInputBean("TagA", null, "aaaa"));
        entityInput.addTag(new TagInputBean("TagB", null, "bbbb"));

        mediationFacade.trackEntity(su.getCompany(), entityInput);
        ContentInputBean contentInputBean = new ContentInputBean(EntityContentHelper.getRandomMap());
        entityInput = new EntityInputBean(fortress, "DAT386", "DAT386", new DateTime(), "abc");
        entityInput.addTag(new TagInputBean("TagA", null, "aaaa"));
        entityInput.setContent(contentInputBean);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertNotNull(result.getEntity());
        assertEquals(1, entityTagService.findEntityTags(su.getCompany(), result.getEntity()).size());
    }

    @Test
    public void renameRelationship() throws Exception {

        SystemUser su = registerSystemUser("renameRelationship", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        entityInput.addTag(new TagInputBean("TagA", null, "AAAA"));
        entityInput.addTag(new TagInputBean("TagB", null, "BBBB"));
        entityInput.addTag(new TagInputBean("TagC", null, "CCCC"));
        entityInput.addTag(new TagInputBean("TagD", null, "DDDD"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(su.getCompany(), entity);

        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());
        assertFalse(entityTagService.relationshipExists(entity, "TagC", "!!Twee!!"));//
        // Remove a single tag
        for (EntityTag value : tagSet) {
            if (value.getTag().getCode().equals("TagC"))
                entityTagService.changeType(entity, value, "!!Twee!!");
        }

        assertTrue(entityTagService.relationshipExists(entity, "TagC", "!!Twee!!"));
    }

    @Test
    public void createAndDeleteEntityTags() throws Exception {

        SystemUser su = registerSystemUser("createAndDeleteEntityTags", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");

        entityInput.addTag(new TagInputBean("TagA", null, "AAAA"));
        entityInput.addTag(new TagInputBean("TagB", null, "BBBB"));
        entityInput.addTag(new TagInputBean("TagC", null, "CCCC"));
        entityInput.addTag(new TagInputBean("TagD", null, "DDDD"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(su.getCompany(), entity);

        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());
        // Remove a single tag
        for (EntityTag value : tagSet) {
            if (value.getTag().getCode().equals("TagB"))
                entityTagService.deleteEntityTags(entity, value);
        }
        tagSet = entityTagService.findEntityTags(su.getCompany(), entity);
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
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        entityInput.addTag(new TagInputBean("TagA", "TestTag", "rlx"));
        entityInput.addTag(new TagInputBean("TagB", "TestTag", "rlx"));
        entityInput.addTag(new TagInputBean("TagC", "TestTag", "rlx"));
        entityInput.addTag(new TagInputBean("TagD", "TestTag", "rlx"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(su.getCompany(), entity);
        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());

        entityService.updateEntity(entity);
        entity = entityService.getEntity(su.getCompany(), entity.getKey());
        EntitySummaryBean summaryBean = entityService.getEntitySummary(null, entity.getKey());
        tagSet = summaryBean.getTags();
        assertNotNull(tagSet);
        Set<Entity> entities = entityTagService.findEntityTags(su.getCompany(), "TagA");
        assertNotNull(entities);
        assertNotSame(entities.size() + " Entities returned!", 0, entities.size());

        assertEquals(entity.getKey(), entities.iterator().next().getKey());
        entities = entityTagService.findEntityTags(su.getCompany(), "TagC");
        assertNotNull(entities);
        assertEquals(entity.getKey(), entities.iterator().next().getKey());
        entities = entityTagService.findEntityTags(su.getCompany(), "TagD");
        assertNotNull(entities);
        assertEquals(entity.getKey(), entities.iterator().next().getKey());
    }

    @Test
    public void nullCodeValue() throws Exception {
        SystemUser su = registerSystemUser("nullCodeValue", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        // In this scenario, the Tag name is the key if the value is null
        TagInputBean tag = new TagInputBean("TagD", null, "DDDD");
        tag.setName(null);
        entityInput.addTag(tag);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(su.getCompany(), entity);
        assertNotNull(tagSet);
        assertEquals(1, tagSet.size());

    }

    @Test
    public void duplicateTagNotCreated() throws Exception {
        SystemUser su = registerSystemUser("duplicateTagNotCreated", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        entityInput.addTag(new TagInputBean("TagA", null, "camel"));
        entityInput.addTag(new TagInputBean("taga", null, "lower"));
        entityInput.addTag(new TagInputBean("tAgA", null, "mixed"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Tag tag = tagService.findTag(su.getCompany(), null, "Taga");
        assertNotNull(tag);
        Collection<EntityTag> entityTags = entityTagService.findEntityTags(su.getCompany(), entity);
        for (EntityTag entityTag : entityTags) {
            Assert.assertEquals("Expected same tag for each relationship", tag.getId(), entityTag.getTag().getId());
        }
        assertEquals("Expected 3 relationships for the same tag", 3, entityTags.size());

    }

    @Test
    public void trackSupressed_EntityTagsAreStillReturned() throws Exception {
        SystemUser su = registerSystemUser("noEntityTagsAreReturned", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

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
            if (id == null)
                id = entityTag.getTag().getId();
            Assert.assertEquals(id, entityTag.getTag().getId());
        }
        assertNull(resultBean.getEntity().getKey());

    }

    @Test
    public void createLogForInvalidEntity() throws Exception {
        SystemUser su = registerSystemUser("createLogForInvalidEntity", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        EntityInputBean entity = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        mediationFacade.trackEntity(su.getCompany(), entity);
        ContentInputBean contentInputBean = new ContentInputBean("Harry", "InvalidKey", new DateTime(), EntityContentHelper.getRandomMap());
        exception.expect(FlockException.class);
        mediationFacade.trackLog(su.getCompany(), contentInputBean);

    }

    @Test
    public void createLogForValidEntityWithNoContent() throws Exception {
        SystemUser su = registerSystemUser("createLogForValidEntityWithNoContent", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        EntityInputBean entity = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TrackResultBean rb = mediationFacade.trackEntity(su.getCompany(), entity);
        ContentInputBean contentInput = new ContentInputBean("Harry", rb.getEntity().getKey(), new DateTime(), null);
        assertNotNull(mediationFacade.trackLog(su.getCompany(), contentInput));
    }

    @Test
    public void differentTagTypeSameTagName() throws Exception {
        SystemUser su = registerSystemUser("differentTagTypeSameTagName", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);

        EntityInputBean entityInput = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TagInputBean tag = new TagInputBean("TagA");
        tag.addEntityLink("Type1");
        tag.addEntityLink("Type2");
        tag.addEntityLink("Type3");
        entityInput.addTag(tag);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(su.getCompany(), entity);
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

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@flockdata.com", "Email", "email-to");
        tagA.addEntityLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@flockdata.com", "Email", "email-cc");
        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey(), true);
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(su.getCompany(), entity);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        EntitySummaryBean summaryBean = entityService.getEntitySummary(null, entity.getKey());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithNullProperties() throws Exception {
        SystemUser su = registerSystemUser("mapRelationshipsWithNullProperties", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean tagA = new TagInputBean("mike@flockdata.com", null, "email-to");
        tagA.addEntityLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@flockdata.com", null, "email-cc");
        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey(), true);
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(entity);
        EntitySummaryBean summaryBean = entityService.getEntitySummary(null, entity.getKey());
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithProperties() throws Exception {
        SystemUser su = registerSystemUser("mapRelationshipsWithProperties", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        Map<String, Object> propA = new HashMap<>();
        Map<String, Object> propB = new HashMap<>();
        propA.put("myValue", 10);
        propB.put("myValue", 20);

        TagInputBean tagA = new TagInputBean("mike@flockdata.com", "Email", "email-to", propA).setLabel("Email");
        tagA.addEntityLink("email-cc", propB);
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "Email", "email-cc");

        inputBean.addTag(tagA);
        inputBean.addTag(tagB);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey(), true);
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(su.getCompany(), entity);
        EntitySummaryBean summaryBean = entityService.getEntitySummary(null, entity.getKey());
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void duplicateRLXTypesNotStored() throws Exception {
        SystemUser su = registerSystemUser("duplicateRLXTypesNotStored", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagInputBean = new TagInputBean("mike@flockdata.com", null, "email-to");
        tagInputBean.addEntityLink("email-to");
        tagInputBean.addEntityLink("email-to");

        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey(), true);
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(su.getCompany(), entity);
        assertEquals("One for the Generic tag and one for exploration", 1, tagResults.size());
    }

    @Test
    public void directedEntityTagsWork() throws Exception {
        SystemUser su = registerSystemUser("directedEntityTagsWork", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagInputBean = new TagInputBean("mike@auditbucket.com", null, "email-to");
        tagInputBean.setReverse(true); // relationships will be reversed
        tagInputBean.addEntityLink("email-to");

        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        // By default, tags are inbound to the Entity. This asserts the reverse also works
        Collection<EntityTag> tagResults = entityTagService.findOutboundTags(entity);
        assertEquals("No tag heading out from the Entity could be found", 1, tagResults.size());

    }

    @Test
    public void tagsAndValuesWithSpaces() throws Exception {

        SystemUser su = registerSystemUser("tagsAndValuesWithSpaces", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@flockdata.com", null, "email-to");
        tagA.addEntityLink("email cc");
        TagInputBean tagB = new TagInputBean("np@flockdata.com", null, "email-cc");

        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(su.getCompany(), entity);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        EntitySummaryBean summaryBean = entityService.getEntitySummary(null, entity.getKey());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void entity_nestedTagStructure() throws Exception {

        SecurityContextHolder.getContext().setAuthentication(authDefault);
        SystemUser su = registerSystemUser("entity_nestedTagStructure", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
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

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
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
        Collection<EntityTag> tags = entityTagService.findEntityTags(su.getCompany(), resultBean.getEntity());
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
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
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
        inputBean.setContent(new ContentInputBean(EntityContentHelper.getRandomMap()));

        // Institution<-city<-state<-country

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(resultBean);
        Collection<EntityTag> tags = entityTagService.findEntityTags(su.getCompany(), resultBean.getEntity());
        assertFalse(tags.isEmpty());

        SearchChange searchChange = searchService.getSearchChange(resultBean);
        assertNotNull(searchChange);
        assertNotNull(searchChange.getTagValues());
    }

    @Test
    public void entity_LocatedGeoData() throws Exception {
        // DAT-452
        SystemUser su = registerSystemUser("entity_LocatedGeoData", mike_admin);
        assertNotNull(su);
        engineConfig.setTestMode(true);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entity_LocatedGeoData", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "geoTracTest", "anyvalue", new DateTime(), "abc");
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country, "Country");
        TagInputBean cityInputTag = new TagInputBean(city, "City");
        TagInputBean stateInputTag = new TagInputBean("CA", "State");

        // Institution is in a city
        inputBean.addTag(cityInputTag.addEntityLink("geodata"));
        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);

        inputBean.setContent(new ContentInputBean(EntityContentHelper.getRandomMap()));

        // Institution<-city<-state<-country

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(resultBean);

        Iterable<EntityTag> tags = entityTagService.getEntityTagsWithGeo(resultBean.getEntity());
        for (EntityTag tag : tags) {
            logger.info(tag.toString());
            assertEquals("geodata", tag.getRelationship());
            assertNotNull("geo data block not found for a located relationship connected to an entity",
                    tag.getGeoData());
        }

        SearchChange searchChange = searchService.getSearchChange(resultBean);
        assertNotNull(searchChange);
        assertNotNull(searchChange.getTagValues());
    }

    @Test
    public void entityTag_SimpleRelationship() throws Exception {
        String name = "Doctor John";
        TagInputBean authorTag = new TagInputBean(name + ":person");
        authorTag.addEntityLink("writer");
        authorTag.addEntityLink("lead");

        SystemUser su = registerSystemUser("targetTagWithAuditRelationship", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean countryTag = new TagInputBean("New Zealand");
        TagInputBean cityTag = new TagInputBean("Auckland");
        TagInputBean institution = new TagInputBean("Auckland University:Institution");
        cityTag.setTargets("jurisdiction", countryTag); // Auckland located in NZ

        institution.addEntityLink("located");
        cityTag.addEntityLink("city");

        inputBean.addTag(cityTag); // Not attached to entity
        inputBean.addTag(countryTag);
        inputBean.addTag(institution);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(result);
        Collection<EntityTag> tags = entityTagService.findEntityTags(su.getCompany(), result.getEntity());
        assertEquals(2, tags.size());
        for (EntityTag tag : tags) {
            assertTrue(tag.getTag().getCode().equals(institution.getCode()) || tag.getTag().getCode().equals(cityTag.getCode()));
        }

    }

    @Test
    public void geoTag() throws Exception {
        SystemUser su = registerSystemUser("geoTag", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));
        assertNotNull(fortress);

        EntityInputBean entityInput = new EntityInputBean(fortress, "geoTest", "geoTest", new DateTime(), "abc");
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country, "Country", "");
        TagInputBean cityInputTag = new TagInputBean("LA", "City", "").setName(city);
        TagInputBean stateInputTag = new TagInputBean("CA", "State", "");

        TagInputBean institutionTag = new TagInputBean("mikecorp", null, "owns");
        // Institution is in a city
        institutionTag.setTargets("located", cityInputTag);
        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);
        entityInput.addTag(institutionTag);

        // Institution<-city<-state<-country

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertNotNull(resultBean);
        assertNotNull(tagService.findTag(fortress.getCompany(), "Country", null, "USA"));

        Iterable<EntityTag> tags = entityTagService.getEntityTagsWithGeo(resultBean.getEntity());
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
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("TEST-CREATE", null, "rlx-test");

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(), EntityContentHelper.getRandomMap());
        inputBean.setContent(logBean);

        inputBean.addTag(tagInput);
        TrackResultBean entityResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity created = entityService.getEntity(su.getCompany(), entityResult.getEntity().getKey());
        Log firstLog = entityService.getLastEntityLog(su.getCompany(), created.getKey()).getLog();
        assertNotNull(created);

        // Test that a tag is removed
        EntityInputBean updatedEntity = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        // Force a change to be detected
        ContentInputBean alb = new ContentInputBean("mike", new DateTime(), EntityContentHelper.getRandomMap());
        updatedEntity.setContent(alb);
        // Updating an existing Entity but the tagCollection is minus TEST-CREATE tag
        // The create call should create a new Tag - TEST-UPDATE - and then remove the TEST-CREATE
        updatedEntity.addTag(new TagInputBean("TEST-UPDATE", null, "camel"));
        entityResult = mediationFacade.trackEntity(su.getCompany(), updatedEntity);
        Entity entity = entityService.getEntity(su.getCompany(), entityResult.getEntity().getKey());
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

        alb = new ContentInputBean("mike", new DateTime(), EntityContentHelper.getRandomMap());
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
        Collection<EntityTag> entityTags = entityTagService.findEntityTags(entity.getSegment().getCompany(), entity);
        for (EntityTag entityTag : entityTags) {
            assertEquals("Archived relationship name was not restored", "rlx-test", entityTag.getRelationship());
        }
    }

    @Test
    public void oneTagRemovedFromASetOfTwo() throws Exception {
        SystemUser su = registerSystemUser("oneTagRemovedFromASetOfTwo", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("TAG-FIRST", null, "rlx-test");

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(), EntityContentHelper.getRandomMap());
        inputBean.setContent(logBean);

        inputBean.addTag(tagInput);
        tagInput = new TagInputBean("TAG-SECOND", null, "rlxb-test");
        inputBean.addTag(tagInput);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity created = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        entityService.getLastEntityLog(su.getCompany(), created.getKey()).getLog();
        assertNotNull(created);
        validateTag(created, null, 2);

        // Test that a tag is removed
        EntityInputBean updatedEntity = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        // Force a change to be detected
        ContentInputBean alb = new ContentInputBean("mike", new DateTime(), EntityContentHelper.getRandomMap());
        updatedEntity.setContent(alb);
        // we are updating an existing entity with two tags and telling it that only one is now valid
        updatedEntity.addTag(new TagInputBean("TAG-FIRST", null, "rlx-test"));
        resultBean = mediationFacade.trackEntity(su.getCompany(), updatedEntity);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
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
        Assert.assertEquals(1, entityTagService.getEntityTags(entity).size());

    }

    @Test
    public void addNewTagToExistingEntity() throws Exception {
        SystemUser su = registerSystemUser("addNewTagToExistingEntity", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        //assertNotNull(result);
        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(), EntityContentHelper.getRandomMap());
        inputBean.setContent(logBean);
        // This should create the same Tag object
        inputBean.addTag(new TagInputBean("TagA", "TestTag", "camel"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);

        assertNotNull(resultBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
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
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        //assertNotNull(result);
        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(), EntityContentHelper.getRandomMap());
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
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(), EntityContentHelper.getRandomMap());
        inputBean.setContent(logBean);

        Map<String, Object> rlxProperties = new HashMap<>();
        rlxProperties.put("stringTest", "blah");
        rlxProperties.put("doubleTest", 100d);
        rlxProperties.put("weight", 99);
        rlxProperties.put("abAdded", "z");

        TagInputBean inBound = new TagInputBean("TAG-IN", "DefTag", "rlx-test", rlxProperties);
        inputBean.addTag(inBound);

        TagInputBean outBound = new TagInputBean("TAG-OUT", null, "rlxb-test").setReverse(true);

        inputBean.addTag(outBound);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity created = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
        entityService.getLastEntityLog(su.getCompany(), created.getKey()).getLog();

        // Total of two tags
        validateTag(created, null, 2);

        Collection<EntityTag> outboundTags = entityTagService.findInboundTags(created);
        assertEquals("One tag should be reversed", 1, outboundTags.size());
        EntityTag trackOut = outboundTags.iterator().next();
        Assert.assertEquals("TAG-IN", trackOut.getTag().getCode());
        assertEquals("blah", trackOut.getProperties().get("stringTest"));
        assertEquals(100d, trackOut.getProperties().get("doubleTest"));
        assertEquals(99, trackOut.getWeight().intValue());
        Long currentWhen = (Long) trackOut.getProperties().get(EntityTagDao.FD_WHEN);
        assertTrue(currentWhen > 0);

        logBean = new ContentInputBean("mike", new DateTime(), EntityContentHelper.getRandomMap());
        inputBean.getTags().clear();
        inputBean.addTag(outBound);
        inputBean.setContent(logBean);

        // Removing the inbound tag
        mediationFacade.trackEntity(su.getCompany(), inputBean);
        validateTag(created, null, 1);
        outboundTags = entityTagService.findOutboundTags(su.getCompany(), created);

        // One remains and is reversed
        assertEquals(1, outboundTags.size());

        // Cancelling last change should restore the inbound tag
        mediationFacade.cancelLastLog(su.getCompany(), created);
        // Total of two tags
        validateTag(created, null, 1);
        // One of which is outbound and the other inbound
        assertEquals(1, outboundTags.size());

        // Check that we still have our custom properties
        outboundTags = entityTagService.getEntityTags(created);
        trackOut = outboundTags.iterator().next();
        Assert.assertEquals("TAG-IN", trackOut.getTag().getCode());
        assertEquals("blah", trackOut.getProperties().get("stringTest"));
        assertEquals(100d, trackOut.getProperties().get("doubleTest"));
        assertEquals(99, trackOut.getWeight().intValue());
        assertEquals(currentWhen, trackOut.getProperties().get(EntityTagDao.FD_WHEN));
    }

    @Test
    public void addNewTagToExistingEntityWithNoLog() throws Exception {
        SystemUser su = registerSystemUser("addNewTagToExistingEntityWithNoLog", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        //assertNotNull(result);
        EntityInputBean inputBean = new EntityInputBean(fortress, "auditTest", "aTest", new DateTime(), "abc1");
        // This should create the same Tag object
        inputBean.addTag(new TagInputBean("TagA", null, "camel"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);

        assertNotNull(resultBean);
        Entity entity = entityService.getEntity(su.getCompany(), resultBean.getEntity().getKey());
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
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("cancelLogTag", true));
        EntityInputBean inputBean = new EntityInputBean(fo, "wally", "CancelDoc", new DateTime(), "ABC123");
        ContentInputBean log = new ContentInputBean("wally", new DateTime(), EntityContentHelper.getRandomMap());
        inputBean.addTag(new TagInputBean("Happy").addEntityLink("testinga"));
        inputBean.addTag(new TagInputBean("Happy Days").addEntityLink("testingb"));
        inputBean.setContent(log);
        TrackResultBean result;
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        // We now have 1 log with tags validated in ES

        // Add another Log - replacing the two existing Tags with two new ones
        log = new ContentInputBean("wally", new DateTime(), EntityContentHelper.getRandomMap());
        inputBean.getTags().clear();
        inputBean.addTag(new TagInputBean("Sad Days").addEntityLink("testingb"));
        inputBean.addTag(new TagInputBean("Days Bay").addEntityLink("testingc"));
        inputBean.setContent(log);
        result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        // We now have 2 logs, sad tags, no happy tags

        // Cancel Log - this will remove the sad tags and leave us with happy tags
        mediationFacade.cancelLastLog(su.getCompany(), result.getEntity());
        Collection<EntityTag> tags = entityTagService.findEntityTags(su.getCompany(), result.getEntity());
        assertEquals(2, tags.size());

    }

    @Test
    public void track_IgnoreGraphAndCheckSearch() throws Exception {
        // Validates that you can still get a SearchChange to index from an entity with no log
        logger.info("## track_IgnoreGraphAndCheckSearch started");
        SystemUser su = registerSystemUser("Isabella");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("TrackGraph", true));

        EntityInputBean entityInput = new EntityInputBean(fortress, "wally", "ignoreGraph", new DateTime(), "ABC123");
        entityInput.setTrackSuppressed(true);
        entityInput.setEntityOnly(true); // If true, the entity will be indexed
        // Track suppressed but search is enabled
        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertNotNull(searchService.getSearchChange(result));

    }

    @Autowired
    SearchHandler searchHandler;

    @Test
    public void search_seperateLogEventUpdatesSameSearchObject() throws Exception {
        logger.info("## search_nGramDefaults");
        SystemUser su = registerSystemUser("Romeo");
        Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ngram", true));
        EntityInputBean inputBean = new EntityInputBean(iFortress, "olivia@sunnybell.com", "CompanyNode", new DateTime());
        inputBean.setDescription("This is a description");

        TrackResultBean trackResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        SearchChange searchChange = searchService.getSearchChange(trackResult);

        searchChange.setSearchKey("SearchKey"); // any value

        SearchResults searchResults = getSearchResults(searchChange);
        searchHandler.handlResults(searchResults);

        assertNotNull(searchChange);

        Map<String, Object> what = EntityContentHelper.getSimpleMap(EntitySearchSchema.WHAT_CODE, "AZERTY");
        what.put(EntitySearchSchema.WHAT_NAME, "NameText");
        // Logging after the entity has been created
        trackResult = mediationFacade.trackLog(su.getCompany(), new ContentInputBean("olivia@sunnybell.com", trackResult.getEntity().getKey(), new DateTime(), what));
        assertNotNull(trackResult.getEntity().getSearchKey());
        SearchChange searchChangeB = searchService.getSearchChange(trackResult);
        assertEquals(searchChange.getEntityId(), searchChangeB.getEntityId());
        assertEquals("The log should be using the same search identifier", searchChange.getSearchKey(), searchChangeB.getSearchKey());

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
            Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("count_NoExistingTagsFullTrackRequest", true));
            EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", new DateTime());
            inputBean.setDescription("This is a description");
            ContentInputBean cib = new ContentInputBean(EntityContentHelper.getRandomMap());
            inputBean.addTag(new TagInputBean("Samsung").setLabel("Law").setEntityLink("plaintiff"));
            inputBean.addTag(new TagInputBean("Apple").setLabel("Law").setEntityLink("defendant"));
            inputBean.setContent(cib);

            TrackResultBean trackResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
            assertNotNull(trackResult);
            Collection<EntityTag> tags = entityTagService.getEntityTags(trackResult.getEntity());
            assertEquals(2, tags.size());
        } finally {
            engineConfig.setStoreEnabled(storeEnabled);
            engineConfig.setStore(existing);

        }
    }

    // Use this to mock the search service result
    private SearchResults getSearchResults(SearchChange searchChange) {
        SearchResults searchResults = new SearchResults();
        SearchResult searchResult = new SearchResult(searchChange);
        searchResults.addSearchResult(searchResult);
        return searchResults;
    }

    @Test
    public void undefined_Tag() throws Exception {
        // DAT-411
        SystemUser su = registerSystemUser("undefined_Tag", mike_admin);
        FortressInputBean fib = new FortressInputBean("undefined_Tag", true);
        fib.setStoreActive(false);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean(fortress, "DAT386", "DAT386", new DateTime(), "abc");
        TagInputBean tagInput = new TagInputBean("MissingTag", "TestUndefined", "rlx").setMustExist(true, "Unknown");
        entityInput.addTag(tagInput);

        mediationFacade.trackEntity(su.getCompany(), entityInput);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertNotNull(result.getEntity());
        Collection<EntityTag> tags = entityTagService.findEntityTags(su.getCompany(), result.getEntity());
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
        fib.setStoreActive(false);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);

        DocumentType documentType = new DocumentType(fortress, "DAT-498");
        documentType.setTagStructure(EntityService.TAG_STRUCTURE.TAXONOMY);
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
        term.setEntityLink("references");
        entityInputBean.addTag(term); // Terms are connected to entities
        TrackResultBean trackResultBean = mediationFacade.trackEntity(su.getCompany(), entityInputBean);
        assertNotNull(trackResultBean.getEntity());
        EntityService.TAG_STRUCTURE etFinder = fortressService.getTagStructureFinder(trackResultBean.getEntity());
        assertNotNull("Unable to create the requested EntityTagFinder implementation ", etFinder);
        assertNotEquals(EntityService.TAG_STRUCTURE.DEFAULT, etFinder);

        Collection<EntityTag> entityTags = entityTagService.findEntityTags(su.getCompany(), trackResultBean.getEntity());
        assertFalse("The custom EntityTag path should have been used to find the tags", entityTags.isEmpty());
        SearchChange searchChange = searchService.getSearchChange(trackResultBean);
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
        SearchChange deserialized = JsonUtils.toObject(json.getBytes(), EntitySearchChange.class);
        assertNotNull(deserialized);
        assertEquals(searchChange.getTagValues().size(), deserialized.getTagValues().size());
        assertEquals(EntityService.TAG_STRUCTURE.TAXONOMY, searchChange.getTagStructure());
    }

    private void validateTag(Entity entity, String tagName, int totalExpected) {
        Collection<EntityTag> entityTags;
        entityTags = entityTagService.findEntityTags(entity.getSegment().getCompany(), entity);
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
        if (totalExpected == 0 && !found)
            return;
        if (totalExpected == 0) {
            fail("The expected tag [" + tagName + "] was found when it was not expected to exist");
            return;
        }
        assertTrue("The expected tag [" + tagName + "] was not found", found);
    }

}
