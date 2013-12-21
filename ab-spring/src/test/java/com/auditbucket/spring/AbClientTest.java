package com.auditbucket.spring;

import com.auditbucket.helper.AbExporter;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;

public class AbClientTest extends AbstractABTest {
    @Autowired
    AbExporter abClient;

    @Test
    public void testStart() {
        assertNotNull(abClient);
    }
}
