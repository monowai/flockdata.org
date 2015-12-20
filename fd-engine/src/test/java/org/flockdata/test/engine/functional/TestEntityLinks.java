package org.flockdata.test.engine.functional;

import junit.framework.TestCase;
import org.flockdata.helper.JsonUtils;
import org.flockdata.model.DocumentType;
import org.flockdata.model.EntityTag;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.SearchTag;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.SearchChange;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

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
        SystemUser su = registerSystemUser("xRef_FromInputBeans", mike_admin);
        Fortress timesheet = fortressService.registerFortress(su.getCompany(), new FortressInputBean("timesheet", true));

        EntityInputBean staff = new EntityInputBean(timesheet.getName(), "wally", "Staff", new DateTime(), "ABC123");
        staff.addTag( new TagInputBean("Cleaner", "Position", "role"));
        mediationFacade.trackEntity(su.getCompany(), staff);

        DocumentType docTypeWork = new DocumentType(timesheet, "Work");
        docTypeWork = conceptService.findOrCreate(timesheet, docTypeWork);

        EntityInputBean workRecord = new EntityInputBean(timesheet.getName(), "wally", docTypeWork.getName(), new DateTime(), "ABC321");

        TrackResultBean workResult = mediationFacade.trackEntity(su.getCompany(), workRecord);

        EntityKeyBean staffKey = new EntityKeyBean(staff.getFortress(), staff.getDocumentName(), staff.getCode());
        EntityKeyBean workKey = new EntityKeyBean(workRecord.getFortress(), workRecord.getDocumentName(), workRecord.getCode());

        Collection<EntityKeyBean> parents = new ArrayList<>();
        parents.add(staffKey);
        entityService.linkEntities(su.getCompany(), workKey, parents, "worked");

        Collection<EntityKeyBean> entities = entityService.getInboundEntities(workResult.getEntity(), true);
        assertTrue ( entities!=null);
        assertFalse(entities.isEmpty());

        // *** End initial setup.

        // We now have two linked entities. Check that they are set in to the SearchDocument
        SearchChange searchDocument = searchService.getSearchChange(workResult);
        validateSearchStaff( searchDocument);
        String json = JsonUtils.getJSON(searchDocument);
        EntitySearchChange deserialized = JsonUtils.getBytesAsObject(json.getBytes(), EntitySearchChange.class);
        assertNotNull("Issue with JSON serialization", deserialized);
        // Results should be exactly the same
        validateSearchStaff( deserialized);

    }
    private void validateSearchStaff(SearchChange searchDocument){
        assertNotNull(searchDocument.getEntityLinks());
        TestCase.assertEquals(1, searchDocument.getEntityLinks().size());
        EntityKeyBean staffDetails = searchDocument.getEntityLinks().iterator().next();
        assertNotNull(staffDetails);
        assertNotNull(staffDetails.getSearchTags());
        assertFalse(staffDetails.getSearchTags().isEmpty());
        SearchTag position = staffDetails.getSearchTags().get("Position").iterator().next();
        assertTrue(position.getCode().equals("Cleaner"));

    }

}
