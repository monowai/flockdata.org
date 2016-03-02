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
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.shared.ClientConfiguration;
import org.flockdata.track.bean.EntityInputBean;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

/**
 * User: mike
 * Date: 27/11/14
 * Time: 8:17 AM
 */
public class AmqpServices {
    ConnectionFactory factory = new ConnectionFactory();
    Connection connection =null ;
    Channel channel  = null;
    String exchange;
    String queue;
    String routingKey;
    AMQP.BasicProperties.Builder builder;
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(AmqpServices.class);

    public AmqpServices(ClientConfiguration configuration) throws FlockException {
        factory.setHost(configuration.getRabbitHost());
        factory.setUsername(configuration.getRabbitUser());
        factory.setPassword(configuration.getRabbitPass());

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            this.queue = configuration.getTrackQueue();
            this.exchange = configuration.getTrackExchange();
            this.routingKey = configuration.getTrackRoutingKey();

            channel.queueBind(queue, exchange, routingKey);
            connection = factory.newConnection();
            HashMap<String,Object> headers = new HashMap<>();
            headers.put(ClientConfiguration.KEY_MSG_KEY, configuration.getApiKey());
            if ( configuration.getApiKey() == null || configuration.getApiKey().equals(""))
                throw new FlockException("Your API key appears to be invalid. Have you run the configure process?");
            builder =
                    new AMQP.BasicProperties().builder()
                            .headers(headers)
                            .deliveryMode( configuration.getPersistentDelivery()?2:null)
                            .replyTo("nullChannel")
                    ;

        } catch (IOException e) {
            logger.error("Unexpected", e);
        }
    }

    public void close(){
        if ( connection != null )
            try {
                if ( channel!=null && channel.isOpen())
                    channel.close();
                if ( connection.isOpen())
                    connection.close();

            } catch (IOException e) {
                logger.error("Unexpected. Don't care", e);
            }
    }


    public void publish(EntityInputBean entityInput) throws IOException {

        channel.basicPublish(exchange, routingKey, builder.build(), JsonUtils.toJsonBytes(entityInput));
    }

    public void publish(Collection<EntityInputBean> entityInputs) throws IOException {

        channel.basicPublish(exchange, routingKey, builder.build(), JsonUtils.toJsonBytes(entityInputs));
    }

}
