/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.functional;

import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.test.helper.EntityContentHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 12/11/14
 * Time: 1:44 PM
 */
public class TestContentDuplicate  extends  EngineBase{
    private Logger logger = LoggerFactory.getLogger(TestEntityTrack.class);

    @org.junit.Before
    public void setup(){
        engineConfig.setDuplicateRegistration(true);
    }

    @Test
    public void reprocess_HistoricContentsNotCreated() throws Exception {
        logger.debug("### reprocess_HistoricContentsNotCreated");
        SystemUser su = registerSystemUser("reprocess_HistoricContentsNotCreated");

        Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("reprocess_HistoricContentsNotCreated", true));
        EntityInputBean inputBean = new EntityInputBean(fortress, "poppy", "TestDoc", DateTime.now(), "123");

        int max = 5;
        List<ContentInputBean> contentBeans = new ArrayList<>();
        for (int i=0; i<max; i++){
            ContentInputBean contentBean = new ContentInputBean("poppy", DateTime.now(), EntityContentHelper.getSimpleMap("name", "a" +i));
            contentBeans.add(contentBean);
            inputBean.setContent(contentBean);
            mediationFacade.trackEntity(su.getCompany(), inputBean);
        }
        Entity entity = entityService.findByCode(su.getCompany(), fortress.getName(), "TestDoc", "123");
        assertEquals(max, entityService.getLogCount(su.getCompany(), entity.getMetaKey()));

        // Reprocess forward
        for (ContentInputBean contentBean : contentBeans) {
            inputBean.setContent(contentBean);
            mediationFacade.trackEntity(su.getCompany(), inputBean);
        }

        assertEquals(max, entityService.getLogCount(su.getCompany(), entity.getMetaKey()));

        // Try reversing out of order
        Collections.reverse(contentBeans);
        for (ContentInputBean contentBean : contentBeans) {
            inputBean.setContent(contentBean);
            mediationFacade.trackEntity(su.getCompany(), inputBean);
        }

        assertEquals(max, entityService.getLogCount(su.getCompany(), entity.getMetaKey()));


    }
}
