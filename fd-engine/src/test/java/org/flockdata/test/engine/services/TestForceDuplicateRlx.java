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

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import org.flockdata.data.Fortress;
import org.flockdata.data.SystemUser;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

/**
 * @author mholdsworth
 * @since 1/12/2013
 */
public class TestForceDuplicateRlx extends EngineBase {

  private Logger logger = LoggerFactory.getLogger(TestForceDuplicateRlx.class);

  @Test
  public void uniqueChangeRLXUnderLoad() throws Exception {
    logger.debug("### uniqueChangeRLXUnderLoad started");
    SystemUser su = registerSystemUser("TestTrack", mike_admin);

    int auditMax = 10;
    int logMax = 10;
    int fName = 1;
    ArrayList<Long> list = new ArrayList<>();

    int fortressMax = 1;
    logger.debug("FortressCount: " + fortressMax + " AuditCount: " + auditMax + " LogCount: " + logMax);
    logger.debug("We will be expecting a total of " + (auditMax * logMax * fortressMax) + " messages to be handled");

    StopWatch watch = new StopWatch();
    watch.start();
    double splitTotals = 0;
    long totalRows = 0;

    DecimalFormat f = new DecimalFormat("##.000");

    while (fName <= fortressMax) {
      String fortressName = "bulkloada" + fName;
      int count = 1;
      long requests = 0;

      Fortress fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean(fortressName, true));
      requests++;
      logger.info("Starting run for " + fortressName);
      while (count <= auditMax) {
        EntityInputBean entityInputBean = new EntityInputBean(fortress, fortress.getName() + "olivia@sunnybell.com", "CompanyNode", new DateTime(), "ABC" + count);
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
      totalRows = totalRows + requests;
      list.add(fortress.getId());
      fName++;
    }

    logger.debug("*** Created data set in " + f.format(splitTotals) + " fortress avg = " + f.format(splitTotals / fortressMax) + " avg processing time per request " + f.format(splitTotals / totalRows) + ". Requests per second " + f.format(totalRows / splitTotals));
  }

  private void createLog(SystemUser su, TrackResultBean arb, int log) throws FlockException, IOException, ExecutionException, InterruptedException {
    mediationFacade.trackLog(su.getCompany(), new ContentInputBean("who cares", arb.getEntity().getKey(), new DateTime(), ContentDataHelper.getSimpleMap("who", log)));
  }


}
