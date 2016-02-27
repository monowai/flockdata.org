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

package org.flockdata.integration;

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
