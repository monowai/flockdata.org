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

package org.flockdata.search;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.JsonUtils;
import org.flockdata.track.bean.SearchChange;

/**
 * @author mholdsworth
 * @since 23/05/2014
 */
public class SearchChangesDeserializer extends JsonDeserializer<SearchChanges> {
  @Override
  public SearchChanges deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    SearchChanges changes = new SearchChanges();
    JsonNode n = jp.getCodec().readTree(jp);
    Collection<JsonNode> columns = n.findValues("changes");
    if (columns != null) {
      ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();

      for (JsonNode node : columns) {
        Iterator<JsonNode> nodes = node.elements();

        while (nodes.hasNext()) {
          JsonNode changeNode = nodes.next();
          if (SearchChange.Type.ENTITY.name().equals(changeNode.findValue("type").asText())) {
            EntitySearchChange change = JsonUtils.toObject(changeNode.toString().getBytes(), EntitySearchChange.class);
            changes.addChange(change);
          } else if (SearchChange.Type.TAG.name().equals(changeNode.findValue("type").asText())) {
            TagSearchChange change = JsonUtils.toObject(changeNode.toString().getBytes(), TagSearchChange.class);
            changes.addChange(change);
          } else {
            throw new IOException("Unrecognized search change " + changeNode.get("type"));
          }

        }
      }
    }
    return changes;
  }
}
