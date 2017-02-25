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
import org.flockdata.track.bean.EntityInputBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

/**
 * Locate an Entity by either it's key or the fortress/type/code strategy
 *
 * @tag Command, Entity, Track
 * @author mholdsworth
 * @since 17/04/2016
 */
public class EntityData extends AbstractRestCommand  {

    public static final String BY_KEY = "/entity/{key}/log/last/data";
    public static final String  BY_CODE = "/entity/{fortress}/{docType}/{code}/log/last/data";
    private EntityInputBean entityInputBean;
    private Map<String, Object> result;
    private String key;

    public EntityData(FdClientIo fdClientIo, EntityInputBean entityInputBean) {
        super(fdClientIo);
        this.entityInputBean = entityInputBean;
    }

    public EntityData(FdClientIo fdClientIo, String key) {
        super(fdClientIo);
        this.key = key;
    }


    public Map<String, Object> result() {
        return result;
    }

    @Override
    public EntityData exec() {
        HttpEntity requestEntity = new HttpEntity<>(fdClientIo.getHeaders());
        result=null;   error =null;
        try {
            ParameterizedTypeReference<Map<String,Object>> responseType = new ParameterizedTypeReference<Map<String, Object>>() {};
            ResponseEntity<Map<String,Object>> response;
            if (key !=null ) {// Locate by FD unique key
                String command = getUrl()+"/api/v1"+BY_KEY;
                response = fdClientIo.getRestTemplate().exchange(command, HttpMethod.GET, requestEntity, responseType, key);
            } else
                response = fdClientIo.getRestTemplate().exchange(getUrl()+"/api/v1"+BY_CODE, HttpMethod.GET, requestEntity, responseType,
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
