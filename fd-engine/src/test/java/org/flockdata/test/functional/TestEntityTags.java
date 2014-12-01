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

/**
 * User: Mike Holdsworth
 * Date: 27/06/13
 * Time: 4:49 PM
 */

import org.flockdata.dao.EntityTagDao;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.registration.model.Tag;
import org.flockdata.test.utils.Helper;
import org.flockdata.track.bean.*;
import org.flockdata.track.model.*;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.Assert.*;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:11 AM
 */
public class TestEntityTags extends EngineBase {

    @Before
    public void clearGraph(){
        cleanUpGraph();
    }
    @Test
    public void tags_MetaTagsUpdatedForExistingEntity() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("tags_MetaTagsUpdatedForExistingEntity", mike_admin);
        assertNotNull(su);
        FortressInputBean fib = new FortressInputBean("ABC", true);

        Fortress fortress = fortressService.registerFortress(su.getCompany(),fib );
        assertNotNull(fortress);

        TagInputBean firstTag = new TagInputBean("firstTag", "demo");

        EntityInputBean entityBean = new EntityInputBean(fortress.getName(), "mtest", "aTest", new DateTime(), "abc");
        entityBean.addTag(firstTag);
        entityBean.setArchiveTags(false);
        ContentInputBean contentBean = new ContentInputBean();
        contentBean.setEvent("Test");
        Map<String, Object> jsonMap = Helper.getRandomMap();
        contentBean.setWhat(jsonMap);
        entityBean.setContent(contentBean);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityBean);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());

        Assert.assertEquals(1, trackService.getLogCount(su.getCompany(), entity.getMetaKey()));
        Assert.assertEquals(1, entityTagService.getEntityTags(su.getCompany(), entity).size());

        // Scenario 1: We send in the header with no content
        entityBean.setContent(null);
        mediationFacade.trackEntity(su.getCompany(), entityBean);
        Assert.assertEquals(1, trackService.getLogCount(su.getCompany(), entity.getMetaKey()));
        Assert.assertEquals(1, entityTagService.getEntityTags(su.getCompany(), entity).size());

        // Scenario 2: We have an existing entity with content logged - it has one existing tag
        //           we now have a second tag added but no content.
        TagInputBean secondTag = new TagInputBean("secondTag", "demo");
        entityBean.addTag(secondTag);
        mediationFacade.trackEntity(su.getCompany(), entityBean);

        Assert.assertEquals(1, trackService.getLogCount(su.getCompany(), entity.getMetaKey()));
        Assert.assertEquals(2, entityTagService.getEntityTags(su.getCompany(), entity).size());

        EntityLog lastLog = logService.getLastLog(entity);
        assertNotNull ( lastLog);
        Map<String,Object> values = mediationFacade.getLogContent(entity, lastLog.getId());
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

        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());

        EntityTagInputBean auditTag = new EntityTagInputBean(resultBean.getEntityBean().getMetaKey(), null, "!!!");
        try {
            entityTagService.processTag(entity, auditTag);
            fail("Null argument exception should have been thrown");
        } catch (IllegalArgumentException ie) {
            // This should have happened
        }
        // First entityTag created
        auditTag = new EntityTagInputBean(entity.getMetaKey(), flopTag.getName(), "ABC");

        entityTagService.processTag(entity, auditTag);

        Boolean tagRlxExists = entityTagService.relationshipExists(entity, flopTag.getName(), "ABC");
        assertTrue("Tag not found " + flopTag.getName(), tagRlxExists);

        entityTagService.processTag(entity, auditTag);
        // Behaviour - Can't add the same tagValue twice for the same combo
        tagRlxExists = entityTagService.relationshipExists(entity, flopTag.getName(), "ABC");
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
        EntityInputBean entityBean = new EntityInputBean(fortress.getName(), "anyone", "aTest", fCreated, "abc");
        TrackResultBean resultBean = mediationFacade.trackEntity(fortress, entityBean);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());

        assertEquals(fCreated.getMillis(), entity.getFortressDateCreated().getMillis());

        EntityTagInputBean tagA = new EntityTagInputBean(entity.getMetaKey(), flopTag.getName(), "ABC");
        entityTagService.processTag(entity, tagA);

        Boolean tagRlxExists = entityTagService.relationshipExists(entity, flopTag.getName(), "ABC");
        assertTrue("Tag not found " + flopTag.getName(), tagRlxExists);

        Collection<EntityTag> tags = entityTagService.getEntityTags(su.getCompany(), entity);
        for (EntityTag tag : tags) {
            assertEquals("Date did not correspond to the Fortress created date", entity.getFortressDateCreated().getMillis(), Long.parseLong(tag.getProperties().get("since").toString()));
        }

        // Creating some content and adding a new Tag to the entity
        DateTime fUpdated = new DateTime().minus(10000);
        ContentInputBean contentInputBean = new ContentInputBean("harry", fUpdated, Helper.getRandomMap());
        entityBean.addTag( new TagInputBean("Tag2", "second"));
        entityBean.setArchiveTags(false);// We don't have a reference to the original tag in the Input
        // as we assigned it in a secondary step, so will accumulate tags and stop them being archived
        entityBean.setContent(contentInputBean);
        mediationFacade.trackEntity(fortress, entityBean);
        entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        assertEquals(fCreated, entity.getFortressDateCreated());
        assertEquals(fUpdated.getMillis(), entity.getFortressDateUpdated().longValue());
        tags = entityTagService.getEntityTags(su.getCompany(), entity);
        assertEquals(2, tags.size());
        for (EntityTag tag : tags) {
            if ( tag.getTag().getName().equalsIgnoreCase(flopTag.getName()))
                assertEquals("Date did not correspond to the Fortress created date", entity.getFortressDateCreated().getMillis(), Long.parseLong(tag.getProperties().get("since").toString()));
            else
                assertEquals("Date did not correspond to the Fortress updated date", entity.getFortressDateUpdated().longValue(), Long.parseLong(tag.getProperties().get("since").toString()));
        }

    }


    @Test
    public void renameRelationship() throws Exception {

        SystemUser su= registerSystemUser("renameRelationship", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        entityInput.addTag(new TagInputBean("TagA", "AAAA"));
        entityInput.addTag(new TagInputBean("TagB", "BBBB"));
        entityInput.addTag(new TagInputBean("TagC", "CCCC"));
        entityInput.addTag(new TagInputBean("TagD", "DDDD"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(entity);

        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());
        assertFalse(entityTagService.relationshipExists(entity, "TagC", "!!Twee!!"));//
        // Remove a single tag
        for (EntityTag value : tagSet) {
            if (value.getTag().getName().equals("TagC"))
                entityTagService.changeType(entity, value, "!!Twee!!");
        }

        assertTrue(entityTagService.relationshipExists(entity, "TagC", "!!Twee!!"));
    }

    @Test
    public void createAndDeleteEntityTags() throws Exception {

        SystemUser su = registerSystemUser("createAndDeleteEntityTags", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        entityInput.addTag(new TagInputBean("TagA", "AAAA"));
        entityInput.addTag(new TagInputBean("TagB", "BBBB"));
        entityInput.addTag(new TagInputBean("TagC", "CCCC"));
        entityInput.addTag(new TagInputBean("TagD", "DDDD"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(entity);

        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());
        // Remove a single tag
        for (EntityTag value : tagSet) {
            if (value.getTag().getName().equals("TagB"))
                entityTagService.deleteEntityTags(entity, value);
        }
        tagSet = entityTagService.findEntityTags(entity);
        assertNotNull(tagSet);
        assertEquals(3, tagSet.size());
        // Ensure that the deleted tag is not in the results
        for (EntityTag entityTag : tagSet) {
            assertFalse(entityTag.getTag().getName().equals("TagB"));
        }
    }

    @Test
    public void nullTagValueCRUD() throws Exception {
        SystemUser su= registerSystemUser("nullTagValueCRUD", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // In this scenario, the Tag name is the key if the value is null
        entityInput.addTag(new TagInputBean("TagA", null));
        entityInput.addTag(new TagInputBean("TagB", null));
        entityInput.addTag(new TagInputBean("TagC", null));
        entityInput.addTag(new TagInputBean("TagD", "DDDD"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(entity);
        assertNotNull(tagSet);
        assertEquals(4, tagSet.size());

        trackService.updateEntity(entity);
        entity = trackService.getEntity(su.getCompany(), entity.getMetaKey());
        EntitySummaryBean summaryBean = trackService.getEntitySummary(null, entity.getMetaKey());
        tagSet = summaryBean.getTags();
        assertNotNull(tagSet);
        Set<Entity> entities = entityTagService.findEntityTags(su.getCompany(), "TagA");
        assertNotNull(entities);
        assertNotSame(entities.size() + " Entities returned!", 0, entities.size());

        assertEquals(entity.getMetaKey(), entities.iterator().next().getMetaKey());
        entities = entityTagService.findEntityTags(su.getCompany(), "TagC");
        assertNotNull(entities);
        assertEquals(entity.getMetaKey(), entities.iterator().next().getMetaKey());
        entities = entityTagService.findEntityTags(su.getCompany(), "TagD");
        assertNotNull(entities);
        assertEquals(entity.getMetaKey(), entities.iterator().next().getMetaKey());
    }

    @Test
    public void nullCodeValue() throws Exception {
        SystemUser su= registerSystemUser("nullCodeValue", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // In this scenario, the Tag name is the key if the value is null
        TagInputBean tag = new TagInputBean("TagD", "DDDD");
        tag.setCode(null ); // This gets set to null if not supplied over an endpoint
        entityInput.addTag(tag);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(entity);
        assertNotNull(tagSet);
        assertEquals(1, tagSet.size());

    }
    @Test
    public void duplicateTagNotCreated() throws Exception {
        SystemUser su= registerSystemUser("duplicateTagNotCreated", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        //assertNotNull(result);
        EntityInputBean entityInput = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        entityInput.addTag(new TagInputBean("TagA", "camel"));
        entityInput.addTag(new TagInputBean("taga", "lower"));
        entityInput.addTag(new TagInputBean("tAgA", "mixed"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        Tag tag = tagService.findTag(su.getCompany(), "Taga");
        assertNotNull(tag);
        Collection<EntityTag> entityTags = entityTagService.findEntityTags(entity);
        for (EntityTag entityTag : entityTags) {
            Assert.assertEquals("Expected same tag for each relationship", tag.getId(), entityTag.getTag().getId());
        }
        assertEquals("Expected 3 relationships for the same tag", 3, entityTags.size());

    }

    @Test
    public void noEntityTagsAreReturned() throws Exception {
        SystemUser su= registerSystemUser("noEntityTagsAreReturned", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC"));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);
        EntityInputBean entity = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        entity.setTrackSuppressed(true);
        // This should create the same Tag object, but return one row for each relationships
        entity.addTag(new TagInputBean("TagA", "camel"));
        entity.addTag(new TagInputBean("taga", "lower"));
        entity.addTag(new TagInputBean("tAgA", "mixed"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entity);

        assertEquals(3, resultBean.getTags().size());
        Long id = null;
        for (EntityTag entityTag : resultBean.getTags()) {
            if ( id == null )
                id = entityTag.getTag().getId();
            Assert.assertEquals(id, entityTag.getTag().getId());
        }
        assertNull(resultBean.getEntityBean().getMetaKey());

    }

    @Test
    public void createLogForInvalidEntity() throws Exception{
        SystemUser su = registerSystemUser("createLogForInvalidEntity", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC"));

        EntityInputBean entity = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        mediationFacade.trackEntity(su.getCompany(), entity);
        ContentInputBean contentInputBean = new ContentInputBean("Harry", "InvalidKey", new DateTime(), Helper.getRandomMap());
        try {
            mediationFacade.trackLog(su.getCompany(), contentInputBean);
            fail("Invalid entity. This should not have worked");
        } catch (FlockException e ){
            // Good stuff
        }

    }
    @Test
    public void createLogForValidEntityWithNoContent() throws Exception{
        SystemUser su = registerSystemUser("createLogForValidEntityWithNoContent", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        EntityInputBean entity = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TrackResultBean rb = mediationFacade.trackEntity(su.getCompany(), entity);
        ContentInputBean contentInput = new ContentInputBean("Harry", rb.getEntityBean().getMetaKey(), new DateTime(),null);
        assertNotNull(mediationFacade.trackLog(su.getCompany(), contentInput));
    }

    @Test
    public void differentTagTypeSameTagName() throws Exception {
        SystemUser su = registerSystemUser("differentTagTypeSameTagName", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        TagInputBean tagInput = new TagInputBean("FLOP");

        tagService.createTag(su.getCompany(), tagInput);

        EntityInputBean entityInput = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        // This should create the same Tag object
        TagInputBean tag = new TagInputBean("TagA");
        tag.addEntityLink("Type1");
        tag.addEntityLink("Type2");
        tag.addEntityLink("Type3");
        entityInput.addTag(tag);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        Collection<EntityTag> tagSet = entityTagService.findEntityTags(entity);
        assertNotNull(tagSet);
        assertEquals(3, tagSet.size());

        EntitySummaryBean summaryBean = trackService.getEntitySummary(su.getCompany(), entity.getMetaKey());
        assertNotNull(summaryBean);
        assertEquals(3, summaryBean.getTags().size());

    }

    @Test
    public void tagListAndSingular() throws Exception {
        SystemUser su = registerSystemUser("tagListAndSingular", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addEntityLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");
        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey(),true);
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(entity);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        EntitySummaryBean summaryBean = trackService.getEntitySummary(null, entity.getMetaKey());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithNullProperties() throws Exception {
        SystemUser su = registerSystemUser("mapRelationshipsWithNullProperties", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addEntityLink("email-cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");
        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey(),true);
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(entity);
        EntitySummaryBean summaryBean = trackService.getEntitySummary(null, entity.getMetaKey());
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void mapRelationshipsWithProperties() throws Exception {
        SystemUser su = registerSystemUser("mapRelationshipsWithProperties", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        Map<String, Object> propA = new HashMap<>();
        Map<String, Object> propB = new HashMap<>();
        propA.put("myValue", 10);
        propB.put("myValue", 20);

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to", propA);
        tagA.addEntityLink("email-cc", propB);
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");

        inputBean.addTag(tagA);
        inputBean.addTag(tagB);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey(), true);
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(entity);
        EntitySummaryBean summaryBean = trackService.getEntitySummary(null, entity.getMetaKey());
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void duplicateRLXTypesNotStored() throws Exception {
        SystemUser su = registerSystemUser("duplicateRLXTypesNotStored", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagInputBean = new TagInputBean("mike@auditbucket.com", "email-to");
        tagInputBean.addEntityLink("email-to");
        tagInputBean.addEntityLink("email-to");

        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey(), true);
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(entity);
        assertEquals("One for the Generic tag and one for exploration", 1, tagResults.size());
    }

    @Test
    public void directedEntityTagsWork()throws Exception{
        SystemUser su = registerSystemUser("directedEntityTagsWork", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagInputBean = new TagInputBean("mike@auditbucket.com", "email-to");
        tagInputBean.setReverse(true); // relationships will be reversed
        tagInputBean.addEntityLink("email-to");

        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        // By default, tags are inbound to the Entity. This asserts the reverse also works
        Collection<EntityTag> tagResults = entityTagService.findOutboundTags(entity);
        assertEquals("No tag heading out from the Entity could be found", 1, tagResults.size());

    }

    @Test
    public void tagsAndValuesWithSpaces() throws Exception {

        SystemUser su = registerSystemUser("tagsAndValuesWithSpaces", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");

        TagInputBean tagA = new TagInputBean("mike@auditbucket.com", "email-to");
        tagA.addEntityLink("email cc");
        TagInputBean tagB = new TagInputBean("np@auditbucket.com", "email-cc");

        inputBean.addTag(tagA);
        inputBean.addTag(tagB);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        Collection<EntityTag> tagResults = entityTagService.findEntityTags(entity);
        assertEquals("Union of type and tag does not total", 3, tagResults.size());
        EntitySummaryBean summaryBean = trackService.getEntitySummary(null, entity.getMetaKey());
        assertEquals(3, summaryBean.getTags().size());
    }

    @Test
    public void entity_nestedTagStructure() throws Exception {

        SecurityContextHolder.getContext().setAuthentication(authDefault);
        SystemUser su = registerSystemUser("entity_nestedTagStructure", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));
        assertNotNull(fortress);

        EntityInputBean entityInput = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
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
        Tag countryTag = tagService.findTag(su.getCompany(), "New Zealand");
        Tag cityTag = tagService.findTag(su.getCompany(), "Wellington");
        Tag sectionTag = tagService.findTag(su.getCompany(), "Thorndon");
        Tag houseTag = tagService.findTag(su.getCompany(), "ABC House");

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

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));
        assertNotNull(fortress);

        EntityInputBean entityInput = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country);
        TagInputBean cityInputTag = new TagInputBean(city);
        TagInputBean stateInputTag = new TagInputBean("CA");

        TagInputBean institutionTag = new TagInputBean("mikecorp", "owns");
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
            Assert.assertEquals("mikecorp", tag.getTag().getName());
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

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country);
        TagInputBean cityInputTag = new TagInputBean(city);
        TagInputBean stateInputTag = new TagInputBean("CA");

        TagInputBean institutionTag = new TagInputBean("mikecorp", "works");
        // Institution is in a city
        institutionTag.setTargets("located", cityInputTag);
        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);
        inputBean.addTag(institutionTag);

        // Institution<-city<-state<-country

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(resultBean);
        Collection<EntityTag> tags = entityTagService.findEntityTags(resultBean.getEntity());
        assertFalse(tags.isEmpty());

        SearchChange searchChange = searchService.getSearchChange(fortress.getCompany(), resultBean, "Blah", new Date());
        assertNotNull(searchChange);
        assertNotNull(searchChange.getTagValues());
    }

    @Test
    public void targetTagWithAuditRelationship() throws Exception {
        String name = "Doctor John";
        TagInputBean authorTag = new TagInputBean(name + ":person");
        authorTag.addEntityLink("writer");
        authorTag.addEntityLink("lead");

        SystemUser su = registerSystemUser("targetTagWithAuditRelationship", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));
        assertNotNull(fortress);

        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc");
        TagInputBean countryTag = new TagInputBean("New Zealand");
        TagInputBean cityTag = new TagInputBean("Auckland");
        TagInputBean institution = new TagInputBean("Auckland University:Institution");
        cityTag.setTargets("jurisdiction", countryTag); // Auckland located in NZ

        institution.addEntityLink("located");
        cityTag.addEntityLink("city");

        inputBean.addTag(cityTag); // Not attached to track
        inputBean.addTag(countryTag);
        inputBean.addTag(institution);

        TrackResultBean result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertNotNull(result);
        Collection<EntityTag> tags = entityTagService.findEntityTags(result.getEntity());
        assertEquals(2, tags.size());
        for (EntityTag tag : tags) {
            assertTrue(tag.getTag().getName().equals(institution.getName()) || tag.getTag().getName().equals(cityTag.getName()));
        }

    }

    @Test
    public void geoTag() throws Exception {
        SystemUser su = registerSystemUser( "geoTag", mike_admin);
        assertNotNull(su);

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));
        assertNotNull(fortress);

        EntityInputBean entityInput = new EntityInputBean("ABC", "geoTest", "geoTest", new DateTime(), "abc");
        String country = "USA";
        String city = "Los Angeles";

        TagInputBean countryInputTag = new TagInputBean(country, "Country", "");
        TagInputBean cityInputTag = new TagInputBean(city, ":City", "");
        TagInputBean stateInputTag = new TagInputBean("CA", "State", "");

        TagInputBean institutionTag = new TagInputBean("mikecorp", "owns");
        // Institution is in a city
        institutionTag.setTargets("located", cityInputTag);
        cityInputTag.setTargets("state", stateInputTag);
        stateInputTag.setTargets("country", countryInputTag);
        entityInput.addTag(institutionTag);

        // Institution<-city<-state<-country

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityInput);
        assertNotNull(resultBean);
        assertNotNull(tagService.findTag(fortress.getCompany(), "Country", "USA"));

        Collection<EntityTag> tags = entityTagService.findEntityTags(resultBean.getEntity());
        assertFalse(tags.isEmpty());

        for (EntityTag tag : tags) {
            assertEquals("mikecorp", tag.getTag().getName());
            assertNotNull(tag.getGeoData());
            assertEquals("CA", tag.getGeoData().getState());
            assertEquals("Los Angeles", tag.getGeoData().getCity());
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
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));

        TagInputBean tagInput = new TagInputBean("TEST-CREATE", "rlx-test");

        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(), Helper.getRandomMap());
        inputBean.setContent(logBean);

        inputBean.addTag(tagInput);
        TrackResultBean entityResult = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity created = trackService.getEntity(su.getCompany(), entityResult.getEntityBean().getMetaKey());
        Log firstLog = trackService.getLastEntityLog(su.getCompany(), created.getMetaKey()).getLog();
        assertNotNull(created);

        // Test that a tag is removed
        EntityInputBean updatedEntity = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        // Force a change to be detected
        ContentInputBean alb = new ContentInputBean("mike", new DateTime(),  Helper.getRandomMap());
        updatedEntity.setContent(alb);
        // Updating an existing ENtity but the tagCollection is minus TEST-CREATE tag
        // The create call should create a new Tag - TEST-UPDATE - and then remove the TEST-CREATE
        updatedEntity.addTag(new TagInputBean("TEST-UPDATE", "camel"));
        entityResult = mediationFacade.trackEntity(su.getCompany(), updatedEntity);
        Entity entity = trackService.getEntity(su.getCompany(), entityResult.getEntityBean().getMetaKey());
        assertNotNull(entity);

        // Should only be one tag
        validateTag(entity, null, 1);
        // It should be the update tag
        validateTag(entity, "TEST-UPDATE", 1);
        // The create tag should not be against the entity but against the log
        validateTag(entity, "TEST-CREATE", 0);

        Collection<EntityTag> results = trackService.getLastLogTags(su.getCompany(), entity.getMetaKey());
        assertEquals(0, results.size()); // No tags against the last change log - tags are against the entity

        results = trackService.getLogTags(su.getCompany(), firstLog.getEntityLog());
        assertEquals(1, results.size());
        Assert.assertEquals("TEST-CREATE", results.iterator().next().getTag().getName());

        // Make sure when we pass NO tags, i.e. just running an update, we don't change ANY tags

        alb = new ContentInputBean("mike", new DateTime(), Helper.getRandomMap());
        updatedEntity.setContent(alb);
        updatedEntity.getTags().clear();
        entityResult = mediationFacade.trackEntity(su.getCompany(), updatedEntity);
        entity = trackService.getEntity(su.getCompany(), entityResult.getEntityBean().getMetaKey());

        // 3 logs
        Assert.assertEquals(3, trackService.getLogCount(su.getCompany(), entity.getMetaKey()));
        // Update tag should still be against the entity
        validateTag(entity, "TEST-UPDATE", 1);

        // Here we will cancel the last two logs getting us back to the initial state
        // should be one tag of TEST-CREATE logged

        trackService.cancelLastLog(su.getCompany(), entity);
        trackService.cancelLastLog(su.getCompany(), entity);
        //ToDo: We are only adding back tags that were removed If tag as added by the cancelled log then it should
        // also be removed The answer here should be 1
        validateTag(entity, null, 1);
        validateTag(entity, "TEST-CREATE", 1);
    }

    @Test
    public void oneTagRemovedFromASetOfTwo() throws Exception {
        SystemUser su = registerSystemUser("oneTagRemovedFromASetOfTwo", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));

        TagInputBean tagInput = new TagInputBean("TAG-FIRST", "rlx-test");

        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(),  Helper.getRandomMap());
        inputBean.setContent(logBean);

        inputBean.addTag(tagInput);
        tagInput = new TagInputBean(("TAG-SECOND"), "rlxb-test");
        inputBean.addTag(tagInput);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity created = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        trackService.getLastEntityLog(su.getCompany(), created.getMetaKey()).getLog();
        assertNotNull(created);
        validateTag(created, null, 2);

        // Test that a tag is removed
        EntityInputBean updatedEntity = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        // Force a change to be detected
        ContentInputBean alb = new ContentInputBean("mike", new DateTime(),  Helper.getRandomMap());
        updatedEntity.setContent(alb);
        // we are updating an existing entity with two tags and telling it that only one is now valid
        updatedEntity.addTag(new TagInputBean("TAG-FIRST", "rlx-test"));
        resultBean = mediationFacade.trackEntity(su.getCompany(), updatedEntity);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        assertNotNull(entity);

        // Should be one tag
        validateTag(entity, null, 1);
        // It should be the update tag
        validateTag(entity, "TAG-FIRST", 1);
        // The create tag should not be against the entity but against the log
        validateTag(entity, "TEST-SECOND", 0);

        Collection<EntityTag> results = trackService.getLastLogTags(su.getCompany(), entity.getMetaKey());
        // No tags removed for the last tag
        assertEquals(0, results.size()); // No tags against the logs
        Assert.assertEquals(1, entityTagService.getEntityTags(su.getCompany(), entity).size());

    }

    @Test
    public void addNewTagToExistingEntity() throws Exception {
        SystemUser su = registerSystemUser("addNewTagToExistingEntity", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));

        //assertNotNull(result);
        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(),  Helper.getRandomMap());
        inputBean.setContent(logBean);
        // This should create the same Tag object
        inputBean.addTag(new TagInputBean("TagA", "camel"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);

        assertNotNull(resultBean);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        assertNotNull(entity);

        validateTag(entity, "TagA", 1);

        //Adding a second tag (the first is already in the entity
        inputBean.addTag(new TagInputBean("TagB", "horse"));
        mediationFacade.trackEntity(su.getCompany(), inputBean);
        validateTag(entity, "TagB", 2);

    }

    @Test
    public void directionalTagsAndRelationshipPropertiesPreserved() throws Exception {
        SystemUser su = registerSystemUser("directionalTagsAndRelationshipPropertiesPreserved", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC",true));

        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        ContentInputBean logBean = new ContentInputBean("mike", new DateTime(),  Helper.getRandomMap());
        inputBean.setContent(logBean);

        Map<String,Object>rlxProperties = new HashMap<>();
        rlxProperties.put("stringTest", "blah");
        rlxProperties.put("doubleTest", 100d);
        rlxProperties.put("weight", 99);
        rlxProperties.put("abAdded", "z");

        TagInputBean inBound = new TagInputBean("TAG-IN", "rlx-test", rlxProperties);
        inputBean.addTag(inBound);

        TagInputBean outBound = new TagInputBean(("TAG-OUT"), "rlxb-test").setReverse(true);

        inputBean.addTag(outBound);
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        Entity created = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        trackService.getLastEntityLog(su.getCompany(), created.getMetaKey()).getLog();

        // Total of two tags
        validateTag(created, null, 2);

        Collection<EntityTag> outboundTags = entityTagService.findInboundTags(su.getCompany(), created);
        assertEquals("One tag should be reversed", 1, outboundTags.size());
        EntityTag trackOut = outboundTags.iterator().next();
        Assert.assertEquals("TAG-IN", trackOut.getTag().getName());
        assertEquals("blah", trackOut.getProperties().get("stringTest"));
        assertEquals(100d, trackOut.getProperties().get("doubleTest"));
        assertEquals((Integer)99, trackOut.getWeight());
        Long currentWhen = (Long)trackOut.getProperties().get(EntityTagDao.FD_WHEN);
        assertTrue(currentWhen>0);

        logBean = new ContentInputBean("mike", new DateTime(),  Helper.getRandomMap());
        inputBean.getTags().clear();
        inputBean.addTag(outBound);
        inputBean.setContent(logBean);

        // Removing the inbound tag
        mediationFacade.trackEntity(su.getCompany(), inputBean);
        validateTag(created, null, 1);
        outboundTags = entityTagService.findOutboundTags(su.getCompany(), created);

        // One is reversed
        assertEquals(1, outboundTags.size());

        // Cancelling last change should restore the inbound tag
        mediationFacade.cancelLastLog(su.getCompany(), created);
        // Total of two tags
        validateTag(created, null, 1);
        // One of which is outbound and the other inbound
        assertEquals(1, outboundTags.size());

        // Check that we still have our custom properties
        outboundTags = entityTagService.getEntityTags(su.getCompany(), created);
        trackOut = outboundTags.iterator().next();
        Assert.assertEquals("TAG-IN", trackOut.getTag().getName());
        assertEquals("blah", trackOut.getProperties().get("stringTest"));
        assertEquals(100d, trackOut.getProperties().get("doubleTest"));
        assertEquals((Integer)99, trackOut.getWeight());
        assertEquals(currentWhen, trackOut.getProperties().get(EntityTagDao.FD_WHEN));
    }

    @Test
    public void addNewTagToExistingEntityWithNoLog() throws Exception {
        SystemUser su = registerSystemUser("addNewTagToExistingEntityWithNoLog", mike_admin);
        fortressService.registerFortress(su.getCompany(), new FortressInputBean("ABC", true));

        //assertNotNull(result);
        EntityInputBean inputBean = new EntityInputBean("ABC", "auditTest", "aTest", new DateTime(), "abc1");
        // This should create the same Tag object
        inputBean.addTag(new TagInputBean("TagA", "camel"));
        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);

        assertNotNull(resultBean);
        Entity entity = trackService.getEntity(su.getCompany(), resultBean.getEntityBean().getMetaKey());
        assertNotNull(entity);

        validateTag(entity, "TagA", 1);

        //Adding a second tag (the first is already in the entity
        inputBean.addTag(new TagInputBean("TagB", "horse"));
        mediationFacade.trackEntity(su.getCompany(), inputBean);
        validateTag(entity, "TagB", 2);

    }
    @Test
    public void search() throws Exception{
        SystemUser su = registerSystemUser("search", mike_admin);
        Fortress fo = fortressService.registerFortress(su.getCompany(), new FortressInputBean("cancelLogTag", true));
        EntityInputBean inputBean = new EntityInputBean(fo.getName(), "wally", "CancelDoc", new DateTime(), "ABC123");
        ContentInputBean log = new ContentInputBean("wally", new DateTime(),  Helper.getRandomMap());
        inputBean.addTag(new TagInputBean("Happy").addEntityLink("testinga"));
        inputBean.addTag(new TagInputBean("Happy Days").addEntityLink("testingb"));
        inputBean.setContent(log);
        TrackResultBean result;
        mediationFacade.trackEntity(su.getCompany(), inputBean);

        // We now have 1 log with tags validated in ES

        // Add another Log - replacing the two existing Tags with two new ones
        log = new ContentInputBean("wally", new DateTime(),  Helper.getRandomMap());
        inputBean.getTags().clear();
        inputBean.addTag(new TagInputBean("Sad Days").addEntityLink("testingb"));
        inputBean.addTag(new TagInputBean("Days Bay").addEntityLink("testingc"));
        inputBean.setContent(log);
        result = mediationFacade.trackEntity(su.getCompany(), inputBean);
        // We now have 2 logs, sad tags, no happy tags

        // Cancel Log - this will remove the sad tags and leave us with happy tags
        mediationFacade.cancelLastLog(su.getCompany(), result.getEntity());
        Collection<EntityTag>tags = entityTagService.findEntityTags(result.getEntity());
        assertEquals(2, tags.size());

    }

    private void validateTag(Entity entity,  String tagName, int totalExpected) {
        Collection<EntityTag> tags;
        tags = entityTagService.findEntityTags(entity);
        if ( tagName == null ) {
            assertEquals("Total Expected Tags is incorrect", totalExpected, tags.size() );
            return;
        }

        boolean found = false;
        for (EntityTag tag : tags){
            if (tag.getTag().getName().equals(tagName)){
                found = true;
                break;
            }
        }
        if ( totalExpected == 0 && !found)
            return ;
        if ( totalExpected == 0 ){
            fail("The expected tag [" + tagName + "] was found when it was not expected to exist");
            return;
        }
        assertTrue("The expected tag ["+ tagName +"] was not found", found);
    }

}
