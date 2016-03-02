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

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.flockdata.client.Importer;
import org.flockdata.shared.ClientConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Command line configuration testing
 * <p/>
 * Created by mike on 6/01/16.
 */
public class TestConfiguration {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void manualConfig() throws Exception {
//        String[] args = {"-"+ClientConfiguration.AMQP+"=true", "-"+ClientConfiguration.KEY_BATCH_SIZE+"=10", "-x=55", "-cp=src/test/resources"};
        String[] args = { "-"+ClientConfiguration.KEY_BATCH_SIZE+"=10", "-x=55", "-cp=src/test/resources"};
        ClientConfiguration configuration = Importer.getConfiguration(args);
        assertTrue(configuration.isAmqp());
        assertEquals(10, configuration.getBatchSize());
        assertEquals("abc123", configuration.getApiKey());
        assertEquals(55, configuration.getStopRowProcessCount());

    }

    @Test
    public void illegalPath() throws Exception {
        String[] args = {"-"+ClientConfiguration.AMQP+"=true", "-x=55", "-c src/test/resources/nonexistent.props"};
        ClientConfiguration configuration = Importer.getConfiguration(args);
        assertEquals(null, configuration.getApiKey());
    }


    @Test
    public void illegalArgument() throws Exception {
        // the setting is -amqp. check we get an error when a typo occurs
        String[] args = {"-amqpx=true"};
        exception.expect(ArgumentParserException.class);
        Importer.getConfiguration(args);

    }
}
