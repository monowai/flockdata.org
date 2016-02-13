package org.flockdata.engine.integration;

import com.google.common.net.MediaType;
import org.flockdata.helper.JsonUtils;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by mike on 14/02/16.
 */
@Component
public class MessageSupport {

    private ObjectToJsonTransformer objectToJsonTransformer;
    private JsonToObjectTransformer j2o;

    @PostConstruct
    public void createTransformer() {
        objectToJsonTransformer = new ObjectToJsonTransformer(
                new Jackson2JsonObjectMapper(JsonUtils.getMapper())
        );
        objectToJsonTransformer.setContentType(MediaType.JSON_UTF_8.toString());

        j2o = new JsonToObjectTransformer(
                new Jackson2JsonObjectMapper( JsonUtils.getMapper())
        );

    }

    public JsonToObjectTransformer jsonToObject(){
        return j2o;
    }

    public ObjectToJsonTransformer objectToJson(){
        return objectToJsonTransformer;
    }

    public Message<?> toJson(Message theObject) {
        return objectToJson().transform(theObject);
    }

    public Message<?> toObject(Message theObject) {
        return j2o.transform(theObject);
    }
}
