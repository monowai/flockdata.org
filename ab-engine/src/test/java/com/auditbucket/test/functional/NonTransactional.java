package com.auditbucket.test.functional;

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.CompanyUser;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.Concept;
import com.auditbucket.track.model.DocumentType;
import com.auditbucket.track.model.MetaHeader;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.support.node.Neo4jHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * User: mike
 * Date: 2/05/14
 * Time: 8:26 AM
 */
public class NonTransactional extends TestEngineBase{

    private Logger logger = LoggerFactory.getLogger(NonTransactional.class);
    @Override
    public void cleanUpGraph() {
        // Nothing
        logger.debug("Not cleaning up");
    }

    @Test
    public void conceptsInUse() throws Exception {
        Neo4jHelper.cleanDb(template);
        engineAdmin.setConceptsEnabled(true);

        Transaction t = beginManualTransaction();

        SystemUser su = regService.registerSystemUser(new RegistrationBean(monowai, mike));
        Assert.assertNotNull(su);

        Fortress fortA = fortressService.registerFortress("fortA");

        DocumentType dType = schemaService.resolveDocType(fortA, "ABC123", true);
        commitManualTransaction(t);// Should only be only one concept

        Assert.assertNotNull(dType);
        Long id = dType.getId();
        dType = schemaService.resolveDocType(fortA, "ABC123", false);
        Assert.assertEquals(id, dType.getId());

        MetaInputBean input = new MetaInputBean(fortA.getName(), "jinks", "DocA", new DateTime());
        input.addTag(new TagInputBean("cust123", "purchased").setIndex("Customer"));
        MetaHeader meta = trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();

        assertNotNull(trackEP.getMetaHeader(meta.getMetaKey(), su.getApiKey(), su.getApiKey()));

        input = new MetaInputBean(fortA.getName(), "jinks", "DocA", new DateTime());
        input.addTag(new TagInputBean("cust124", "purchased").setIndex("Customer"));

        trackEP.trackHeader(input, su.getApiKey(), su.getApiKey()).getBody().getMetaHeader();
        waitAWhile("Concepts creating...");

        Collection<String> docs = new ArrayList<>();
        docs.add("DocA");
        Collection<Concept> concepts = queryEP.getConcepts(docs, su.getApiKey(), su.getApiKey());
        org.junit.Assert.assertNotNull(concepts);
        assertEquals(1, concepts.size());

        // add a second concept
        input = new MetaInputBean(fortA.getName(), "jinks", "DocA", new DateTime());
        input.addTag(new TagInputBean("cust123", "sold").setIndex("Rep"));
        trackEP.trackHeader(input, su.getApiKey(), su.getApiKey());
        waitAWhile("Concepts creating...");

        concepts = queryEP.getRelationships(docs, su.getApiKey(), su.getApiKey());
        assertEquals("Second concept wasn't added", 2, concepts.size());

        Boolean foundCustomer= false, foundRep= false;
        for (Concept concept : concepts) {
            if (concept.getName().equals("Customer")){
                foundCustomer = true;
                assertEquals(1, concept.getRelationships().size());
            }
            if (concept.getName().equals("Rep")) {
                foundRep = true;
                assertEquals(1, concept.getRelationships().size());
            }

        }
        assertTrue("Didn't find Customer concept", foundCustomer);
        assertTrue("Didn't find Rep concept", foundRep);

    }
    @Test
    public void multipleFortressUserRequestsThreaded() throws Exception {
        Neo4jHelper.cleanDb(template);
        Transaction t = template.getGraphDatabase().beginTx();
        logger.info("Starting multipleFortressUserRequestsThreaded");
        String uname = "mike";
        // Assume the user has now logged in.
        //org.neo4j.graphdb.Transaction t = graphDatabaseService.beginTx();
        String company = "MFURT";
        SystemUser su = regService.registerSystemUser(new RegistrationBean(company, uname).setIsUnique(false));
        setSecurity();
        CompanyUser nonAdmin = regService.addCompanyUser(uname, company);
        assertNotNull(nonAdmin);

        Fortress fortress = fortressEP.registerFortress(new FortressInputBean("multipleFortressUserRequestsThreaded", true), su.getApiKey(), null).getBody();
        // This is being done to create the schema index which otherwise errors when the threads kick off
        fortressService.getFortressUser(fortress, "don'tcare");
        fortress = fortressEP.registerFortress(new FortressInputBean("testThis", true), su.getApiKey(), null).getBody();
        assertNotNull(fortress);

        commitManualTransaction(t);
        Thread.sleep(200);

        int count = 5;

        CountDownLatch latch = new CountDownLatch(count);
        // Run threaded tests
        ArrayList<FuAction> actions = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();
        int i = 0;
        while (i <= count) {
            FuAction action = new FuAction(fortress, Integer.toString(i), "mike", latch);
            actions.add(action);
            threads.add(new Thread(action));
            threads.get(i).start();
            i++;
        }

        boolean timedOut = !latch.await(60, TimeUnit.SECONDS);
        assertFalse(timedOut);

        assertNotNull(fortressService.findByName(fortress.getCompany(), fortress.getName()));
        i = 0;
        while (i < count) {
            assertFalse("Fu" + i + "Fail", actions.get(i).isFailed());
            i++;
        }
        // Check we only get one back
        // Not 100% sure this works
        FortressUser fu = fortressService.getFortressUser(fortress, uname);
        assertNotNull(fu);

    }

    class FuAction implements Runnable {
        Fortress fortress;
        String uname;
        CountDownLatch latch;
        boolean failed;

        public FuAction(Fortress fortress, String id, String uname, CountDownLatch latch) {
            logger.info("Preparing FuAction {}, {}", id, latch.getCount());
            this.fortress = fortress;
            this.uname = uname;
            this.latch = latch;
        }

        public boolean isFailed() {
            return failed;
        }

        public void run() {
            logger.info("Running " + this);
            int runCount = 50;
            int i = 0;
            failed = false;
            int deadLockCount = 0;
            while (i < runCount) {
                boolean deadlocked = false;
                try {
                    FortressUser fu = fortressService.getFortressUser(this.fortress, uname);
                    assertNotNull(fu);
                } catch (Exception e) {
                    deadLockCount++;
                    Thread.yield();
                    if (deadLockCount == 100) {
                        failed = true;
                        logger.error("Exception count exceeded");
                        return;
                    }
                    deadlocked = true;
                }

                if (!deadlocked)
                    i++;
            } // End while
            logger.info("Finishing {}", failed);
            failed = false;
            latch.countDown();
        }
    }
}
