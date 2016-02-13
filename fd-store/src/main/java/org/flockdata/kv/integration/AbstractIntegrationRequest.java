package org.flockdata.kv.integration;

import com.google.common.net.MediaType;
import org.flockdata.helper.JsonUtils;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;

import javax.annotation.PostConstruct;

/**
 * Useful handlers for integration classes
 *
 * Created by mike on 13/02/16.
 */
public class AbstractIntegrationRequest {

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
}
