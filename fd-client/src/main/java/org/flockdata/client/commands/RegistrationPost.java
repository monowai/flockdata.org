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

import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.SystemUserResultBean;
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
 * Handles the RegistrationInputBean through to Flockdata
 *
 * @author mholdsworth
 * @tag Command, SystemUser, Security, Administration
 * @since 13/04/2016
 */
@Component
public class RegistrationPost {

  private FdIoInterface fdIoInterface;

  @Autowired
  public RegistrationPost(FdIoInterface fdIoInterface) {
    this.fdIoInterface = fdIoInterface;
  }


  public CommandResponse<SystemUserResultBean> exec(RegistrationBean registrationBean) {
    String error = null;
    HttpEntity requestEntity = new HttpEntity<>(registrationBean, fdIoInterface.getHeaders());
    SystemUserResultBean result = null;
    try {
      ResponseEntity<SystemUserResultBean> response = fdIoInterface.getRestTemplate().exchange(fdIoInterface.getUrl() + "/api/v1/profiles/", HttpMethod.POST, requestEntity, SystemUserResultBean.class);
      result = response.getBody();
    } catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException e) {
      error = e.getMessage();
    }

    return new CommandResponse<>(error, result);// Everything worked
  }
}
