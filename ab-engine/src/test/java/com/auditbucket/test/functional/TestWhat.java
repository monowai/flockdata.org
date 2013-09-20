package com.auditbucket.test.functional;

import com.auditbucket.engine.service.AuditService;
import com.auditbucket.engine.service.WhatService;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:root-context.xml")
@Transactional
public class TestWhat {

    @Autowired
    WhatService whatService;

    public void logWhat(){
        // Given

        // When

        //Then
    }
}
