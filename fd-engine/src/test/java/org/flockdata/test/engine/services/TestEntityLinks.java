/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

import junit.framework.TestCase;
import org.flockdata.model.*;
import org.flockdata.profile.ContentModelDeserializer;
import org.flockdata.profile.ContentModelResult;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.service.ContentModelService;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.SearchTag;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.service.BatchService;
import org.flockdata.transform.ColumnDefinition;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Entities can be connected to other entities. This checks some of the behaviour
 *
 * Created by mike on 19/12/15.
 */
public class TestEntityLinks extends EngineBase {

    @Autowired
    ContentModelService contentModelService;

    @Autowired
    BatchService batchService;

    @Override
    @Before
    public void cleanUpGraph() {
        // DAT-348
        logger.debug("Cleaning Graph DB");
        super.cleanUpGraph();
    }

    @Test
    public void doNothing(){
        // Commenting out while Bamboo is failing to create artifacts  despite it being quarantineed
    }

    @Test
    public void testLinkedToSearch () throws Exception{
        // Initial setup
        cleanUpGraph();
        SystemUser su = registerSystemUser("testLinkedToSearch", mike_admin);
        Fortress timesheetFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("timesheet", true));

        EntityInputBean staff = new EntityInputBean(timesheetFortress, "wally", "Staff", new DateTime(), "ABC123");
        staff.addTag( new TagInputBean("Cleaner", "Position", new EntityTagRelationshipInput("role")));
        mediationFacade.trackEntity(su.getCompany(), staff);

        DocumentType docTypeWork = new DocumentType(timesheetFortress, "Work");
        docTypeWork = conceptService.findOrCreate(timesheetFortress, docTypeWork);

        EntityInputBean workRecord = new EntityInputBean(timesheetFortress, "wally", docTypeWork.getName(), new DateTime(), "ABC321");

        TrackResultBean workResult = mediationFacade.trackEntity(su.getCompany(), workRecord);

        EntityKeyBean staffKey = new EntityKeyBean(staff.getDocumentType().getName(), staff.getFortress().getName(), staff.getCode());
        EntityKeyBean workKey = new EntityKeyBean(workRecord.getDocumentType().getName(), workRecord.getFortress().getName(), workRecord.getCode());

        // Validating the search doc after linking of previously created entities is not currently supported.

//        Collection<EntityKeyBean> parents = new ArrayList<>();
//        parents.add(staffKey.setParent(true));
//        entityService.linkEntities(su.getCompany(), workKey, parents, "worked");
//
//        Collection<EntityKeyBean> entities = entityService.getInboundEntities(workResult.getEntity(), true);
//        assertTrue ( entities!=null);
//        assertFalse(entities.isEmpty());
//
//        // *** End initial setup.
//
//        // We now have two linked entities. Check that they are set in to the SearchDocument
//        EntitySearchChange searchDocument = searchService.getEntityChange(workResult);
//        validateSearchStaff( searchDocument);
//        String json = JsonUtils.toJson(searchDocument);
//        assertNotNull(json);
//        EntitySearchChange deserialized = JsonUtils.toObject(json.getBytes(), EntitySearchChange.class);
//        assertNotNull("Issue with JSON serialization", deserialized);
//        // Results should be exactly the same
//        validateSearchStaff( deserialized);

    }

    private void validateSearchStaff(EntitySearchChange searchDocument){
        assertNotNull(searchDocument.getEntityLinks());
        assertEquals(0, searchDocument.getEntityLinks().size());
        assertNotNull ( searchDocument.getParent());
        EntityKeyBean staffDetails = searchDocument.getParent();
        assertNotNull(staffDetails);
        assertNotNull(staffDetails.getSearchTags());
        assertFalse(staffDetails.getSearchTags().isEmpty());
        assertNotNull("The Position label name should have been converted to lowercase", staffDetails.getSearchTags().get("role").get("position"));
        SearchTag position = staffDetails.getSearchTags().get("role").get("position").iterator().next();
        assertTrue(position.getCode().equals("Cleaner"));

    }

    @Test
    public void testEntityLinks () throws Exception {
        // Initial setup
        cleanUpGraph();
        SystemUser su = registerSystemUser("testEntityLinks", mike_admin);
        Fortress timesheetFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("timesheet", true));

        EntityInputBean staff = new EntityInputBean(timesheetFortress, "wally", "Staff", new DateTime(), "ABC123");
        staff.addTag(new TagInputBean("Cleaner", "Position", new EntityTagRelationshipInput("role")));

        mediationFacade.trackEntity(su.getCompany(), staff);

        DocumentType docTypeWork = new DocumentType(timesheetFortress, "Work");
        docTypeWork = conceptService.findOrCreate(timesheetFortress, docTypeWork);

        EntityInputBean workRecord = new EntityInputBean(timesheetFortress, "wally", docTypeWork.getName(), new DateTime(), "ABC321");
        // Checking that the entity is linked when part of the track request
        workRecord.addEntityLink("worked", new EntityKeyBean("Staff", "timesheet", "ABC123").setParent(true));
        TrackResultBean workResult = mediationFacade.trackEntity(su.getCompany(), workRecord);
        EntitySearchChange searchDocument = searchService.getEntityChange(workResult);
        validateSearchStaff( searchDocument);
    }

    @Test
    public void error_whenEntityDoesNotExist() throws Exception {
        cleanUpGraph();

        // Track works where there is
        //  an existing entity to link to
        //  a non-existing entity link requested
        SystemUser su = registerSystemUser("linkedBehaviour", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("Staff", true));

        // One timesheet entry will be assigned to this staff member
        EntityInputBean eStaff = new EntityInputBean(fortress, "wally", "Staff", new DateTime(), "30250");
        eStaff.addTag( new TagInputBean("Cleaner", "Position", new EntityTagRelationshipInput("role")));
        mediationFacade.trackEntity(su.getCompany(), eStaff);

        DocumentType timesheet = conceptService.findDocumentType(fortress, "timesheet", true);

        ContentModel params = ContentModelDeserializer.getContentModel("/models/test-entitylinks.json");
        contentModelService.saveEntityModel(su.getCompany(), fortress, timesheet, params );
        batchService.process(su.getCompany(), fortress, timesheet, "/data/test-entitylinks.csv", false);

        assertNotNull( "couldn't find the entity we created", entityService.findByCode(su.getCompany(), fortress.getName(), "Timesheet", "1" ));
        // recorded is the relationship type in the content profile definition
        String rlxName = "recorded";
        Map<String, Collection<Entity>> linkedEntities =  getLinkedEntities(su.getCompany(), fortress.getName(), "timesheet", "1", rlxName);
        assertEquals("This timesheet should have a reference to an existing staff", 1, linkedEntities.get(rlxName).size());
        linkedEntities =  getLinkedEntities(su.getCompany(), fortress.getName(), "timesheet", "2", rlxName);
        // Default behaviour is to ignore
        TestCase.assertEquals("This timesheet should not have an associated staff member as it did not exist", 0, linkedEntities.get(rlxName).size()); ;

    }

    @Test
    public void work_whenEntityDoesNotExist() throws Exception {
        cleanUpGraph();

        // Track works where there is
        //  an existing entity to link to
        //  a non-existing entity link requested
        SystemUser su = registerSystemUser("work_whenEntityDoesNotExist", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("Staff", true));

        // One timesheet entry will be assigned to this staff member
        EntityInputBean eStaff = new EntityInputBean(fortress, "wally", "Staff", new DateTime(), "30250");
        eStaff.addTag( new TagInputBean("Cleaner", "Position", new EntityTagRelationshipInput("role")));
        mediationFacade.trackEntity(su.getCompany(), eStaff);

        DocumentType timesheet = conceptService.findDocumentType(fortress, "timesheet", true);
        String rlxName = "recorded";

        ContentModel params = ContentModelDeserializer.getContentModel("/models/test-entitylinks.json");
        ColumnDefinition colDef = params.getColumnDef("EmployeeNumber");
        colDef.getEntityLinks().iterator().next().get(rlxName);
        contentModelService.saveEntityModel(su.getCompany(), fortress, timesheet, params );
        batchService.process(su.getCompany(), fortress, timesheet, "/data/test-entitylinks.csv", false);
        // recorded is the relationship type in the content profile definition
        Map<String, Collection<Entity>> linkedEntities =  getLinkedEntities(su.getCompany(), fortress.getName(), "timesheet", "1", rlxName);
        assertEquals("This timesheet should have a reference to an existing staff", 1, linkedEntities.get(rlxName).size());
        linkedEntities =  getLinkedEntities(su.getCompany(), fortress.getName(), "timesheet", "2", rlxName);
        // Default behaviour is to ignore
        TestCase.assertEquals("This timesheet should not have an associated staff member as it did not exist", 0, linkedEntities.get(rlxName).size()); ;

    }

    @Test
    public void entityRelationshipsOnly () throws Exception {
        SystemUser su = registerSystemUser("entityRelationshipsOnly", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("entityRelationshipsOnly", true));


        EntityInputBean eStaff = new EntityInputBean(fortress, "wally", "Staff", new DateTime(), "30250");
        mediationFacade.trackEntity(su.getCompany(), eStaff);

        eStaff = new EntityInputBean(fortress, "mary", "Staff", new DateTime(), "30251");
        mediationFacade.trackEntity(su.getCompany(), eStaff);

        // Cross reference two entities in the same fortress irrespective of the Document type
        EntityInputBean xRef = new EntityInputBean(fortress, "Entity");
        xRef.setCode("30250");

        xRef.addEntityLink("manages", new EntityKeyBean("30251", "Entity"));

        // Specifying the DocumentType as "Entity" instructs FD to treat the payload only as
        // creating relationships. Entity is a reserved docType. This is a useful mechanism
        // if you are processing a file that contains only relationship data for a range of entities that
        // exist as different DocTypes. fortress is mandatory.
        mediationFacade.trackEntity(su.getCompany(), xRef); // Should create only the relationship
        assertNull ("Entity document type should not exist", conceptService.findDocumentType(fortress, "Entity"));
        Map<String, Collection<Entity>> crossRefResults = entityService.getCrossReference(su.getCompany(), fortress.getName(), "30251", "manages");
        assertEquals("Should have found 1 relationship", 1, crossRefResults.size());
        Entity foundEntity = crossRefResults.get("manages").iterator().next();
        assertEquals ("30250", foundEntity.getCode());


    }

    @Test
    public void linkToEntitiesWhenTrackIsSuppressed() throws Exception{
        // Tests that a child record, with track suppressed, connects to a graph persistent parent relationship
        cleanUpGraph();
        SystemUser su = registerSystemUser("linkToParentWhenTrackIsSuppressed", mike_admin);
        Fortress staffFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ParentFortress", true));

        EntityInputBean parent = new EntityInputBean(staffFortress, "wally", "Staff", new DateTime(), "ABC123");
        TrackResultBean parentTrack = mediationFacade.trackEntity(su.getCompany(), parent); // Persistent entity

        ContentModel model = ContentModelDeserializer.getContentModel("/models/parent-link-track-suppressed.json");
        // Store server side
        Fortress childFortress = fortressService.registerFortress(su.getCompany(), model.getFortress());
        DocumentType documentType = conceptService.save(new DocumentType(childFortress.getDefaultSegment(), model.getDocumentType()));
        ContentModelResult savedModel = contentModelService.saveEntityModel(su.getCompany(), childFortress, documentType, model);
        assertNotNull( savedModel);
        model= contentModelService.get(su.getCompany(), "WorkData", "WorkRecord");
        assertNotNull (model);

        ColumnDefinition columnDefinition = model.getContent().get("staffID");
        Assert.assertEquals("didn't find the parent relationship", 1,columnDefinition.getEntityLinks().size() );
        Map<String, String> entityKey = columnDefinition.getEntityLinks().iterator().next();
        assertTrue("Parent property was not serialized", Boolean.parseBoolean(entityKey.get("parent")));

        Map<String, List<EntityKeyBean>> relationships = new HashMap<>();
        List<EntityKeyBean> entityKeys = new ArrayList<>();
        entityKeys.add(new EntityKeyBean(parentTrack.getDocumentType(), staffFortress, parent.getCode()));
        relationships.put("worked", entityKeys);


        Collection<TagInputBean>tags = new ArrayList<>();
        TagInputBean tag = new TagInputBean("anyCode", "FindMe");
        tag.addEntityTagLink(new EntityTagRelationshipInput("linked"));
        tags.add(tag) ;


        EntityInputBean workRecord = new EntityInputBean("WorkData", "WorkRecord")
                .setCode("123")
                .setTrackSuppressed(true)
                .setEntityOnly(true)
                .setTags(tags)
                .setEntityLinks(relationships) // Links the work entity (non-graph persistent) to persistent Staff entity
                ;

        TrackResultBean trackWork = mediationFacade.trackEntity(su.getCompany(), workRecord);
        assertNotNull (trackWork);
        assertTrue ( trackWork.isNewEntity());
        assertEquals("Tag was not created in track request", 1, tagService.findTags(su.getCompany(),"FindMe" ).size());
        EntitySearchChange entitySearchChange = searchService.getEntityChange(trackWork);
        assertNotNull ( entitySearchChange);
        assertEquals (1, entitySearchChange.getEntityLinks().size());
        assertNotNull(entitySearchChange.getTagValues());
        Assert.assertEquals(1, entitySearchChange.getTagValues().size());
        Map<String, ArrayList<SearchTag>> searchTags = entitySearchChange.getTagValues().get("linked");
        assertEquals (1, searchTags.size());
        ArrayList<SearchTag> foundTag = entitySearchChange.getTagValues().get("linked").get("findme");
        assertEquals("Should be one tag associated with the search doc", 1, foundTag.size());
    }


    @Test
    public void parentLinkWhenTrackIsSuppressed() throws Exception{
        // Tests that a child record, with track suppressed, connects to a graph persistent parent relationship
        cleanUpGraph();
        SystemUser su = registerSystemUser("parentLinkWhenTrackIsSuppressed", mike_admin);
        Fortress staffFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ParentFortress", true));

        EntityInputBean parent = new EntityInputBean(staffFortress, "wally", "Staff", new DateTime(), "ABC123");
        TrackResultBean parentTrack = mediationFacade.trackEntity(su.getCompany(), parent); // Persistent entity


        Map<String, List<EntityKeyBean>> relationships = new HashMap<>();
        List<EntityKeyBean> entityKeys = new ArrayList<>();
        entityKeys.add(new EntityKeyBean(parentTrack.getDocumentType(), staffFortress, parent.getCode())
                            .setParent(true));

        relationships.put("worked", entityKeys);

        EntityInputBean workRecord = new EntityInputBean("WorkData", "WorkRecord")
                .setCode("123")
                .setTrackSuppressed(true)
                .setEntityOnly(true)
                .setEntityLinks(relationships) // Links the work entity (non-graph persistent) to persistent Staff entity
                ;

        TrackResultBean trackWork = mediationFacade.trackEntity(su.getCompany(), workRecord);
        assertNotNull (trackWork);
        assertTrue ( trackWork.isNewEntity());

        EntitySearchChange entitySearchChange = searchService.getEntityChange(trackWork);
        assertNotNull ( entitySearchChange);
        assertNotNull ( "parent flag in the entityKey was not respected",  entitySearchChange.getParent());
        assertEquals ( "The only entityLink was a Parent so it shouldn't be in this collection", 0, entitySearchChange.getEntityLinks().size());

    }

    public Map<String,Collection<Entity>>getLinkedEntities(Company company, String fortressName, String docType, String code, String rlxName) throws Exception{
        Entity entity = entityService.findByCode(company, fortressName, docType, code);
        assertNotNull (entity);
        return entityService.getCrossReference(company, entity.getKey(), rlxName);

    }
}
