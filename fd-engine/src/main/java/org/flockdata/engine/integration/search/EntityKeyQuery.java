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
import org.flockdata.engine.integration.MessageSupport;
import org.flockdata.search.model.EntityKeyResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Finds Keys for a given set of query parameters. This can be used to drive queries in the Graph as
 * the key will give you a starting point
 *
 * Created by mike on 14/02/16.
 */

@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
public class EntityKeyQuery {

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Autowired
    MessageSupport messageSupport;

    @Bean
    MessageChannel receiveKeyReply(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel keyResult () {
        return new DirectChannel();
    }

    @Bean
    MessageChannel doKeyQuery() {
        return new DirectChannel();
    }

    // Must be public else SI won't pick it up and will throw a NotFoundException
    @Transformer(inputChannel= "sendKeyQuery", outputChannel="doKeyQuery")
    public Message<?> transformMkPayload(Message message){
        return messageSupport.toJson(message);
    }

    @Bean
    IntegrationFlow fdKeyQueryFlow() {

        return IntegrationFlows.from(doKeyQuery())
                .handle(handler())
                .get();
    }

    private MessageHandler handler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(engineConfig.getFdSearch()+ "/v1/query/keys");
        handler.setExpectedResponseType(EntityKeyResults.class);
        handler.setHttpMethod(HttpMethod.POST);
//        handler.setOutputChannel(receiveKeyReply());
        return handler;
    }

//    // ToDo: Can this be integrated to the handler?
//    @Transformer(inputChannel="receiveKeyReply", outputChannel="keyResult")
//    public KeyResults transforMkResponse(Message<String> theObject) throws IOException {
//        return JsonUtils.toObject(theObject.getPayload().getBytes(), KeyResults.class);
//    }

}
