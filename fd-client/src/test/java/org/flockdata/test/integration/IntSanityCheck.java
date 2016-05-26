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
import org.flockdata.client.commands.Health;
import org.flockdata.client.commands.Login;
import org.flockdata.client.commands.Ping;
import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.shared.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.flockdata.test.integration.IntegrationHelper.ADMIN_REGRESSION_PASS;
import static org.flockdata.test.integration.IntegrationHelper.ADMIN_REGRESSION_USER;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Basic connectivity checks
 *
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
public class IntSanityCheck {

    @Autowired ClientConfiguration clientConfiguration;
    @Autowired FdRestWriter fdRestWriter;
    @Autowired IntegrationHelper integrationHelper;

    // Uncomment this ClassRue to run the stack only for this class
    //  otherwise levae it commented and run the Suite
//    @ClassRule
//    public static FdDocker stack = new FdDocker();

    @Before
    public void setupServices(){
        integrationHelper.waitForServices();
    }

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

    @Test
    public void healthChecks() {
        // If the services can't see each other, its not worth proceeding
        integrationHelper.login(fdRestWriter, ADMIN_REGRESSION_USER, "123").exec();
        Login login = integrationHelper.login(ADMIN_REGRESSION_USER, ADMIN_REGRESSION_PASS);
        integrationHelper.assertWorked("Login error ", login.exec());
        assertTrue("Unexpected login error "+login.error(), login.worked());
        Health health = new Health(clientConfiguration, fdRestWriter);
        integrationHelper.assertWorked("Health Check", health.exec());

        Map<String, Object> healthResult = health.result();
        assertTrue("Should be more than 1 entry in the health results", healthResult.size() > 1);
        assertNotNull("Could not find an entry for fd-search", healthResult.get("fd-search"));
        assertTrue("Failure for fd-engine to connect to fd-search in the container", healthResult.get("fd-search").toString().toLowerCase().startsWith("ok"));
        assertNotNull("Could not find an entry for fd-store", healthResult.get("fd-store"));
        assertTrue("Failure for fd-engine to connect to fd-store in the container", healthResult.get("fd-store").toString().toLowerCase().startsWith("ok"));


    }

}
