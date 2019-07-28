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

package org.flockdata.integration;

import javax.annotation.PostConstruct;
import org.flockdata.helper.JsonUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * @author mholdsworth
 * @tag Json, Integration
 * @since 14/02/2016
 */
@Component
@Profile( {"fd-server"})
public class MessageSupport {

  private ObjectToJsonTransformer objectToJsonTransformer;
  private JsonToObjectTransformer j2o;

  @PostConstruct
  public void createTransformer() {
    objectToJsonTransformer = new ObjectToJsonTransformer(
        new Jackson2JsonObjectMapper(JsonUtils.getMapper())
    );
    objectToJsonTransformer.setContentType(MediaType.APPLICATION_JSON_UTF8.getType());

    j2o = new JsonToObjectTransformer(
        new Jackson2JsonObjectMapper(JsonUtils.getMapper())
    );

  }

  public JsonToObjectTransformer jsonToObject() {
    return j2o;
  }

  ObjectToJsonTransformer objectToJson() {
    return objectToJsonTransformer;
  }

  public Message<?> toJson(Message theObject) {
    return objectToJson().transform(theObject);
  }

  public Message<?> toObject(Message theObject) {
    return j2o.transform(theObject);
  }
}
