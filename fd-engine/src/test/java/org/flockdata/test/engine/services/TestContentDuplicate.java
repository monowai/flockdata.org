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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.flockdata.data.Entity;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mholdsworth
 * @since 12/11/2014
 */
public class TestContentDuplicate extends EngineBase {
  private Logger logger = LoggerFactory.getLogger(TestEntityTrack.class);

  @Test
  public void reprocess_HistoricContentsNotCreated() throws Exception {
    logger.debug("### reprocess_HistoricContentsNotCreated");
    SystemUser su = registerSystemUser("reprocess_HistoricContentsNotCreated");

    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("reprocess_HistoricContentsNotCreated", true));
    EntityInputBean inputBean = new EntityInputBean(fortress, "poppy", "TestDoc", DateTime.now(), "123");

    int max = 5;
    List<ContentInputBean> contentBeans = new ArrayList<>();
    for (int i = 0; i < max; i++) {
      ContentInputBean contentBean = new ContentInputBean("poppy", DateTime.now(), ContentDataHelper.getSimpleMap("name", "a" + i));
      contentBeans.add(contentBean);
      inputBean.setContent(contentBean);
      mediationFacade.trackEntity(su.getCompany(), inputBean);
    }
    Entity entity = entityService.findByCode(su.getCompany(), fortress.getName(), "TestDoc", "123");
    assertEquals(max, entityService.getLogCount(su.getCompany(), entity.getKey()));

    // Reprocess forward
    for (ContentInputBean contentBean : contentBeans) {
      inputBean.setContent(contentBean);
      mediationFacade.trackEntity(su.getCompany(), inputBean);
    }

    assertEquals(max, entityService.getLogCount(su.getCompany(), entity.getKey()));

    // Try reversing out of order
    Collections.reverse(contentBeans);
    for (ContentInputBean contentBean : contentBeans) {
      inputBean.setContent(contentBean);
      mediationFacade.trackEntity(su.getCompany(), inputBean);
    }

    assertEquals(max, entityService.getLogCount(su.getCompany(), entity.getKey()));


  }
}
