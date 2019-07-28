package org.flockdata.search.integration;

import lombok.extern.slf4j.Slf4j;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.Exchanges;
import org.flockdata.integration.MessageSupport;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.Message;


/**
 * @author mikeh
 * @since 2018-11-16
 */
@Configuration
@IntegrationComponentScan
@Slf4j
public class OutboundResultHandler {

  private AmqpRabbitConfig rabbitConfig;
  private MessageSupport messageSupport;
  private Exchanges exchanges;

  @Autowired(required = false)
  void setRabbitConfig(AmqpRabbitConfig rabbitConfig) {
    this.rabbitConfig = rabbitConfig;
  }

  @Autowired(required = false)
  void setMessageSupport(MessageSupport messageSupport) {
    this.messageSupport = messageSupport;
  }

  @Autowired(required = false)
  void setExchanges(Exchanges exchanges) {
    this.exchanges = exchanges;
  }

  @Transformer(inputChannel = "searchReply", outputChannel = "searchDocSyncResult")
  @Profile("fd-server")
  public Message<?> transformSearchResults(Message message) {
    return messageSupport.toJson(message);
  }

  @Bean
  @ServiceActivator(inputChannel = "searchDocSyncResult")
  @Profile("fd-server")
  public AmqpOutboundEndpoint writeEntitySearchResult(AmqpTemplate amqpTemplate) {
    AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
    outbound.setLazyConnect(rabbitConfig.getAmqpLazyConnect());
    outbound.setExchangeName(exchanges.fdExchangeName());
    outbound.setRoutingKey(exchanges.fdEngineBinding());
    outbound.setExpectReply(false);
    return outbound;

  }

}
