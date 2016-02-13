package org.flockdata.engine.integration;

import com.google.common.net.MediaType;
import org.flockdata.helper.JsonUtils;
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

    public Message<?> toJson(Message theObject) {
        return objectToJson().transform(theObject);
    }
}
