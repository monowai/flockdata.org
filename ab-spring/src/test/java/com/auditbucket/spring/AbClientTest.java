package com.auditbucket.spring;

import com.auditbucket.client.rest.FdRestWriter;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;

public class AbClientTest extends AbstractABTest {
    @Autowired
    FdRestWriter abClient;

    @Test
    public void testStart() {
        assertNotNull(abClient);
    }
}
