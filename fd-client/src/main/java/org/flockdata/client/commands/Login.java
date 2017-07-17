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

import jline.console.ConsoleReader;
import org.flockdata.registration.LoginRequest;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.transform.FdIoInterface;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.core.CommandMarker;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;

/**
 * Logs the user into the FlockData service in order to make authorised calls
 *
 * @author mholdsworth
 * @tag Command, Security
 * @since 4/04/2016
 */

@Component
public class Login implements CommandMarker {
    /**
     * @param fdIoInterface
     * @return an error message (if one occurred) and null if everything is worked. Call result() to get, umm, the result
     */
    public CommandResponse<SystemUserResultBean> exec(FdIoInterface fdIoInterface, String user, String pass) {
        String error = null;
        SystemUserResultBean result = null;
        try {
            ResponseEntity<SystemUserResultBean> response;
            HttpEntity<LoginRequest> request = new HttpEntity<>(new LoginRequest(user, pass), fdIoInterface.getHeaders());
            response = fdIoInterface.getRestTemplate().exchange(fdIoInterface.getUrl() + "/api/login", HttpMethod.POST, request, SystemUserResultBean.class);
            result = response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
            error = String.format("Login for %s on %s failed with %s", fdIoInterface.getUrl(), user, e.getMessage());
        }
        return new CommandResponse<>(error, result);
    }

    public String readPassword() {
        String question = "password : ";
        try {
            ConsoleReader consolereader = new ConsoleReader();
            return consolereader.readLine(question, '*');
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String readLogin( String currentLogin ) {

        String question = String.format("login [%s]: " ,currentLogin);
        try {
            ConsoleReader consolereader = new ConsoleReader();
            String login =  consolereader.readLine(question);
            if ( login.equalsIgnoreCase(""))
                login= currentLogin;
            return login;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }
}
