package org.flockdata.test.engine.functional;

import junit.framework.TestCase;
import org.flockdata.model.Fortress;
import org.flockdata.model.FortressSegment;
import org.flockdata.model.SystemUser;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by mike on 13/10/15.
 */
public class TestFortressSegments extends EngineBase{

    @Test
    public void fortressHasDefaultSegment() throws Exception{
        SystemUser su = registerSystemUser("testDefaultSegment");
        Fortress fortress = createFortress(su);
        assertNotNull ( fortress.getDefaultSegment());
        FortressSegment segment = fortressService.getDefaultSegment(fortress);
        assertNotNull ( segment);
        assertEquals(FortressSegment.DEFAULT, segment.getCode());
        assertTrue(segment.isDefault());
    }

    @Test
    public void addSegmentToExistingFortress() throws Exception{
        SystemUser su = registerSystemUser("addSegmentToExistingFortress");
        Fortress fortress = createFortress(su);
        FortressSegment segment = new FortressSegment(fortress, "SecondSegment");
        FortressSegment createdSegment = fortressService.addSegment(segment);
        assertNotNull( createdSegment);
        assertTrue(createdSegment.getId() >0 );

        TestCase.assertEquals(2, fortressService.getSegments(fortress).size());
    }


    @Test
    public void addDuplicateSegment() throws Exception{
        SystemUser su = registerSystemUser("addDuplicateSegment");
        Fortress fortress = createFortress(su);
        FortressSegment segment = new FortressSegment(fortress, "SecondSegment");
        FortressSegment createdSegment = fortressService.addSegment(segment);
        assertNotNull( createdSegment);
        assertTrue(createdSegment.getId() >0 );

        FortressSegment duplicateSegment = fortressService.addSegment(segment);
        TestCase.assertEquals(createdSegment.getId(), duplicateSegment.getId());

        TestCase.assertEquals(2, fortressService.getSegments(fortress).size());
    }

    @Test
    public void createSegmentForIllegalFortress() throws Exception{
        exception.expect(IllegalArgumentException.class);
        new FortressSegment(null, "SecondSegment");

    }

}
