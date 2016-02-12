package org.flockdata.search.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;

import javax.annotation.PostConstruct;

/**
 * Rabbit MQ / AMQP
 * <p/>
 * Created by mike on 3/07/15.
 */

@Configuration
@EnableRabbit
@IntegrationComponentScan
public class AmqpRabbitConfig {

    private Logger logger = LoggerFactory.getLogger("configuration");

    @Value("${rabbit.host:localhost}")
    String rabbitHost;

    @Value("${rabbit.port:5672}")
    Integer rabbitPort;

    @Value("${rabbit.user:guest}")
    String rabbitUser;

    @Value("${rabbit.pass:guest}")
    String rabbitPass;

    @Value("${rabbit.publisher.confirms:false}")
    Boolean publisherConfirms;

    @Value("${rabbit.publisher.returns:false}")
    Boolean publisherReturns;

    @Value("${rabbit.publisherCacheSize:22}")
    Integer publisherCacheSize;

    @Value("${amqp.lazyConnect:false}")
    Boolean amqpLazyConnect;

    @Autowired
    AsyncConfig asyncConfig;

    @PostConstruct
    public void logStatus (){
        logger.info( "**** FlockData AMQP Configuration deployed");
        logger.info ( "rabbit.host: {}, rabbit.port {}, rabbit.user {}",rabbitHost, rabbitPort, rabbitUser);
    }


    @Bean (name="connectionFactory")
    CachingConnectionFactory getConnectionFactory() throws Exception {
        CachingConnectionFactory connect = new CachingConnectionFactory();
        connect.setHost(rabbitHost);
        connect.setPort(rabbitPort);
        connect.setUsername(rabbitUser);
        connect.setPassword(rabbitPass);
        connect.setPublisherConfirms(publisherConfirms);
        connect.setPublisherReturns(publisherReturns);
        connect.setExecutor(asyncConfig.searchExecutor());
        connect.setChannelCacheSize(publisherCacheSize);
        return connect;
    }

    public Boolean getAmqpLazyConnect() {
        return amqpLazyConnect;
    }
}

