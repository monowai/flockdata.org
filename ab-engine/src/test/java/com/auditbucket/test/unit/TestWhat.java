package com.auditbucket.test.unit;

import com.auditbucket.audit.model.*;
import com.auditbucket.dao.AuditDao;
import com.auditbucket.engine.service.WhatService;
import com.auditbucket.registration.model.FortressUser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class TestWhat {
    @Mock
    AuditDao auditDao;
    @InjectMocks
    WhatService whatService;

    @Test
    public void testLogWhatNeo4jStore() {
        //Given
        AuditChange change = new AuditChangeTest();
        String jsonText = "{\"json\":\"json\"}";
        Mockito.when(auditDao.save(change, jsonText)).thenReturn("value1");
        //When
        String value = whatService.logWhat(change, jsonText);
        //Then
        Assert.assertEquals(value, "value1");
    }

    @Test
    public void testGetWhatNeo4jStore() {
        //Given
        AuditChange change = new AuditChangeTest();
        String jsonText = "{\"json\":\"json\"}";
        AuditWhat auditWhatTest = new AuditWhatTest();
        Mockito.when(auditDao.getWhat(any(Long.class))).thenReturn(auditWhatTest);

        //When
        AuditWhat auditWhat = whatService.getWhat(change);

        //Then
        Assert.assertEquals(auditWhat.getId(), auditWhatTest.getId());
    }
}


