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
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.bean.EntityInputBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Locate an Entity by either it's key or the fortress/type/code strategy
 *
 * @tag Command, Entity, Track
 * @author mholdsworth
 * @since 17/04/2016
 */
public class EntityGet extends AbstractRestCommand  {

    private EntityInputBean entityInputBean;

    private EntityBean result;

    private String key;

    public EntityGet(FdTemplate fdTemplate, EntityInputBean entityInputBean) {
        super(fdTemplate);
        this.entityInputBean = entityInputBean;
    }

    public EntityGet(FdTemplate fdTemplate, String key) {
        super(fdTemplate);
        this.key = key;
    }


    public EntityBean result() {
        return result;
    }

    @Override
    public EntityGet exec() {
        HttpEntity requestEntity = new HttpEntity<>(fdTemplate.getHeaders());
        result=null;   error =null;
        try {

            ResponseEntity<EntityBean> response ;
            if (key !=null ) // Locate by FD unique key
                response = fdTemplate.getRestTemplate().exchange(getUrl()+"/api/v1/entity/{key}", HttpMethod.GET, requestEntity, EntityBean.class, key);
            else
                response = fdTemplate.getRestTemplate().exchange(getUrl()+"/api/v1/entity/{fortress}/{docType}/{code}", HttpMethod.GET, requestEntity, EntityBean.class,
                        entityInputBean.getFortress().getName(),
                        entityInputBean.getDocumentType().getName(),
                        entityInputBean.getCode());

            result = response.getBody();//JsonUtils.toCollection(response.getBody(), TagResultBean.class);

        } catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
            error= e.getMessage();
        }
        return this;// Everything worked
    }
}
