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

package org.flockdata.client.amqp;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.flockdata.helper.JsonUtils;
import org.flockdata.registration.TagInputBean;
import org.flockdata.shared.AmqpRabbitConfig;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.track.bean.EntityInputBean;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

/**
 * User: mike
 * Date: 27/11/14
 * Time: 8:17 AM
 */
@Component
@Configuration
public class AmqpServices {
    private ConnectionFactory factory ;
    private Connection connection = null;
    private Channel channel = null;

    private AMQP.BasicProperties entityProps;
    private AMQP.BasicProperties tagProps;
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(AmqpServices.class);

    @Autowired
    private ClientConfiguration configuration;

    @Autowired
    private AmqpRabbitConfig rabbitConfig;

    private static boolean prepared = false;

    //    @PostConstruct
    private void prepare() {
        if (!prepared) {
            entityProps = null;
            tagProps = null;
            factory = new ConnectionFactory();
            factory.setHost(rabbitConfig.getHost());
            factory.setPort(rabbitConfig.getPort());
            factory.setUsername(rabbitConfig.getUser());
            factory.setPassword(rabbitConfig.getPass());

            try {
                connection = factory.newConnection();
                channel = connection.createChannel();

                channel.queueBind(configuration.getTrackQueue(), configuration.getTrackExchange(), configuration.getTrackRoutingKey());
                prepared = true;
            } catch (IOException e) {
                logger.error("Unexpected", e);
            }
        }

    }

    private String getApiKey() {
        if (configuration.getApiKey() == null || configuration.getApiKey().equals(""))
            throw new RuntimeException("No API key is set. Please configure one and try again");
        return configuration.getApiKey();

    }

    private AMQP.BasicProperties getTagProps() {
        if (tagProps == null) {
            tagProps =
                    new AMQP.BasicProperties().builder()
                            .headers(getHeaders("T", getApiKey()))
                            .deliveryMode(rabbitConfig.getPersistentDelivery() ? 2 : null)
                            .replyTo("nullChannel").build();
        }
        return tagProps;
    }

    private AMQP.BasicProperties getEntityProps() {
        if (entityProps == null)
            entityProps =
                    new AMQP.BasicProperties().builder()
                            .headers(getHeaders("E", getApiKey()))
                            .deliveryMode(rabbitConfig.getPersistentDelivery() ? 2 : null)
                            .replyTo("nullChannel").build()
                    ;
        return entityProps;
    }

    @PreDestroy
    public void close() {
        prepared = false;
        if (connection != null)
            try {
                if (channel != null && channel.isOpen())
                    channel.close();
                if (connection.isOpen())
                    connection.close();

            } catch (IOException e) {
                logger.error("Unexpected. Don't care", e);
            }
    }


    public void publish(Collection<EntityInputBean> entityInputs) throws IOException {
        if ( prepared && !channel.isOpen())
            prepared = false;
        prepare();

        channel.basicPublish(configuration.getTrackExchange(),
                configuration.getTrackRoutingKey(),
                getEntityProps(),
                JsonUtils.toJsonBytes(entityInputs));
    }

    public void publishTags(Collection<TagInputBean> tagInputs) throws IOException {
        prepare();
        channel.basicPublish(configuration.getTrackExchange(),
                configuration.getTrackRoutingKey(),
                getTagProps(),
                JsonUtils.toJsonBytes(tagInputs));
    }

    private HashMap<String, Object> getHeaders(String type, String apiKey) {
        HashMap<String, Object> headers;
        headers = new HashMap<>();
        headers.put(ClientConfiguration.KEY_MSG_KEY, apiKey);
        headers.put(ClientConfiguration.KEY_MSG_TYPE, type);
        return headers;
    }

}
