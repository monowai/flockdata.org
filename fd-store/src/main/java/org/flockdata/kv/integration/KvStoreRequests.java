package org.flockdata.kv.integration;

import com.google.common.net.MediaType;
import org.flockdata.helper.JsonUtils;
import org.flockdata.kv.KvConfig;
import org.flockdata.search.model.EsSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mike on 13/02/16.
 */

@Configuration
@IntegrationComponentScan
@EnableIntegration
@Profile({"integration","production"})
public class KvStoreRequests {

    @Autowired
    KvConfig kvConfig;

    private List<HttpMessageConverter<?>> messageConverters;

    @Bean
    MessageChannel doDataQuery(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel sendDataQuery(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel receiveContentReply(){
        return new DirectChannel();
    }

    @Transformer(inputChannel="doDataQuery", outputChannel="sendDataQuery")
    public Message<?> transformRequest(Message theObject){
        return objectToJson().transform(theObject);
    }

    @Bean
    IntegrationFlow dataQuery() {

        return IntegrationFlows.from(sendDataQuery())
                .handle(dataQueryHandler())
                .get();
    }

    private MessageHandler dataQueryHandler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getDataQuery());
        handler.setHttpMethod(HttpMethod.POST);

        handler.setExpectedResponseType(EsSearchResult.class);
//        handler.setMessageConverters(getMessageConverters());
        return handler;
    }


    public String getDataQuery() {
        //return kvConfig.getFdSearchUrl()+ "/api/data";
        return kvConfig.getFdSearchUrl()+ "/v1/query/data";
    }


    private ObjectToJsonTransformer objectToJsonTransformer;

    public ObjectToJsonTransformer objectToJson(){
        return objectToJsonTransformer;
    }



    public List<HttpMessageConverter<?>> getMessageConverters() {
        if ( messageConverters == null ) {
            messageConverters = new ArrayList<>();
            messageConverters.add(new MappingJackson2HttpMessageConverter()) ;
        }
        return messageConverters;
    }

    @PostConstruct
    public void createTransformers() {
        objectToJsonTransformer = new ObjectToJsonTransformer(
                new Jackson2JsonObjectMapper(JsonUtils.getMapper())
        );
        objectToJsonTransformer.setContentType(MediaType.JSON_UTF_8.toString());
    }

}
