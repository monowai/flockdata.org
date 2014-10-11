package com.auditbucket.test.functional;

import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.test.utils.Helper;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.model.EntityLog;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static junit.framework.Assert.assertEquals;
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
