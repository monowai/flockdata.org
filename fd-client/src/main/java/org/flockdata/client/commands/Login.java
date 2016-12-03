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
import org.flockdata.registration.LoginRequest;
import org.flockdata.registration.SystemUserResultBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Logs the user into the FlockData service in order to make authorised calls
 * <p>
 * @tag Command, Security
 * @author mholdsworth
 * @since 4/04/2016
 */

public class Login extends AbstractRestCommand {

    SystemUserResultBean result;

    public Login(FdTemplate fdTemplate) {
        super(fdTemplate);
    }

    public SystemUserResultBean result() {
        return result;
    }

    /**
     * @return an error message (if one occurred) and null if everything is worked. Call result() to get, umm, the result
     */
    @Override    // Command
    public Login exec() {
        result = null;
        error = null;
        try {
            ResponseEntity<SystemUserResultBean> response;
            HttpEntity<LoginRequest> request = new HttpEntity<>(new LoginRequest(fdTemplate.getUser(), fdTemplate.getPass()), fdTemplate.getHeaders());
            response = fdTemplate.getRestTemplate().exchange(getUrl() + "/api/login", HttpMethod.POST, request, SystemUserResultBean.class);
            result = response.getBody();
        } catch (HttpClientErrorException |HttpServerErrorException | ResourceAccessException e) {
            error = String.format("Login for %s on %s failed with %s", getUrl(), fdTemplate.getUser(), e.getMessage());
        }
        return this;
    }
}
