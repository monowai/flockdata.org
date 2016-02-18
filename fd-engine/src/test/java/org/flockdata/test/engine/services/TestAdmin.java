package org.flockdata.test.engine.services;

import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Created by mike on 19/02/16.
 */
public class TestAdmin extends EngineBase {

    @Test
    public void deleteFortressWithEntitiesAndTagsOnly() throws Exception {

        SystemUser su = registerSystemUser("deleteFortressPurgesEntitiesAndLogs", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));
        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "YYY");

        TagInputBean tagInputBean = new TagInputBean("DeleteTest", "NamedTag", "deltest");
        inputBean.addTag(tagInputBean);


        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String metaKey = resultBean.getEntity().getMetaKey();

        assertNotNull(metaKey);
        assertNotNull(entityService.getEntity(su.getCompany(), metaKey));

        inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "YYY");
        inputBean.addTag(tagInputBean);

        mediationFacade.trackEntity(su.getCompany(), inputBean);

        SecurityContextHolder.getContext().setAuthentication(null);
        // Assert that unauthorised user can't purge a fortress
        try {
            mediationFacade.purge(su.getCompany(), fortress.getName());
            fail("An authorisation exception should have been thrown");
        } catch (Exception e) {
            // This is good
        }
        setSecurity();
        mediationFacade.purge(fortress);
        EngineBase.waitAWhile("Waiting for Async processing to complete");
        assertNull(fortressService.findByName(su.getCompany(), fortress.getName()));
        // This should fail
        exception.expect(NotFoundException.class);
        entityService.getEntity(su.getCompany(), metaKey);
    }

    @Test
    public void unauthorisedUserCannotDeleteFortress() throws Exception {

        SystemUser su = registerSystemUser("deleteFortressPurgesEntitiesAndLogs", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("deleteFortressPurgesEntitiesAndLogs", true));
        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "deleteFortressPurgesEntitiesAndLogs", new DateTime(), "YYY");

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String metaKey = resultBean.getEntity().getMetaKey();

        assertNotNull(metaKey);
        assertNotNull(entityService.getEntity(su.getCompany(), metaKey));

        setUnauthorized();
        // Assert that unauthorised user can't purge a fortress
        exception.expect(AuthenticationException.class);
        mediationFacade.purge(fortress);
    }

    @Test
    public void purgedFortressRemovesEntities() throws Exception {

        SystemUser su = registerSystemUser("deleteFortressPurgesEntitiesAndLogs", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("deleteFortressPurgesEntitiesAndLogs", true));
        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "deleteFortressPurgesEntitiesAndLogs", new DateTime(), "YYY");

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String metaKey = resultBean.getEntity().getMetaKey();

        assertNotNull(metaKey);
        assertNotNull(entityService.getEntity(su.getCompany(), metaKey));

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", metaKey, new DateTime(), EntityContentHelper.getRandomMap()));
        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", metaKey, new DateTime(), EntityContentHelper.getRandomMap()));

        Assert.assertEquals(2, entityService.getLogCount(su.getCompany(), resultBean.getEntity().getMetaKey()));

        mediationFacade.purge(fortress);
        EngineBase.waitAWhile("Waiting for Async processing to complete");
        assertNull(fortressService.findByName(su.getCompany(), fortress.getName()));
        exception.expect(NotFoundException.class);
        entityService.getEntity(su.getCompany(), metaKey);
    }

    @Test
    public void deleteFortressPurgesDataWithTags() throws Exception {

        SystemUser su = registerSystemUser("deleteFortressPurgesDataWithTags", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));
        EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "testDupe", new DateTime(), "YYY");
        TagInputBean tagInputBean = new TagInputBean("DeleteTest", "NamedTag", "deltest");
        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), inputBean);
        String metaKey = resultBean.getEntity().getMetaKey();

        assertNotNull(metaKey);
        assertNotNull(entityService.getEntity(su.getCompany(), metaKey));

        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("wally", metaKey, new DateTime(), EntityContentHelper.getRandomMap()));

        inputBean.setCode("123abc");
        inputBean.setMetaKey(null);
        inputBean.setContent(new ContentInputBean("wally", metaKey, new DateTime(), EntityContentHelper.getRandomMap()));
        mediationFacade.trackEntity(fortress.getDefaultSegment(), inputBean);

        SecurityContextHolder.getContext().setAuthentication(null);
        // Assert that unauthorised user can't purge a fortress
        exception.expect(SecurityException.class);
        mediationFacade.purge(su.getCompany(), fortress.getName());
        setSecurity();
        mediationFacade.purge(fortress);
        EngineBase.waitAWhile("Waiting for Async processing to complete");

        assertNull(fortressService.findByName(su.getCompany(), fortress.getName()));
        // This should fail
        exception.expect(NotFoundException.class);
        assertNull(entityService.getEntity(su.getCompany(), metaKey));


    }

    @Test
    public void purgeFortressClearsDown() throws Exception {
        setSecurity();
        SystemUser su = registerSystemUser("deleteFortressPurgesEntitiesAndLogs", mike_admin);
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("purgeFortressClearsDown", true));

        EntityInputBean trackBean = new EntityInputBean(fortress, "olivia@ast.com", "CompanyNode", null, "abc2");
        trackBean.addTag(new TagInputBean("anyName", "TestTag", "rlx"));
        trackBean.addTag(new TagInputBean("otherName", "TestTag", "rlxValue").setReverse(true));
        ContentInputBean logBean = new ContentInputBean("me", DateTime.now(), EntityContentHelper.getRandomMap());
        trackBean.setContent(logBean);
        String resultA = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getMetaKey();

        assertNotNull(resultA);

        trackBean = new EntityInputBean(fortress, "olivia@ast.com", "CompanyNode", null, "abc3");
        trackBean.addTag(new TagInputBean("anyName", "TestTag", "rlx"));
        trackBean.addTag(new TagInputBean("otherName", "TestTag", "rlxValue").setReverse(true));
        logBean = new ContentInputBean("me", DateTime.now(), EntityContentHelper.getRandomMap());
        trackBean.setContent(logBean);

        String resultB = mediationFacade.trackEntity(su.getCompany(), trackBean).getEntity().getMetaKey();

        Collection<String> others = new ArrayList<>();
        others.add(resultB);
        entityService.crossReference(su.getCompany(), resultA, others, "rlxName");

        others = new ArrayList<>();
        others.add(resultA);
        entityService.crossReference(su.getCompany(), resultB, others, "rlxNameB");

        mediationFacade.purge(fortress);
        EngineBase.waitAWhile("Waiting for Async processing to complete");
        try {
            assertNull(entityService.getEntity(su.getCompany(), resultA));
            fail("Should have thrown notFound exception");
        } catch (NotFoundException n) {
            // good
        }

        try {
            assertNull(entityService.getEntity(su.getCompany(), resultB));
            fail("Should have thrown notFound exception");
        } catch (NotFoundException n) {
            // good
        }

    }

}
