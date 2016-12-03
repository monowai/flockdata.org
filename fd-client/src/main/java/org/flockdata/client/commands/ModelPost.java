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
import org.flockdata.profile.ContentModelResult;
import org.flockdata.profile.model.ContentModel;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Collection;

/**
 * Store a ContentModel in the service
 * @tag Command, ContentModel
 * @author mholdsworth
 * @since 17/04/2016
 */
public class ModelPost extends AbstractRestCommand {

    private Collection<ContentModelResult> results;

    private Collection<ContentModel> models;

    public ModelPost(FdTemplate fdTemplate,Collection<ContentModel> models) {
        super(fdTemplate);
        this.models = models;
    }


    public Collection<ContentModelResult> result() {
        return results;
    }

    @Override
    public ModelPost exec() {
        results=null;   error =null;
        try {

            HttpEntity requestEntity = new HttpEntity<>(models, fdTemplate.getHeaders());
            ParameterizedTypeReference<Collection<ContentModelResult>> responseType = new ParameterizedTypeReference<Collection<ContentModelResult>>() {};
            ResponseEntity<Collection<ContentModelResult>> response;
            response = fdTemplate.getRestTemplate().exchange(getUrl()+ "/api/v1/model/", HttpMethod.POST, requestEntity, responseType);


            results = response.getBody();//JsonUtils.toCollection(response.getBody(), TagResultBean.class);
        } catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException e) {
            error= e.getMessage();
        }
        return this;// Everything worked
    }
}
