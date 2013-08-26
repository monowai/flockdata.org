package com.auditbucket.spring;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;

public class AbClientTest extends AbstractABTest {
    @Autowired
    AbClient abClient;

    @Test
    public void testStart() {
        assertNotNull(abClient);
    }
}
