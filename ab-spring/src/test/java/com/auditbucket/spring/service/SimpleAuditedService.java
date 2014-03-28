package com.auditbucket.spring.service;

import com.auditbucket.spring.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleAuditedService {
    private static Logger logger = LoggerFactory.getLogger(SimpleAuditedService.class);

    @DatagioHeader
    public Customer save(Customer customer) {
        logger.info("call save Method");
        if (customer.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,3}$")) {
            customer.setId(324325L);
            return customer;
        } else {
            throw new IllegalArgumentException("invalid email");
        }
    }

    @DatagioLog
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
