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

package org.flockdata.search.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.helper.FdJsonObjectMapper;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * User: mike
 * Date: 23/05/14
 * Time: 12:29 PM
 */
public class JsonSearchChange extends JsonDeserializer<EntitySearchChanges> {
    @Override
    public EntitySearchChanges deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        EntitySearchChanges changes = new EntitySearchChanges();
        JsonNode n = jp.getCodec().readTree(jp);
        Collection<JsonNode> columns = n.findValues("changes");
        if ( columns !=null ){
            ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();

            for (JsonNode node : columns) {
                Iterator<JsonNode>nodes = node.elements();

                while (nodes.hasNext()){
                    EntitySearchChange change=mapper.readValue(nodes.next().toString(), EntitySearchChange.class);
                    changes.addChange(change);

                }
            }
        }
        return changes;
    }
}
