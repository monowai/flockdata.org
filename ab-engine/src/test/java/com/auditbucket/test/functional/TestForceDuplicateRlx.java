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

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * User: Mike Holdsworth
 * Since: 1/12/13
 */
public class TestForceDuplicateRlx extends TestEngineBase {

    private Logger logger = LoggerFactory.getLogger(TestForceDuplicateRlx.class);

    @Test
    public void uniqueChangeRLXUnderLoad() throws Exception {
        logger.info("uniqueChangeRLXUnderLoad started");
        regService.registerSystemUser(new RegistrationBean("TestTrack", mike).setIsUnique(false));

        int auditMax = 10;
        int logMax = 10;
        int fortress = 1;
        String simpleJson = "{\"who\":";
        ArrayList<Long> list = new ArrayList<>();

        int fortressMax = 1;
        logger.info("FortressCount: " + fortressMax + " AuditCount: " + auditMax + " LogCount: " + logMax);
        logger.info("We will be expecting a total of " + (auditMax * logMax * fortressMax) + " messages to be handled");

        StopWatch watch = new StopWatch();
        watch.start();
        double splitTotals = 0;
        long totalRows = 0;
        int auditSleepCount;  // Discount all the time we spent sleeping

        DecimalFormat f = new DecimalFormat("##.000");

        while (fortress <= fortressMax) {
            String fortressName = "bulkloada" + fortress;
            int audit = 1;
            long requests = 0;
            auditSleepCount = 0;

            Fortress iFortress = fortressService.registerFortress(new FortressInputBean(fortressName, true));
            requests++;
            logger.info("Starting run for " + fortressName);
            while (audit <= auditMax) {
                MetaInputBean aib = new MetaInputBean(iFortress.getName(), fortress + "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC" + audit);
                TrackResultBean arb = trackEP.trackHeader(aib, null, null).getBody();
                requests++;
                int log = 1;
                while (log <= logMax) {
                    createLog(simpleJson, aib, arb, log);
                    requests++;
                    log++;
                } // Logs created
                audit++;
            } // Audit headers finished with
            watch.split();
            double fortressRunTime = (watch.getSplitTime() - auditSleepCount) / 1000d;
            logger.info("*** " + iFortress.getName() + " took " + fortressRunTime + "  avg processing time for [" + requests + "] RPS= " + f.format(fortressRunTime / requests) + ". Requests per second " + f.format(requests / fortressRunTime));

            splitTotals = splitTotals + fortressRunTime;
            totalRows = totalRows + requests;
            watch.reset();
            watch.start();
            list.add(iFortress.getId());
            fortress++;
        }

        logger.info("*** Created data set in " + f.format(splitTotals) + " fortress avg = " + f.format(splitTotals / fortressMax) + " avg processing time per request " + f.format(splitTotals / totalRows) + ". Requests per second " + f.format(totalRows / splitTotals));
        watch.reset();
    }
    private void createLog(String simpleJson, MetaInputBean aib, TrackResultBean arb, int log) throws DatagioException, IOException {
        trackEP.trackLog(new LogInputBean(arb.getMetaKey(), aib.getFortressUser(), new DateTime(), simpleJson + log + "}"), null, null);
    }


}
