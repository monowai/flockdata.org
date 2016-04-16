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

import org.flockdata.client.rest.FdRestWriter;
import org.flockdata.registration.LoginRequest;
import org.flockdata.registration.UserProfile;
import org.flockdata.shared.ClientConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * HTTP login
 *
 * Created by mike on 4/04/16.
 */

public class Login extends AbstractRestCommand{

    UserProfile result;

    public Login(ClientConfiguration clientConfiguration, FdRestWriter restWriter) {
        super(clientConfiguration, restWriter);
    }

    public UserProfile getResult() {
        return result;
    }

    @Override    // Command
    public String exec() {
        String exec = url + "/api/login";
        try {
            ResponseEntity<UserProfile> response;
            HttpEntity<LoginRequest> request = new HttpEntity<>(new LoginRequest(user,pass), httpHeaders);
            response = restTemplate.exchange(exec, HttpMethod.POST, request, UserProfile.class);
            result = response.getBody();
        } catch (HttpClientErrorException e) {

            if (e.getMessage().startsWith("401"))
                error = "auth";
            else
                error = e.getMessage();
        } catch (HttpServerErrorException | ResourceAccessException e) {
            error = e.getMessage();
        }
        return error;
    }
}
