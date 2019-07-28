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

import org.flockdata.spring.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SimpleTrackedService {
  private static Logger logger = LoggerFactory.getLogger(SimpleTrackedService.class);

  @FlockEntity
  Customer save(Customer customer) {
    logger.info("call save Method");
    if (customer.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,3}$")) {
      customer.setId(324325L);
      return customer;
    } else {
      throw new IllegalArgumentException("invalid email");
    }
  }

  @FlockLog
  Customer update(Customer customer) {
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
    @FdUid
    private Long id;

    private String name;

    @FdCallerRef
    private String email;

    public Long getId() {
      return id;
    }

    void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    void setName(String name) {
      this.name = name;
    }

    String getEmail() {
      return email;
    }

    void setEmail(String email) {
      this.email = email;
    }

    public String toString() {
      return String.format("[id=%s,name=%s,email=%s]", id, name, email);
    }
  }
}
