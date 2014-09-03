/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.test.functional;

import com.auditbucket.authentication.handler.ApiKeyInterceptor;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.helper.JsonUtils;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.test.utils.TestHelper;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.junit.Assert.*;

/**
 * User: mike
 * Date: 19/05/14
 * Time: 3:46 PM
 */
@Transactional
@WebAppConfiguration
public class TestAdminCalls extends TestEngineBase {

    @Autowired
    private MediationFacade mediationFacade;

//    private Logger logger = LoggerFactory.getLogger(TestTrack.class);

    @Autowired
    protected WebApplicationContext wac;

    MockMvc mockMvc;


    @Test
    public void deleteFortressWithHeadersAndTagsOnly() throws Exception {

        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike_admin));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");

        TagInputBean tagInputBean = new TagInputBean("DeleteTest", "NamedTag", "deltest");
        inputBean.addTag(tagInputBean);


        TrackResultBean resultBean = mediationFacade.createHeader(su.getCompany(), inputBean);
        String ahKey = resultBean.getMetaKey();

        assertNotNull(ahKey);
        assertNotNull(trackService.getHeader(ahKey));

        inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");
        inputBean.addTag(tagInputBean);

        mediationFacade.createHeader(su.getCompany(), inputBean);

        SecurityContextHolder.getContext().setAuthentication(null);
        // Assert that unauthorised user can't purge a fortress
        try {
            adminEP.purgeFortress(fo.getName(), null, null);
            fail("An authorisation exception should have been thrown");
        } catch (Exception e) {
            // This is good
        }
        setSecurity();
        adminEP.purgeFortress(fo.getName(), null, null);
        assertNull(trackService.getHeader(ahKey));
        assertNull(fortressService.findByName(fo.getName()));
    }

    @Test
    public void deleteFortressPurgesHeaderAndLogs() throws Exception {

        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike_admin));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");

        TrackResultBean resultBean = mediationFacade.createHeader(su.getCompany(), inputBean);
        String ahKey = resultBean.getMetaKey();

        assertNotNull(ahKey);
        assertNotNull(trackService.getHeader(ahKey));

        mediationFacade.processLog(new LogInputBean("wally", ahKey, new DateTime(), TestHelper.getRandomMap()));
        mediationFacade.processLog(new LogInputBean("wally", ahKey, new DateTime(), TestHelper.getRandomMap()));

        assertEquals(2, trackService.getLogCount(resultBean.getMetaKey()));

        SecurityContextHolder.getContext().setAuthentication(null);
        // Assert that unauthorised user can't purge a fortress
        try {
            adminEP.purgeFortress(fo.getName(), null, null);
            fail("An authorisation exception should have been thrown");
        } catch (Exception e) {
            // This is good
        }
        setSecurity();
        adminEP.purgeFortress(fo.getName(), su.getApiKey(), su.getApiKey());
        assertNull(trackService.getHeader(ahKey));
        assertNull(fortressService.findByName(fo.getName()));
    }

    @Test
    public void deleteFortressPurgesDataWithTags() throws Exception {

        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike_admin));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "testDupe", new DateTime(), "YYY");
        TagInputBean tagInputBean = new TagInputBean("DeleteTest", "NamedTag", "deltest");
        inputBean.addTag(tagInputBean);

        TrackResultBean resultBean = mediationFacade.createHeader(su.getCompany(), inputBean);
        String ahKey = resultBean.getMetaKey();

        assertNotNull(ahKey);
        assertNotNull(trackService.getHeader(ahKey));

        mediationFacade.processLog(new LogInputBean("wally", ahKey, new DateTime(), TestHelper.getRandomMap()));

        inputBean.setCallerRef("123abc");
        inputBean.setMetaKey(null);
        inputBean.setLog(new LogInputBean("wally", ahKey, new DateTime(), TestHelper.getRandomMap()));
        mediationFacade.createHeader(fo, inputBean);

        SecurityContextHolder.getContext().setAuthentication(null);
        // Assert that unauthorised user can't purge a fortress
        try {
            adminEP.purgeFortress(fo.getName(), null, null);
            fail("An authorisation exception should have been thrown");
        } catch (Exception e) {
            // This is good
        }
        setSecurity();
        adminEP.purgeFortress(fo.getName(), su.getApiKey(), su.getApiKey());
        assertNull(trackService.getHeader(ahKey));
        assertNull(fortressService.findByName(fo.getName()));


    }

    @Test
    public void purgeFortressClearsDown() throws Exception {
        setSecurity();
        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike_admin));
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("purgeFortressClearsDown", true));

        MetaInputBean trackBean = new MetaInputBean(fortress.getName(), "olivia@ast.com", "CompanyNode", null, "abc2");
        trackBean.addTag(new TagInputBean("anyName", "rlx"));
        trackBean.addTag(new TagInputBean("otherName", "rlxValue").setReverse(true));
        LogInputBean logBean = new LogInputBean("me", DateTime.now(), TestHelper.getRandomMap());
        trackBean.setLog(logBean);
        String resultA = mediationFacade.createHeader(su.getCompany(), trackBean).getMetaKey();

        assertNotNull(resultA);

        trackBean = new MetaInputBean(fortress.getName(), "olivia@ast.com", "CompanyNode", null, "abc3");
        trackBean.addTag(new TagInputBean("anyName", "rlx"));
        trackBean.addTag(new TagInputBean("otherName", "rlxValue").setReverse(true));
        logBean = new LogInputBean("me", DateTime.now(), TestHelper.getRandomMap());
        trackBean.setLog(logBean);

        String resultB = mediationFacade.createHeader(su.getCompany(), trackBean).getMetaKey();

        Collection<String> others = new ArrayList<>();
        others.add(resultB);
        trackEP.putCrossReference(resultA, others, "rlxName", su.getApiKey(), su.getApiKey());

        others = new ArrayList<>();
        others.add(resultA);
        trackEP.putCrossReference(resultB, others, "rlxNameB", su.getApiKey(), su.getApiKey());

        mediationFacade.purge(fortress.getName(), su.getApiKey());
        assertNull(trackService.getHeader(resultA));
        assertNull(trackService.getHeader(resultB));

    }

    @Test
    public void testPing() {
        assertTrue(adminEP.getPing().equalsIgnoreCase("pong!"));
    }

    @Test
    public void testHealth() throws Exception {
        setSecurity();
        SystemUser su = regService.registerSystemUser(new RegistrationBean(mike_admin, "healthCheck"));
        Map<String, Object> results = getHealth(su);
        assertFalse("We didn't get back the health results for an admin user", results.isEmpty());
        assertEquals("!Unreachable! Connection refused", results.get("ab-search"));
        setSecurityEmpty();

		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/health/")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized()).andReturn();
        setSecurity();
        // Create a data access user
        su = regService.registerSystemUser(new RegistrationBean("anyone", "healthCheck"));
        setSecurityEmpty();
        results = getHealth(su);
        assertFalse("The user has no AUTH credentials but a valid APIKey - this should pass", results.isEmpty());

        // Hacking with an invalid API Key. Should fail
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/health/")
                        .header(ApiKeyInterceptor.API_KEY, "_invalidAPIKey_")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized()).andReturn();


    }

    Map<String, Object> getHealth(SystemUser su) throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.get("/admin/health/")
                        .header(ApiKeyInterceptor.API_KEY, (su != null ? su.getApiKey() : ""))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();

        return JsonUtils.getAsMap(json);
    }


}
