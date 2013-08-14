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
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditResultBean;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
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
public class TestAuditIntegration {

    static int fortressMax = 2;
    static JestClient client;
    @Autowired
    AuditService auditService;
    @Autowired
    RegistrationService regService;
    @Autowired
    FortressService fortressService;

    @Autowired
    private GraphDatabaseService graphDatabaseService;
    @Autowired
    private Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(TestAuditIntegration.class);
    private String email = "test@ab.com";
    Authentication authA = new UsernamePasswordAuthenticationToken(email, "user1");


    @BeforeClass
    public static void cleanupElasticSearch() throws Exception {
        ClientConfig clientConfig = new ClientConfig.Builder("http://localhost:9201").multiThreaded(false).build();

        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        factory.setClientConfig(clientConfig);
        client = factory.getObject();
        client.execute(new DeleteIndex.Builder("monowai.audittest").build());
        client.execute(new DeleteIndex.Builder("monowai.suppress").build());
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
        Neo4jHelper.cleanDb(template);
    }

    //@Test
    public void createHeaderTimeLogsWithSearchActivated() throws Exception {
        int max = 10;
        String ahKey;
        logger.info("createHeaderTimeLogsWithSearchActivated started");
        SecurityContextHolder.getContext().setAuthentication(authA);
        String company = "Monowai";
        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", false));

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", new Date(), "ABC123");
        AuditResultBean auditResult;
        auditResult = auditService.createHeader(inputBean);
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
            auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + i + "}"));
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
                " \"default_field\" :\"@what.blah\", " +
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
    //@Test
    public void suppressIndexingOnDemand() throws Exception {
        String escJson = "{\"who\":";
        SecurityContextHolder.getContext().setAuthentication(authA);
        regService.registerSystemUser(new RegistrationBean("TestAudit", email, "bah"));
        Fortress iFortress = fortressService.registerFortress(new FortressInputBean("suppress", false));
        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new Date());

        AuditResultBean indexedResult = auditService.createHeader(inputBean);
        AuditHeader indexHeader = auditService.getHeader(indexedResult.getAuditKey());
        auditService.createLog(new AuditLogInputBean(indexHeader.getAuditKey(), inputBean.getFortressUser(), new DateTime(), escJson + "\"andy\"}"));

        waitForHeaderToUpdate(indexHeader);
        String indexName = indexHeader.getIndexName();
        Thread.sleep(1000);
        doESCheck(indexName, "andy");

        inputBean = new AuditHeaderInputBean(iFortress.getName(), "olivia@sunnybell.com", "CompanyNode", new Date());
        inputBean.setSuppressSearch(true);
        AuditResultBean noIndex = auditService.createHeader(inputBean);
        AuditHeader noIndexHeader = auditService.getHeader(noIndex.getAuditKey());

        auditService.createLog(new AuditLogInputBean(noIndexHeader.getAuditKey(), inputBean.getFortressUser(), new DateTime(), escJson + "\"bob\"}"));
        Thread.sleep(1000);
        // Bob's not there because we said we didn't want to index that header
        doESCheck(indexName, "bob", 0);
        doESCheck(indexName, "andy");


    }

    @Test
    public void stressWithHighVolume() throws Exception {
        logger.info("stressWithHighVolume started");
        SecurityContextHolder.getContext().setAuthentication(authA);
        Neo4jHelper.cleanDb(graphDatabaseService, true);
        regService.registerSystemUser(new RegistrationBean("TestAudit", email, "bah"));
        //SecurityContextHolder.getContext().setAuthentication(authMike);
        int auditMax = 10;
        int logMax = 10;
        int fortress = 1;
        String simpleJson = "{\"who\":";
        ArrayList<Long> list = new ArrayList<Long>();

        StopWatch watch = new StopWatch();
        watch.start();
        double splitTotals = 0;
        long totalRows = 0;
        int auditSleepCount;  // Discount all the time we spent sleeping

        logger.info("FortressCount: " + fortressMax + " AuditCount: " + auditMax + " LogCount: " + logMax);
        logger.info("We will be expecting a total of " + (auditMax * logMax * (fortress + 1)) + " messages to be handled");
        DecimalFormat f = new DecimalFormat("##.000");

        while (fortress <= fortressMax) {
            String fortressName = "bulkloada" + fortress;
            int audit = 1;
            long rows = 0;
            auditSleepCount = 0;

            Fortress iFortress = fortressService.registerFortress(new FortressInputBean(fortressName, false));
            rows++;
            logger.info("Starting run for " + fortressName);
            while (audit <= auditMax) {
                boolean searchChecked = false;
                AuditHeaderInputBean aib = new AuditHeaderInputBean(iFortress.getName(), fortress + "olivia@sunnybell.com", "CompanyNode", new Date(), "ABC" + audit);
                AuditResultBean arb = auditService.createHeader(aib);
                rows++;
                int log = 1;
                while (log <= logMax) {
                    //String escJson = Helper.getBigJsonText(log);
                    //auditService.createLog(new AuditLogInputBean(arb.getAuditKey(), aib.getFortressUser(), new DateTime(), escJson ));

                    auditService.createLog(new AuditLogInputBean(arb.getAuditKey(), aib.getFortressUser(), new DateTime(), simpleJson + log + "}"));
                    if (!searchChecked) {
                        searchChecked = true;
                        AuditHeader auditHeader = auditService.getHeader(arb.getAuditKey(), false);
                        rows++;
                        int checkCount = waitForHeaderToUpdate(auditHeader);
                        auditSleepCount = auditSleepCount + (400 * checkCount);
                        rows = rows + checkCount;
                    } // searchCheck done
                    log++;
                } // Logs created
                audit++;
            } // Audit headers finished with
            watch.split();
            double fortressRunTime = (watch.getSplitTime() - auditSleepCount) / 1000d;
            logger.info("*** " + iFortress.getName() + " took " + fortressRunTime + "  avg processing time for [" + rows + "] RPS= " + f.format(fortressRunTime / rows) + ". Rows per second " + f.format(rows / fortressRunTime));

            splitTotals = splitTotals + fortressRunTime;
            totalRows = totalRows + rows;
            watch.reset();
            watch.start();
            list.add(iFortress.getId());
            fortress++;
        }

        logger.info("*** Created data set in " + f.format(splitTotals) + " fortress avg = " + f.format(splitTotals / fortressMax) + " avg processing time per row " + f.format(splitTotals / totalRows) + ". Rows per second " + f.format(totalRows / splitTotals));
        watch.reset();

        doSearchTests(auditMax, list, watch);
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
        Thread.sleep(5000); // give things a final chance to complete
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
                    assertNotNull(header.getLastChange());

                    //logger.info(header.getAuditKey() + " - " + when);
                    assertTrue("fortress " + fortress + " run " + x + " header " + header.getAuditKey() + " - " + header.getLastChange().getAuditLog().getId(), header.getLastChange().getAuditLog().isIndexed());
                    doESCheck(header.getIndexName(), header.getAuditKey());
                    totalSearchRequests++;
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

    private boolean doESCheck(String index, String queryString) throws Exception {
        return doESCheck(index, queryString, 1);
    }

    private boolean doESCheck(String index, String queryString, int expectedHitCount) throws Exception {
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
        Assert.assertEquals(expectedHitCount, nbrResult);
        return true;
    }


}
