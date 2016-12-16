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

package org.flockdata.spring.service;

import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.FdPayloadWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
@TestPropertySource("/fd-client-config.properties")
@ContextConfiguration(classes = {
        ClientConfiguration.class,
        FdPayloadWriter.class,
        SimpleTrackedService.class
})
public class SimpleFlockServiceTest {

    @Autowired
    private SimpleTrackedService simpleTrackedService;

    @Test
    public void testCreateEntityAnnotation() {
        SimpleTrackedService.Customer customer = new SimpleTrackedService.Customer();
        customer.setId(1L);
        customer.setName("name");
        customer.setEmail("email@email.com");
        simpleTrackedService.save(customer);
    }

    @Test
    public void testCreateEntityLogAnnotation() {
        SimpleTrackedService.Customer customer = new SimpleTrackedService.Customer();
        customer.setId(1L);
        customer.setName("name");
        customer.setEmail("email@email.com");
        simpleTrackedService.update(customer);
    }
}
