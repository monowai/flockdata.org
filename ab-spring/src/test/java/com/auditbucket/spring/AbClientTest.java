package com.auditbucket.spring;

import com.auditbucket.helper.AbRestClient;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;

public class AbClientTest extends AbstractABTest {
    @Autowired
    AbRestClient abClient;

    @Test
    public void testStart() {
        assertNotNull(abClient);
    }
}
