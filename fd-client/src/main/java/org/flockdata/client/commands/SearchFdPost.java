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

import org.flockdata.search.EsSearchRequestResult;
import org.flockdata.search.QueryParams;
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
 * Run a query against ElasticSearch
 *
 * @author mholdsworth
 * @tag Command, Search, Query
 * @since 17/04/2016
 */
@Component
public class SearchFdPost {

  private FdIoInterface fdIoInterface;

  @Autowired
  public SearchFdPost(FdIoInterface fdIoInterface) {
    this.fdIoInterface = fdIoInterface;
  }


  public CommandResponse<EsSearchRequestResult> exec(QueryParams queryParams) {
    String error;

    EsSearchRequestResult result = null;
    HttpEntity requestEntity = new HttpEntity<>(queryParams, fdIoInterface.getHeaders());

    try {

      ResponseEntity<EsSearchRequestResult> response;
      response = fdIoInterface.getRestTemplate().exchange(fdIoInterface.getUrl() + "/api/v1/query/", HttpMethod.POST, requestEntity, EsSearchRequestResult.class);

      result = response.getBody();
      error = result.getFdSearchError();
    } catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException e) {
      error = e.getMessage();
    }
    return new CommandResponse<>(error, result);// Everything worked
  }
}
