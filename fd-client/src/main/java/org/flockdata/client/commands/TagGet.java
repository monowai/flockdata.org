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
import org.flockdata.registration.TagResultBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Locate a tag by it's Label and Code
 *
 * @tag Command, Tag, Query
 * @author mholdsworth
 * @since 17/04/2016
 */
public class TagGet extends AbstractRestCommand  {

    private String label;
    private String code;

    private TagResultBean result;

    public TagGet(FdClientIo fdClientIo, String label, String code) {
        super(fdClientIo);
        this.label = label;
        this.code = code;
    }

    public TagResultBean result() {
        return result;
    }

    @Override
    public TagGet exec() {
        result=null; error =null;
        HttpEntity requestEntity = new HttpEntity<>(fdClientIo.getHeaders());

        try {
            ResponseEntity<TagResultBean> response ;
                response = fdClientIo.getRestTemplate().exchange(getUrl()+"/api/v1/tag/{label}/{code}", HttpMethod.GET, requestEntity, TagResultBean.class, label,code );

            result = response.getBody();//JsonUtils.toCollection(response.getBody(), TagResultBean.class);
        } catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException e) {
            error= e.getMessage();
        }
        return this;// Everything worked
    }
}
