package org.flockdata.test.integration;

import static org.junit.Assert.assertNotNull;

import org.flockdata.client.FdClientIo;
import org.flockdata.client.amqp.FdRabbitClient;
import org.flockdata.client.commands.*;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.FdTemplate;
import org.flockdata.integration.FileProcessor;
import org.flockdata.integration.IndexManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

/**
 * @author Mike Holdsworth
 * @since 7/10/17
 */
@ContextConfiguration(classes = {
    ClientConfiguration.class,
    IndexManager.class,
    FileProcessor.class,
    FdTemplate.class,
    FdClientIo.class,
    FdRabbitClient.class,
    EnginePing.class,
    StorePing.class,
    SearchPing.class,
    RegistrationPost.class,
    EntityLogsGet.class,
    SearchEsPost.class,
    SearchFdPost.class,
    EntityData.class,
    Health.class,
    EntityGet.class,
    Login.class,
    ModelGet.class,
    ModelPost.class,
    ModelFieldStructure.class,
    TagGet.class,
    TagsGet.class,
    AdminPurgeFortressSegment.class,
    AdminPurgeFortress.class,
    AmqpRabbitConfig.class,
    RestTemplate.class,
    TrackEntityPost.class,
    SearchHelper.class,
    IntegrationHelper.class

})
@RunWith(SpringRunner.class)
@Configuration
@ActiveProfiles("dev")
public class TestWiring {
    @Autowired
    private FdTemplate fdTemplate;

    @Autowired
    private FdClientIo fdClientIo;

    @Test
    public void wiringWorks() {
        assertNotNull(fdTemplate);
        assertNotNull(fdClientIo);
    }
}
