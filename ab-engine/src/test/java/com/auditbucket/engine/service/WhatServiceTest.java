package com.auditbucket.engine.service;

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.engine.repo.neo4j.model.AuditLogRelationship;
import com.auditbucket.test.unit.AuditChangeTest;
import com.auditbucket.test.unit.AuditLogTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class WhatServiceTest {
    @Autowired
    private WhatService whatService;
    @Test
    public void testLogWhat() throws Exception {
        AuditChangeTest compareFrom = new AuditChangeTest(WhatService.REDIS);
        AuditLogTest auditLogTest = new AuditLogTest(System.currentTimeMillis());
        compareFrom.setAuditLog(auditLogTest);

        whatService.logWhat(compareFrom,"{}");
    }

    @Test
    public void testGetWhat() throws Exception {

    }

    @Test
    public void testIsSame() throws Exception {

    }

    @Test
    public void testGetDelta() throws Exception {

    }
}
