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

import org.flockdata.transform.FdIoInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Simple ping which replies with pong
 *
 * @author mholdsworth
 * @tag Command, Administration
 * @since 4/04/2016
 */

public class Ping {

  private static Logger logger = LoggerFactory.getLogger(Ping.class);
  String api = null;
  private FdIoInterface fdIoInterface;


  @Autowired
  public Ping(FdIoInterface fdIoInterface) {
    this.fdIoInterface = fdIoInterface;
  }

  public String getPath() {
    return "/api/v1/admin/ping/";
  }

  public CommandResponse<String> exec() {
    String result = null;
    String error;

    if (getApi() == null) {
      this.api = fdIoInterface.getUrl();
    }

    String exec = getApi() + getPath();
    logger.debug("Pinging [{}]", getApi());
    HttpEntity requestEntity = new HttpEntity<>(null);
    try {
      ResponseEntity<String> response = fdIoInterface.getRestTemplate().exchange(exec, HttpMethod.GET, requestEntity, String.class);
      result = response.getBody();
      error = null;
    } catch (HttpClientErrorException e) {
      if (e.getMessage().startsWith("401")) {
        error = "auth";
      } else {
        error = e.getMessage();
      }
    } catch (HttpServerErrorException | ResourceAccessException e) {
      error = e.getMessage();
    }
    return new CommandResponse<>(error, result);
  }

  public String getApi() {
    return api;
  }
}
