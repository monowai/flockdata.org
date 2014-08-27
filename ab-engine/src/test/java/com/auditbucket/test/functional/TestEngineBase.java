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

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.auditbucket.company.endpoint.CompanyEP;
import com.auditbucket.engine.endpoint.AdminEP;
import com.auditbucket.engine.endpoint.QueryEP;
import com.auditbucket.engine.endpoint.TrackEP;
import com.auditbucket.engine.repo.neo4j.model.FortressNode;
import com.auditbucket.engine.service.EngineConfig;
import com.auditbucket.engine.service.FortressService;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.engine.service.SchemaService;
import com.auditbucket.engine.service.SearchServiceFacade;
import com.auditbucket.engine.service.TagService;
import com.auditbucket.engine.service.TagTrackService;
import com.auditbucket.engine.service.TrackEventService;
import com.auditbucket.engine.service.TrackService;
import com.auditbucket.engine.service.WhatService;
import com.auditbucket.fortress.endpoint.FortressEP;
import com.auditbucket.geography.endpoint.GeographyEP;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.SystemUserResultBean;
import com.auditbucket.registration.endpoint.RegistrationEP;
import com.auditbucket.registration.endpoint.TagEP;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.registration.service.SystemUserService;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * User: mike
 * Date: 16/06/14
 * Time: 7:54 AM
 */
@RunWith(SpringJUnit4ClassRunner.class)

@Ignore
@WebAppConfiguration
@ContextConfiguration(locations = {"classpath:root-context.xml", "classpath:apiDispatcher-servlet.xml"})
public class TestEngineBase {
    @Autowired
    FortressEP fortressEP;

    @Autowired
    QueryEP queryEP;

    @Autowired
    RegistrationService regService;

    @Autowired
    RegistrationEP registrationEP;

    @Autowired
    SchemaService schemaService;

    @Autowired
    FortressService fortressService;

    @Autowired
    TrackService trackService;

    @Autowired
    TagTrackService tagTrackService;

    @Autowired
    TrackEP trackEP;

    @Autowired
    RegistrationEP regEP;

    @Autowired
    GeographyEP geographyEP;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    TrackEventService trackEventService;

    @Autowired
    SystemUserService systemUserService;

    @Autowired
    TagEP tagEP;

    @Autowired
    TagService tagService;

    @Autowired
    AdminEP adminEP;

    @Autowired
    EngineConfig engineAdmin;

    @Autowired
    WhatService whatService;

    @Autowired
    CompanyService companyService;

    @Autowired
    CompanyEP companyEP;

    @Autowired
    SearchServiceFacade searchService;

    @Autowired
    Neo4jTemplate template;

    @Autowired
    protected WebApplicationContext wac;

    static MockMvc mockMvc;

    @Autowired
    SecurityHelper securityHelper;
    
    private static Logger logger = LoggerFactory.getLogger(TestEngineBase.class);

    // These have to be in simple-security.xml that is authorised to create registrations
    String sally = "sally";
    String mike = "mike";
    String harry = "harry";

    String monowai = "Monowai"; // just a test constant

    static final ObjectMapper mapper = new ObjectMapper();

    Authentication authDefault = new UsernamePasswordAuthenticationToken(mike, "123");

    public static Fortress createFortress(SystemUserResultBean su, String fortressName) throws Exception {
        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/fortress/")
        				.header("Api-Key", su.getApiKey())
                        //.("company", su.getCompany())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getJSON(new FortressInputBean(fortressName, true)))
                        ).andExpect(MockMvcResultMatchers.status().isCreated()).andReturn();

        return getBytesAsObject(response.getResponse().getContentAsByteArray(), FortressNode.class);
    }

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        //setSecurity();
        Neo4jHelper.cleanDb(template);
        engineAdmin.setConceptsEnabled(false);
        engineAdmin.setDuplicateRegistration(true);
    }

    @Before
    public void setSecurity() {
        engineAdmin.setMultiTenanted(false);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        SecurityContextHolder.getContext().setAuthentication(authDefault);
    }

    public static void setSecurity(Authentication auth) {
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static Authentication setSecurity(String userName) {
        Authentication auth = new UsernamePasswordAuthenticationToken(userName, "123");
        setSecurity(auth);
        return auth;
    }

    public static void setSecurityEmpty(){
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    Transaction beginManualTransaction() {
        Transaction t = template.getGraphDatabase().beginTx();
        return t;
    }

    void commitManualTransaction(Transaction t) {
        t.success();
        t.close();
    }

    public static void waitAWhile() throws Exception {
        waitAWhile(null, 3000);
    }

    public static void waitAWhile(int millis) throws Exception {
        waitAWhile(null, millis);
    }

    public static void waitAWhile(String message) throws Exception {
        String ss = System.getProperty("sleepSeconds");
        if (ss == null || ss.equals(""))
            ss = "1";
        if (message == null)
            message = "Slept for {} seconds";
        waitAWhile(message, Long.decode(ss) * 1000);
    }

    /**
     * Processing delay for threads and integration to complete. If you start getting sporadic
     * Heuristic exceptions, chances are you need to call this routine to give other threads
     * time to commit their work.
     * Likewise, waiting for results from ab-search can take a while. We can't know how long this
     * is so you can experiment on your own environment by passing in -DsleepSeconds=1
     *
     * @param milliseconds to pause for
     * @throws Exception
     */
    public static void waitAWhile(String message, long milliseconds) throws Exception {
        Thread.sleep(milliseconds);
        logger.trace(message, milliseconds / 1000d);
    }
    long waitForALog(MetaHeader header, String apiKey) throws Exception {
        // Looking for the first searchKey to be logged against the metaHeader
        long thenTime = System.currentTimeMillis();
        int i = 0;
        long ts = header.getFortressLastWhen();

        MetaHeader metaHeader = trackEP.getMetaHeader(header.getMetaKey(), apiKey, apiKey).getBody();
        TrackLog log = trackEP.getLastChange(metaHeader.getMetaKey(), apiKey, apiKey).getBody();

        int timeout = 100;
        while (log == null && i <= timeout) {
            log = trackEP.getLastChange(metaHeader.getMetaKey(), apiKey, apiKey).getBody();
            if ( log!=null && metaHeader.getFortressLastWhen() == ts )
                return i;
            Thread.yield();
            if (i > 20)
                waitAWhile("Waiting for the log to arrive {}");
            i++;
        }
        if (i > 22)
            logger.info("Wait for log got to [{}] for metaId [{}]", i, metaHeader.getId());
        return System.currentTimeMillis() - thenTime;
    }

    public byte[] getObjectAsJsonBytes(Object object) throws IOException {

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsBytes(object);
    }

    public static <T> T getBytesAsObject (byte[] bytes, Class<T> clazz) throws IOException {
        return mapper.readValue(bytes, clazz);
    }
    
    public static String getJSON(Object obj) {
    	ObjectMapper mapper = new ObjectMapper();
    	String json = null;
    	try {
			json = mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return json;
    }



}
