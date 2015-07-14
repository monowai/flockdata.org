/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.test.engine.suites;

import org.flockdata.test.engine.functional.*;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.springframework.retry.annotation.EnableRetry;

/**
 * The periodic failing of TestEntityDeadlock. Batching tests in to a suite for convenience
 *
 * Created by mike on 2/04/15.
 */


@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestCsvImportIntegration.class,
        TestBatch.class,
        TestCallerRef.class,
        TestEntityDeadlock.class,
        TestForceDuplicateRlx.class,
        TestNonTransactional.class,
        TestTagDeadlock.class,
        TestSchemaManagement.class
})
@EnableRetry
public class Concurrency {

    public Concurrency(){}

}
