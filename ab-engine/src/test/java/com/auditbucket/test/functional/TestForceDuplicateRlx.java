/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
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

import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.test.utils.Helper;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.TrackResultBean;

import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * User: Mike Holdsworth
 * Since: 1/12/13
 */
public class TestForceDuplicateRlx extends TestEngineBase {

    private Logger logger = LoggerFactory.getLogger(TestForceDuplicateRlx.class);

    @Test
    @Transactional
    public void uniqueChangeRLXUnderLoad() throws Exception {
        logger.info("uniqueChangeRLXUnderLoad started");
        SystemUser su = registerSystemUser("TestTrack", mike_admin);

        int auditMax = 10;
        int logMax = 10;
        int fortress = 1;
        //String simpleJson = "{\"who\":";
        ArrayList<Long> list = new ArrayList<>();

        int fortressMax = 1;
        logger.info("FortressCount: " + fortressMax + " AuditCount: " + auditMax + " LogCount: " + logMax);
        logger.info("We will be expecting a total of " + (auditMax * logMax * fortressMax) + " messages to be handled");

        StopWatch watch = new StopWatch();
        watch.start();
        double splitTotals = 0;
        long totalRows = 0;
        int sleepCount;  // Discount all the time we spent sleeping

        DecimalFormat f = new DecimalFormat("##.000");

        while (fortress <= fortressMax) {
            String fortressName = "bulkloada" + fortress;
            int count = 1;
            long requests = 0;
            sleepCount = 0;

            Fortress iFortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean(fortressName, true));
            requests++;
            logger.info("Starting run for " + fortressName);
            while (count <= auditMax) {
                EntityInputBean entityInputBean = new EntityInputBean(iFortress.getName(), fortress + "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC" + count);
                TrackResultBean arb = mediationFacade.trackEntity(su.getCompany(), entityInputBean);
                requests++;
                int log = 1;
                while (log <= logMax) {
                    createLog(su, arb, log);
                    requests++;
                    log++;
                } // Logs created
                count++;
            } // finished with Entities
//            watch.split();
//            double fortressRunTime = (watch.getSplitTime() - sleepCount) / 1000d;
//            logger.info("*** " + iFortress.getName() + " took " + fortressRunTime + "  avg processing time for [" + requests + "] RPS= " + f.format(fortressRunTime / requests) + ". Requests per second " + f.format(requests / fortressRunTime));

//            splitTotals = splitTotals + fortressRunTime;
            totalRows = totalRows + requests;
//            watch.reset();
//            watch.start();
            list.add(iFortress.getId());
            fortress++;
        }

        logger.info("*** Created data set in " + f.format(splitTotals) + " fortress avg = " + f.format(splitTotals / fortressMax) + " avg processing time per request " + f.format(splitTotals / totalRows) + ". Requests per second " + f.format(totalRows / splitTotals));
//        watch.reset();
    }
    private void createLog(SystemUser su, TrackResultBean arb, int log) throws FlockException, IOException, ExecutionException, InterruptedException {
        mediationFacade.trackLog(su.getCompany(), new ContentInputBean("who cares", arb.getMetaKey(), new DateTime(), Helper.getSimpleMap("who", log)));
    }


}
