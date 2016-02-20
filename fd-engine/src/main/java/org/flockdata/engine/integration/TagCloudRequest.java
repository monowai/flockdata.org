package org.flockdata.engine.integration;

import org.flockdata.engine.PlatformConfig;
import org.flockdata.helper.JsonUtils;
import org.flockdata.search.model.TagCloud;
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
@Profile({"integration","production"})
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
