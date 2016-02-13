package org.flockdata.search.integration;

import com.google.common.net.MediaType;
import org.flockdata.helper.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import javax.annotation.PostConstruct;

/**
 * Created by mike on 13/02/16.
 */
@Configuration
@Profile({"integration","production"})
public class QueryRequests {
    private ObjectToJsonTransformer transformer;

    @Autowired
    SearchChannels channels;
    private JsonToObjectTransformer jsonToObjectTransformer;

    @Transformer(inputChannel="contentReply", outputChannel="doContentReply")
    public Message<?> entityData(Message theObject){
        return objectToJson().transform(theObject);
    }


//    @Bean
//    public MessageHandler handleMetaKey(@Value("${search.api.base.uri}/metaKey") URI uri) {
//        HttpRequestExecutingMessageHandler httpHandler = new HttpRequestExecutingMessageHandler(uri);
//        httpHandler.setExpectedResponseType(String.class);
//        httpHandler.setHttpMethod(HttpMethod.POST);
//        return httpHandler;
//    }
//
//    @Bean
//    public IntegrationFlow metaKeyFlow(MessageHandler handleMetaKey) {
//        return IntegrationFlows.from(channels.doMetaKeyQuery())
//                .handle(handleMetaKey)
//                .transform(objectToJsonTransformer())
//                .get();
//    }
//
//    @Bean
//    public MessageHandler handleEntityQuery(@Value("${search.api.base.uri}/data") URI uri) {
//        HttpRequestExecutingMessageHandler httpHandler = new HttpRequestExecutingMessageHandler(uri);
//        httpHandler.setExpectedResponseType(String.class);
//        httpHandler.setHttpMethod(HttpMethod.POST);
//        return httpHandler;
//    }
//    @Bean
//    public IntegrationFlow entityFlow(MessageHandler handleEntityQuery) {
//        return IntegrationFlows.from(channels.doContentQuery())
//                .handle(handleEntityQuery)
//                .transform(objectToJson())
//                .get();
//    }


    //    <int-http:inbound-gateway request-channel="doContentQuery"
//    reply-channel="doContentReply"
//    path="/api/esContent"
//    request-payload-type="org.flockdata.search.model.QueryParams"
//    supported-methods="POST">



    private ObjectToJsonTransformer objectToJsonTransformer;

    public ObjectToJsonTransformer objectToJson(){
        return objectToJsonTransformer;
    }

    @PostConstruct
    public void createTransformers() {
        objectToJsonTransformer = new ObjectToJsonTransformer(
                new Jackson2JsonObjectMapper(JsonUtils.getMapper())
        );
        objectToJsonTransformer.setContentType(MediaType.JSON_UTF_8.toString());
    }


    @Bean
    public MessageHandler logger() {
        LoggingHandler loggingHandler = new LoggingHandler("INFO");
        loggingHandler.setLoggerName("logger");
        // This is redundant because the default expression is exactly "payload"
        // loggingHandler.setExpression("payload");
        return loggingHandler;
    }

}
