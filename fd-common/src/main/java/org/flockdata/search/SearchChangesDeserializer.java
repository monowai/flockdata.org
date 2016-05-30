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

package org.flockdata.search;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.JsonUtils;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.SearchChanges;
import org.flockdata.search.model.TagSearchChange;
import org.flockdata.track.bean.SearchChange;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * User: mike
 * Date: 23/05/14
 * Time: 12:29 PM
 */
public class SearchChangesDeserializer extends JsonDeserializer<SearchChanges> {
    @Override
    public SearchChanges deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        SearchChanges changes = new SearchChanges();
        JsonNode n = jp.getCodec().readTree(jp);
        Collection<JsonNode> columns = n.findValues("changes");
        if ( columns !=null ){
            ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();

            for (JsonNode node : columns) {
                Iterator<JsonNode>nodes = node.elements();

                while (nodes.hasNext()){
                    JsonNode changeNode = nodes.next();
                    if (SearchChange.Type.ENTITY.name().equals(changeNode.findValue("type").asText())) {
                        EntitySearchChange change = JsonUtils.toObject(changeNode.toString().getBytes(), EntitySearchChange.class);
                        changes.addChange(change);
                    } else if (SearchChange.Type.TAG.name().equals(changeNode.findValue("type").asText())) {
                        TagSearchChange change = JsonUtils.toObject(changeNode.toString().getBytes(), TagSearchChange.class);
                        changes.addChange(change);
                    } else
                        throw new IOException("Unrecognized search change "+ changeNode.get("type"));

                }
            }
        }
        return changes;
    }
}
