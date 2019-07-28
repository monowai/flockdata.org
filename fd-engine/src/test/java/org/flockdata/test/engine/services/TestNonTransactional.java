/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.FortressUserNode;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.EntityToEntityLinkInput;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mholdsworth
 * @since 2/05/2014
 */
public class TestNonTransactional extends EngineBase {

  private Logger logger = LoggerFactory.getLogger(TestNonTransactional.class);

  @Override
  public void cleanUpGraph() {
    // DAT-348
    super.cleanUpGraph();
  }

  @Test
  public void crossReferenceTags() throws Exception {
    SystemUser su = registerSystemUser("crossReferenceTags", mike_admin);
    Thread.sleep(500);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("auditTest", true));
    TagInputBean tag = new TagInputBean("ABC", "Device", "sold");
    ArrayList<TagInputBean> tags = new ArrayList<>();
    tags.add(tag);
    mediationFacade.createTags(su.getCompany(), tags);
    Thread.sleep(300); // Let the schema changes occur

    EntityInputBean inputBean = new EntityInputBean(fortress, "wally", "DocTypeA", new DateTime(), "ABC123");
    inputBean.addTag(new TagInputBean("ABC", "Device", "sold"));
    TrackResultBean docA = mediationFacade.trackEntity(su.getCompany(), inputBean);

    // These are the two records that will cite the previously created entity
    EntityInputBean inputBeanB = new EntityInputBean(fortress, "wally", "DocTypeB", new DateTime(), "ABC321");
    inputBeanB.addTag(new TagInputBean("ABC", "Device", "applies"));
    TrackResultBean docB = mediationFacade.trackEntity(su.getCompany(), inputBeanB);

    Map<String, List<EntityKeyBean>> refs = new HashMap<>();
    List<EntityKeyBean> codeRefs = new ArrayList<>();

    codeRefs.add(new EntityKeyBean("ABC321", "123", "444"));
    codeRefs.add(new EntityKeyBean("ABC333", "123", "444"));

    refs.put("cites", codeRefs);
    EntityToEntityLinkInput bean = new EntityToEntityLinkInput(inputBean);
    List<EntityToEntityLinkInput> inputs = new ArrayList<>();
    inputs.add(bean);
    Collection<EntityTag> tagsA = entityTagService.findEntityTags(docA.getEntity());
    assertEquals(1, tagsA.size());
    Collection<EntityTag> tagsB = entityTagService.findEntityTags(docB.getEntity());
    assertEquals(1, tagsB.size());

  }

  @Test
  public void multipleFortressUserRequestsThreaded() throws Exception {
    cleanUpGraph();
    Transaction t = neo4jTemplate.getGraphDatabase().beginTx();
    logger.info("### Starting multipleFortressUserRequestsThreaded");
    // Assume the user has now logged in.
    //org.neo4j.graphdb.Transaction t = graphDatabaseService.beginTx();
    String company = "MFURT";
    SystemUser su = registerSystemUser(company, mike_admin);
    setSecurity();

    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("multipleFortressUserRequestsThreaded"));
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
      FuAction action = new FuAction(fortress, Integer.toString(i), mike_admin, latch);
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
    FortressUserNode fu = fortressService.getFortressUser(fortress, mike_admin);
    assertNotNull(fu);

  }

  class FuAction implements Runnable {
    Fortress fortress;
    String uname;
    CountDownLatch latch;
    boolean failed;

    public FuAction(FortressNode fortress, String id, String uname, CountDownLatch latch) {
      this.fortress = fortress;
      this.uname = uname;
      this.latch = latch;
    }

    public boolean isFailed() {
      return failed;
    }

    public void run() {
      logger.debug("Running " + this);
      int runCount = 50;
      int i = 0;
      failed = false;
      int deadLockCount = 0;
      while (i < runCount) {
        boolean deadlocked = false;
        try {
          FortressUserNode fu = fortressService.getFortressUser(this.fortress, uname);
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

        if (!deadlocked) {
          i++;
        }
      } // End while
      logger.debug("Finishing {}", failed);
      failed = false;
      latch.countDown();
    }
  }
}
