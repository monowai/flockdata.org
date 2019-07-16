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

/**
 * Primarily focused on Entities
 *
 * @author mholdsworth
 * @since 2/04/2015
 */

import org.flockdata.test.engine.services.TestCallerCode;
import org.flockdata.test.engine.services.TestEntityCrossLink;
import org.flockdata.test.engine.services.TestEntityLinks;
import org.flockdata.test.engine.services.TestEntityUsers;
import org.flockdata.test.engine.unit.TestEntityKeys;
import org.flockdata.test.engine.unit.TestEntitySearch;
import org.flockdata.test.engine.unit.TestHashcodeAndEquality;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {
    TestHashcodeAndEquality.class,
    TestEntityKeys.class,
    TestEntitySearch.class,
    TestCallerCode.class,
    TestEntityUsers.class,
    TestEntityCrossLink.class,
    TestEntityLinks.class
})
public class Entity {
    Entity() {
    }
}
