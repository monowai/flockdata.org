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

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.track.bean.SearchChange;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mholdsworth
 * @since 30/09/2014
 */
public class TestEntityUsers extends EngineBase {

  private Logger logger = LoggerFactory.getLogger(TestEntityTrack.class);

  @Test
  public void created_UserAgainstEntityAndLog() throws Exception {
    logger.debug("### created_UserAgainstEntityAndLog");
    String entityCode = "mk1hz";
    SystemUser su = registerSystemUser("created_UserAgainstEntityAndLog");

    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("created_UserAgainstEntityAndLog", true));
    EntityInputBean entityBean = new EntityInputBean(fortress, "poppy", "CompanyNode", DateTime.now(), entityCode);


    entityBean.setContent(new ContentInputBean("billie", null, DateTime.now(), ContentDataHelper.getSimpleMap("name", "a"), "Answer"));
    mediationFacade.trackEntity(su.getCompany(), entityBean);

    EntityNode entity = (EntityNode) entityService.findByCode(fortress, "CompanyNode", entityCode);
    Assert.assertEquals("poppy", entity.getCreatedBy().getCode().toLowerCase());

    Collection<EntityLogResult> logs = entityService.getEntityLogs(su.getCompany(), entity.getKey());
    assertEquals(1, logs.size());
    EntityLogResult log = logs.iterator().next();
    assertEquals("billie", log.getMadeBy().toLowerCase());

    entityBean.setContent(new ContentInputBean("nemo", DateTime.now(), ContentDataHelper.getSimpleMap("name", "b")));
    mediationFacade.trackEntity(su.getCompany(), entityBean);
    assertTrue("Event name incorrect", log.getEvent().getCode().equalsIgnoreCase("answer"));

    entity = (EntityNode) entityService.findByCode(fortress, "CompanyNode", entityCode);
    Assert.assertEquals("poppy", entity.getCreatedBy().getCode().toLowerCase());

    logs = entityService.getEntityLogs(su.getCompany(), entity.getKey());
    assertTrue(logs.size() == 2);
    boolean billieFound = false;
    boolean nemoFound = false;
    for (EntityLogResult entityLog : logs) {
      if (entityLog.getMadeBy().equals("billie")) {
        billieFound = true;
      }
      if (entityLog.getMadeBy().equals("nemo")) {
        nemoFound = true;
      }
    }
    assertTrue("Didn't find Billie & Nemo", billieFound && nemoFound);


  }

  @Test
  public void created_EntityWithNoUser() throws Exception {
    logger.debug("### created_EntityWithNoUser");
    String callerRef = "mk1hz";
    SystemUser su = registerSystemUser("created_EntityWithNoUser");

    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("created_EntityWithNoUser", true));
    EntityInputBean entityBean = new EntityInputBean(fortress, null, "CompanyNode", DateTime.now(), callerRef);

    // No fortress user
    ContentInputBean contentInputBean = new ContentInputBean(null, null, DateTime.now(), ContentDataHelper.getSimpleMap("name", "a"), "Answer");
    entityBean.setContent(contentInputBean);
    TrackResultBean resultBean = mediationFacade.trackEntity(su.getCompany(), entityBean);

    EntityNode entity = (EntityNode) entityService.findByCode(fortress, "CompanyNode", callerRef);
    Assert.assertEquals(null, entity.getCreatedBy());

    SearchChange searchChange = searchService.getEntityChange(resultBean);
    assertNotNull(searchChange);

    searchChange = searchService.rebuild(entity, resultBean.getCurrentLog());
    assertNotNull(searchChange);

  }
}
