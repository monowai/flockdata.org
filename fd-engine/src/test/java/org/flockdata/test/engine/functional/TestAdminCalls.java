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

package org.flockdata.test.engine.functional;

import org.flockdata.helper.ApiKeyInterceptor;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.test.engine.endpoint.EngineEndPoints;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * User: mike
 * Date: 19/05/14
 * Time: 3:46 PM
 */
@WebAppConfiguration
public class TestAdminCalls extends WacBase {

    MockMvc mockMvc;

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
        waitAWhile("Waiting for Async processing to complete");
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

        SecurityContextHolder.getContext().setAuthentication(null);
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

        assertEquals(2, entityService.getLogCount(su.getCompany(), resultBean.getEntity().getMetaKey()));

        mediationFacade.purge(fortress);
        waitAWhile("Waiting for Async processing to complete");
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
        waitAWhile("Waiting for Async processing to complete");

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
        waitAWhile("Waiting for Async processing to complete");
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

    @Test
    public void testPing() throws Exception {
        setSecurity();
        EngineEndPoints eep = new EngineEndPoints(wac);
        String result = eep.adminPing();
        assertTrue("pong".equalsIgnoreCase(result));
        setSecurityEmpty(); // Unsecured should also work
        result = eep.ping();
        assertTrue("pong".equalsIgnoreCase(result));
    }

    /**
     * Ensures that an authorised user, but one who is not associated with a company, can ping the service
     * while an unauthorised user cannot
     *
     * @throws Exception
     */
    @Test
    public void authPing() throws Exception {

        setSecurityEmpty(); // Unsecured should fail
        EngineEndPoints eep = new EngineEndPoints(wac);
        String result = eep.adminPing();
        assertFalse(result.contains("Pong"));

        setSecurity(mike_admin);// Secured user should work
        result = eep.adminPing();
        assertTrue("pong".equalsIgnoreCase(result));

    }

    @Test
    public void auth_Health() throws Exception {
        setSecurityEmpty();
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/health/")
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized()).andReturn();

    }

    @Test
    public void testHealth() throws Exception {
        setSecurity();
        EngineEndPoints engineEndPoints = new EngineEndPoints(wac);
        SystemUser su = registerSystemUser("healthCheck", mike_admin);
        Map<String, Object> results = engineEndPoints.getHealth(su);
        assertFalse("We didn't get back the health results for a valid api account", results.isEmpty());
        if (results.get("fd-search").toString().equalsIgnoreCase("ok"))
            logger.warn("fd-search is running in a not unit test fashion....");
        else
            assertTrue(results.get("fd-search").toString().contains("!Unreachable!"));

        // No api key, auth only DAT-203
        setSecurityEmpty();
        engineEndPoints = new EngineEndPoints(wac);
        engineEndPoints.login("mike", "123");
        results = engineEndPoints.getHealth(null);
        assertFalse("We didn't get back the health results for an admin user", results.isEmpty());

        setSecurityEmpty();

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/health/")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized()).andReturn();
        setSecurity();
        // Create a data access user
        su = registerSystemUser("anyone", "healthCheck");
        setSecurityEmpty();
        results = engineEndPoints.getHealth(su);
        assertFalse("The user has no AUTH credentials but a valid APIKey - this should pass", results.isEmpty());

        // Hacking with an invalid API Key. Should fail
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/health/")
                        .header(ApiKeyInterceptor.API_KEY, "_invalidAPIKey_")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized()).andReturn();


    }


}
