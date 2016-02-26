/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.client.amqp;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.ClientConfiguration;
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
