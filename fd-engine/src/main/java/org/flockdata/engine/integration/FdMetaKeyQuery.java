package org.flockdata.engine.integration;

import org.flockdata.engine.PlatformConfig;
import org.flockdata.helper.JsonUtils;
import org.flockdata.search.model.MetaKeyResults;
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
 * Finds MetaKeys for a given set of query parameters. This can be used to drive queries in the Graph as
 * the metaKey will give you a starting point
 *
 * Created by mike on 14/02/16.
 */

@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
public class FdMetaKeyQuery {

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Autowired
    MessageSupport messageSupport;

    @Bean
    MessageChannel receiveMetaKeyReply(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel metaKeyResult () {
        return new DirectChannel();
    }

    @Bean
    MessageChannel doMetaKeyQuery() {
        return new DirectChannel();
    }

    // ToDo: Can we handle this more via the flow or handler?
    // Must be public else SI won't pick it up and will throw a NotFoundException
    @Transformer(inputChannel= "sendMetaKeyQuery", outputChannel="doMetaKeyQuery")
    public Message<?> transformMkPayload(Message message){
        return messageSupport.toJson(message);
    }

    @Bean
    IntegrationFlow fdMetaKeyQueryFlow() {

        return IntegrationFlows.from(doMetaKeyQuery())
                .handle(fdMetaKeyQueryHandler())
                .get();
    }

    private MessageHandler fdMetaKeyQueryHandler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(engineConfig.getFdSearch()+ "/v1/query/metaKeys");
        handler.setExpectedResponseType(String.class);
        handler.setHttpMethod(HttpMethod.POST);
        handler.setOutputChannel(receiveMetaKeyReply());
        return handler;
    }

    // ToDo: Can this be integrated to the handler?
    @Transformer(inputChannel="receiveMetaKeyReply", outputChannel="metaKeyResult")
    public MetaKeyResults transforMkResponse(Message<String> theObject) throws IOException {
        return JsonUtils.toObject(theObject.getPayload().getBytes(), MetaKeyResults.class);
    }

}
