/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.test.engine.suites;

import org.flockdata.test.engine.services.TestAdmin;
import org.flockdata.test.engine.services.TestCallerCode;
import org.flockdata.test.engine.services.TestEntitySearch;
import org.flockdata.test.engine.services.TestEntityTrack;
import org.flockdata.test.engine.services.TestLogCounts;
import org.flockdata.test.engine.services.TestTrackEvents;
import org.flockdata.test.engine.services.TestTxReference;
import org.flockdata.test.engine.unit.TestBatchSplitter;
import org.flockdata.test.engine.unit.TestInputBeans;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author mholdsworth
 * @since 2/04/2015
 */
@RunWith(Suite.class)
@Suite.SuiteClasses( {
    TestAdmin.class,
    TestInputBeans.class,
    TestTxReference.class,
    TestEntityTrack.class,
    TestBatchSplitter.class,
    TestLogCounts.class,
    TestEntitySearch.class,
    TestCallerCode.class,
    TestTrackEvents.class
})
public class Track {
  Track() {
  }
}
