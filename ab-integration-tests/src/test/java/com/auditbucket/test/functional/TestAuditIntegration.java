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

import java.util.ArrayList;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: nabil
 * Date: 16/07/13
 * Time: 22:51
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
public class TestAuditIntegration {

    static int fortressCount = 2;
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
    private String company = "Monowai";
    private String email = "test@ab.com";
    private String emailB = "mark@null.com";
    Authentication authA = new UsernamePasswordAuthenticationToken(email, "user1");


    @BeforeClass
    public static void cleanupElasticSearch() throws Exception {
        ClientConfig clientConfig = new ClientConfig.Builder("http://localhost:9201").multiThreaded(false).build();

        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        factory.setClientConfig(clientConfig);
        client = factory.getObject();
        client.execute(new DeleteIndex.Builder("monowai.audittest").build());
        for (int i = 1; i < fortressCount + 1; i++) {
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

    //    @Test
    public void createHeaderTimeLogsWithSearchActivated() throws Exception {
        double max = 10d;
        String ahKey = null;
        logger.info("createHeaderTimeLogsWithSearchActivated started");
        SecurityContextHolder.getContext().setAuthentication(authA);
        Transaction tx = null;
        regService.registerSystemUser(new RegistrationBean(company, email, "bah"));
        Fortress fo = fortressService.registerFortress(new FortressInputBean("auditTest", true));

        AuditHeaderInputBean inputBean = new AuditHeaderInputBean(fo.getName(), "wally", "TestAudit", new Date(), "ABC123");
        AuditResultBean auditResult = null;
        try {
            tx = graphDatabaseService.beginTx();
            auditResult = auditService.createHeader(inputBean);
            tx.success();
        } finally {
            tx.finish();
        }
        ahKey = auditResult.getAuditKey();

        assertNotNull(ahKey);
        logger.info(ahKey);

//        byte[] docs = alRepo.findOne(auditService.getHeader(ahKey));
//        assertNotNull(docs);
        assertNotNull(auditService.getHeader(ahKey));
        assertNotNull(auditService.findByCallerRef(fo.getId(), "TestAudit", "ABC123"));
        assertNotNull(fortressService.getFortressUser(fo, "wally", true));
        assertNull(fortressService.getFortressUser(fo, "wallyz", false));

        int i = 0;

        StopWatch watch = new StopWatch();
        logger.info("Start-");
        watch.start();
        while (i < max) {
            try {
                tx = graphDatabaseService.beginTx();
                auditService.createLog(new AuditLogInputBean(ahKey, "wally", new DateTime(), "{\"blah\":" + i + "}"));
                tx.success();
            } finally {
                tx.finish();
            }
            i++;
        }
        watch.stop();
        logger.info("End " + watch.getTime() / 1000d + " avg = " + (watch.getTime() / 1000d) / max);


        // Test that we get the expected number of log events
        assertEquals(max, (double) auditService.getAuditLogCount(ahKey));

        Thread.sleep(5000);
        // Putting asserts On elasticsearch
        for (int k = 0; k < 10; k++) {
            String query = "{" +
                    "   \"query\": {  " +
                    "\"query_string\" : { " +
                    " \"default_field\" :\"blah\", " +
                    " \"query\" :\"" + k + "\" " +
                    "}  " +
                    "}  " +
                    "}";
            Search search = new Search.Builder(query)
                    .addIndex("monowai.audittest")
                    .build();

            JestResult result = client.execute(search);
            assertNotNull(result);
            assertNotNull(result.getJsonObject());
            assertNotNull(result.getJsonObject().getAsJsonObject("hits"));
            assertNotNull(result.getJsonObject().getAsJsonObject("hits").get("total"));
            int nbrResult = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();
            Assert.assertEquals(nbrResult, 1);

        }

    }

    @Test
    public void stressWithHighVolume() throws Exception {
        logger.info("stressWithHighVolume started");
        SecurityContextHolder.getContext().setAuthentication(authA);
        Neo4jHelper.cleanDb(graphDatabaseService, true);
        regService.registerSystemUser(new RegistrationBean("TestAudit", email, "bah"));
        //SecurityContextHolder.getContext().setAuthentication(authMike);
        int auditCount = 1;
        int logCount = 2000;
        String escJson = "{\"who\":";
        int fortress = 1;
        ArrayList<Long> list = new ArrayList<Long>();

        StopWatch watch = new StopWatch();
        watch.start();
        double splits = 0;
        int sleepCount;  // Discount all the time we spent sleeping

        logger.info("FortressCount: " + fortressCount + " AuditCount: " + auditCount + " LogCount: " + logCount);
        logger.info("We will be expecting a total of " + (auditCount * logCount * (fortress + 1)) + " messages to be handled");

        sleepCount = 0;
        while (fortress <= fortressCount) {

            String fortressName = "bulkloada" + fortress;
            Fortress iFortress = fortressService.registerFortress(new FortressInputBean(fortressName, false));
            int audit = 1;
            logger.info("Starting run for " + fortressName);
            while (audit <= auditCount) {
                boolean searchChecked = false;
                boolean searchWorking = true;
                AuditHeaderInputBean aib = new AuditHeaderInputBean(iFortress.getName(), fortress + "olivia@sunnybell.com", "CompanyNode", new Date(), "ABC" + audit);
                AuditResultBean arb = auditService.createHeader(aib);
                int log = 1;
                while (log <= logCount && searchWorking) {
                    auditService.createLog(new AuditLogInputBean(arb.getAuditKey(), aib.getFortressUser(), new DateTime(), escJson + log + "}"));
                    if (!searchChecked) {
                        searchChecked = true;

                        int i = 0;
                        AuditHeader auditHeader = auditService.getHeader(arb.getAuditKey(), false);
                        while (auditHeader.getSearchKey() == null && i < 50) {
                            auditHeader = auditService.getHeader(arb.getAuditKey(), false);
                            Thread.sleep(400);
                            i++;
                        }
                        logger.info("Wait for search got to " + i);
                        searchWorking = auditHeader.getSearchKey() != null;
                        assertTrue("Search reply not received from ab-search", searchWorking);
                        sleepCount = sleepCount + 400 * i;
                    }

                    log++;
                }
                //Thread.sleep(sleepCount);
                audit++;
            }
            watch.split();

            logger.info(iFortress.getName() + " took " + ((watch.getSplitTime() - sleepCount) / 1000d) + "  process time per row " + ((watch.getSplitTime() - sleepCount) / 1000d) / (logCount + 2));
            splits = splits + (watch.getSplitTime() - sleepCount);
            watch.reset();
            watch.start();
            list.add(iFortress.getId());
            fortress++;
        }

        logger.info("Created data set");
        double sub = splits / 1000d;
        logger.info("Created data set in " + sub + " fortress avg = " + sub / fortressCount + " avg seconds per row " + sub / (fortressCount * auditCount * logCount) + " rows per second " + (fortressCount * auditCount * logCount) / sub);
        watch.reset();

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
                    AuditHeader header = auditService.findByCallerRef(list.get(fortress), "CompanyNode", "ABC" + random);
                    assertNotNull("ABC" + random, header);
                    assertNotNull("Looks like ab-search is not sending back results", header.getSearchKey());
                    AuditLog when = auditService.getLastChange(header);
                    assertNotNull(when.getAuditChange());
                    //logger.info(header.getAuditKey() + " - " + when);
                    assertTrue("fortress " + fortress + " run " + x + " header " + header.getAuditKey() + " - " + when.getId(), when.isIndexed());
                    totalSearchRequests++;
                    x++;
                } while (x < auditCount);
                fortress++;
            } while (fortress < fortressCount);
            search++;
        } while (search < searchLoops);

        watch.stop();
        double end = watch.getTime() / 1000d;
        logger.info("Total Search Requests = " + totalSearchRequests + ". Total time for searches " + end + " avg requests per second = " + totalSearchRequests / end);


    }


}
