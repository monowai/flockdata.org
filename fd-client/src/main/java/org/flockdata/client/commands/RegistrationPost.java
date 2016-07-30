/*
 *  Copyright 2012-2016 the original author or authors.
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

import org.flockdata.client.FdTemplate;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.SystemUserResultBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Handles the RegistrationInputBean through to Flockdata
 *
 * Created by mike on 13/04/16.
 */
public class RegistrationPost extends AbstractRestCommand {
    /**
     * Set's the basic immutable properties for this command
     *
     * @param clientConfiguration URL, APIkey, user, password
     * @param restWriter          Helper class to access HTTP resources
     */

    private  RegistrationBean registrationBean;

    private SystemUserResultBean result =null;

    public RegistrationPost(FdTemplate fdTemplate, RegistrationBean registrationBean) {
        super(fdTemplate);
        this.registrationBean=registrationBean;

    }

    public SystemUserResultBean result() {
        return result;
    }

    @Override
    public RegistrationPost exec() {
        result=null; error =null;
        HttpEntity requestEntity = new HttpEntity<>(registrationBean, fdTemplate.getHeaders());

        try {
            ResponseEntity<SystemUserResultBean> response = fdTemplate.getRestTemplate().exchange(getUrl()+"/api/v1/profiles/", HttpMethod.POST, requestEntity, SystemUserResultBean.class);
            result = response.getBody();
        } catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException e) {
            error= e.getMessage();
        }
        return this;// Everything worked
    }
}
