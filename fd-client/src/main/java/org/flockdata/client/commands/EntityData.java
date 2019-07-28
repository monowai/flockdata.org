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

import java.util.Map;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.transform.FdIoInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
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
public class EntityData {

  private static final String BY_KEY = "/entity/{key}/log/last/data";
  private static final String BY_CODE = "/entity/{fortress}/{docType}/{code}/log/last/data";

  private FdIoInterface fdIoInterface;

  @Autowired
  public EntityData(FdIoInterface fdIoInterface) {
    this.fdIoInterface = fdIoInterface;
  }


  public CommandResponse<Map<String, Object>> exec(String key) {
    return exec(key, null);
  }

  public CommandResponse<Map<String, Object>> exec(EntityInputBean entityInputBean) {
    return exec(null, entityInputBean);
  }

  public CommandResponse<Map<String, Object>> exec(String key, EntityInputBean entityInputBean) {
    HttpEntity requestEntity = new HttpEntity<>(fdIoInterface.getHeaders());
    Map<String, Object> result = null;
    String error = null;

    try {
      ParameterizedTypeReference<Map<String, Object>> responseType = new ParameterizedTypeReference<Map<String, Object>>() {
      };
      ResponseEntity<Map<String, Object>> response;
      if (key != null) {// Locate by FD unique key
        String command = fdIoInterface.getUrl() + "/api/v1" + BY_KEY;
        response = fdIoInterface.getRestTemplate().exchange(command, HttpMethod.GET, requestEntity, responseType, key);
      } else {
        response = fdIoInterface.getRestTemplate().exchange(fdIoInterface.getUrl() + "/api/v1" + BY_CODE, HttpMethod.GET, requestEntity, responseType,
            entityInputBean.getFortress().getName(),
            entityInputBean.getDocumentType().getName(),
            entityInputBean.getCode());
      }

      result = response.getBody();//JsonUtils.toCollection(response.getBody(), TagResultBean.class);

    } catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
      error = e.getMessage();
    }
    return new CommandResponse<>(error, result);// Everything worked
  }
}
