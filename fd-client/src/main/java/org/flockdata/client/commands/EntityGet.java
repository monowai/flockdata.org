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

import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityResultBean;
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
 * Locate an Entity by either it's key or the fortress/type/code strategy
 *
 * @author mholdsworth
 * @tag Command, Entity, Track
 * @since 17/04/2016
 */
@Component
public class EntityGet {

  private FdIoInterface fdIoInterface;

  @Autowired
  public EntityGet(FdIoInterface fdIoInterface) {
    this.fdIoInterface = fdIoInterface;
  }


  public CommandResponse<EntityResultBean> exec(EntityInputBean entityInputBean, String key) {

    HttpEntity requestEntity = new HttpEntity<>(fdIoInterface.getHeaders());
    EntityResultBean result = null;
    String error = null;
    try {
      ResponseEntity<EntityResultBean> response;
      if (key == null) {
        response = fdIoInterface.getRestTemplate().exchange(fdIoInterface.getUrl() + "/api/v1/entity/{fortress}/{docType}/{code}", HttpMethod.GET, requestEntity, EntityResultBean.class,
            entityInputBean.getFortress().getName(),
            entityInputBean.getDocumentType().getName(),
            entityInputBean.getCode());
      } else {
        response = fdIoInterface.getRestTemplate().exchange(fdIoInterface.getUrl() + "/api/v1/entity/{key}", HttpMethod.GET, requestEntity, EntityResultBean.class, key);
      }

      result = response.getBody();

    } catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
      error = e.getMessage();
    }
    return new CommandResponse<>(error, result);
  }
}
