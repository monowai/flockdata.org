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

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditLog;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditResultBean;
import com.auditbucket.bean.AuditSummaryBean;
import com.auditbucket.engine.service.AuditManagerService;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.search.AuditSearchSchema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.ClientConfig;
import io.searchbox.core.Search;
import io.searchbox.indices.DeleteIndex;
import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * User: nabil
 * Date: 16/07/13
 * Time: 22:51
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
//@Transactional
public class TestAuditIntegration {

    private static int fortressMax = 2;
    private static JestClient client;
    @Autowired
    AuditService auditService;
    @Autowired
    RegistrationService regService;
    @Autowired
    FortressService fortressService;
    @Autowired
    AuditManagerService auditManager;

    @Autowired
    private GraphDatabaseService graphDatabaseService;
    @Autowired
    private Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(TestAuditIntegration.class);
    private String email = "test@ab.com";
    private Authentication authA = new UsernamePasswordAuthenticationToken(email, "user1");

    @BeforeClass
    public static void cleanupElasticSearch() throws Exception {
        ClientConfig clientConfig = new ClientConfig.Builder("http://localhost:9201").multiThreaded(false).build();

        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        factory.setClientConfig(clientConfig);
        client = factory.getObject();

        client.execute(new DeleteIndex.Builder("testaudit.suppress").build());
        client.execute(new DeleteIndex.Builder("testaudit.ngram").build());
        client.execute(new DeleteIndex.Builder("monowai.audittest").build());
        for (int i = 1; i < fortressMax + 1; i++) {
            client.execute(new DeleteIndex.Builder("testaudit.bulkloada" + i).build());
        }

    }

    @AfterClass
    public static void shutDownElasticSearch() throws Exception {
        client.shutdownClient();
    }

    @Rollback(false)
    @BeforeTransaction
    public void cleanUpGraph() {
        // This will fail if running over REST. Haven't figured out how to use a view to look at the embedded db
        // See: https://github.com/SpringSource/spring-data-neo4j/blob/master/spring-data-neo4j-examples/todos/src/main/resources/META-INF/spring/applicationContext-graph.xml
        SecurityContextHolder.getContext().setAuthentication(authA);
        if ("http".equals(System.getProperty("neo4j")))
            return;
        Transaction tx = graphDatabaseService.beginTx();
        try {

            Neo4jHelper.cleanDb(template);
            tx.success();
        } finally {
            tx.finish();
        }

    }

    //@Test
    public void immutableHeadersWithNoLogsAreIndexed() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authA);
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", false));
        DateTime now = new DateTime();
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", now, "ZZZ123");
        inputBean.setEvent("immutableHeadersWithNoLogsAreIndexed");
        AuditResultBean auditResult;
        auditResult = auditManager.createHeader(inputBean);
        Thread.sleep(2000);
        AuditSummaryBean summary = auditManager.getAuditSummary(auditResult.getAuditKey());
        assertNotNull(summary);
        assertSame("Not change logs were expected", 0, summary.getChanges().size());
        assertNotNull(summary.getHeader().getSearchKey());
        // Check we can find the Event in ElasticSearch
        doEsQuery(summary.getHeader().getIndexName(), inputBean.getEvent(), 1);

        // No Event, so should not be in elasticsearch
        inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", now, "ZZZ999");
        auditResult = auditManager.createHeader(inputBean);
        summary = auditManager.getAuditSummary(auditResult.getAuditKey());
        assertNotNull(summary);
        assertSame("Not change logs were expected", 0, summary.getChanges().size());
        assertNull(summary.getHeader().getSearchKey());
        // Check we can find the Event in ElasticSearch
        doEsQuery(summary.getHeader().getIndexName(), "ZZZ999", 0);
    }

    //@Test
    public void createHeaderTimeLogsWithSearchActivated() throws Exception {
        int max = 3;
        String ahKey;
        logger.info("createHeaderTimeLogsWithSearchActivated started");
        SecurityContextHolder.getContext().setAuthentication(authA);
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", false));

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", new DateTime(), "ABC123");
        AuditResultBean auditResult;
        auditResult = auditManager.createHeader(inputBean);
        ahKey = auditResult.getAuditKey();

        assertNotNull(ahKey);

        AuditHeader auditHeader = auditService.getHeader(ahKey);
        assertNotNull(auditHeader);
        assertNotNull(auditService.findByCallerRef(fo.getId(), "TestAudit", "ABC123"));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        int i = 0;

        StopWatch watch = new StopWatch();
        logger.info("Start-");
        watch.start();
        while (i < max) {
            auditManager.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + i + "}"));
            i++;
        }
        watch.stop();

        // Test that we get the expected number of log events
        assertEquals(max, auditService.getAuditLogCount(ahKey));

        Thread.sleep(5000);
        // Putting asserts On elasticsearch
        String query = "{" +
                "   \"query\": {  " +
                "\"query_string\" : { " +
                " \"default_field\" :\"" + AuditSearchSchema.WHAT + ".blah\", " +
                " \"query\" :\"*\" " +
                "}  " +
                "}  " +
                "}";
        Search search = new Search.Builder(query)
                .addIndex(auditHeader.getIndexName())
                .build();

        JestResult result = client.execute(search);
        assertNotNull(result);
        assertNotNull(result.getJsonObject());
        assertNotNull(result.getJsonObject().getAsJsonObject("hits"));
        assertNotNull(result.getJsonObject().getAsJsonObject("hits").get("total"));
        int nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        // Only the last change is indexed, so there should be one result
        Assert.assertEquals(1, nbrResult);

    }

    /**
     * Suppresses the indexing of a log record even if the fortress is set to index everything
     *
     * @throws Exception
     */
    @Test
    public void suppressIndexingOnDemand() throws Exception {
        String escJson = "{\"who\":";
        SecurityContextHolder.getContext().setAuthentication(authA);
        regService.registerSystemUser(new RegistrationBean("TestAudit", email, "bah"));
        Fortress iFortress = fortressService.registerFortress(new FortressInputBean("suppress", false));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());

        AuditResultBean indexedResult = auditManager.createHeader(inputBean);
        AuditHeader indexHeader = auditService.getHeader(indexedResult.getAuditKey());
        auditManager.createLog(new AuditLogInputBean(indexHeader.getAuditKey(), inputBean.getFortressUser(), new DateTime(), escJson + "\"andy\"}"));

        waitForHeaderToUpdate(indexHeader);
        String indexName = indexHeader.getIndexName();
        Thread.sleep(1000);
        doEsQuery(indexName, "andy");

        inputBean = new AuditHeaderInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());
        inputBean.setSuppressSearch(true);
        AuditResultBean noIndex = auditManager.createHeader(inputBean);
        AuditHeader noIndexHeader = auditService.getHeader(noIndex.getAuditKey());

        auditManager.createLog(new AuditLogInputBean(noIndexHeader.getAuditKey(), inputBean.getFortressUser(), new DateTime(), escJson + "\"bob\"}"));
        Thread.sleep(1000);
        // Bob's not there because we said we didn't want to index that header
        doEsQuery(indexName, "bob", 0);
        doEsQuery(indexName, "andy");
    }

    @Test
    public void testWhatIndexingDefaultAttributeWithNGram() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(authA);
        regService.registerSystemUser(new RegistrationBean("TestAudit", email, "bah"));
        Fortress iFortress = fortressService.registerFortress(new FortressInputBean("ngram", false));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new DateTime());

        AuditResultBean indexedResult = auditManager.createHeader(inputBean);
        AuditHeader indexHeader = auditService.getHeader(indexedResult.getAuditKey());
        String what = "{\"code\":\"AZERTY\",\"name\":\"Name\",\"description\":\"this is a description\"}";
        auditManager.createLog(new AuditLogInputBean(indexHeader.getAuditKey(), inputBean.getFortressUser(), new DateTime(), what));

        waitForHeaderToUpdate(indexHeader);
        String indexName = indexHeader.getIndexName();
        Thread.sleep(1000);

        doEsTermQuery(indexName, "@what.description", "des", 1);
        doEsTermQuery(indexName, "@what.description", "de", 0);
        doEsTermQuery(indexName, "@what.description", "descripti", 1);
        doEsTermQuery(indexName, "@what.description", "descriptio", 1);
        doEsTermQuery(indexName, "@what.description", "description", 0);
        doEsTermQuery(indexName, "@what.description", "is is a de", 1);
        doEsTermQuery(indexName, "@what.description", "is is a des", 0);

    }

    @Test
    public void stressWithHighVolume() throws Exception {
        logger.info("stressWithHighVolume started");
        SecurityContextHolder.getContext().setAuthentication(authA);
        //Neo4jHelper.cleanDb(graphDatabaseService, true);
        regService.registerSystemUser(new RegistrationBean("TestAudit", email, "bah"));

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
                AuditHeaderInputBean aib = new AuditHeaderInputBean(iFortress.getName(), fortress + "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC" + audit);
                AuditResultBean arb = auditManager.createHeader(aib);
                requests++;
                int log = 1;
                while (log <= logMax) {
                    //String escJson = Helper.getBigJsonText(log);
                    //auditService.createLog(new AuditLogInputBean(arb.getAuditKey(), aib.getFortressUser(), new DateTime(), escJson ));

                    createLog(simpleJson, aib, arb, log);
                    //Thread.sleep(100);
                    requests++;
                    if (!searchChecked) {
                        searchChecked = true;
                        AuditHeader auditHeader = auditService.getHeader(arb.getAuditKey(), false);
                        requests++;
                        int checkCount = waitForHeaderToUpdate(auditHeader);
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

    private void createLog(String simpleJson, AuditHeaderInputBean aib, AuditResultBean arb, int log) throws IOException {
        auditManager.createLog(new AuditLogInputBean(arb.getAuditKey(), aib.getFortressUser(), new DateTime(), simpleJson + log + "}"));
    }

    private void validateLogsIndexed(ArrayList<Long> list, int auditMax, int expectedLogCount) throws Exception {
        logger.info("Validating logs are indexed");
        int fortress = 1;
        int audit = 1;
        //DecimalFormat f = new DecimalFormat("##.000");
        while (fortress <= fortressMax) {
            while (audit <= auditMax) {
                AuditHeader header = auditService.findByCallerRefFull(list.get(fortress), "CompanyNode", "ABC" + audit);
                StopWatch watch = new StopWatch();
                watch.start();
                Set<AuditLog> logs = auditService.getAuditLogs(header.getId());
                watch.split();
                assertNotNull(logs);
                //logger.info("retrieved [{}] logs in [{}] millis", logs.size(), f.format(watch.getSplitTime()));
                assertEquals("Wrong number of logs returned", expectedLogCount, logs.size());
                for (AuditLog log : logs) {
                    assertEquals("logId [" + log.getId() + "] changeId[" + log.getAuditChange().getId() + "], event[ " + log.getAuditChange().getEvent() + "]", true, log.isIndexed());
                }

                audit++;
            } // Audit headers finished with
            fortress++;
        }

    }

    private int waitForHeaderToUpdate(AuditHeader header) throws Exception {
        // Looking for the first searchKey to be logged against the auditHeader
        int i = 0;
        int timeout = 50;

        AuditHeader auditHeader = auditService.getHeader(header.getAuditKey(), false);
        if (auditHeader.getSearchKey() != null)
            return 0;
        while (auditHeader.getSearchKey() == null && i <= timeout) {
            auditHeader = auditService.getHeader(header.getAuditKey(), false);
            Thread.sleep(400);
            i++;
        }
        if (i > 4)
            logger.info("Wait for search got to " + i);
        boolean searchWorking = auditHeader.getSearchKey() != null;
        assertTrue("Search reply not received from ab-search", searchWorking);
        return i;
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

                    AuditHeader header = auditService.findByCallerRefFull(list.get(fortress), "CompanyNode", "ABC" + random);
                    assertNotNull("ABC" + random, header);
                    assertNotNull("Looks like ab-search is not sending back results", header.getSearchKey());
                    //AuditLog when = auditService.getLastAuditLog(header);
                    AuditLog auditLog = auditService.getLastAuditLog(header);
                    assertNotNull(auditLog);

                    //logger.info(header.getAuditKey() + " - " + when);
                    assertTrue("fortress " + fortress + " run " + x + " header " + header.getAuditKey() + " - " + auditLog.getId(), auditLog.isIndexed());
                    String result = doEsFieldQuery(header.getIndexName(), "@auditKey", header.getAuditKey(), 1);
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

        assertNotNull(node.get(AuditSearchSchema.CREATED));
        assertNotNull(node.get(AuditSearchSchema.WHO));
        assertNotNull(node.get(AuditSearchSchema.WHEN));
        assertNotNull(node.get(AuditSearchSchema.AUDIT_KEY));
        assertNotNull(node.get(AuditSearchSchema.DOC_TYPE));
        assertNotNull(node.get(AuditSearchSchema.FORTRESS));

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
                "              \"query\" : \"" + queryString + "\"\n" +
                "           }\n" +
                "      }\n" +
                "}";
        Search search = new Search.Builder(query)
                .addIndex(index)
                .build();

        JestResult result = client.execute(search);
        assertNotNull(result);
        assertNotNull(result.getJsonObject());
        assertNotNull(result.getJsonObject().getAsJsonObject("hits"));
        assertNotNull(result.getJsonObject().getAsJsonObject("hits").get("total"));
        int nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        Assert.assertEquals(result.getJsonString(), expectedHitCount, nbrResult);
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

        JestResult result = client.execute(search);
        String message = index + " - " + field + " - " + queryString + (result == null ? "[noresult]" : "\r\n" + result.getJsonString());
        assertNotNull(message, result);
        assertNotNull(message, result.getJsonObject());
        assertNotNull(message, result.getJsonObject().getAsJsonObject("hits"));
        assertNotNull(message, result.getJsonObject().getAsJsonObject("hits").get("total"));
        int nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
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

        JestResult result = client.execute(search);
        String message = index + " - " + field + " - " + queryString + (result == null ? "[noresult]" : "\r\n" + result.getJsonString());
        assertNotNull(message, result);
        assertNotNull(message, result.getJsonObject());
        assertNotNull(message, result.getJsonObject().getAsJsonObject("hits"));
        assertNotNull(message, result.getJsonObject().getAsJsonObject("hits").get("total"));
        int nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
        Assert.assertEquals(result.getJsonString(), expectedHitCount, nbrResult);
        return result.getJsonObject()
                .getAsJsonObject("hits")
                .getAsJsonArray("hits")
                .getAsJsonArray()
                .iterator()
                .next()
                .getAsJsonObject().get("_source").toString();
    }

}
