/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

import com.auditbucket.engine.endpoint.QueryEP;
import com.auditbucket.engine.endpoint.TrackEP;
import com.auditbucket.engine.service.*;
import com.auditbucket.helper.JsonUtils;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.MetaSearchSchema;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.search.model.SearchResult;
import com.auditbucket.track.bean.*;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
import com.auditbucket.track.model.TrackTag;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.mapping.GetMapping;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Allows the ab-engine services to be tested against ab-search.
 * ab-search is stated by Cargo as a Tomcat server.
 * ab-engine runs as a usual Spring test runner.
 * This approach means that RabbitMQ has to be installed to allow integration to occur as
 * no web interface is launched for ab-engine
 * <p/>
 * User: nabil
 * Date: 16/07/13
 * Time: 22:51
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"classpath:root-context.xml", "classpath:apiDispatcher-servlet.xml"})
public class TestABIntegration {
    private static boolean runMe = true; // pass -Dab.debug=true to disable all tests
    private static int fortressMax = 1;
    private static JestClient esClient;

    @Autowired
    TrackService trackService;
    @Autowired
    TrackEP trackEP;
    @Autowired
    RegistrationService regService;

    @Autowired
    CompanyService companyService;

    @Autowired
    FortressService fortressService;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    TagTrackService tagTrackService;

    @Autowired
    QueryService queryService;

    @Autowired
    QueryEP queryEP;

    @Autowired
    WhatService whatService;

    static MockMvc mockMvc;

    @Autowired
    WebApplicationContext wac;

    private static Logger logger = LoggerFactory.getLogger(TestABIntegration.class);
    private static Authentication AUTH_MIKE = new UsernamePasswordAuthenticationToken("mike", "123");

    String company = "Monowai";
    static Properties properties = new Properties();
    int esTimeout = 10; // Max attempts to find the result in ES

    @AfterClass
    public static void waitAWhile() throws Exception {
        waitAWhile(null, 3000);
    }

    public static void waitAWhile(String message) throws Exception {
        String ss = System.getProperty("sleepSeconds");
        if (ss == null || ss.equals(""))
            ss = "1";
        if (message == null)
            message = "Slept for {} seconds";
        waitAWhile(message, Long.decode(ss) * 1000);
    }

    @BeforeClass
    @Rollback(false)
    public static void cleanupElasticSearch() throws Exception {
        FileInputStream f = new FileInputStream("./src/test/resources/config.properties");
        properties.load(f);
        String abDebug = System.getProperty("ab.debug");
        if (abDebug != null)
            runMe = !Boolean.parseBoolean(abDebug);

        HttpClientConfig clientConfig = new HttpClientConfig.Builder("http://localhost:" + properties.get("es.http.port")).multiThreaded(false).build();
        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(clientConfig);
        //factory.setClientConfig(clientConfig);
        esClient = factory.getObject();

        deleteEsIndex("ab.monowai.suppress");
        deleteEsIndex("ab.monowai.testfortress");
        deleteEsIndex("ab.monowai.ngram");
        deleteEsIndex("ab.monowai.rebuildtest");
        deleteEsIndex("ab.monowai.audittest");
        deleteEsIndex("ab.monowai.suppress");
        deleteEsIndex("ab.monowai.headerwithtagsprocess");
        deleteEsIndex("ab.monowai.trackgraph");
        deleteEsIndex("ab.monowai.audittest");
        deleteEsIndex("ab.monowai.111");

        for (int i = 1; i < fortressMax + 1; i++) {
            deleteEsIndex("ab.monowai.bulkloada" + i);
        }

    }

    @Before
    public void setupUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
        if (companyService.findByName("monowai") == null) {
            regService.registerSystemUser(new RegistrationBean("monowai", "mike").setIsUnique(false));
            waitAWhile("Registering Auth user System Access {}");
        }
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    private static void deleteEsIndex(String indexName) throws Exception {
        logger.info("%% Delete Index {}", indexName);
        esClient.execute(new DeleteIndex.Builder(indexName).build());
    }

    @AfterClass
    public static void shutDownElasticSearch() throws Exception {
        esClient.shutdownClient();
    }

    @Test
    public void companyAndFortressWithSpaces() throws Exception {
//        assumeTrue(runMe);
        logger.info("## companyAndFortressWithSpaces");

        SystemUser su = registerSystemUser("co-fortress");
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("Audit Test", false));
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        MetaHeader header = mediationFacade.createHeader(su.getCompany(), inputBean).getMetaHeader();
        String ahKey = header.getMetaKey();
        assertNotNull(ahKey);
        header = trackService.getHeader(ahKey);
        assertEquals("ab.monowai.audittest", header.getIndexName());
        mediationFacade.processLog(new LogInputBean("wally", ahKey, new DateTime(), getRandomMap()));
        waitForHeaderToUpdate(header.getMetaKey(), su.getApiKey());

        doEsQuery(header.getIndexName(), header.getMetaKey());
    }

    @Test
    public void headerWithOnlyTagsProcess() throws Exception {
        assumeTrue(runMe);
        logger.info("## headersWithTagsProcess");
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
        SystemUser su = registerSystemUser("Mark");
        String apiKey = su.getApiKey();
        Fortress fo = fortressService.registerFortress(new FortressInputBean("headerWithTagsProcess", false));
        DateTime now = new DateTime();
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", now, "ABCXYZ123");
        inputBean.setMetaOnly(true);
        inputBean.addTag(new TagInputBean("testTagNameZZ", "someAuditRLX"));
        inputBean.setEvent("TagTest");
        TrackResultBean result = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody();
        logger.debug("Created Request ");
        waitForHeaderToUpdate(result.getMetaHeader(), su.getApiKey());
        TrackedSummaryBean summary = trackEP.getAuditSummary(result.getMetaKey(), apiKey, apiKey).getBody();
        assertNotNull(summary);
        // Check we can find the Event in ElasticSearch
        doEsQuery(summary.getHeader().getIndexName(), inputBean.getEvent(), 1);
        // Can we find the Tag
        doEsQuery(summary.getHeader().getIndexName(), "testTagNameZZ", 1);

    }

    @Test
    public void immutableHeadersWithNoLogsAreIndexed() throws Exception {
        assumeTrue(runMe);
        logger.info("## immutableHeadersWithNoLogsAreIndexed");
        SystemUser su = registerSystemUser("Manfred");
        Fortress fo = fortressService.registerFortress(new FortressInputBean("immutableHeadersWithNoLogsAreIndexed", false));
        DateTime now = new DateTime();
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", now, "ZZZ123");
        inputBean.setEvent("immutableHeadersWithNoLogsAreIndexed");
        inputBean.setMetaOnly(true); // Must be true to make over to search
        TrackResultBean auditResult;
        auditResult = mediationFacade.createHeader(su.getCompany(), inputBean);
        waitForHeaderToUpdate(auditResult.getMetaHeader(), su.getApiKey());
        TrackedSummaryBean summary = mediationFacade.getTrackedSummary(auditResult.getMetaKey());
        assertNotNull(summary);
        assertSame("change logs were not expected", 0, summary.getChanges().size());
        assertNotNull("Search record not received", summary.getHeader().getSearchKey());
        // Check we can find the Event in ElasticSearch
        doEsQuery(summary.getHeader().getIndexName(), inputBean.getEvent(), 1);

        // Not flagged as meta only so will not appear in the search index until a log is created
        inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", now, "ZZZ999");
        auditResult = mediationFacade.createHeader(su.getCompany(), inputBean);
        summary = mediationFacade.getTrackedSummary(auditResult.getMetaKey());
        assertNotNull(summary);
        assertSame("No change logs were expected", 0, summary.getChanges().size());
        assertNull(summary.getHeader().getSearchKey());
        // Check we can find the Event in ElasticSearch
        doEsQuery(summary.getHeader().getIndexName(), "ZZZ999", 0);
    }

    @Test
    public void rebuildESIndexFromEngine() throws Exception {
        assumeTrue(runMe);
        logger.info("## rebuildESIndexFromEngine");
        SystemUser su = registerSystemUser("David");
        Fortress fo = fortressService.registerFortress(new FortressInputBean("rebuildTest", false));

        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        inputBean.setLog(new LogInputBean("wally", new DateTime(), getRandomMap()));
        TrackResultBean auditResult = mediationFacade.createHeader(su.getCompany(), inputBean);

        MetaHeader metaHeader = trackService.getHeader(auditResult.getMetaKey());
        waitForHeaderToUpdate(metaHeader, su.getApiKey());
        assertEquals("ab.monowai.rebuildtest", metaHeader.getIndexName());

        doEsQuery(metaHeader.getIndexName(), "*");
        deleteEsIndex(metaHeader.getIndexName());

        // Rebuild....
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
        Long fResult = mediationFacade.reindex(fo.getCompany(), fo.getCode());
        waitForHeaderToUpdate(metaHeader, su.getApiKey());
        Assert.assertEquals(1l, fResult.longValue());

        doEsQuery(metaHeader.getIndexName(), "*");

    }

    @Test
    public void
    createHeaderTimeLogsWithSearchActivated() throws Exception {
        assumeTrue(runMe);
        logger.info("## createHeaderTimeLogsWithSearchActivated");
        deleteEsIndex("ab.monowai.111");
        int max = 3;
        String ahKey;
        SystemUser su = registerSystemUser("Olivia");
        Fortress fo = fortressService.registerFortress(new FortressInputBean("111", false));

        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        TrackResultBean auditResult;
        auditResult = mediationFacade.createHeader(su.getCompany(), inputBean);
        ahKey = auditResult.getMetaKey();

        assertNotNull(ahKey);

        MetaHeader metaHeader = trackService.getHeader(ahKey);
        assertNotNull(metaHeader);
        assertNotNull(trackService.findByCallerRef(fo, "TestTrack", "ABC123"));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        int i = 0;

        StopWatch watch = new StopWatch();
        logger.info("Start-");
        watch.start();
        while (i < max) {
            mediationFacade.processLog(new LogInputBean("wally", ahKey, new DateTime(), getSimpleMap("blah", i)));
            i++;
        }
        waitForALog(metaHeader, su.getApiKey());
        watch.stop();
        // Test that we get the expected number of log events
        if (!"rest".equals(System.getProperty("neo4j"))) // Don't check if running over rest
            assertEquals("This will fail if the DB is not cleared down, i.e. testing over REST", max, trackService.getLogCount(ahKey));

        doEsFieldQuery(metaHeader.getIndexName(), MetaSearchSchema.WHAT + ".blah", "*", 1);
    }

    @Test
    public void auditsByPassGraphByCallerRef() throws Exception {
        assumeTrue(runMe);
        logger.info("## auditsByPassGraphByCallerRef started");
        deleteEsIndex("ab.monowai.trackgraph");
        SystemUser su = registerSystemUser("Isabella");
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("TrackGraph", false));

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        inputBean.setTrackSuppressed(true);
        inputBean.setMetaOnly(true); // If true, the header will be indexed
        // Track suppressed but search is enabled
        mediationFacade.createHeader(su.getCompany(), inputBean);
        waitAWhile();

        String indexName = MetaSearchSchema.parseIndex(fortress);
        assertEquals("ab.monowai.trackgraph", indexName);

        // Putting asserts On elasticsearch
        doEsQuery(indexName, "*", 1);
        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setTrackSuppressed(true);
        inputBean.setMetaOnly(true);
        mediationFacade.createHeader(su.getCompany(), inputBean);
        waitAWhile();
        doEsQuery(indexName, "*", 2);

        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setTrackSuppressed(true);
        inputBean.setMetaOnly(true);
        MetaHeader header = mediationFacade.createHeader(su.getCompany(), inputBean).getMetaHeader();
        Assert.assertNull(header.getMetaKey());
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(indexName, "*", 2);

        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setTrackSuppressed(true);
        inputBean.setMetaOnly(true);
        mediationFacade.createHeader(su.getCompany(), inputBean);
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(indexName, "*", 2);

        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC125");
        inputBean.setTrackSuppressed(true);
        inputBean.setMetaOnly(true);
        mediationFacade.createHeader(su.getCompany(), inputBean);
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(indexName, "*", 3);

    }

    @Test
    public void searchDocIsRewrittenAfterCancellingLogs() throws Exception {
        // DAT-27
        assumeTrue(runMe);
        logger.info("## searchDocRewrite");
        SystemUser su = registerSystemUser("Felicity");
        Fortress fo = fortressService.registerFortress(new FortressInputBean("cancelLogTag", false));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "CancelDoc", new DateTime(), "ABC123");
        LogInputBean log = new LogInputBean("wally", new DateTime(), getRandomMap());
        inputBean.addTag(new TagInputBean("Happy").addMetaLink("testinga"));
        inputBean.addTag(new TagInputBean("Happy Days").addMetaLink("testingb"));
        inputBean.setLog(log);
        TrackResultBean result = mediationFacade.createHeader(su.getCompany(), inputBean);

        waitForHeaderToUpdate(result.getMetaHeader(), su.getApiKey());
        // ensure that non-analysed tags work
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testinga.code", "happy", 1);
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testingb.code", "happy days", 1);
        // We now have 1 log with tags validated in ES

        // Add another Log - replacing the two existing Tags with two new ones
        log = new LogInputBean("wally", new DateTime(), getRandomMap());
        inputBean.getTags().clear();
        inputBean.addTag(new TagInputBean("Sad Days").addMetaLink("testingb"));
        inputBean.addTag(new TagInputBean("Days Bay").addMetaLink("testingc"));
        inputBean.setLog(log);
        result = mediationFacade.createHeader(su.getCompany(), inputBean);
        waitForHeaderToUpdate(result.getMetaHeader(), su.getApiKey());
        // We now have 2 logs, sad tags, no happy tags

        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testingb.code", "sad days", 1);
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testingc.code", "days bay", 1);
        // These were removed in the update
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testinga.code", "happy", 0);
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testingb.code", "happy days", 0);

        // Cancel Log - this will remove the sad tags and leave us with happy tags
        mediationFacade.cancelLastLogSync(su.getCompany(), result.getMetaKey());
        waitForHeaderToUpdate(result.getMetaHeader(), su.getApiKey());
        Set<TrackTag> tags = tagTrackService.findTrackTags(result.getMetaHeader());
        assertEquals(2, tags.size());

        // These should have been added back in due to the cancel operation
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testinga.code", "happy", 1);
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testingb.code", "happy days", 1);

        // These were removed in the cancel
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testingb.code", "sad days", 0);
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testingc.code", "days bay", 0);


    }

    @Test
    public void tagKeySearch() throws Exception {
        // DAT-95
        assumeTrue(runMe);
        logger.info("## tagKeySearch");
        SystemUser su = registerSystemUser("Cameron");
        Fortress fo = fortressService.registerFortress(new FortressInputBean("tagKeySearch", false));
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        LogInputBean log = new LogInputBean("wally", new DateTime(), getRandomMap());
        inputBean.addTag(new TagInputBean("Happy").addMetaLink("testinga"));
        inputBean.addTag(new TagInputBean("Happy Days").addMetaLink("testingb"));
        inputBean.addTag(new TagInputBean("Sad Days").addMetaLink("testingb"));
        inputBean.addTag(new TagInputBean("Days Bay").addMetaLink("testingc"));
        inputBean.setLog(log);
        TrackResultBean result = mediationFacade.createHeader(su.getCompany(), inputBean); // Mock result as we're not tracking
        waitForHeaderToUpdate(result.getMetaHeader(), su.getApiKey());
        // ensure that non-analysed tags work
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testinga.code", "happy", 1);
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testingb.code", "happy days", 1);
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testingb.code", "sad days", 1);
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testingc.code", "days bay", 1);
        doEsTermQuery(result.getMetaHeader().getIndexName(), MetaSearchSchema.TAG + ".testingc.code", "days", 0);

    }

    @Test
    public void searchIndexWithNoMetaKeysDoesNotError() throws Exception {
        // DAT-83
        assumeTrue(runMe);
        logger.info("## searchDocWithNoMetaKeyWorks");
        SystemUser su = registerSystemUser("Harry");
        Fortress fo = fortressService.registerFortress(new FortressInputBean("noMetaKey", false));

        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        inputBean.setTrackSuppressed(true); // Write a search doc only
        inputBean.setLog(new LogInputBean("wally", new DateTime(), getRandomMap()));
        // First header and log, but not stored in graph
        mediationFacade.createHeader(su.getCompany(), inputBean); // Mock result as we're not tracking

        inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setLog(new LogInputBean("wally", new DateTime(), getRandomMap()));
        TrackResultBean result = mediationFacade.createHeader(su.getCompany(), inputBean);
        MetaHeader metaHeader = trackService.getHeader(result.getMetaKey());
        assertEquals("ab.monowai." + fo.getCode(), metaHeader.getIndexName());

        waitForHeaderToUpdate(metaHeader, su.getApiKey()); // 2nd document in the index
        // We have one with a metaKey and one without
        doEsQuery("ab.monowai." + fo.getCode(), "*", 2);

        QueryParams qp = new QueryParams(fo);
        qp.setSimpleQuery("*");
        String queryResult = runMetaQuery(qp);
        logger.info(queryResult);

        // Two search docs,but one without a metaKey

    }

    @Test
    public void engineQueryResultsReturn() throws Exception {
        // DAT-83
        assumeTrue(runMe);
        logger.info("## engineQueryResultsReturn");
        SystemUser su = registerSystemUser("Kiwi");
        Fortress fo = fortressService.registerFortress(new FortressInputBean("QueryTest", false));

        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        inputBean.setLog(new LogInputBean("wally", new DateTime(), getRandomMap()));

        mediationFacade.createHeader(su.getCompany(), inputBean); // Mock result as we're not tracking

        inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setLog(new LogInputBean("wally", new DateTime(), getRandomMap()));
        TrackResultBean result = mediationFacade.createHeader(su.getCompany(), inputBean);

        MetaHeader metaHeader = trackService.getHeader(result.getMetaKey());
        assertEquals("ab.monowai." + fo.getCode(), metaHeader.getIndexName());

        waitForHeaderToUpdate(metaHeader, su.getApiKey()); // 2nd document in the index
        // We have one with a metaKey and one without
        doEsQuery("ab.monowai." + fo.getCode(), "*", 2);

        QueryParams qp = new QueryParams(fo);
        qp.setSimpleQuery("*");
        runMetaQuery(qp);
        //EsSearchResult queryResults =runSearchQuery(su, qp);
        //EsSearchResult queryResults =mediationFacade.search(su.getCompany(), qp);
        EsSearchResult queryResults = runSearchQuery(su, qp);
        assertNotNull(queryResults);
        assertEquals(2, queryResults.getResults().size());

        // Two search docs,but one without a metaKey

    }

    @Test
    public void utcDateFieldsThruToSearch() throws Exception {
        // DAT-196
//        assumeTrue(runMe);
        logger.info("## utcDateFieldsThruToSearch");
        SystemUser su = registerSystemUser("Kiwi-UTC");
        FortressInputBean fib = new FortressInputBean("utcDateFieldsThruToSearch", false);
        fib.setTimeZone("Europe/Copenhagen"); // Arbitrary TZ
        Fortress fo = fortressService.registerFortress(fib);

        DateTimeZone ftz = DateTimeZone.forTimeZone(TimeZone.getTimeZone(fib.getTimeZone()));
        DateTimeZone utz = DateTimeZone.UTC;
        DateTimeZone ltz = DateTimeZone.getDefault();

        DateTime fortressDateCreated = new DateTime(2013, 12, 6, 4,30,DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/Copenhagen")));
        DateTime lastUpdated = new DateTime(DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/Copenhagen")));

        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", fortressDateCreated, "ABC123");
        inputBean.setLog(new LogInputBean("wally", lastUpdated, getRandomMap()));

        TrackResultBean result = mediationFacade.createHeader(su.getCompany(), inputBean); // Mock result as we're not tracking

        MetaHeader metaHeader = trackService.getHeader(result.getMetaKey());

        assertEquals("ab.monowai." + fo.getCode(), metaHeader.getIndexName());
        assertEquals("DateCreated not in Fortress TZ", 0, fortressDateCreated.compareTo(metaHeader.getFortressDateCreated()));

        TrackLog log = trackService.getLastLog(su.getCompany(), result.getMetaKey());
        assertEquals("LogDate not in Fortress TZ", 0, lastUpdated.compareTo(log.getFortressWhen(ftz)));


        waitForHeaderToUpdate(metaHeader, su.getApiKey()); // 2nd document in the index
        // We have one with a metaKey and one without
        doEsQuery("ab.monowai." + fo.getCode(), "*", 1);

        QueryParams qp = new QueryParams(fo);
        qp.setSimpleQuery("*");
        runMetaQuery(qp);
        EsSearchResult queryResults = runSearchQuery(su, qp);
        assertNotNull(queryResults);
        assertEquals(1, queryResults.getResults().size());
        for (SearchResult searchResult : queryResults.getResults()) {
            logger.info("whenCreated utc-{}", new DateTime(searchResult.getWhenCreated(), utz));
            assertEquals(fortressDateCreated, new DateTime( searchResult.getWhenCreated(), ftz));
            logger.info("whenCreated ftz-{}", new DateTime(searchResult.getWhenCreated(), ftz));
            assertEquals(new DateTime(fortressDateCreated, utz), new DateTime(searchResult.getWhenCreated(),utz));
            logger.info("lastUpdate  utc-{}", new DateTime (searchResult.getLastUpdate(), utz));
            assertEquals(lastUpdated, new DateTime( searchResult.getLastUpdate(), ftz));
            logger.info("lastUpdate  ftz-{}", new DateTime (searchResult.getLastUpdate(), ftz));
            assertEquals(new DateTime(lastUpdated, utz), new DateTime(searchResult.getLastUpdate(),utz));
            assertNotNull ( searchResult.getAbTimestamp());
            logger.info("timestamp   ltz-{}", new DateTime (searchResult.getAbTimestamp(), ltz));

        }

    }


    private EsSearchResult runSearchQuery(SystemUser su, QueryParams input) throws Exception {
        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post("/query/")
                        .header("Api-Key", su.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.getJSON(input))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        return JsonUtils.getBytesAsObject(response.getResponse().getContentAsByteArray(), EsSearchResult.class);
    }


    /**
     * Suppresses the indexing of a log record even if the fortress is set to index everything
     *
     * @throws Exception
     */
    @Test
    public void suppressIndexingOnDemand() throws Exception {
        assumeTrue(runMe);
        logger.info("## suppressIndexOnDemand");

        SystemUser su = registerSystemUser("Barbara");
        Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("suppress", false));
        MetaInputBean inputBean = new MetaInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());

        //Transaction tx = getTransaction();
        TrackResultBean indexedResult = mediationFacade.createHeader(su.getCompany(), inputBean);
        MetaHeader indexHeader = trackService.getHeader(su.getCompany(), indexedResult.getMetaKey());

        LogResultBean resultBean = mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", indexHeader.getMetaKey(), new DateTime(), getSimpleMap("who", "andy"))).getLogResult();
        junit.framework.Assert.assertNotNull(resultBean);

        waitForHeaderToUpdate(indexHeader, su.getApiKey());
        String indexName = indexHeader.getIndexName();

        doEsQuery(indexName, "andy");

        inputBean = new MetaInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        inputBean.setSearchSuppressed(true);
        TrackResultBean noIndex = mediationFacade.createHeader(su.getCompany(), inputBean);
        MetaHeader noIndexHeader = trackService.getHeader(su.getCompany(), noIndex.getMetaKey());

        mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", noIndexHeader.getMetaKey(), new DateTime(),  getSimpleMap("who", "bob")));
        // Bob's not there because we said we didn't want to index that header
        doEsQuery(indexName, "bob", 0);
        doEsQuery(indexName, "andy");
    }

    @Test
    public void tagKeyReturnsSingleSearchResult() throws Exception {
        assumeTrue(runMe);
        logger.info("## tagKeyReturnsSingleSearchResult");

        SystemUser su = registerSystemUser("Peter");
        Fortress iFortress = fortressService.registerFortress(new FortressInputBean("suppress"));
        MetaInputBean metaInput = new MetaInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        String relationshipName = "example"; // Relationship names is indexed are @tag.relationshipName.code in ES
        TagInputBean tag = new TagInputBean("Code Test Works", relationshipName);
        metaInput.addTag(tag);

        TrackResultBean indexedResult = mediationFacade.createHeader(su.getCompany(), metaInput);
        MetaHeader indexHeader = trackService.getHeader(indexedResult.getMetaKey());
        String indexName = indexHeader.getIndexName();

        Set<TrackTag> tags = trackEP.getTrackTags(indexHeader.getMetaKey(), null, null);
        assertNotNull(tags);
        assertEquals(1, tags.size());

        LogResultBean resultBean = mediationFacade.processLog(new LogInputBean("olivia@sunnybell.com", indexHeader.getMetaKey(), new DateTime(), getRandomMap())).getLogResult();
        assertNotNull(resultBean);

        waitForHeaderToUpdate(indexHeader, su.getApiKey());
        doEsTermQuery(indexName, "@tag." + relationshipName + ".code", "code test works", 1);

    }

    @Test
    public void testCancelUpdatesSearchCorrectly() throws Exception {
        assumeTrue(runMe);
        // DAT-53
        logger.info("## testCancelUpdatesSearchCorrectly");

        SystemUser su = registerSystemUser("Rocky");
        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("testCancelUpdatesSearchCorrectly", false));
        DateTime dt = new DateTime().toDateTime();
        DateTime firstDate = dt.minusDays(2);
        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "olivia@sunnybell.com", "CompanyNode", firstDate, "clb1");
        inputBean.setLog(new LogInputBean("olivia@sunnybell.com", firstDate, getSimpleMap("house", "house1")));
        String ahWP = mediationFacade.createHeader(su.getCompany(), inputBean).getMetaKey();

        MetaHeader metaHeader = trackService.getHeader(ahWP);
        waitForHeaderToUpdate(metaHeader, su.getApiKey());

        doEsTermQuery(metaHeader.getIndexName(), MetaSearchSchema.WHAT + ".house", "house1", 1); // First log

        LogResultBean secondLog = mediationFacade.processLog(new LogInputBean("isabella@sunnybell.com", metaHeader.getMetaKey(), firstDate.plusDays(1), getSimpleMap("house", "house2"))).getLogResult();
        assertNotSame(0l, secondLog.getWhatLog().getTrackLog().getFortressWhen());
        Set<TrackLog> logs = trackService.getLogs(fortress.getCompany(), metaHeader.getMetaKey());
        assertEquals(2, logs.size());
        metaHeader = trackService.getHeader(ahWP);
        waitAWhile();
        assertEquals(secondLog.getWhatLog().getTrackLog().getFortressWhen(), metaHeader.getFortressLastWhen());
        doEsTermQuery(metaHeader.getIndexName(), MetaSearchSchema.WHAT + ".house", "house2", 1); // replaced first with second

        // Test block
        mediationFacade.cancelLastLogSync(su.getCompany(), metaHeader.getMetaKey());
        logs = trackService.getLogs(fortress.getCompany(), metaHeader.getMetaKey());
        assertEquals(1, logs.size());
        metaHeader = trackService.getHeader(ahWP); // Refresh the header
        waitAWhile();
        doEsTermQuery(metaHeader.getIndexName(), MetaSearchSchema.WHAT + ".house", "house1", 1); // Cancelled, so Back to house1

        // Last change cancelled
        // DAT-96
        mediationFacade.cancelLastLogSync(su.getCompany(), metaHeader.getMetaKey());
        logs = trackService.getLogs(fortress.getCompany(), metaHeader.getMetaKey());
        junit.framework.Assert.assertTrue(logs.isEmpty());
        doEsQuery(metaHeader.getIndexName(), "*", 0);

        metaHeader = trackService.getHeader(ahWP); // Refresh the header
        assertEquals("Search Key wasn't set to null", null, metaHeader.getSearchKey());
    }

    @Test
    public void testWhatIndexingDefaultAttributeWithNGram() throws Exception {
        assumeTrue(runMe);
        logger.info("## testWhatIndexingDefaultAttributeWithNGram");
        SystemUser su = registerSystemUser("Romeo");
        Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ngram", false));
        MetaInputBean inputBean = new MetaInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        inputBean.setDescription("This is a description");

        TrackResultBean indexedResult = mediationFacade.createHeader(su.getCompany(), inputBean);
        MetaHeader indexHeader = trackService.getHeader(su.getCompany(), indexedResult.getMetaKey());

        Map<String,Object> what = getSimpleMap(MetaSearchSchema.WHAT_CODE, "AZERTY");
        what.put(MetaSearchSchema.WHAT_NAME, "NameText");
        indexHeader = mediationFacade.processLog(su.getCompany(), new LogInputBean("olivia@sunnybell.com", indexHeader.getMetaKey(), new DateTime(), what)).getMetaHeader();
        waitForHeaderToUpdate(indexHeader, su.getApiKey());

        String indexName = indexHeader.getIndexName();
        getMapping(indexName);

        // This is a description
        // 123456789012345678901

        // All text is converted to lowercase, so you have to search with lower
        doEsTermQuery(indexName, MetaSearchSchema.DESCRIPTION, "des", 1);
        doEsTermQuery(indexName, MetaSearchSchema.DESCRIPTION, "de", 0);
        doEsTermQuery(indexName, MetaSearchSchema.DESCRIPTION, "descripti", 1);
        doEsTermQuery(indexName, MetaSearchSchema.DESCRIPTION, "descriptio", 1);
        doEsTermQuery(indexName, MetaSearchSchema.DESCRIPTION, "this", 1);
        // ToDo: Figure out ngram details
        //doEsTermQuery(indexName, MetaSearchSchema.DESCRIPTION, "this is a description", 0);


    }

    private SystemUser registerSystemUser(String loginToCreate) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(AUTH_MIKE);
        SystemUser su = regService.registerSystemUser(new RegistrationBean(company, loginToCreate));
        // creating company alters the schema that sometimes throws a heuristic exception.
        Thread.sleep(600);
        Thread.yield();
        return su;
    }

    @Test
    public void stressWithHighVolume() throws Exception {
        assumeTrue(false);// Suppressing this for the time being
        logger.info("## stressWithHighVolume");
        int auditMax = 10, logMax = 10, fortress = 1;

        for (int i = 1; i < fortressMax + 1; i++) {
            deleteEsIndex("ab.monowai.bulkloada" + i);
            doEsQuery("ab.monowai.bulkloada" + i, "*", -1);
        }

        waitAWhile("Wait {} secs for index to delete ");

        SystemUser su = registerSystemUser("Gina");

        ArrayList<Long> list = new ArrayList<>();

        logger.info("FortressCount: " + fortressMax + " AuditCount: " + auditMax + " LogCount: " + logMax);
        logger.info("We will be expecting a total of " + (auditMax * logMax * fortressMax) + " messages to be handled");

        StopWatch watch = new StopWatch();
        long totalRows = 0;

        DecimalFormat f = new DecimalFormat("##.000");

        watch.start();
        while (fortress <= fortressMax) {

            String fortressName = "bulkloada" + fortress;
            StopWatch fortressWatch = new StopWatch();
            fortressWatch.start();
            int audit = 1;
            long requests = 0;

            Fortress iFortress = fortressService.registerFortress(new FortressInputBean(fortressName, false));
            requests++;
            logger.info("Starting run for " + fortressName);
            while (audit <= auditMax) {
                boolean searchChecked = false;
                MetaInputBean aib = new MetaInputBean(iFortress.getName(), fortress + "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC" + audit);
                TrackResultBean arb = mediationFacade.createHeader(su.getCompany(), aib);
                String metaKey = arb.getMetaHeader().getMetaKey();
                requests++;
                int log = 1;
                while (log <= logMax) {
                    Thread.yield();
                    createLog(metaKey, log);
                    Thread.yield(); // Failure to yield Getting a frustrating thread update problem causing
//                    IllegalStateException( "Unable to delete relationship since it is already deleted."
                    // under specifically stressed situations like this. We need to be able to detect and recover
                    // from the scenario
                    requests++;
                    if (!searchChecked) {
                        searchChecked = true;
                        requests++;
                        watch.suspend();
                        fortressWatch.suspend();
                        waitForHeaderToUpdate(metaKey, su.getApiKey());
                        watch.resume();
                        fortressWatch.resume();
                    } // searchCheck done
                    log++;
                } // Logs created
                audit++;
            } // Audit headers finished with
            fortressWatch.stop();
            double fortressRunTime = (fortressWatch.getTime()) / 1000d;
            logger.info("*** {} took {}  [{}] Avg processing time= {}. Requests per second {}",
                    iFortress.getName(),
                    fortressRunTime,
                    requests,
                    f.format(fortressRunTime / requests),
                    f.format(requests / fortressRunTime));
            watch.split();
            //splitTotals = splitTotals + fortressRunTime;
            totalRows = totalRows + requests;
            list.add(iFortress.getId());
            fortress++;
        }
        watch.stop();
        double totalTime = watch.getTime() / 1000d;
        logger.info("*** Processed {} requests. Data sets created in {} secs. Fortress avg = {} avg requests per second {}",
                totalRows,
                f.format(totalTime),
                f.format(totalTime / fortressMax),
                f.format(totalRows / totalTime));

        validateLogsIndexed(list, auditMax, logMax);
        doSearchTests(auditMax, list);
    }

    @Test
    public void simpleQueryEPWorksForImportedRecord() throws Exception {
        assumeTrue(runMe);
        String searchFor = "testing";

        SystemUser su = registerSystemUser("Nik");

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("TestFortress", false));

        LogInputBean log = new LogInputBean("mikeTest", new DateTime(), getSimpleMap("who", searchFor));
        MetaInputBean input = new MetaInputBean("TestFortress", "mikeTest", "Query", new DateTime(), "abzz");
        input.setLog(log);

        TrackResultBean result = trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody();
        waitForHeaderToUpdate(result.getMetaHeader().getMetaKey(), su.getApiKey());


        QueryParams q = new QueryParams(fortress).setSimpleQuery(searchFor);
        doEsQuery("ab.*", searchFor, 1);

        String qResult = runQuery(q);
        assertNotNull(qResult);
        assertTrue("Couldn't find a hit in the result [" + result + "]", qResult.contains("total\" : 1"));

    }

    @Test
    public void utfText() throws Exception {
        assumeTrue(runMe);
        Map<String,Object> json = getSimpleMap("Athlete", "Katerina Neumannová");
        SystemUser su = registerSystemUser("Utf8");

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("UTF8-Test", false));

        LogInputBean log = new LogInputBean("mikeTest", new DateTime(), json);
        MetaInputBean input = new MetaInputBean(fortress.getName(), "mikeTest", "Query", new DateTime(), "abzz");
        input.setLog(log);

        TrackResultBean result = trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody();
        waitForHeaderToUpdate(result.getMetaHeader(), su.getApiKey());
        doEsQuery(result.getMetaHeader().getIndexName(), json.get("Athlete").toString(), 1);

    }

    private String runQuery(QueryParams queryParams) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHeaders(null, "mike", "123");
        HttpEntity<QueryParams> requestEntity = new HttpEntity<>(queryParams, httpHeaders);

        try {
            return restTemplate.exchange("http://localhost:9081/ab-search/v1/query/", HttpMethod.POST, requestEntity, String.class).getBody();
        } catch (HttpClientErrorException e) {
            logger.error("AB Client Audit error {}", e.getMessage());
        } catch (HttpServerErrorException e) {
            logger.error("AB Server Audit error {}", e.getMessage());

        }
        return null;
    }

    private String runMetaQuery(QueryParams queryParams) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = getHeaders(null, "mike", "123");
        HttpEntity<QueryParams> requestEntity = new HttpEntity<>(queryParams, httpHeaders);

        try {
            return restTemplate.exchange("http://localhost:9081/ab-search/v1/query/metaKeys", HttpMethod.POST, requestEntity, String.class).getBody();
        } catch (HttpClientErrorException e) {
            logger.error("AB Client Audit error {}", e.getMessage());
        } catch (HttpServerErrorException e) {
            logger.error("AB Server Audit error {}", e.getMessage());

        }
        return null;
    }

    private TrackResultBean createLog(String metaKey, int log) throws Exception {
        return mediationFacade.processLog(new LogInputBean("olivia@sunnybell.com", metaKey, new DateTime(), getSimpleMap("who", log)));
    }

    private void validateLogsIndexed(ArrayList<Long> list, int auditMax, int expectedLogCount) throws Exception {
        logger.info("Validating logs are indexed");
        int fortress = 0;
        int audit = 1;
        //DecimalFormat f = new DecimalFormat("##.000");
        while (fortress < fortressMax) {
            while (audit <= auditMax) {
                MetaHeader header = trackService.findByCallerRefFull(list.get(fortress), "CompanyNode", "ABC" + audit);
                Set<TrackLog> logs = trackService.getLogs(header.getId());
                assertNotNull(logs);
                assertEquals("Wrong number of logs returned", expectedLogCount, logs.size());
                for (TrackLog log : logs) {
                    assertEquals("logId [" + log.getId() + "] changeId[" + log.getLog().getId() + "], event[ " + log.getLog().getEvent() + "]", true, log.isIndexed());
                }

                audit++;
            } // Audit headers finished with
            fortress++;
        }

    }

    private long waitForHeaderToUpdate(MetaHeader metaHeader, String apiKey) throws Exception {
        return waitForHeaderToUpdate(metaHeader.getMetaKey(), apiKey);
    }

    private long waitForHeaderToUpdate(String metaKey, String apiKey) throws Exception {
        // Looking for the first searchKey to be logged against the metaHeader
        long thenTime = System.currentTimeMillis();
        int i = 0;

        MetaHeader metaHeader = trackEP.getMetaHeader(metaKey, apiKey, apiKey).getBody();
        if (metaHeader.getSearchKey() != null)
            return 0;

        int timeout = 100;
        while (metaHeader.getSearchKey() == null && i <= timeout) {
            metaHeader = trackEP.getMetaHeader(metaKey, apiKey, apiKey).getBody();
            Thread.yield();
            if (i > 20)
                waitAWhile("Sleeping for the header to update {}");
            i++;
        }
        if (i > 22)
            logger.info("Wait for search got to [{}] for metaId [{}]", i, metaHeader.getId());
        boolean searchWorking = metaHeader.getSearchKey() != null;
        assertTrue("Search reply not received from ab-search", searchWorking);
        return System.currentTimeMillis() - thenTime;
    }

    private void doSearchTests(int auditCount, ArrayList<Long> list) throws Exception {
        int fortress;
        int searchLoops = 200;
        int search = 0;
        int totalSearchRequests = 0;
        logger.info("Performing Search Tests for {} fortresses", fortressMax);
        StopWatch watch = new StopWatch();
        watch.start();

        do {
            fortress = 0;
            do {
                int x = 1;
                do {
                    int random = (int) (Math.random() * ((auditCount) + 1));
                    if (random == 0)
                        random = 1;

                    MetaHeader header = trackService.findByCallerRefFull(list.get(fortress), "CompanyNode", "ABC" + random);
                    assertNotNull("ABC" + random, header);
                    assertNotNull("Looks like ab-search is not sending back results", header.getSearchKey());
                    TrackLog trackLog = trackService.getLastLog(header);
                    assertNotNull(trackLog);

                    assertTrue("fortress " + fortress + " run " + x + " header " + header.getMetaKey() + " - " + trackLog.getId(), trackLog.isIndexed());
                    String result = doEsTermQuery(header.getIndexName(), MetaSearchSchema.META_KEY, header.getMetaKey(), 1, true);
                    totalSearchRequests++;
                    validateResultFieds(result);

                    x++;
                } while (x < auditCount);
                fortress++;
            } while (fortress < fortressMax);
            search++;
        } while (search < searchLoops);

        watch.stop();
        double end = watch.getTime() / 1000d;
        logger.info("Total Search Requests = " + totalSearchRequests + ". Total time for searches " + end + " avg requests per second = " + totalSearchRequests / end);
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    private void validateResultFieds(String result) throws Exception {
        JsonNode node = objectMapper.readTree(result);

        assertNotNull(node.get(MetaSearchSchema.CREATED));
        assertNotNull(node.get(MetaSearchSchema.WHO));
        assertNotNull(node.get(MetaSearchSchema.WHEN));
        assertNotNull(node.get(MetaSearchSchema.META_KEY));
        assertNotNull(node.get(MetaSearchSchema.DOC_TYPE));
        assertNotNull(node.get(MetaSearchSchema.FORTRESS));

    }

    private String doEsQuery(String index, String queryString) throws Exception {
        return doEsQuery(index, queryString, 1);
    }

    private String doEsQuery(String index, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given AuditKey.
        // Let's assert that
        //waitAWhile();
        int runCount = 0, nbrResult;
        JestResult jResult;
        do {
            if (runCount > 0)
                waitAWhile("Sleep {} for ES Query to work");
            String query = "{\n" +
                    "    query: {\n" +
                    "          query_string : {\n" +
                    "              \"query\" : \"" + queryString + "\"" +
                    "           }\n" +
                    "      }\n" +
                    "}";

            //
            Search search = new Search.Builder(query)
                    .addIndex(index)
                    .build();

            jResult = esClient.execute(search);
            assertNotNull(jResult);
            if (expectedHitCount == -1) {
                assertEquals("Expected the index [" + index + "] to be deleted but message was [" + jResult.getErrorMessage() + "]", true, jResult.getErrorMessage().contains("IndexMissingException"));
                logger.debug("Confirmed index {} was deleted and empty", index);
                return null;
            }
            if (jResult.getErrorMessage() == null) {
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject());
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else {
                nbrResult = 0;// Index has not yet been created in ElasticSearch, we'll try again
            }
            runCount++;
        } while (nbrResult != expectedHitCount && runCount < esTimeout);
        logger.debug("ran ES query - result count {}, runCount {}", nbrResult, runCount);

        junit.framework.Assert.assertNotNull(jResult);
        Assert.assertEquals(index + "\r\n" + jResult.getJsonString(), expectedHitCount, nbrResult);
        return jResult.getJsonString();

        //return result.getJsonString();
    }

    private String getMapping(String indexName) throws Exception {
        GetMapping mapping = new GetMapping.Builder()
                .addIndex(indexName)
                .build();

        JestResult jResult = esClient.execute(mapping);
        return jResult.getJsonString();
    }

    private String doEsTermQuery(String indexName, String metaKey, String metaKey1, int i) throws Exception {
        return doEsTermQuery(indexName, metaKey, metaKey1, i, false);
    }

    private String doEsTermQuery(String index, String field, String queryString, int expectedHitCount, boolean supressLog) throws Exception {
        // There should only ever be one document for a given AuditKey.
        // Let's assert that
        int runCount = 0, nbrResult;
        JestResult jResult;

        do {
            if (runCount > 0)
                waitAWhile("Sleep {} for ES Query to work");
            runCount++;
            String query = "{\n" +
                    "    query: {\n" +
                    "          term : {\n" +
                    "              \"" + field + "\" : \"" + queryString + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex(index)
                    .build();

            jResult = esClient.execute(search);
            String message = index + " - " + field + " - " + queryString + (jResult == null ? "[noresult]" : "\r\n" + jResult.getJsonString());
            assertNotNull(message, jResult);
            if (jResult.getErrorMessage() == null) {
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject());
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else
                nbrResult = 0;// Index has not yet been created in ElasticSearch, we'll try again

        } while (nbrResult != expectedHitCount && runCount < esTimeout);

        if (!supressLog) {
            logger.debug("ran ES Term Query - result count {}, runCount {}", nbrResult, runCount);
            logger.trace("searching index [{}] field [{}] for [{}]", index, field, queryString);
        }
        Assert.assertEquals(jResult.getJsonString(), expectedHitCount, nbrResult);
        if (nbrResult != 0) {
            return jResult.getJsonObject()
                    .getAsJsonObject("hits")
                    .getAsJsonArray("hits")
                    .getAsJsonArray()
                    .iterator()
                    .next()
                    .getAsJsonObject().get("_source").toString();
        } else {
            return null;
        }
    }

    /**
     * Use this carefully. Due to ranked search results, you can get more results than you expect. If
     * you are looking for an exact match then consider doEsTermQuery
     *
     * @param index            to search
     * @param field            field containing queryString
     * @param queryString      text to search for
     * @param expectedHitCount result count
     * @return query _source
     * @throws Exception if expectedHitCount != actual hit count
     */
    private String doEsFieldQuery(String index, String field, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given AuditKey.
        // Let's assert that
        int runCount = 0, nbrResult;

        JestResult result;
        do {
            if (runCount > 0)
                waitAWhile("Sleep {} for ES Query to work");

            runCount++;
            String query = "{\n" +
                    "    query: {\n" +
                    "          query_string : {\n" +
                    "              \"default_field\" : \"" + field + "\",\n" +
                    "              \"query\" : \"" + queryString + "\"\n" +
                    "           }\n" +
                    "      }\n" +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex(index)
                    .build();

            result = esClient.execute(search);
            String message = index + " - " + field + " - " + queryString + (result == null ? "[noresult]" : "\r\n" + result.getJsonString());
            assertNotNull(message, result);
            assertNotNull(message, result.getJsonObject());
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits"));
            assertNotNull(message, result.getJsonObject().getAsJsonObject("hits").get("total"));
            nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        } while (nbrResult != expectedHitCount && runCount < esTimeout);

        logger.debug("ran ES Field Query - result count {}, runCount {}", nbrResult, runCount);
        Assert.assertEquals("Unexpected hit count searching '" + index + "' for {" + queryString + "} in field {" + field + "}", expectedHitCount, nbrResult);
        return result.getJsonObject()
                .getAsJsonObject("hits")
                .getAsJsonArray("hits")
                .getAsJsonArray()
                .iterator()
                .next()
                .getAsJsonObject().get("_source").toString();
    }

    public static HttpHeaders getHeaders(final String apiKey, final String username, final String password) {

        return new HttpHeaders() {
            {
                if (username != null && password != null) {
                    String auth = username + ":" + password;
                    byte[] encodedAuth = Base64.encodeBase64(
                            auth.getBytes(Charset.forName("US-ASCII")));
                    String authHeader = "Basic " + new String(encodedAuth);
                    set("Authorization", authHeader);
                } else if (apiKey != null)
                    set("Api-Key", apiKey);
                setContentType(MediaType.APPLICATION_JSON);
                set("charset", "UTF-8");
            }
        };

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
        logger.debug(message, milliseconds / 1000d);
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
            if (log != null && metaHeader.getFortressLastWhen() == ts)
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
    public static Map<String, Object> getSimpleMap(String key, Object value){
        Map<String, Object> result = new HashMap<>();
        result.put(key, value);
        return result;
    }

    public static Map<String, Object> getRandomMap(){
        return getSimpleMap("Key", "Test"+System.currentTimeMillis());
    }

    public static Map<String, Object> getBigJsonText(int i) {
        Map<String, Object> map = getSimpleMap("Key", "Random");
        int count = 0;
        do {
            map.put("Key"+count, "Now is the time for all good men to come to the aid of the party");
            count++;
        } while ( count < i);
        return map;
    }

}
