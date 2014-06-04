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

import com.auditbucket.engine.endpoint.TrackEP;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.engine.service.TrackService;
import com.auditbucket.engine.service.WhatService;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.search.model.MetaSearchSchema;
import com.auditbucket.search.model.QueryParams;
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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateTime;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 *
 * Allows the ab-engine services to be tested against ab-search.
 * ab-search is stated by Cargo as a Tomcat server.
 * ab-engine runs as a usual Spring test runner.
 * This approach means that RabbitMQ has to be installed to allow integration to occur as
 * no web interface is launched for ab-engine
 *
 * User: nabil
 * Date: 16/07/13
 * Time: 22:51
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
public class TestAuditIntegration {
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
    FortressService fortressService;
    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    WhatService whatService;

    private static Logger logger = LoggerFactory.getLogger(TestAuditIntegration.class);
    private static Authentication authA = new UsernamePasswordAuthenticationToken("mike", "123");

    String company = "Monowai";
    static Properties properties = new Properties();

    @AfterClass
    public static void waitAWhile() throws Exception {
        waitAWhile(null, 3000);
    }
    public static void waitAWhile(String message) throws Exception {
        String ss = System.getProperty("sleepSeconds");
        if ( ss==null || ss.equals(""))
            ss = "1";
        if ( message == null )
            message = "Slept for {} seconds";
        waitAWhile(message, Long.decode(ss) * 1000);
    }

    @BeforeClass
    @Rollback(false)
    public static void cleanupElasticSearch() throws Exception {
        FileInputStream f = new FileInputStream("./src/test/resources/config.properties");
        properties.load(f);
        String abDebug = System.getProperty("ab.debug");
        if ( abDebug!= null )
            runMe= !Boolean.parseBoolean(abDebug);

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
        SecurityContextHolder.getContext().setAuthentication(authA);
    }

    private static void deleteEsIndex(String indexName) throws Exception {
        logger.info ("%% Delete Index {}", indexName);
        esClient.execute(new DeleteIndex.Builder(indexName).build());
    }

    @AfterClass
    public static void shutDownElasticSearch() throws Exception {
        esClient.shutdownClient();
    }

    private boolean defaultAuthUser;

    /**
     * A lot of calls in this class assume that the BasicAuth user can create and read data
     * In order for the authenticated user to create data they have to belong to the company.
     * By default the secured user does not belong, so we set that up here
     *
     * @throws Exception
     */
    @Before
    public void secureSystemUserCanWriteData() throws Exception{
        if (!defaultAuthUser){
            defaultAuthUser = true;
            regService.registerSystemUser(new RegistrationBean(company, "mike").setIsUnique(false));
            waitAWhile("Registering Auth user System Access {}");

        }

    }

    @Test
    public void companyAndFortressWithSpaces() throws Exception {
        assumeTrue(runMe);
        logger.info("## companyAndFortressWithSpaces");

        SystemUser su = registerSystemUser("co-fortress");
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("Audit Test", false));
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        MetaHeader header = mediationFacade.createHeader(inputBean, null).getMetaHeader();
        String ahKey = header.getMetaKey();
        assertNotNull(ahKey);
        header = trackService.getHeader(ahKey);
        assertEquals("ab.monowai.audittest", header.getIndexName());
        mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + 1 + "}"));
        waitForHeaderToUpdate(header, su.getApiKey());

        doEsQuery(header.getIndexName(), header.getMetaKey());
    }

    @Test
    public void headerWithTagsProcess() throws Exception {
        assumeTrue(runMe);
        logger.info("## headersWithTagsProcess");
        SecurityContextHolder.getContext().setAuthentication(authA);
        SystemUser su = registerSystemUser("Mark");
        String apiKey = su.getApiKey();
        Fortress fo = fortressService.registerFortress(new FortressInputBean("headerWithTagsProcess", false));
        DateTime now = new DateTime();
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", now, "ZZZ123");
        inputBean.setIsMetaOnly(true);
        inputBean.setTag(new TagInputBean("testTagNameZZ", "someAuditRLX"));
        inputBean.setEvent("TagTest");
        TrackResultBean auditResult;
        auditResult = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody();
        waitForHeaderToUpdate(auditResult.getMetaHeader(), su.getApiKey());
        TrackedSummaryBean summary = trackEP.getAuditSummary(auditResult.getMetaKey(), apiKey, apiKey).getBody();
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
        inputBean.setIsMetaOnly(true); // Must be true to make over to search
        TrackResultBean auditResult;
        auditResult = mediationFacade.createHeader(inputBean, su.getApiKey());
        waitForHeaderToUpdate(auditResult.getMetaHeader(), su.getApiKey());
        TrackedSummaryBean summary = mediationFacade.getTrackedSummary(auditResult.getMetaKey());
        assertNotNull(summary);
        assertSame("change logs were not expected", 0, summary.getChanges().size());
        assertNotNull("Search record not received", summary.getHeader().getSearchKey());
        // Check we can find the Event in ElasticSearch
        doEsQuery(summary.getHeader().getIndexName(), inputBean.getEvent(), 1);

        // Not flagged as meta only so will not appear in the search index until a log is created
        inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", now, "ZZZ999");
        auditResult = mediationFacade.createHeader(inputBean, null);
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
        inputBean.setLog(new LogInputBean("wally", new DateTime(), "{\"blah\":1}"));
        TrackResultBean auditResult = mediationFacade.createHeader(inputBean, null);

        MetaHeader metaHeader = trackService.getHeader(auditResult.getMetaKey());
        waitForHeaderToUpdate(metaHeader, su.getApiKey());
        assertEquals("ab.monowai.rebuildtest", metaHeader.getIndexName());

        doEsQuery(metaHeader.getIndexName(), "*");
        deleteEsIndex(metaHeader.getIndexName());

        // Rebuild....
        Future<Long> fResult = mediationFacade.reindex(fo.getCompany(), fo.getCode());
        waitForHeaderToUpdate(metaHeader, su.getApiKey());
        Assert.assertEquals(1l, fResult.get().longValue());
        doEsQuery(metaHeader.getIndexName(), "*");

    }

    @Test
    public void createHeaderTimeLogsWithSearchActivated() throws Exception {
        assumeTrue(runMe);
        logger.info("## createHeaderTimeLogsWithSearchActivated");
        deleteEsIndex("ab.monowai.111");
        int max = 3;
        String ahKey;
        registerSystemUser("Olivia");
        Fortress fo = fortressService.registerFortress(new FortressInputBean("111", false));

        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        TrackResultBean auditResult;
        auditResult = mediationFacade.createHeader(inputBean, null);
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
            mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + i + "}"));
            i++;
        }
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
        mediationFacade.createHeader(inputBean, su.getApiKey());

        String indexName = MetaSearchSchema.parseIndex(fortress);
        assertEquals("ab.monowai.trackgraph", indexName);

        // Putting asserts On elasticsearch
        doEsQuery(indexName, "*", 1);
        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setTrackSuppressed(true);
        inputBean.setMetaOnly(true);
        mediationFacade.createHeader(inputBean, null);
        doEsQuery(indexName, "*", 2);

        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setTrackSuppressed(true);
        inputBean.setMetaOnly(true);
        MetaHeader header = mediationFacade.createHeader(inputBean, null).getMetaHeader();
        Assert.assertNull(header.getMetaKey());
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(indexName, "*", 2);

        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setTrackSuppressed(true);
        inputBean.setMetaOnly(true);
        mediationFacade.createHeader(inputBean, null);
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(indexName, "*", 2);

        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC125");
        inputBean.setTrackSuppressed(true);
        inputBean.setMetaOnly(true);
        mediationFacade.createHeader(inputBean, null);
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(indexName, "*", 3);

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
        inputBean.setLog(new LogInputBean("wally", new DateTime(), "{\"blah\":124}"));
        mediationFacade.createHeader(inputBean, null); // Mock result as we're not tracking

        inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setLog(new LogInputBean("wally", new DateTime(), "{\"blah\":124}"));
        TrackResultBean auditResult = mediationFacade.createHeader(inputBean, null);
        MetaHeader metaHeader = trackService.getHeader(auditResult.getMetaKey());
        assertEquals("ab.monowai.nometakey", metaHeader.getIndexName());

        waitForHeaderToUpdate(metaHeader, su.getApiKey()); // 2nd document in the index
        // We have one with a metaKey and one without
        doEsQuery("ab.monowai.nometakey", "*", 2);

        QueryParams qp = new QueryParams(fo);
        qp.setSimpleQuery("*");
        String queryResult = runMetaQuery(qp);
        logger.info(queryResult);

        // Two search docs,but one without a metaKey

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
        String escJson = "{\"who\":";
        SystemUser su = registerSystemUser("Barbara");
        Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("suppress", false));
        MetaInputBean inputBean = new MetaInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());

        //Transaction tx = getTransaction();
        TrackResultBean indexedResult = mediationFacade.createHeader(inputBean, su.getApiKey());
        MetaHeader indexHeader = trackService.getHeader(su.getCompany(), indexedResult.getMetaKey());

        LogResultBean resultBean = mediationFacade.processLog(su.getCompany(), new LogInputBean(indexHeader.getMetaKey(), "olivia@sunnybell.com", new DateTime(), escJson + "\"andy\"}"));
        junit.framework.Assert.assertNotNull(resultBean);

        waitForHeaderToUpdate(indexHeader, su.getApiKey());
        String indexName = indexHeader.getIndexName();

        doEsQuery(indexName, "andy");

        inputBean = new MetaInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        inputBean.setSearchSuppressed(true);
        TrackResultBean noIndex = mediationFacade.createHeader(inputBean, su.getApiKey());
        MetaHeader noIndexHeader = trackService.getHeader(su.getCompany(), noIndex.getMetaKey());

        mediationFacade.processLog(su.getCompany(), new LogInputBean(noIndexHeader.getMetaKey(), "olivia@sunnybell.com", new DateTime(), escJson + "\"bob\"}"));
        // Bob's not there because we said we didn't want to index that header
        doEsQuery(indexName, "bob", 0);
        doEsQuery(indexName, "andy");
    }

    @Test
    public void tagKeyReturnsSingleSearchResult() throws Exception {
        assumeTrue(runMe);
        logger.info("## tagKeyReturnsSingleSearchResult");
        String escJson = "{\"who\":";
        SystemUser su = registerSystemUser("Peter");
        Fortress iFortress = fortressService.registerFortress(new FortressInputBean("suppress"));
        MetaInputBean metaInput = new MetaInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        String relationshipName = "example"; // Relationship names is indexed are @tag.relationshipName.key in ES
        TagInputBean tag = new TagInputBean("Key Test Works", relationshipName);
        metaInput.setTag(tag);

        TrackResultBean indexedResult = mediationFacade.createHeader(metaInput, null);
        MetaHeader indexHeader = trackService.getHeader(indexedResult.getMetaKey());
        String indexName = indexHeader.getIndexName();
//        doEsFieldQuery(indexName, "@tag." + relationshipName + ".key", "keytestworks", 1);

        Set<TrackTag> tags = trackEP.getAuditTags(indexHeader.getMetaKey(), null, null).getBody();
        assertNotNull(tags);
        assertEquals(1, tags.size());

        LogResultBean resultBean = mediationFacade.processLog(new LogInputBean(indexHeader.getMetaKey(), "olivia@sunnybell.com", new DateTime(), escJson + "\"andy\"}"));
        assertNotNull(resultBean);

        waitForHeaderToUpdate(indexHeader, su.getApiKey());
        doEsTermQuery(indexName, "@tag." + relationshipName + ".key", "keytestworks", 1);

    }

    @Test
    public void testWhatIndexingDefaultAttributeWithNGram() throws Exception {
        assumeTrue(runMe);
        logger.info("## testWhatIndexingDefaultAttributeWithNGram");
        SystemUser su = registerSystemUser("Romeo");
        Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("ngram", false));
        MetaInputBean inputBean = new MetaInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());

        TrackResultBean indexedResult = mediationFacade.createHeader(inputBean, su.getApiKey());
        MetaHeader indexHeader = trackService.getHeader(su.getCompany(), indexedResult.getMetaKey());
        String what = "{\"code\":\"AZERTY\",\"name\":\"NameText\",\"description\":\"this is a description\"}";
        mediationFacade.processLog(su.getCompany(), new LogInputBean(indexHeader.getMetaKey(), "olivia@sunnybell.com", new DateTime(), what));
        waitForHeaderToUpdate(indexHeader, su.getApiKey());
        String indexName = indexHeader.getIndexName();

        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "des", 1);
        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "de", 0);
        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "descripti", 1);
        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "descriptio", 1);
        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "description", 0);
        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "is is a de", 1);
        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_DESCRIPTION, "is is a des", 0);

        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_NAME, "Name", 1);
        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_NAME, "Nam", 1);
        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_NAME, "NameText", 1);

        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_CODE, "AZ", 1);
        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_CODE, "AZER", 1);
        doEsTermQuery(indexName, MetaSearchSchema.WHAT + "." + MetaSearchSchema.WHAT_CODE, "AZERTY", 0);

    }

    private SystemUser registerSystemUser(String loginToCreate) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authA);
        SystemUser su = regService.registerSystemUser(new RegistrationBean(company, loginToCreate));
        // creating company alters the schema that sometimes throws a heuristic exception.
        Thread.sleep(600);
        Thread.yield();
        return su;
    }

    @Test
    public void stressWithHighVolume() throws Exception {
        assumeTrue(runMe);
        logger.info("## stressWithHighVolume");
        int auditMax = 10, logMax = 10, fortress = 1;

        for (int i = 1; i < fortressMax + 1; i++) {
            deleteEsIndex("ab.monowai.bulkloada" + i);
            doEsQuery("ab.monowai.bulkloada"+i, "*", -1);
        }

        waitAWhile("Wait {} secs for index to delete ");

        SystemUser su = registerSystemUser("Gina");

        String simpleJson = "{\"who\":";
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
                TrackResultBean arb = mediationFacade.createHeader(aib, null);
                requests++;
                int log = 1;
                while (log <= logMax) {
                    createLog(simpleJson, arb, log);
                    requests++;
                    if (!searchChecked) {
                        searchChecked = true;
                        MetaHeader metaHeader = trackService.getHeader(arb.getMetaKey());
                        requests++;
                        watch.suspend();
                        fortressWatch.suspend();
                        waitForHeaderToUpdate(metaHeader, su.getApiKey());
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
        double totalTime = watch.getTime()/1000d;
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
        String escJson = "{\"who\":\"" + searchFor + "\"}";

//        RestTemplate restTemplate = new RestTemplate();
//        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
//        AbRestClient restClient = new AbRestClient("http://localhost:9081/", "mike", "123", 1);
//        assertEquals("Pong!", restClient.ping());

        SystemUser su = registerSystemUser("Nik");

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("TestFortress", false));

        LogInputBean log = new LogInputBean("mikeTest", new DateTime(), escJson);
        MetaInputBean input = new MetaInputBean("TestFortress", "mikeTest", "Query", new DateTime(), "abzz");
        input.setLog(log);

        TrackResultBean result = trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody();
        waitForHeaderToUpdate(result.getMetaHeader(), su.getApiKey());


        QueryParams q = new QueryParams(fortress).setSimpleQuery(searchFor);
        doEsQuery("ab.*", searchFor, 1);

        String qResult = runQuery(q);
        assertNotNull(qResult);
        assertTrue("Couldn't find a hit in the result [" + result + "]", qResult.contains("total\" : 1"));

    }

    @Test
    public void utfText()throws Exception{
        assumeTrue(runMe);
        String json = "{\"Athlete\":\"Katerina Neumannová\",\"Age\":\"28\",\"Country\":\"Czech Republic\",\"Year\":\"2002\",\"Closing Ceremony Date\":\"2/24/02\",\"Sport\":\"Cross Country Skiing\",\"Gold Medals\":\"0\",\"Silver Medals\":\"2\",\"Bronze Medals\":\"0\",\"Total Medals\":\"2\"}";
        SystemUser su = registerSystemUser("Utf8");

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("UTF8-Test", false));

        LogInputBean log = new LogInputBean("mikeTest", new DateTime(), json);
        MetaInputBean input = new MetaInputBean(fortress.getName(), "mikeTest", "Query", new DateTime(), "abzz");
        input.setLog(log);

        TrackResultBean result = trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody();
        waitForHeaderToUpdate(result.getMetaHeader(), su.getApiKey());
        doEsQuery(result.getMetaHeader().getIndexName(), "Neumannová", 1);

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
    private void createLog(String simpleJson, TrackResultBean arb, int log) throws DatagioException, IOException {
        mediationFacade.processLog(new LogInputBean(arb.getMetaKey(), "olivia@sunnybell.com", new DateTime(), simpleJson + log + "}"));
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

    private long waitForHeaderToUpdate(MetaHeader header, String apiKey) throws Exception {
        // Looking for the first searchKey to be logged against the metaHeader
        long thenTime = System.currentTimeMillis();
        int i = 0;
        int timeout = 300;

        MetaHeader metaHeader = trackEP.getMetaHeader(header.getMetaKey(), apiKey, apiKey).getBody();
        if (metaHeader.getSearchKey() != null)
            return 0;
        while (metaHeader.getSearchKey() == null && i <= timeout) {
            metaHeader = trackEP.getMetaHeader(header.getMetaKey(), apiKey, apiKey).getBody();
            Thread.yield();
            if (i > 20)
                waitAWhile("Sleeping for the header to update {}");
            i++;
        }
        if (i > 20)
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
        logger.info ("Performing Search Tests for {} fortresses", fortressMax);
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
    int esTimeout = 10; // Max attempts to find the result in ES
    private String doEsQuery(String index, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given AuditKey.
        // Let's assert that
        //waitAWhile();
        int runCount = 0, nbrResult ;
        JestResult jResult ;
        do {
            if (runCount >0)
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
            if ( expectedHitCount == -1 ){
                assertEquals("Expected the index [" + index + "] to be deleted but message was [" + jResult.getErrorMessage() + "]", true, jResult.getErrorMessage().contains("IndexMissingException"));
                logger.debug("Confirmed index {} was deleted and empty", index);
                return null;
            }
            if (jResult.getErrorMessage()==null ){
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject());
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else {
                nbrResult =0;// Index has not yet been created in ElasticSearch, we'll try again
            }
            runCount ++;
        } while ( nbrResult != expectedHitCount && runCount < esTimeout );
        logger.debug("ran ES query - result count {}, runCount {}", nbrResult, runCount);

        junit.framework.Assert.assertNotNull(jResult);
        Assert.assertEquals(index + "\r\n" + jResult.getJsonString(), expectedHitCount, nbrResult);
        return null;

        //return result.getJsonString();
    }

    private String doEsTermQuery(String indexName, String metaKey, String metaKey1, int i ) throws Exception{
        return doEsTermQuery(indexName, metaKey, metaKey1, i, false);
    }

    private String doEsTermQuery(String index, String field, String queryString, int expectedHitCount, boolean supressLog) throws Exception {
        // There should only ever be one document for a given AuditKey.
        // Let's assert that
        int runCount = 0, nbrResult ;
        JestResult jResult;

        do{
            if (runCount >0)
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
            if (jResult.getErrorMessage()==null ){
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject());
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits"));
                assertNotNull(jResult.getErrorMessage(), jResult.getJsonObject().getAsJsonObject("hits").get("total"));
                nbrResult = jResult.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            } else
                nbrResult =0;// Index has not yet been created in ElasticSearch, we'll try again

        } while ( nbrResult != expectedHitCount && runCount < esTimeout );

        if ( !supressLog ){
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
     * @param index     to search
     * @param field     field containing queryString
     * @param queryString text to search for
     * @param expectedHitCount result count
     * @return query _source
     * @throws Exception if expectedHitCount != actual hit count
     */
    private String doEsFieldQuery(String index, String field, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given AuditKey.
        // Let's assert that
        int runCount = 0, nbrResult ;

        JestResult result;
        do{
            if (runCount >0)
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
        } while ( nbrResult != expectedHitCount && runCount < esTimeout );

        logger.debug("ran ES Field Query - result count {}, runCount {}", nbrResult, runCount);
        Assert.assertEquals("Unexpected hit count searching '" + index + "' for {" + queryString + "} in field {" + field+ "}", expectedHitCount, nbrResult);
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
                } else if ( apiKey != null )
                    set("Api-Key", apiKey);
                setContentType(MediaType.APPLICATION_JSON);
                set("charset", "UTF-8");
            }
        };

    }

    /**
     *
     * Processing delay for threads and integration to complete. If you start getting sporadic
     * Heuristic exceptions, chances are you need to call this routine to give other threads
     * time to commit their work.
     * Likewise, waiting for results from ab-search can take a while. We can't know how long this
     * is so you can experiment on your own environment by passing in -DsleepSeconds=1
     *
     * @param milliseconds to pause for
     *
     * @throws Exception
     */
    public static void waitAWhile(String message, long milliseconds) throws Exception {
        Thread.sleep(milliseconds);
        logger.trace(message, milliseconds / 1000d);
    }


}
