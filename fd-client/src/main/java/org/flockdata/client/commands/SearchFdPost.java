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
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.flockdata.shared.ClientConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Locate a tag
 * Created by mike on 17/04/16.
 */
public class SearchFdPost extends AbstractRestCommand {

    private QueryParams queryParams;

    private EsSearchResult result;

    /**
     *
     * @param clientConfiguration where to find the API
     * @param fdRestWriter  dispatch mechanism
     * @param queryParams   query params. Company will be added automatically based on login used
     */
    public SearchFdPost(ClientConfiguration clientConfiguration, FdRestWriter fdRestWriter, QueryParams queryParams) {
        super(clientConfiguration, fdRestWriter);
        this.queryParams = queryParams;
    }


    public EsSearchResult result() {
        return result;
    }

    @Override
    public SearchFdPost exec() {
        result =null; error =null;
        HttpEntity requestEntity = new HttpEntity<>(queryParams,httpHeaders);

        try {

            ResponseEntity<EsSearchResult> response;
            response = restTemplate.exchange(url + "/api/v1/query/", HttpMethod.POST, requestEntity, EsSearchResult.class);

            result = response.getBody();
        } catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException e) {
            error= e.getMessage();
        }
        return this;// Everything worked
    }
}
