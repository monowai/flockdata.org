/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
            ObjectMapper mapper = new ObjectMapper();

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
