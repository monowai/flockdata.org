/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.client.commands;

import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.transform.FdIoInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Get logs associated with an entity key. Lets you see the actual data stored against the entity
 *
 * @author mholdsworth
 * @tag Command, Fortress, Entity, Log, Query
 * @since 17/04/2016
 */
@Component
public class EntityLogsGet {

  private FdIoInterface fdIoInterface;

  @Autowired
  public EntityLogsGet(FdIoInterface fdIoInterface) {
    this.fdIoInterface = fdIoInterface;
  }

  public CommandResponse<EntityLogResult[]> exec(String key) {
    String error = null;
    HttpEntity requestEntity = new HttpEntity<>(fdIoInterface.getHeaders());
    EntityLogResult[] results = null;

    try {

      ResponseEntity<EntityLogResult[]> response;
      response = fdIoInterface.getRestTemplate().exchange(fdIoInterface.getUrl() + "/api/v1/entity/{key}/log?withData=true", HttpMethod.GET, requestEntity, EntityLogResult[].class, key);


      results = response.getBody();//JsonUtils.toCollection(response.getBody(), TagResultBean.class);
    } catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException e) {
      error = e.getMessage();
    }
    return new CommandResponse<>(error, results);// Everything worked
  }
}
