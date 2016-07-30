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
import org.flockdata.profile.model.ContentModel;
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
public class ModelGet extends AbstractRestCommand {

    private ContentModel results;

    private String fortress;
    private String type;

    public ModelGet(FdTemplate fdTemplate, String fortress, String type) {
        super(fdTemplate);
        this.fortress = fortress.toLowerCase();
        this.type = type.toLowerCase();
    }


    public ContentModel result() {
        return results;
    }

    @Override
    public ModelGet exec() {
        results=null;   error =null;

        try {
            HttpEntity requestEntity = new HttpEntity<>(fdTemplate.getHeaders());
            ResponseEntity<ContentModel> response;
            response = fdTemplate.getRestTemplate().exchange(getUrl()+"/api/v1/model/{fortress}/{type}", HttpMethod.GET, requestEntity, ContentModel.class, fortress, type);
            results = response.getBody();//JsonUtils.toCollection(response.getBody(), TagResultBean.class);

        } catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException e) {
            error= e.getMessage();
        }
        return this;// Everything worked
    }
}
