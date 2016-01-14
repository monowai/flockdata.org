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

import org.flockdata.spring.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SimpleTrackedService {
    private static Logger logger = LoggerFactory.getLogger(SimpleTrackedService.class);

    @FlockEntity
    public Customer save(Customer customer) {
        logger.info("call save Method");
        if (customer.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,3}$")) {
            customer.setId(324325L);
            return customer;
        } else {
            throw new IllegalArgumentException("invalid email");
        }
    }

    @FlockLog
    public Customer update(Customer customer) {
        logger.info("call update Method");
        if (customer.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,3}$")) {
            customer.setId(324325L);
            return customer;
        } else {
            throw new IllegalArgumentException("invalid email");
        }
    }

    @Trackable
    public static class Customer {
        @DatagioUid
        private Long id;

        private String name;

        @DatagioCallerRef
        private String email;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String toString() {
            return String.format("[id=%s,name=%s,email=%s]", id, name, email);
        }
    }
}
