/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.integration.engine;

import static org.flockdata.integration.ClientConfiguration.KEY_MSG_KEY;
import static org.flockdata.integration.ClientConfiguration.KEY_MSG_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import org.flockdata.authentication.SecurityHelper;
import org.flockdata.data.Company;
import org.flockdata.engine.tag.MediationFacade;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.Exchanges;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.stereotype.Service;

/**
 * Integration mechanism for message queue input
 *
 * @author mholdsworth
 * @tag Track, Messaging, Security
 * @since 27/12/2015
 */
@Service
@Profile( {"fd-server"})
public class TrackRequests {

  private final MediationFacade mediationFacade;

  private final SecurityHelper securityHelper;


  private Exchanges exchanges;
  private Logger logger = LoggerFactory.getLogger(TrackRequests.class);

  @Autowired
  public TrackRequests(MediationFacade mediationFacade, SecurityHelper securityHelper) {
    this.mediationFacade = mediationFacade;
    this.securityHelper = securityHelper;
  }

  @Autowired(required = false)
  void setExchanges(Exchanges exchanges) {
    this.exchanges = exchanges;
  }

  @Bean
  MessageChannel doTrackEntity() {
    return new DirectChannel();
  }

  @Bean
  MessageChannel startEntityWrite() {
    return new DirectChannel();
  }

  @Bean
  MessageChannel trackResult() {
    return new DirectChannel();
  }

  public MessageHandler handler() throws FlockException, ExecutionException, InterruptedException {

    return message -> {
      try {

        Object oKey = message.getHeaders().get(KEY_MSG_KEY);
        if (oKey == null) {
          throw new AmqpRejectAndDontRequeueException("No api key");
        }
        Company company = securityHelper.getCompany(oKey.toString());
        if (company == null) {
          logger.error("Could not resolve company for apiKey [{}]", oKey);
          throw new AmqpRejectAndDontRequeueException("Illegal api key");
        }

        Object oType = message.getHeaders().get(KEY_MSG_TYPE);
        if (oType == null || oType.toString().equalsIgnoreCase("E")) {
          Collection<EntityInputBean> inputBeans = JsonUtils.toCollection((byte[]) message.getPayload(), EntityInputBean.class);
          mediationFacade.trackEntities(company, inputBeans);
        } else {
          Collection<TagInputBean> inputBeans = JsonUtils.toCollection((byte[]) message.getPayload(), TagInputBean.class);
          mediationFacade.createTags(company, inputBeans);
        }
      } catch (IOException e) {
        throw new AmqpRejectAndDontRequeueException("Unable to de-serialize the payload", e);
      } catch (InterruptedException |
          ExecutionException |
          InvalidDataAccessResourceUsageException |
          InvalidDataAccessApiUsageException |
          FlockException e) {
        logger.error(e.getMessage());
        throw new AmqpRejectAndDontRequeueException(String.format("Processing exception %s", e.getMessage()), e);
      }

    };
  }

  @Bean
  public IntegrationFlow writeEntityChangeFlow(ConnectionFactory connectionFactory, RetryOperationsInterceptor trackInterceptor) throws InterruptedException, FlockException, ExecutionException, IOException {
    return IntegrationFlows.from(
        Amqp.inboundAdapter(connectionFactory, exchanges.fdTrackQueue())
            .maxConcurrentConsumers(exchanges.trackConcurrentConsumers())
            .mappedRequestHeaders(KEY_MSG_KEY, KEY_MSG_TYPE)
            .adviceChain(trackInterceptor)
            .outputChannel(doTrackEntity())
            .prefetchCount(exchanges.trackPreFetchCount())
    )
        .handle(handler())
        .get();
  }

  @Bean
  public MessageChannel loggingChannel() {
    return MessageChannels.direct().get();
  }

  @Bean
  public MessageChannel errorLoggingChannel() {
    return MessageChannels.direct().get();
  }


  @Bean
  public IntegrationFlow integrationLogging() {
    return IntegrationFlows.from("errorLoggingChannel").handle((p, h) -> {
      logger.error("Outgoing Request - " + p.toString());
      return p;
    }).channel("nullChannel").get();
  }


  @ServiceActivator(inputChannel = "doTrackEntity")
  Collection<TagResultBean> sendResult() {
    return new ArrayList<>();
    // What do with the response?
  }

//
//    @MessagingGateway(errorChannel = "trackError", asyncExecutor = "fd-track")
//    @Async("fd-track")
//    public interface InboundGateway {
//        //    ToDo: where to send the reply
//        @Gateway(requestChannel = "startEntityWrite", replyTimeout = 10000, replyChannel = "trackResult")
//        Future<TrackResultBean> doTrackEntity(EntityInputBean entityInputBean, @Header(value="apiKey") String apiKey);
//
//    }

}
