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

package org.flockdata.test.client;

import junit.framework.TestCase;
import org.flockdata.client.Configure;
import org.flockdata.transform.ClientConfiguration;

import java.io.File;

/**
 * Simple ancestor for encapsulating profile and writer functionality
 *
 * Created by mike on 12/02/15.
 */
public class AbstractImport {
    // Re-implement an FdWriter class if you want to validate data in the flush routines

    private static MockFdWriter fdWriter = new MockFdWriter();

    static ClientConfiguration getClientConfiguration(String jsonConfig) {
        File file = new File(jsonConfig);
        ClientConfiguration configuration = Configure.readConfiguration(file);
        TestCase.assertNotNull(configuration);
        if ( configuration.getDefaultUser() == null )
            configuration.setDefaultUser("test");
        return configuration;
    }

    public static MockFdWriter getFdWriter() {
        return fdWriter;
    }

}
