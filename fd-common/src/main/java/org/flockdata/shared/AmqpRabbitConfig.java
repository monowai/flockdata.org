/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.IntegrationComponentScan;

import javax.annotation.PostConstruct;

/**
 *
 * Rabbit MQ / AMQP
 *
 * Created by mike on 3/07/15.
 */

@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
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

    @PostConstruct
    public void logStatus (){
        logger.info( "**** FlockData AMQP Configuration deployed");
        logger.info ( "rabbit.host: [{}], rabbit.port [{}], rabbit.user [{}]",rabbitHost, rabbitPort, rabbitUser);
    }

    @Bean
    RabbitTemplate rabbitTemplate () throws Exception {
        return new RabbitTemplate(connectionFactory());
    }

    @Bean
    ConnectionFactory connectionFactory() throws Exception {
        CachingConnectionFactory connect = new CachingConnectionFactory();
        connect.setHost(rabbitHost);
        connect.setPort(rabbitPort);
        connect.setUsername(rabbitUser);
        connect.setPassword(rabbitPass);
        connect.setPublisherConfirms(publisherConfirms);
        connect.setPublisherReturns(publisherReturns);
//        connect.setExecutor(executorConfig.engineExecutor());
        connect.setChannelCacheSize(publisherCacheSize);
        return connect;
    }

    @Autowired
    Exchanges exchanges;

    @Bean
    public AmqpAdmin amqpAdmin() throws Exception {
        AmqpAdmin amqpAdmin = new RabbitAdmin(connectionFactory());
        return amqpAdmin;
    }

    public Boolean getAmqpLazyConnect() {
        return amqpLazyConnect;
    }

    public String getHost() {
        return rabbitHost;
    }

    public Integer getPort() {
        return rabbitPort;
    }
}