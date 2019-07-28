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

import java.util.Collection;
import org.flockdata.data.ContentModel;
import org.flockdata.model.ContentModelResult;
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
 * Store a ContentModel in the service
 *
 * @author mholdsworth
 * @tag Command, ContentModel
 * @since 17/04/2016
 */
@Component
public class ModelPost {

  private FdIoInterface fdIoInterface;

  @Autowired
  public ModelPost(FdIoInterface fdIoInterface) {
    this.fdIoInterface = fdIoInterface;
  }


  public CommandResponse<Collection<ContentModelResult>> exec(Collection<ContentModel> models) {
    Collection<ContentModelResult> results = null;
    String error = null;
    try {

      HttpEntity requestEntity = new HttpEntity<>(models, fdIoInterface.getHeaders());
      ParameterizedTypeReference<Collection<ContentModelResult>> responseType = new ParameterizedTypeReference<Collection<ContentModelResult>>() {
      };
      ResponseEntity<Collection<ContentModelResult>> response;
      response = fdIoInterface.getRestTemplate().exchange(fdIoInterface.getUrl() + "/api/v1/model/", HttpMethod.POST, requestEntity, responseType);


      results = response.getBody();//JsonUtils.toCollection(response.getBody(), TagResultBean.class);
    } catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException e) {
      error = e.getMessage();
    }
    return new CommandResponse<>(error, results);// Everything worked
  }
}
