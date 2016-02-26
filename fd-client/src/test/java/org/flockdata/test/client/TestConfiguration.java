/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.test.client;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.flockdata.client.Importer;
import org.flockdata.transform.ClientConfiguration;
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
