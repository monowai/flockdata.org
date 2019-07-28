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
import org.springframework.http.MediaType;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;

/**
 * Useful handlers for integration classes
 *
 * @author mholdsworth
 * @since 13/02/2016
 */
public class AbstractIntegrationRequest {

  private ObjectToJsonTransformer objectToJsonTransformer;
  private JsonToObjectTransformer jsonToObjectTransformer;

  public ObjectToJsonTransformer objectToJson() {
    return objectToJsonTransformer;
  }

  public JsonToObjectTransformer jsonToObjectTransformer() {
    return jsonToObjectTransformer;
  }

  @PostConstruct
  public void createTransformers() {
    objectToJsonTransformer = new ObjectToJsonTransformer(
        new Jackson2JsonObjectMapper(JsonUtils.getMapper())
    );

    objectToJsonTransformer.setContentType(MediaType.APPLICATION_JSON_UTF8.getType());
    jsonToObjectTransformer = new JsonToObjectTransformer(new Jackson2JsonObjectMapper(JsonUtils.getMapper()));
  }
}
