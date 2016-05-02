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

package org.flockdata.test.integration;

import org.flockdata.client.amqp.AmqpServices;
import org.flockdata.client.commands.Ping;
import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.shared.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Created by mike on 6/05/16.
 */
@SpringApplicationConfiguration({
        ClientConfiguration.class,
        FdBatcher.class,
        FdRestWriter.class,
        Exchanges.class,
        FileProcessor.class,
        IntegrationHelper.class,
        AmqpRabbitConfig.class,
        AmqpServices.class

})
@RunWith(SpringJUnit4ClassRunner.class)
@Configuration
@ActiveProfiles("fd-server")
public class SanityCheckTesting {

    @Autowired ClientConfiguration clientConfiguration;
    @Autowired FdRestWriter fdRestWriter;
    @Autowired IntegrationHelper integrationHelper;

    // Uncomment this ClassRue to run the stack only for this class
    //  otherwise levae it commented and run the Suite
//    @ClassRule
//    public static FdDocker stack = new FdDocker();

    @Test
    public void pingFdStore() {
        clientConfiguration.setServiceUrl(integrationHelper.getStore());
        String result = fdRestWriter.ping();
        assertEquals("Couldn't ping fd-store", "pong", result);
    }

    @Test
    public void pingFdEngine() {
        String result = fdRestWriter.ping();
        assertEquals("Couldn't ping fd-engine", "pong", result);
    }

    @Test
    public void pingFdSearch() {
        clientConfiguration.setServiceUrl(integrationHelper.getSearch());

        Ping ping = new Ping(clientConfiguration, fdRestWriter);
        ping.exec();
        assertTrue(ping.error(), ping.worked());

        assertEquals("Couldn't ping fd-search", "pong", ping.result());
    }

}
