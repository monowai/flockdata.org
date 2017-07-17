/*
 *  Copyright 2012-2017 the original author or authors.
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

import org.flockdata.client.FdClientIo;
import org.flockdata.client.FdTemplate;
import org.flockdata.client.amqp.FdRabbitClient;
import org.flockdata.client.commands.*;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.Exchanges;
import org.flockdata.integration.FileProcessor;
import org.flockdata.transform.FdIoInterface;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author mike
 * @tag
 * @since 17/12/16
 */
@ContextConfiguration(classes = {
        ClientConfiguration.class,
        FdTemplate.class,
        FdClientIo.class,
        FdRabbitClient.class,
        Login.class,
        SearchEsPost.class,
        AmqpRabbitConfig.class,
        Exchanges.class,
        ModelGet.class,
        ModelPost.class,
        ModelFieldStructure.class,
        FileProcessor.class

})
@RunWith(SpringRunner.class)
@ActiveProfiles({"fd-client", "fd-server", "dev"})
public class TestWiring {

    @Autowired
    private FdIoInterface fdIoInterface;
    @Autowired
    private Exchanges exchanges;
    @Autowired
    private FdRabbitClient rabbitClient;
    @Autowired
    private FdClientIo template;

    @Autowired
    private ClientConfiguration clientConfiguration;

    @Test
    public void wiringWorks() throws Exception {
        assertNotNull(exchanges);
        assertNotNull(rabbitClient);
        assertNotNull(template);
        assertNotNull(fdIoInterface);
        assertNotNull(clientConfiguration);
    }

    @Test
    public void clientDefaults () throws Exception{
        assertNull(clientConfiguration.getApiKey());
        assertNotNull(clientConfiguration.getHttpUser());
    }
}
