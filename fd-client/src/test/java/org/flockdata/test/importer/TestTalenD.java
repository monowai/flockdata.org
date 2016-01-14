package org.flockdata.test.importer;

import org.flockdata.client.Importer;
import org.flockdata.transform.ClientConfiguration;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;

/**
 * Emulates the flow one uses in TalenD
 * Created by mike on 13/01/16.
 */
public class TestTalenD {
    @Test
    public void args() throws Exception{
        String[] args = {"-amqp=true", "-b 10", "-c ./src/test/resources/client.config"};
        ClientConfiguration configuration = Importer.getConfiguration(args);
        assertFalse(configuration.isDefConfig());
        System.out.println(System.getProperty("user.dir"));
    }
}
