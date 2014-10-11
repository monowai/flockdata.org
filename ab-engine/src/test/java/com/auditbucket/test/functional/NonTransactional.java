package com.auditbucket.test.functional;

import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.CrossReferenceInputBean;
import com.auditbucket.track.model.TrackTag;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.EntityKey;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.support.node.Neo4jHelper;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 2/05/14
 * Time: 8:26 AM
 */
public class NonTransactional extends EngineBase {

    private Logger logger = LoggerFactory.getLogger(NonTransactional.class);
    @Override
    public void cleanUpGraph() {
        // Nothing
        logger.debug("Not cleaning up");
    }

    @After
    public void clearGraph(){
        super.cleanUpGraph();
    }

    @Test
    public void crossReferenceTags() throws Exception {
        SystemUser su = registerSystemUser("crossReferenceTags", mike_admin);
        Thread.sleep(500);
        Fortress fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest"));
        TagInputBean tag = new TagInputBean("ABC", "Device", "sold");
        ArrayList<TagInputBean> tags = new ArrayList<>();
        tags.add(tag);
        mediationFacade.createTags(su.getCompany(), tags);
        Thread.sleep(300); // Let the schema changes occur

        EntityInputBean inputBean = new EntityInputBean(fortressA.getName(), "wally", "DocTypeA", new DateTime(), "ABC123");
        inputBean.addTag( new TagInputBean("ABC", "Device", "sold"));
        TrackResultBean docA = mediationFacade.trackEntity(su.getCompany(), inputBean);

        // These are the two records that will cite the previously created entity
        EntityInputBean inputBeanB = new EntityInputBean(fortressA.getName(), "wally", "DocTypeB", new DateTime(), "ABC321");
        inputBeanB.addTag( new TagInputBean("ABC", "Device", "applies"));
        TrackResultBean docB = mediationFacade.trackEntity(su.getCompany(), inputBeanB);

        Map<String, List<EntityKey>> refs = new HashMap<>();
        List<EntityKey> callerRefs = new ArrayList<>();

        callerRefs.add(new EntityKey("ABC321"));
        callerRefs.add(new EntityKey("ABC333"));

        refs.put("cites",callerRefs);
        CrossReferenceInputBean bean = new CrossReferenceInputBean(fortressA.getName(), "ABC123",refs);
        List<CrossReferenceInputBean > inputs = new ArrayList<>();
        inputs.add(bean);
        Collection<TrackTag> tagsA = entityTagService.getEntityTags(su.getCompany(), docA.getEntity());
        assertEquals(1, tagsA.size());
        Collection<TrackTag> tagsB = entityTagService.getEntityTags(su.getCompany(), docB.getEntity());
        assertEquals(1, tagsB.size());

    }

    @Test
    public void multipleFortressUserRequestsThreaded() throws Exception {
        Neo4jHelper.cleanDb(template);
        Transaction t = template.getGraphDatabase().beginTx();
        logger.info("Starting multipleFortressUserRequestsThreaded");
        // Assume the user has now logged in.
        //org.neo4j.graphdb.Transaction t = graphDatabaseService.beginTx();
        String company = "MFURT";
        SystemUser su = registerSystemUser(company, mike_admin);
        setSecurity();

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("multipleFortressUserRequestsThreaded"));
        // This is being done to create the schema index which otherwise errors when the threads kick off
        fortressService.getFortressUser(fortress, "don'tcare");
        fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("testThis", true));
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
            FuAction action = new FuAction(fortress, Integer.toString(i), EngineBase.mike_admin, latch);
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
        FortressUser fu = fortressService.getFortressUser(fortress, mike_admin);
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
