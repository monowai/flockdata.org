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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Simple ping which replies with pong
 *
 * @tag Command, Administration
 * @author mholdsworth
 * @since 4/04/2016
 */

public class Ping extends AbstractRestCommand {

    String result;
    private HttpHeaders httpHeaders;

    private String url = null;

    public Ping(FdClientIo fdClientIo) {
        super(fdClientIo);
        this.url = fdClientIo.getUrl();
    }

    public Ping(FdClientIo fdClientIo, String url) {
        super(fdClientIo);
        this.url = url;

    }

    @Override
    public String getUrl() {
        return this.url;
    }

    public String result() {
        return result;
    }

    @Override    // Command
    public Ping exec() {
        result = null;
        error = null;
        String exec = getUrl() + "/api/ping/";
        HttpEntity requestEntity = new HttpEntity<>(httpHeaders);
        try {
            ResponseEntity<String> response = fdClientIo.getRestTemplate().exchange(exec, HttpMethod.GET, requestEntity, String.class);
            result = response.getBody();
            error = null;
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
