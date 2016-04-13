/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.shared;

import com.google.common.net.MediaType;
import org.flockdata.helper.JsonUtils;
import org.springframework.context.annotation.Profile;
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
@Profile({"fd-server"})
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

    ObjectToJsonTransformer objectToJson(){
        return objectToJsonTransformer;
    }

    public Message<?> toJson(Message theObject) {
        return objectToJson().transform(theObject);
    }

    public Message<?> toObject(Message theObject) {
        return j2o.transform(theObject);
    }
}
