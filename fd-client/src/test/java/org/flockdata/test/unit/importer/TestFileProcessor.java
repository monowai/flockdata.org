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

package org.flockdata.test.unit.importer;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collection;
import org.flockdata.integration.FileProcessor;
import org.junit.Test;

/**
 * Validate File Processor routines - Please add to this as you can
 *
 * @author mholdsworth
 * @since 24/03/2015
 */
public class TestFileProcessor {
    @Test
    public void validate_RowsToProcess() throws Exception {
        FileProcessor fp = new FileProcessor();
        assertFalse("No check should be made", fp.stopProcessing(1));

        fp = new FileProcessor(50, 50);
        // Skip the first 50 rows
        assertFalse("Less than skip count failed", fp.stopProcessing(49));
        assertFalse("Processing should continue", fp.stopProcessing(50));
        assertFalse("Processing should start", fp.stopProcessing(51));
        assertTrue("Processing should stop", fp.stopProcessing(100));
        assertTrue("Processing should stop", fp.stopProcessing(101));
        assertTrue("Processing should really have stopped", fp.stopProcessing(101));

        fp = new FileProcessor(50000, 10);
        assertFalse("Processing should continue with no log message", fp.stopProcessing(1));

        fp = new FileProcessor(50000, 1);
        assertFalse("Processing should not continue", fp.stopProcessing(50000));

    }

    @Test
    public void collection_FilesInDirectory() throws Exception {

        FileProcessor fileProcessor = new FileProcessor();
        Collection<String> files = fileProcessor.resolveFiles("/data/dummy1.json");
        assertFalse(files.isEmpty());
        assertEquals(1, files.size());

        files = fileProcessor.resolveFiles("./data/*.json");
        assertFalse(files.isEmpty());
        assertEquals(2, files.size());

        files = fileProcessor.resolveFiles("./data/*.json");
        assertFalse(files.isEmpty());
        assertEquals(2, files.size());

        files = fileProcessor.resolveFiles("./data/*");
        assertFalse(files.isEmpty());
        assertTrue("Not enough files found", files.size() > 5);

        files = fileProcessor.resolveFiles("./data/");
        assertFalse(files.isEmpty());
        assertTrue("Not enough files found", files.size() > 5);

        files = fileProcessor.resolveFiles("/model/csvtest.json");
        assertFalse(files.isEmpty());
        assertEquals(1, files.size());

    }
}
