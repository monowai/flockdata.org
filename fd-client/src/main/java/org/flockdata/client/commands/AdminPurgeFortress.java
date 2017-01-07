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

import org.flockdata.client.FdClientIo;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Purges data in a fortress
 *
 * @author mholdsworth
 * @since 4/04/2016
 * @tag Command, Fortress, Administration
 */

public class AdminPurgeFortress extends AbstractRestCommand {

    private String result;
    private String fortress;

    public AdminPurgeFortress(FdClientIo fdClientIo, String fortress) {
        super(fdClientIo);
        this.fortress = fortress;
    }

    public String result() {
        return result;
    }

    @Override    // Command
    public AdminPurgeFortress exec() {
        String exec = getUrl() + "/api/v1/admin/{fortress}";
        result=null; error =null;
        HttpEntity requestEntity = new HttpEntity<>(fdClientIo.getHeaders());
        try {
            ResponseEntity<String> response;
            response = fdClientIo.getRestTemplate().exchange(exec, HttpMethod.DELETE, requestEntity, String.class, fortress);
            result = response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getMessage().startsWith("401"))
                error = "auth";
            else
                error = e.getMessage();
        } catch (HttpServerErrorException | ResourceAccessException e) {
            error = e.getMessage();
        }
        return this;
    }
}
