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
import org.flockdata.search.model.TagCloud;
import org.flockdata.shared.MessageSupport;
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

import java.io.IOException;

/**
 * Created by mike on 13/02/16.
 */
@Configuration
@IntegrationComponentScan
@Profile({"fd-server"})
public class TagCloudRequest {

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Autowired
    MessageSupport messageSupport;

    @Bean
    MessageChannel tagCloudResult() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel tagCloudReply() {
        return new DirectChannel();
    }

    // ToDo: Can we handle this more via the flow or handler?
    @Transformer(inputChannel="sendTagCloudRequest", outputChannel="doTagCloudQuery")
    public Message<?> transformTagCloudParams(Message theObject){
        return messageSupport.toJson(theObject);
    }

    @Bean
    IntegrationFlow tagCloudQuery() {

        return IntegrationFlows.from("doTagCloudQuery")
                .handle(tagCloudHandler())
                .get();
    }

    private MessageHandler tagCloudHandler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(engineConfig.getFdSearch()+ "/v1/query/tagCloud");
        handler.setExpectedResponseType(String.class);
        handler.setHttpMethod(HttpMethod.POST);
        handler.setOutputChannel(tagCloudReply());
        return handler;
    }

    // ToDo: Can this be integrated to the handler?
    @Transformer(inputChannel= "tagCloudReply", outputChannel="tagCloudResult")
    public TagCloud transformTagCloudResponse(Message<String> theObject) throws IOException {
        return JsonUtils.toObject(theObject.getPayload().getBytes(), TagCloud.class);
    }


}
