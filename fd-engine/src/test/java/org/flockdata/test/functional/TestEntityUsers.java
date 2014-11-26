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

package org.flockdata.test.functional;

import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.test.utils.Helper;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.EntityLog;
import org.junit.Assert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * User: mike
 * Date: 30/09/14
 * Time: 9:39 AM
 */
public class TestEntityUsers extends EngineBase {

        private Logger logger = LoggerFactory.getLogger(TestTrack.class);

        @org.junit.Before
        public void setup(){
            engineConfig.setDuplicateRegistration(true);
        }

        @Test
        public void created_UserAgainstEntityAndLog() throws Exception {
            logger.debug("### created_UserAgainstEntityAndLog");
            String callerRef = "mk1hz";
            SystemUser su = registerSystemUser("created_UserAgainstEntityAndLog");

            Fortress fortWP = fortressService.registerFortress(su.getCompany(), new FortressInputBean("created_UserAgainstEntityAndLog", true));
            EntityInputBean entityBean = new EntityInputBean(fortWP.getName(), "poppy", "CompanyNode", DateTime.now(), callerRef);


            entityBean.setContent(new ContentInputBean("billie", null, DateTime.now(), Helper.getSimpleMap("name", "a"), "Answer"));
            mediationFacade.trackEntity(su.getCompany(), entityBean);
            logger.info("Tracked...");
            Entity entity = trackService.findByCallerRef(fortWP, "CompanyNode", callerRef);
            Assert.assertEquals("poppy", entity.getCreatedBy().getCode().toLowerCase());

            Set<EntityLog> logs = trackService.getEntityLogs(su.getCompany(), entity.getMetaKey());
            assertEquals(1, logs.size());
            EntityLog log = logs.iterator().next();
            assertEquals("billie", log.getLog().getWho().getCode().toLowerCase());

            entityBean.setContent(new ContentInputBean("nemo", DateTime.now(), Helper.getSimpleMap("name", "b")));
            mediationFacade.trackEntity(su.getCompany(), entityBean);
            assertTrue("Event name incorrect", log.getLog().getEvent().getCode().equalsIgnoreCase("answer"));

            entity = trackService.findByCallerRef(fortWP, "CompanyNode", callerRef);
            Assert.assertEquals("poppy", entity.getCreatedBy().getCode().toLowerCase());

            logs = trackService.getEntityLogs(su.getCompany(), entity.getMetaKey());
            assertTrue(logs.size()==2);
            boolean billieFound = false;
            boolean nemoFound = false;
            for (EntityLog entityLog : logs) {
                if ( entityLog.getLog().getWho().getCode().equals("billie"))
                    billieFound = true;
                if (entityLog.getLog().getWho().getCode().equals("nemo"))
                    nemoFound = true;
            }
            assertTrue("Didn't find Billie & Nemo", billieFound&&nemoFound);


        }
}
