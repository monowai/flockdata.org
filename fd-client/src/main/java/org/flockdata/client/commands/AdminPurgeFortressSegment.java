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
 * Removes data from a Fortress Segment
 * @author mholdsworth
 * @since 4/04/2016
 * @tag Command, Fortress, Administration, Segment
 */

public class AdminPurgeFortressSegment extends AbstractRestCommand {

    private String result;
    private String fortress, docType, segment;

    public AdminPurgeFortressSegment(FdClientIo fdClientIo, String fortress, String docType, String segment) {
        super(fdClientIo);
        this.fortress = fortress;
        this.docType = docType;
        this.segment = segment;
    }

    public String result() {
        return result;
    }

    @Override    // Command
    public AdminPurgeFortressSegment exec() {
        String exec = getUrl()+ "/api/v1/admin/{fortress}/{docType}/{segment}";
        result=null; error =null;
        HttpEntity requestEntity = new HttpEntity<>(fdClientIo.getHeaders());
        try {
            ResponseEntity<String> response;
            response = fdClientIo.getRestTemplate().exchange(exec, HttpMethod.DELETE, requestEntity, String.class, fortress,docType,segment);
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
