package com.auditbucket.test.unit;

import com.auditbucket.audit.model.*;
import com.auditbucket.dao.AuditDao;
import com.auditbucket.engine.service.WhatService;
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
        AuditWhat auditWhatTest = new AuditNullWhatTest();
        Mockito.when(auditDao.getWhat(any(Long.class))).thenReturn(auditWhatTest);

        //When
        AuditWhat auditWhat = whatService.getWhat(change);

        //Then
        Assert.assertEquals(auditWhat.getId(), auditWhatTest.getId());
    }

    @Test
    public void testIsSameNeo4jStoreCompareWithNull() {
        // Given
        AuditChange compareFrom = new AuditChangeTest();
        //When
        boolean isSame = whatService.isSame(compareFrom, null);
        //Then
        Assert.assertFalse(isSame);
    }

    @Test
    public void testIsSameNeo4jStoreCompareWithEmpty() {
        // Given
        AuditChange compareFrom = new AuditChangeTest();
        //When
        boolean isSame = whatService.isSame(compareFrom, "");
        //Then
        Assert.assertFalse(isSame);
    }

    @Test
    public void testIsSameNeo4jStoreCompareFromNull() {
        // Given
        AuditChange compareFrom = null;
        //When
        boolean isSame = whatService.isSame(compareFrom, "{}");
        //Then
        Assert.assertFalse(isSame);
    }

    @Test
    public void testIsSameNeo4jStoreGetWhatNull() {
        // Given
        AuditChange compareFrom = new AuditChangeTest();
        Mockito.when(auditDao.getWhat(any(Long.class))).thenReturn(null);
        //When
        boolean isSame = whatService.isSame(compareFrom, "{}");
        //Then
        Assert.assertFalse(isSame);
    }

    // test when auditWhat.getWhat() is null
    @Test
    public void testIsSameNeo4jStoreWhatNull() {
        // Given
        AuditChange compareFrom = new AuditChangeTest();
        AuditNullWhatTest auditNullWhatTest = new AuditNullWhatTest();

        Mockito.when(auditDao.getWhat(any(Long.class))).thenReturn(auditNullWhatTest);
        //When
        boolean isSame = whatService.isSame(compareFrom, "{}");
        //Then
        Assert.assertFalse(isSame);
    }

    // test when auditWhat.getWhat() is null
    @Test
    public void testIsSameNeo4jStoreWhatNotNullDifferentSize() {
        // Given
        AuditChange compareFrom = new AuditChangeTest();
        AuditNotNullWhatTest auditNullWhatTest = new AuditNotNullWhatTest();

        Mockito.when(auditDao.getWhat(any(Long.class))).thenReturn(auditNullWhatTest);
        //When
        boolean isSame = whatService.isSame(compareFrom, "{}");
        //Then
        Assert.assertFalse(isSame);
    }

    // test when auditWhat.getWhat() is null
    @Test
    public void testIsSameNeo4jStoreWhatNotNullSameSize() {
        // Given
        AuditChange compareFrom = new AuditChangeTest();
        AuditNotNullWhatTest auditNullWhatTest = new AuditNotNullWhatTest();

        Mockito.when(auditDao.getWhat(any(Long.class))).thenReturn(auditNullWhatTest);
        //When
        boolean isSame = whatService.isSame(compareFrom, "{\"who\":\"mike\"}");
        //Then
        Assert.assertTrue(isSame);
    }


    //@Test
    public void testIsSameNeo4jStoreTrue() {
        // Given
        AuditChange compareFrom = new AuditChangeTest();
        Mockito.when(auditDao.getWhat(any(Long.class))).thenReturn(compareFrom.getWhat());
        String compareWith = "";
        //When
        boolean isSame = whatService.isSame(compareFrom, compareWith);

        //Then
        Assert.assertTrue(isSame);
    }

    //@Test
    public void testIsSameNeo4jStoreFalse() {
        AuditChange compareFrom = new AuditChangeTest();

        String compareWith = "";
        boolean isSame = whatService.isSame(compareFrom, compareWith);

    }


}


