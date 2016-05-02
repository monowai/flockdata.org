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
import org.flockdata.registration.TagResultBean;
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
public class TagsGet extends AbstractRestCommand {

    private String label;

    private TagResultBean[] result;

    public TagsGet(ClientConfiguration clientConfiguration, FdRestWriter fdRestWriter, String label) {
        super(clientConfiguration, fdRestWriter);
        this.label = label;
    }

    public TagResultBean[] result() {
        return result;
    }

    @Override
    public TagsGet exec() {
        result =null; error =null;
        HttpEntity requestEntity = new HttpEntity<>(httpHeaders);

        try {
            ResponseEntity<TagResultBean[]> response;
            response = restTemplate.exchange(url + "/api/v1/tag/{label}", HttpMethod.GET, requestEntity, TagResultBean[].class, label);

            result = response.getBody();//JsonUtils.toCollection(response.getBody(), TagResultBean.class);
        }catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException e) {
            error= e.getMessage();
        }
        return this;// Everything worked
    }
}
