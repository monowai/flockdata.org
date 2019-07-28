/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.transform.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.track.bean.EntityTagRelationshipDefinition;

/**
 * JSON
 *
 * @author mholdsworth
 * @since 29/07/2015
 */
public class EntityTagRelationshipDeserializer extends JsonDeserializer<Collection<EntityTagRelationshipDefinition>> {

  @Override
  public Collection<EntityTagRelationshipDefinition> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    Collection<EntityTagRelationshipDefinition> values = new ArrayList<>();
    JsonNode node = jp.getCodec().readTree(jp);
    ObjectMapper om = FdJsonObjectMapper.getObjectMapper();
    for (JsonNode jsonNode : node) {
      values.add(om.readValue(jsonNode.toString(), EntityTagRelationshipDefinition.class));

    }
    return values;
  }
}
