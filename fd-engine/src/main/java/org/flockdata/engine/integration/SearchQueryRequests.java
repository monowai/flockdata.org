package org.flockdata.engine.integration;

import com.google.common.net.MediaType;
import org.flockdata.engine.PlatformConfig;
import org.flockdata.helper.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import javax.annotation.PostConstruct;

/**
 * Created by mike on 13/02/16.
 */
@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
public class SearchQueryRequests {
    @Autowired
    FdSearchChannels channels;

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    // Seems we have to transform via this
    @Transformer(inputChannel="sendSearchRequest", outputChannel="doFdViewQuery")
    public Message<?> fdQueryTransform(Message theObject){
        return objectToJson().transform(theObject);
    }

    @Bean
    IntegrationFlow fdViewQuery() {

        return IntegrationFlows.from("doFdViewQuery")
                .handle(fdViewQueryHandler())
                .transform(objectToJson())
                .get();
    }

    private MessageHandler fdViewQueryHandler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getFdViewQuery());
        handler.setExpectedResponseType(org.flockdata.search.model.EsSearchResult.class);
        handler.setHttpMethod(HttpMethod.POST);
        return handler;
    }

    // Seems we have to transform via this
    @Transformer(inputChannel="doMetaKeyQuery", outputChannel="doMetaKeyQuery")
    public Message<?> transformRequest(Message theObject){
        return objectToJson().transform(theObject);
    }

    @Bean
    IntegrationFlow metaKeyQuery() {

        return IntegrationFlows.from("doMetaKeyQuery")
                .handle(metaKeyHandler())
                .transform(objectToJson())
                .get();
    }

    private MessageHandler metaKeyHandler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getMetaKeyQuery());
        handler.setExpectedResponseType(org.flockdata.search.model.MetaKeyResults.class);
        handler.setHttpMethod(HttpMethod.POST);
        return handler;
    }



    @Bean
    IntegrationFlow tagCloudQuery() {

        return IntegrationFlows.from("doTagCloudQuery")
                .handle(tagCloudHandler())
                .transform(objectToJson())
                .get();
    }

    private MessageHandler tagCloudHandler() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(getTagCloudQuery());
        handler.setExpectedResponseType(org.flockdata.search.model.TagCloud.class);
        handler.setHttpMethod(HttpMethod.POST);
        return handler;
    }


    public String getFdViewQuery() {
        return engineConfig.getFdSearch()+ "/v1/query/fdView";
    }

    public String getMetaKeyQuery() {
        return engineConfig.getFdSearch()+ "/v1/query/metaKeys";
    }

    public String getTagCloudQuery() {
        return engineConfig.getFdSearch()+ "/v1/query/tagCloud";
    }


    private ObjectToJsonTransformer transformer;

    @PostConstruct
    public void createTransformer() {
        transformer = new ObjectToJsonTransformer(
                new Jackson2JsonObjectMapper(JsonUtils.getMapper())
        );
        transformer.setContentType(MediaType.JSON_UTF_8.toString());
        //return transformer;
    }

    public ObjectToJsonTransformer objectToJson(){
        return transformer;
    }


}
