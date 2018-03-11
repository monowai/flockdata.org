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

import org.flockdata.search.QueryParams;
import org.flockdata.transform.FdIoInterface;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.Map;

/**
 * Passthrough query to ES
 *
 * @tag Command, Search, Query
 * @author mholdsworth
 * @since 17/04/2016
 */
@Component
public class SearchEsPost {


    public CommandResponse<Map<String,Object>> exec(FdIoInterface fdIoInterface, QueryParams queryParams) {
        String error= null;
        Map<String,Object> result = null;


        try {
            HttpEntity requestEntity = new HttpEntity<>(queryParams, fdIoInterface.getHeaders());
            ParameterizedTypeReference<Map<String,Object>> responseType = new ParameterizedTypeReference<Map<String, Object>>() {};
            ResponseEntity<Map<String,Object>> response;
            response = fdIoInterface.getRestTemplate().exchange(fdIoInterface.getUrl()+ "/api/v1/query/es", HttpMethod.POST, requestEntity, responseType);

            result = response.getBody();
            if ( result.containsKey("errors")){
                ArrayList<String> errors = (ArrayList<String>) result.get("errors");
                error = errors.get(0);
            }



        } catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException e) {
            error= e.getMessage();
        }
        return new CommandResponse<>(error,result);
    }

}
