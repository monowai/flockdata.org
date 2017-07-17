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

import org.flockdata.helper.JsonUtils;
import org.flockdata.transform.FdIoInterface;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HealthCheck to a service to see if it can see other services
 *
 * @tag Command, Administration
 * @author mholdsworth
 * @since 4/04/2016
 */

@Component
public class Health {
    public CommandResponse<Map<String,Object>> exec(FdIoInterface fdIoInterface) {
        String error = null;
        Map<String,Object>result = new HashMap<>();
        String exec = fdIoInterface.getUrl() + "/api/v1/admin/health/";
        HttpEntity requestEntity = new HttpEntity<>(fdIoInterface.getHeaders());
        try {
            ResponseEntity<String> response;
            response = fdIoInterface.getRestTemplate().exchange(exec, HttpMethod.GET, requestEntity, String.class);
            result = JsonUtils.toMap(response.getBody());
        } catch (HttpClientErrorException e) {
            if (e.getMessage().startsWith("401"))
                error = "auth";
            else
                error = e.getMessage();
        } catch (HttpServerErrorException | ResourceAccessException | IOException e) {
            error = e.getMessage();
        }
        return new CommandResponse<>(error, result);
    }
}
