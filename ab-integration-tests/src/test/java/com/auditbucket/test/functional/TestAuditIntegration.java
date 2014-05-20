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

import com.auditbucket.client.AbRestClient;
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
import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * User: nabil
 * Date: 16/07/13
 * Time: 22:51
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
public class TestAuditIntegration {
    private boolean ignoreMe = true;
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

    @Autowired
    private Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(TestAuditIntegration.class);
    private String email = "mike";
    private Authentication authA = new UsernamePasswordAuthenticationToken(email, "123");
    static Properties properties = new Properties() ;
    @AfterClass
    public static void pauseForAWhile() throws Exception{
        System.out.println("Waiting for a while");
        Thread.sleep(5000);
    }
    @BeforeClass
    @Rollback(false)
    public static void cleanupElasticSearch() throws Exception {
        FileInputStream f = new FileInputStream("./src/test/resources/config.properties");
        properties.load(f);

        HttpClientConfig clientConfig = new HttpClientConfig.Builder("http://localhost:"+properties.get("es.http.port")).multiThreaded(false).build();
        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(clientConfig);
        //factory.setClientConfig(clientConfig);
        esClient = factory.getObject();

        esClient.execute(new DeleteIndex.Builder("ab.testaudit.suppress").build());
        esClient.execute(new DeleteIndex.Builder("ab.testco.testfortress").build());
        esClient.execute(new DeleteIndex.Builder("ab.testaudit.ngram").build());
        esClient.execute(new DeleteIndex.Builder("ab.companywithspace.audittest").build());
        esClient.execute(new DeleteIndex.Builder("ab.monowai.trackgraph").build());
        esClient.execute(new DeleteIndex.Builder("ab.monowai.audittest").build());

        for (int i = 1; i < fortressMax + 1; i++) {
            esClient.execute(new DeleteIndex.Builder("ab.testaudit.bulkloada" + i).build());
        }

    }

    @AfterClass
    public static void shutDownElasticSearch() throws Exception {
        esClient.shutdownClient();
    }

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() throws Exception {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml

        SecurityContextHolder.getContext().setAuthentication(authA);
        if ("rest".equals(System.getProperty("neo4j")))
            return;
        Neo4jHelper.cleanDb(template);

    }

    @Test
    public void companyAndFortressWithSpaces() throws Exception {
        assumeTrue(!ignoreMe);
        SecurityContextHolder.getContext().setAuthentication(authA);
        regService.registerSystemUser(new RegistrationBean("Company With Space", email).setIsUnique(false));
        Thread.sleep(1000);
        Fortress fortressA = fortressService.registerFortress(new FortressInputBean("Audit Test", false));
        String docType = "TestAuditX";
        String callerRef = "ABC123X";
        MetaInputBean inputBean = new MetaInputBean(fortressA.getName(), "wally", docType, new DateTime(), callerRef);

        String ahKey = mediationFacade.createHeader(inputBean, null).getMetaKey();
        assertNotNull(ahKey);
        MetaHeader header = trackService.getHeader(ahKey);
        mediationFacade.processLog(new LogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + 1 + "}"));
        Thread.sleep(3000);
        doEsQuery(header.getIndexName(), header.getMetaKey());
    }

    @Test
    public void headerWithTagsProcess() throws Exception {
        assumeTrue(!ignoreMe);
        SecurityContextHolder.getContext().setAuthentication(authA);
        String company = "Monowai";
        String apiKey = regService.registerSystemUser(new RegistrationBean(company, email).setIsUnique(false)).getApiKey();
        Fortress fo = fortressService.registerFortress(new FortressInputBean("headerWithTagsProcess", false));
        DateTime now = new DateTime();
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", now, "ZZZ123");
        inputBean.setTag(new TagInputBean("testTagNameZZ", "someAuditRLX"));
        inputBean.setEvent("TagTest");
        TrackResultBean auditResult;
        auditResult = trackEP.trackHeader(inputBean, apiKey, apiKey).getBody();
        Thread.sleep(4000);
        TrackedSummaryBean summary = trackEP.getAuditSummary(auditResult.getMetaKey(), apiKey, apiKey).getBody();
        assertNotNull(summary);
        // Check we can find the Event in ElasticSearch
        doEsQuery(summary.getHeader().getIndexName(), inputBean.getEvent(), 1);
        // Can we find the Tag
        doEsQuery(summary.getHeader().getIndexName(), "testTagNameZZ", 1);

    }

    @Test
    public void immutableHeadersWithNoLogsAreIndexed() throws Exception {
        assumeTrue(!ignoreMe);
        SecurityContextHolder.getContext().setAuthentication(authA);
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, email).setIsUnique(false));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("immutableHeadersWithNoLogsAreIndexed", false));
        DateTime now = new DateTime();
        MetaInputBean inputBean = new MetaInputBean(fo.getName(), "wally", "TestTrack", now, "ZZZ123");
        inputBean.setEvent("immutableHeadersWithNoLogsAreIndexed");
        TrackResultBean auditResult;
        auditResult = mediationFacade.createHeader(inputBean, null);
        Thread.sleep(4000);
        TrackedSummaryBean summary = mediationFacade.getTrackedSummary(auditResult.getMetaKey());
        assertNotNull(summary);
        assertSame("change logs were not expected", 0, summary.getChanges().size());
        assertNotNull("Search record not received", summary.getHeader().getSearchKey());
        // Check we can find the Event in ElasticSearch
        doEsQuery(summary.getHeader().getIndexName(), inputBean.getEvent(), 1);

        // No Event, so should not be in elasticsearch
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
    public void createHeaderTimeLogsWithSearchActivated() throws Exception {
        assumeTrue(!ignoreMe);
        int max = 3;
        String ahKey;
        logger.info("createHeaderTimeLogsWithSearchActivated started");
        SecurityContextHolder.getContext().setAuthentication(authA);
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, email).setIsUnique(false));
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
        Thread.sleep(8000);
        // Test that we get the expected number of log events
        if (!"rest".equals(System.getProperty("neo4j"))) // Don't check if running over rest
            assertEquals("This will fail if the DB is not cleared down, i.e. testing over REST", max, trackService.getLogCount(ahKey));

        // Putting asserts On elasticsearch
        String query = "{" +
                "   \"query\": {  " +
                "\"query_string\" : { " +
                " \"default_field\" :\"" + MetaSearchSchema.WHAT + ".blah\", " +
                " \"query\" :\"*\" " +
                "}  " +
                "}  " +
                "}";
        Search search = new Search.Builder(query)
                .addIndex(metaHeader.getIndexName())
                .build();

        JestResult result = esClient.execute(search);
        assertNotNull(result);
        assertNotNull(result.getJsonObject());
        assertNotNull(result.getJsonObject().getAsJsonObject("hits"));
        assertNotNull(result.getJsonObject().getAsJsonObject("hits").get("total"));
        int nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        // Only the last change is indexed, so there should be one result
        Assert.assertEquals(1, nbrResult);

    }

    @Test
    public void auditsByPassGraphByCallerRef() throws Exception {
        assumeTrue(!ignoreMe);
        logger.info("auditsByPassGraphByCallerRef started");
        SecurityContextHolder.getContext().setAuthentication(authA);
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, email).setIsUnique(false));
        Fortress fortress = fortressService.registerFortress(new FortressInputBean("TrackGraph", false));

        MetaInputBean inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC123");
        inputBean.setTrackSuppressed(true);
        mediationFacade.createHeader(inputBean, null);

        String indexName = MetaSearchSchema.parseIndex(fortress);

        // Putting asserts On elasticsearch
        Thread.sleep(2000); // Let the messaging take effect
        doEsQuery(indexName, "*", 1);
        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setTrackSuppressed(true);
        mediationFacade.createHeader(inputBean, null);
        Thread.sleep(2000); // Let the messaging take effect
        doEsQuery(indexName, "*", 2);

        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setTrackSuppressed(true);
        mediationFacade.createHeader(inputBean, null);
        Thread.sleep(2000); // Let the messaging take effect
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(indexName, "*", 2);

        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC124");
        inputBean.setTrackSuppressed(true);
        mediationFacade.createHeader(inputBean, null);
        Thread.sleep(2000); // Let the messaging take effect
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(indexName, "*", 2);

        inputBean = new MetaInputBean(fortress.getName(), "wally", "TestTrack", new DateTime(), "ABC125");
        inputBean.setTrackSuppressed(true);
        mediationFacade.createHeader(inputBean, null);
        Thread.sleep(2000); // Let the messaging take effect
        // Updating the same caller ref should not create a 3rd record
        doEsQuery(indexName, "*", 3);

    }

    /**
     * Suppresses the indexing of a log record even if the fortress is set to index everything
     *
     * @throws Exception
     */
    @Test
    public void suppressIndexingOnDemand() throws Exception {
        assumeTrue(!ignoreMe);

        String escJson = "{\"who\":";
        SecurityContextHolder.getContext().setAuthentication(authA);
        regService.registerSystemUser(new RegistrationBean("TestTrack", email).setIsUnique(false));
        Fortress iFortress = fortressService.registerFortress(new FortressInputBean("suppress", false));
        MetaInputBean inputBean = new MetaInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());

        //Transaction tx = getTransaction();
        TrackResultBean indexedResult = mediationFacade.createHeader(inputBean, null);
        MetaHeader indexHeader = trackService.getHeader(indexedResult.getMetaKey());

        LogResultBean resultBean = mediationFacade.processLog(new LogInputBean(indexHeader.getMetaKey(), inputBean.getFortressUser(), new DateTime(), escJson + "\"andy\"}"));
        junit.framework.Assert.assertNotNull(resultBean);

        waitForHeaderToUpdate(indexHeader);
        Thread.sleep(1000);
        String indexName = indexHeader.getIndexName();

        doEsQuery(indexName, "andy");

        inputBean = new MetaInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        inputBean.setSearchSuppressed(true);
        TrackResultBean noIndex = mediationFacade.createHeader(inputBean, null);
        MetaHeader noIndexHeader = trackService.getHeader(noIndex.getMetaKey());

        mediationFacade.processLog(new LogInputBean(noIndexHeader.getMetaKey(), inputBean.getFortressUser(), new DateTime(), escJson + "\"bob\"}"));
        Thread.sleep(1000);
        // Bob's not there because we said we didn't want to index that header
        doEsQuery(indexName, "bob", 0);
        doEsQuery(indexName, "andy");
    }

    @Test
    public void tagKeyReturnsUniqueResult() throws Exception {
        assumeTrue(!ignoreMe);

        String escJson = "{\"who\":";
        SecurityContextHolder.getContext().setAuthentication(authA);
        regService.registerSystemUser(new RegistrationBean("TestTrack", email).setIsUnique(false));
        Fortress iFortress = fortressService.registerFortress(new FortressInputBean("suppress", false));
        MetaInputBean metaInput = new MetaInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        String tagSearch = "example"; // Relationship names is indexed as @tag.relationship.key in ES
        TagInputBean tag = new TagInputBean("Key Test Works", tagSearch);
        metaInput.setTag(tag);


        TrackResultBean indexedResult = mediationFacade.createHeader(metaInput, null);
        MetaHeader indexHeader = trackService.getHeader(indexedResult.getMetaKey());

        Set<TrackTag> tags = trackEP.getAuditTags(indexHeader.getMetaKey(), null, null).getBody();
        assertNotNull(tags);
        assertEquals(1, tags.size());

        LogResultBean resultBean = mediationFacade.processLog(new LogInputBean(indexHeader.getMetaKey(), metaInput.getFortressUser(), new DateTime(), escJson + "\"andy\"}"));
        assertNotNull(resultBean);
        Thread.sleep(2000);

        waitForHeaderToUpdate(indexHeader);
        String indexName = indexHeader.getIndexName();
        doEsFieldQuery(indexName, "@tag." + tagSearch + ".key", "keytestworks", 1);

    }

    @Test
    public void testWhatIndexingDefaultAttributeWithNGram() throws Exception {
        assumeTrue(!ignoreMe);
        SecurityContextHolder.getContext().setAuthentication(authA);
        regService.registerSystemUser(new RegistrationBean("TestTrack", email).setIsUnique(false));
        Fortress iFortress = fortressService.registerFortress(new FortressInputBean("ngram", false));
        MetaInputBean inputBean = new MetaInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());

        TrackResultBean indexedResult = mediationFacade.createHeader(inputBean, null);
        MetaHeader indexHeader = trackService.getHeader(indexedResult.getMetaKey());
        String what = "{\"code\":\"AZERTY\",\"name\":\"NameText\",\"description\":\"this is a description\"}";
        mediationFacade.processLog(new LogInputBean(indexHeader.getMetaKey(), inputBean.getFortressUser(), new DateTime(), what));
        waitForHeaderToUpdate(indexHeader);
        String indexName = indexHeader.getIndexName();
        Thread.sleep(1000);

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

    @Test
    public void stressWithHighVolume() throws Exception {
        assumeTrue(ignoreMe);
        logger.info("stressWithHighVolume started");
        SecurityContextHolder.getContext().setAuthentication(authA);
        //Neo4jHelper.cleanDb(graphDatabaseService, true);
        regService.registerSystemUser(new RegistrationBean("TestTrack", email).setIsUnique(false));

        int auditMax = 10;
        int logMax = 10;
        int fortress = 1;
        //fortressMax = 10;
        String simpleJson = "{\"who\":";
        ArrayList<Long> list = new ArrayList<>();

        logger.info("FortressCount: " + fortressMax + " AuditCount: " + auditMax + " LogCount: " + logMax);
        logger.info("We will be expecting a total of " + (auditMax * logMax * fortressMax) + " messages to be handled");

        StopWatch watch = new StopWatch();
        watch.start();
        double splitTotals = 0;
        long totalRows = 0;
        int auditSleepCount;  // Discount all the time we spent sleeping

        DecimalFormat f = new DecimalFormat("##.000");

        while (fortress <= fortressMax) {
            String fortressName = "bulkloada" + fortress;
            int audit = 1;
            long requests = 0;
            auditSleepCount = 0;

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
                    //String escJson = Helper.getBigJsonText(log);
                    //trackService.createLog(new LogInputBean(arb.getMetaKey(), aib.getFortressUser(), new DateTime(), escJson ));

                    createLog(simpleJson, aib, arb, log);
                    //Thread.sleep(100);
                    requests++;
                    if (!searchChecked) {
                        searchChecked = true;
                        MetaHeader metaHeader = trackService.getHeader(arb.getMetaKey());
                        requests++;
                        int checkCount = waitForHeaderToUpdate(metaHeader);
                        auditSleepCount = auditSleepCount + (400 * checkCount);
                        requests = requests + checkCount;
                    } // searchCheck done
                    log++;
                } // Logs created
                audit++;
            } // Audit headers finished with
            watch.split();
            double fortressRunTime = (watch.getSplitTime() - auditSleepCount) / 1000d;
            logger.info("*** " + iFortress.getName() + " took " + fortressRunTime + "  avg processing time for [" + requests + "] RPS= " + f.format(fortressRunTime / requests) + ". Requests per second " + f.format(requests / fortressRunTime));

            splitTotals = splitTotals + fortressRunTime;
            totalRows = totalRows + requests;
            watch.reset();
            watch.start();
            list.add(iFortress.getId());
            fortress++;
        }

        logger.info("*** Created data set in " + f.format(splitTotals) + " fortress avg = " + f.format(splitTotals / fortressMax) + " avg processing time per request " + f.format(splitTotals / totalRows) + ". Requests per second " + f.format(totalRows / splitTotals));
        watch.reset();
        Thread.sleep(5000); // give things a final chance to complete

        validateLogsIndexed(list, auditMax, logMax);
        doSearchTests(auditMax, list, watch);
    }

    @Test
    public void simpleQueryEPWorksForImportedRecord() throws Exception {
        assumeTrue(!ignoreMe);
        SecurityContextHolder.getContext().setAuthentication(authA);
        String searchFor = "testing";
        String escJson = "{\"who\":\""+searchFor+"\"}";

//        RestTemplate restTemplate = new RestTemplate();
//        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
//        AbRestClient restClient = new AbRestClient("http://localhost:9081/", "mike", "123", 1);
//        assertEquals("Pong!", restClient.ping());
        SystemUser su = regService.registerSystemUser(new RegistrationBean("TestCo", "mike").setIsUnique(false));

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("TestFortress", false));
        Thread.sleep(300);

        LogInputBean log = new LogInputBean("mikeTest", new DateTime(), escJson );
        MetaInputBean input = new MetaInputBean("TestFortress", "mikeTest", "Query", new DateTime(), "abzz");
        input.setLog(log);

        TrackResultBean result = trackEP.trackHeader(input, su.getApiKey(), su.getApiKey() ).getBody();
        waitForHeaderToUpdate(result.getMetaHeader(), su.getApiKey());
        Thread.sleep(2000);
//        restClient.writeAudit(input, "Hello World");


        QueryParams q = new QueryParams( fortress).setSimpleQuery(searchFor);
        doEsQuery("ab.*",searchFor,1);

        String qResult = runQuery(q);
        assertNotNull(qResult);
        assertTrue("Couldn't find a hit in the result ["+result+"]", qResult.contains("total\" : 1"));

    }

    private String runQuery(QueryParams queryParams) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders httpHeaders = AbRestClient.getHeaders(null, "mike", "123");
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

    private void createLog(String simpleJson, MetaInputBean aib, TrackResultBean arb, int log) throws DatagioException, IOException {
        mediationFacade.processLog(new LogInputBean(arb.getMetaKey(), aib.getFortressUser(), new DateTime(), simpleJson + log + "}"));
    }

    private void validateLogsIndexed(ArrayList<Long> list, int auditMax, int expectedLogCount) throws Exception {
        logger.info("Validating logs are indexed");
        int fortress = 2;
        int audit = 1;
        //DecimalFormat f = new DecimalFormat("##.000");
        while (fortress <= fortressMax) {
            while (audit <= auditMax) {
                MetaHeader header = trackService.findByCallerRefFull(list.get(fortress), "CompanyNode", "ABC" + audit);
                StopWatch watch = new StopWatch();
                watch.start();
                Set<TrackLog> logs = trackService.getLogs(header.getId());
                watch.split();
                assertNotNull(logs);
                //logger.info("retrieved [{}] logs in [{}] millis", logs.size(), f.format(watch.getSplitTime()));
                assertEquals("Wrong number of logs returned", expectedLogCount, logs.size());
                for (TrackLog log : logs) {
                    assertEquals("logId [" + log.getId() + "] changeId[" + log.getChange().getId() + "], event[ " + log.getChange().getEvent() + "]", true, log.isIndexed());
                }

                audit++;
            } // Audit headers finished with
            fortress++;
        }

    }

    private int waitForHeaderToUpdate(MetaHeader header, String apiKey) throws Exception{
        // Looking for the first searchKey to be logged against the metaHeader
        int i = 0;
        int timeout = 50;

        MetaHeader metaHeader = trackEP.getMetaHeader(header.getMetaKey(), apiKey, apiKey).getBody();
        if (metaHeader.getSearchKey() != null)
            return 0;
        while (metaHeader.getSearchKey() == null && i <= timeout) {
            metaHeader = trackEP.getMetaHeader(header.getMetaKey(), apiKey, apiKey).getBody();
            Thread.sleep(400);
            i++;
        }
        if (i > 10)
            logger.info("Wait for search got to [{}] for metaId [{}]", i, metaHeader.getId());
        boolean searchWorking = metaHeader.getSearchKey() != null;
        assertTrue("Search reply not received from ab-search", searchWorking);
        return i;
    }

    private int waitForHeaderToUpdate(MetaHeader header) throws Exception {
        return waitForHeaderToUpdate(header, null);
    }

    private void doSearchTests(int auditCount, ArrayList<Long> list, StopWatch watch) throws Exception {
        int fortress;
        int searchLoops = 200;
        int search = 0;
        int totalSearchRequests = 0;

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
                    //TrackLog when = trackService.getLastAuditLog(header);
                    TrackLog trackLog = trackService.getLastLog(header);
                    assertNotNull(trackLog);

                    //logger.info(header.getMetaKey() + " - " + when);
                    assertTrue("fortress " + fortress + " run " + x + " header " + header.getMetaKey() + " - " + trackLog.getId(), trackLog.isIndexed());
                    String result = doEsFieldQuery(header.getIndexName(), MetaSearchSchema.META_KEY, header.getMetaKey(), 1);
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
        String query = "{\n" +
                "    query: {\n" +
                "          query_string : {\n" +
                "              \"query\" : \"" + queryString + "\"" +
                "           }\n" +
                "      }\n" +
                "}";

        logger.info("searching index [{}] for [{}]", index, queryString);
        Search search = new Search.Builder(query)
                .addIndex(index)
                .build();

        JestResult result = esClient.execute(search);
        assertNotNull(result);
        assertNotNull(result.getErrorMessage(), result.getJsonObject());
        assertNotNull(result.getErrorMessage(), result.getJsonObject().getAsJsonObject("hits"));
        assertNotNull(result.getErrorMessage(), result.getJsonObject().getAsJsonObject("hits").get("total"));
        int nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        Assert.assertEquals(index + "\r\n" + result.getJsonString(), expectedHitCount, nbrResult);
        return null;

        //return result.getJsonString();
    }

    private String doEsTermQuery(String index, String field, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given AuditKey.
        // Let's assert that
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

        JestResult result = esClient.execute(search);
        String message = index + " - " + field + " - " + queryString + (result == null ? "[noresult]" : "\r\n" + result.getJsonString());
        assertNotNull(message, result);
        assertNotNull(message, result.getJsonObject());
        assertNotNull(message, result.getJsonObject().getAsJsonObject("hits"));
        assertNotNull(message, result.getJsonObject().getAsJsonObject("hits").get("total"));
        int nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        logger.info("searching index [{}] for [{}]", index, queryString);
        Assert.assertEquals(result.getJsonString(), expectedHitCount, nbrResult);
        if (nbrResult != 0) {
            return result.getJsonObject()
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

    private String doEsFieldQuery(String index, String field, String queryString, int expectedHitCount) throws Exception {
        // There should only ever be one document for a given AuditKey.
        // Let's assert that
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

        JestResult result = esClient.execute(search);
        String message = index + " - " + field + " - " + queryString + (result == null ? "[noresult]" : "\r\n" + result.getJsonString());
        assertNotNull(message, result);
        assertNotNull(message, result.getJsonObject());
        assertNotNull(message, result.getJsonObject().getAsJsonObject("hits"));
        assertNotNull(message, result.getJsonObject().getAsJsonObject("hits").get("total"));
        int nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();

        Assert.assertEquals("Unexpected hit count searching for {" + queryString + "} in field {" + field + "}", expectedHitCount, nbrResult);
        return result.getJsonObject()
                .getAsJsonObject("hits")
                .getAsJsonArray("hits")
                .getAsJsonArray()
                .iterator()
                .next()
                .getAsJsonObject().get("_source").toString();
    }

}
