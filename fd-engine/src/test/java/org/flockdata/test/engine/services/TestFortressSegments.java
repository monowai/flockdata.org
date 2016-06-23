package org.flockdata.test.engine.services;

import junit.framework.TestCase;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.model.FortressSegment;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.*;

/**
 * Data for a given fortress+docType can be segmented for easier management
 * This class tests fundamental assertions about the functionality
 *
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
    public void segmentsForDocumentType() throws Exception{
        SystemUser su = registerSystemUser("segmentsForDocumentType");
        Fortress fortress = createFortress(su);
        EntityInputBean inputBeanA = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), "111");
        inputBeanA.setSegment("ABC");
        EntityInputBean inputBeanB = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), "222");
        inputBeanB.setSegment("CBA");
        EntityInputBean inputBeanC = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), "333");
        inputBeanC.setSegment("CBA");

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBeanA);
        mediationFacade.trackEntity(su.getCompany(), inputBeanB);
        mediationFacade.trackEntity(su.getCompany(), inputBeanC);
        DocumentType resolved = conceptService.findDocumentTypeWithSegments( resultBean.getDocumentType());
        assertEquals(2, resolved.getSegments().size());
    }

    @Test
    public void createSegmentForIllegalFortress() throws Exception{
        exception.expect(IllegalArgumentException.class);
        new FortressSegment(null, "SecondSegment");

    }

    @Test
    public void moveEntityAcrossSegments() throws Exception{
        logger.debug("### moveEntityAcrossSegments");
        engineConfig.setTestMode(true); // Force sync processing of the content and log

        String entityCode = "123";
        SystemUser su = registerSystemUser("user");
        String fortressName = "DAT-509";

        FortressInputBean fib = new FortressInputBean(fortressName, true);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), fib);
        EntityInputBean inputBean = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), entityCode);
        inputBean.setContent(new ContentInputBean("poppy", DateTime.now(), EntityContentHelper.getSimpleMap("name", "a")));

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertTrue(resultBean.getEntity().getSegment().isDefault());

        inputBean.setSegment("MoveMeHere");
        resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertFalse(resultBean.getEntity().getSegment().isDefault());

        inputBean.setSegment(fortress.getDefaultSegment().getCode());
        resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        assertTrue(resultBean.getEntity().getSegment().isDefault());

    }

}
