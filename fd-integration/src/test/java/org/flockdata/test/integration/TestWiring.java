/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.integration;

import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.Exchanges;
import org.flockdata.integration.FileProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static junit.framework.TestCase.assertNotNull;

/**
 * @author mike
 * @tag
 * @since 17/12/16
 */
@ContextConfiguration(classes = {
        ClientConfiguration.class,
        AmqpRabbitConfig.class,
        Exchanges.class,
        FileProcessor.class

})
@RunWith(SpringRunner.class)
@ActiveProfiles({"fd-client", "fd-server", "dev"})
public class TestWiring {
    @Autowired
    private ClientConfiguration clientConfiguration;
    @Autowired
    private AmqpRabbitConfig rabbitConfig;
    @Autowired
    private Exchanges exchanges;
    @Autowired
    private FileProcessor fileProcessor;

    @Test
    public void autowiring() throws Exception {
        // Ensure basic components deploy with sensible defaults
        assertNotNull(clientConfiguration);
        assertNotNull(rabbitConfig);
        assertNotNull(exchanges);
        assertNotNull(fileProcessor);
    }
}
