/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.engine.integration.search;

import org.flockdata.engine.PlatformConfig;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.MessageSupport;
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.io.IOException;

/**
 * Striped down search support. Designed for fd-view. ToDo: Move to a "Backend for Frontend" module
 *
 * Created by mike on 14/02/16.
 */

@Configuration
@IntegrationComponentScan
@Profile({"fd-server"})
public class FdViewQuery {

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Autowired
    MessageSupport messageSupport;

    @Bean
    MessageChannel receiveFdViewReply(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel fdViewResult () {
        return new DirectChannel();
    }

    @Bean
    MessageChannel doFdViewQuery() {
        return new DirectChannel();
    }

    // ToDo: Can we handle this more via the flow or handler?
    @Transformer(inputChannel="sendSearchRequest", outputChannel="doFdViewQuery")
    public Message<?> fdQueryTransform(Message theObject){
        return messageSupport.toJson(theObject);
    }

    @Bean
    IntegrationFlow fdViewQueryFlow() {

        return IntegrationFlows.from(doFdViewQuery())
                .handle(fdViewQueryHandler())
                .get();
    }

    private MessageHandler fdViewQueryHandler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(engineConfig.getFdSearch()+ "/v1/query/fdView");
        handler.setExpectedResponseType(String.class);
        handler.setHttpMethod(HttpMethod.POST);
        handler.setOutputChannel(receiveFdViewReply());
        return handler;
    }

    // ToDo: Can this be integrated to the handler?
    @Transformer(inputChannel="receiveFdViewReply", outputChannel="fdViewResult")
    public EsSearchResult fdViewResponse(Message<String> theObject) throws IOException {
        return JsonUtils.toObject(theObject.getPayload().getBytes(), EsSearchResult.class);
    }

    @MessagingGateway
    public interface FdViewQueryGateway {

        @Gateway(requestChannel = "sendSearchRequest", replyChannel = "fdViewResult")
        EsSearchResult fdSearch(QueryParams queryParams);

    }


}
