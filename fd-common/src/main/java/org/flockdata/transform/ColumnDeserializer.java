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

package org.flockdata.transform;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.helper.FdJsonObjectMapper;

import java.io.IOException;
import java.util.ArrayList;

/**
 * User: mike
 * Date: 27/05/14
 * Time: 4:25 PM
 */
public class ColumnDeserializer extends JsonDeserializer<ArrayList<ColumnDefinition>> {
    @Override
    public ArrayList<ColumnDefinition> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ArrayList<ColumnDefinition> values = new ArrayList<>();
        JsonNode node = jp.getCodec().readTree(jp);
        ObjectMapper om = FdJsonObjectMapper.getObjectMapper();
        for (JsonNode jsonNode : node) {
            values.add(om.readValue(jsonNode.toString(), ColumnDefinition.class));

        }
        return values;
    }
}
