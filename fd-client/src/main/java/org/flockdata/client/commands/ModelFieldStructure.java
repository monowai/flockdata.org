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

import org.flockdata.search.ContentStructure;
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
 * Returns metadata about the field structures from ElasticSearch.
 *
 * @author mholdsworth
 * @tag Command, Fortress, Search, ElasticSearch
 * @since 31/08/2016
 */
@Component
public class ModelFieldStructure {

  private FdIoInterface fdIoInterface;

  @Autowired
  public ModelFieldStructure(FdIoInterface fdIoInterface) {
    this.fdIoInterface = fdIoInterface;
  }


  public CommandResponse<ContentStructure> exec(String fortress, String documentType) {
    HttpEntity requestEntity = new HttpEntity<>(fdIoInterface.getHeaders());
    ContentStructure result = null;
    String error = null;
    try {

      ResponseEntity<ContentStructure> response;
      response = fdIoInterface.getRestTemplate().exchange(fdIoInterface.getUrl() + "/api/v1/model/{fortress}/{docType}/fields", HttpMethod.GET, requestEntity, ContentStructure.class,
          fortress,
          documentType);

      result = response.getBody();//JsonUtils.toCollection(response.getBody(), TagResultBean.class);

    } catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
      error = e.getMessage();
    }
    return new CommandResponse<>(error, result);// Everything worked
  }


}
