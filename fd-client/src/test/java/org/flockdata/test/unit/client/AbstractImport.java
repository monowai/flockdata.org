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

package org.flockdata.test.unit.client;

import org.flockdata.shared.ClientConfiguration;
import org.flockdata.shared.FileProcessor;
import org.flockdata.transform.PayloadBatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Simple ancestor for encapsulating profile and writer functionality
 *
 * Created by mike on 12/02/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({"dev", "fd-batch"})
@SpringApplicationConfiguration({
        ClientConfiguration.class,
        MockPayloadBatcher.class,
        FileProcessor.class,
        MockFdWriter.class,
})
@TestPropertySource({"/application_dev.properties"})
public class AbstractImport {
    // Re-implement an FdWriter class if you want to validate data in the flush routines

    @Autowired
    protected PayloadBatcher fdBatcher;
    @Autowired
    protected FileProcessor fileProcessor;
    @Autowired
    protected ClientConfiguration clientConfiguration;

    protected PayloadBatcher getFdBatcher(){
        return fdBatcher;
    }

    @Before
    public void clearLoader(){
        // PayloadBatcher is normally cleared down when it is flushed at the end of an import process
        // For testing purposes we need to analyse the batched payload without flushing
        // If we don't reset the batched payload then any previous runs contents will be in the result
        fdBatcher.reset();
    }

    @Test
    public void autoWiringWorks(){
        assertNotNull(clientConfiguration);
        assertTrue(""+clientConfiguration.getBatchSize(),clientConfiguration.getBatchSize()>10);
        assertNotNull(fdBatcher);
        assertNotNull(fileProcessor);
    }


}
