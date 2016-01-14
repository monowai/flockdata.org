/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.spring.service;

import org.flockdata.search.IndexHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:fd-spring.xml", "classpath:fd-spring-test.xml"
})
public class SimpleFlockServiceTest {
    @Autowired
    private SimpleTrackedService simpleTrackedService;
    @Autowired
    IndexHelper indexHelper;

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
