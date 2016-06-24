package org.flockdata.test.engine.services;

import junit.framework.TestCase;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.*;
import org.flockdata.profile.ContentModelDeserializer;
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
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Entities can be connected to other entities. This checks some of the behaviour
 *
 * Created by mike on 19/12/15.
 */
public class TestEntityLinks extends EngineBase {
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
        staff.addTag( new TagInputBean("Cleaner", "Position", "role"));
        mediationFacade.trackEntity(su.getCompany(), staff);

        DocumentType docTypeWork = new DocumentType(timesheetFortress, "Work");
        docTypeWork = conceptService.findOrCreate(timesheetFortress, docTypeWork);

        EntityInputBean workRecord = new EntityInputBean(timesheetFortress, "wally", docTypeWork.getName(), new DateTime(), "ABC321");

        TrackResultBean workResult = mediationFacade.trackEntity(su.getCompany(), workRecord);

        EntityKeyBean staffKey = new EntityKeyBean(staff.getDocumentType().getName(), staff.getFortress().getName(), staff.getCode());
        EntityKeyBean workKey = new EntityKeyBean(workRecord.getDocumentType().getName(), workRecord.getFortress().getName(), workRecord.getCode());

        Collection<EntityKeyBean> parents = new ArrayList<>();
        parents.add(staffKey);
        entityService.linkEntities(su.getCompany(), workKey, parents, "worked");

        Collection<EntityKeyBean> entities = entityService.getInboundEntities(workResult.getEntity(), true);
        assertTrue ( entities!=null);
        assertFalse(entities.isEmpty());

        // *** End initial setup.

        // We now have two linked entities. Check that they are set in to the SearchDocument
        EntitySearchChange searchDocument = searchService.getEntityChange(workResult);
        validateSearchStaff( searchDocument);
        String json = JsonUtils.toJson(searchDocument);
        assertNotNull(json);
        EntitySearchChange deserialized = JsonUtils.toObject(json.getBytes(), EntitySearchChange.class);
        assertNotNull("Issue with JSON serialization", deserialized);
        // Results should be exactly the same
        validateSearchStaff( deserialized);

    }

    private void validateSearchStaff(EntitySearchChange searchDocument){
        assertNotNull(searchDocument.getEntityLinks());
        assertEquals(1, searchDocument.getEntityLinks().size());
        EntityKeyBean staffDetails = searchDocument.getEntityLinks().iterator().next();
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
        staff.addTag(new TagInputBean("Cleaner", "Position", "role"));

        mediationFacade.trackEntity(su.getCompany(), staff);

        DocumentType docTypeWork = new DocumentType(timesheetFortress, "Work");
        docTypeWork = conceptService.findOrCreate(timesheetFortress, docTypeWork);

        EntityInputBean workRecord = new EntityInputBean(timesheetFortress, "wally", docTypeWork.getName(), new DateTime(), "ABC321");
        // Checking that the entity is linked when part of the track request
        workRecord.addEntityLink("worked", new EntityKeyBean("Staff", "timesheet", "ABC123"));
        TrackResultBean workResult = mediationFacade.trackEntity(su.getCompany(), workRecord);
        EntitySearchChange searchDocument = searchService.getEntityChange(workResult);
        validateSearchStaff( searchDocument);
    }

    @Autowired
    ContentModelService contentModelService;

    @Autowired
    BatchService batchService;

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
        eStaff.addTag( new TagInputBean("Cleaner", "Position", "role"));
        mediationFacade.trackEntity(su.getCompany(), eStaff);

        DocumentType timesheet = conceptService.findDocumentType(fortress, "timesheet", true);

        ContentModel params = ContentModelDeserializer.getContentModel("/models/test-entitylinks.json");
        contentModelService.saveEntityModel(su.getCompany(), fortress, timesheet, params );
        batchService.process(su.getCompany(), fortress, timesheet, "/data/test-entitylinks.csv", false);
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
        eStaff.addTag( new TagInputBean("Cleaner", "Position", "role"));
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

    public Map<String,Collection<Entity>>getLinkedEntities(Company company, String fortressName, String docType, String code, String rlxName) throws Exception{
        Entity entity = entityService.findByCode(company, fortressName, docType, code);
        assertNotNull (entity);
        return entityService.getCrossReference(company, entity.getKey(), rlxName);

    }
}
