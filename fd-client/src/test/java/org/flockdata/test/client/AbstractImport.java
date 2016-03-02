/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.client;

import org.flockdata.shared.ClientConfiguration;

/**
 * Simple ancestor for encapsulating profile and writer functionality
 *
 * Created by mike on 12/02/15.
 */

public class AbstractImport {
    // Re-implement an FdWriter class if you want to validate data in the flush routines

    private static MockFdWriter fdWriter = new MockFdWriter();

    protected ClientConfiguration getClientConfiguration() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setBatchSize(100);
        return clientConfiguration;
    }

    public static MockFdWriter getFdWriter() {
        return fdWriter;
    }

}
