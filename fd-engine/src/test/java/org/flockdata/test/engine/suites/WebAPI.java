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

package org.flockdata.test.engine.suites;

/**
 * Suite for Web services
 *
 * @author mholdsworth
 * @since 2/04/2015
 */

import org.flockdata.test.engine.mvc.TestAPISecurity;
import org.flockdata.test.engine.mvc.TestAdminCalls;
import org.flockdata.test.engine.mvc.TestAuthenticationEP;
import org.flockdata.test.engine.mvc.TestCompanyEP;
import org.flockdata.test.engine.mvc.TestContentModel;
import org.flockdata.test.engine.mvc.TestDocEP;
import org.flockdata.test.engine.mvc.TestEntityEP;
import org.flockdata.test.engine.mvc.TestFortressEP;
import org.flockdata.test.engine.mvc.TestNeoRestInterface;
import org.flockdata.test.engine.mvc.TestPathEP;
import org.flockdata.test.engine.mvc.TestSystemUserRegistration;
import org.flockdata.test.engine.mvc.TestTagEP;
import org.flockdata.test.engine.mvc.TestTrackEP;
import org.flockdata.test.engine.services.TestApiKeyInterceptor;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {
    TestAdminCalls.class,
    TestAPISecurity.class,
    TestAuthenticationEP.class,
    TestCompanyEP.class,
    TestDocEP.class,
    TestFortressEP.class,
    TestNeoRestInterface.class,
    TestPathEP.class,
    TestSystemUserRegistration.class,
    TestTagEP.class,
    TestTrackEP.class,
    TestContentModel.class,
    TestEntityEP.class,
    TestApiKeyInterceptor.class


})
public class WebAPI {
    public WebAPI() {
    }
}
