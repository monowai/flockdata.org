package org.flockdata.test.integration;

import org.flockdata.FdEngine;
import org.flockdata.registration.service.RegistrationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created by mike on 21/02/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@WebAppConfiguration
@WebIntegrationTest
@ActiveProfiles("integration")
@SpringApplicationConfiguration(FdEngine.class)
public class TestStack {

    @Autowired
    RegistrationService regService;

    private static Logger logger = LoggerFactory.getLogger(TestStack.class);
    @Test
    public void testNothing(){
        logger.info("Hello world");

    }
}
