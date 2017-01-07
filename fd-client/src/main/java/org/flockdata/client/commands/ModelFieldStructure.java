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
import org.flockdata.search.ContentStructure;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Returns metadata about the field structures from ElasticSearch.
 *
 * @tag Command, Fortress, Search, ElasticSearch
 * @author mholdsworth
 * @since 31/08/2016
 */
public class ModelFieldStructure extends AbstractRestCommand{
    private ContentStructure result;
    private String fortress;
    private String documentType ;

    public ModelFieldStructure(FdClientIo fdClientIo, String fortress, String documentType) {
        super(fdClientIo);
        this.fortress = fortress;
        this.documentType = documentType;
    }


    @Override
    public ModelFieldStructure exec() {
        HttpEntity requestEntity = new HttpEntity<>(fdClientIo.getHeaders());
        result=null;   error =null;
        try {

            ResponseEntity<ContentStructure> response ;
                response = fdClientIo.getRestTemplate().exchange(getUrl()+"/api/v1/model/{fortress}/{docType}/fields", HttpMethod.GET, requestEntity, ContentStructure.class,
                        fortress,
                        documentType);

            result = response.getBody();//JsonUtils.toCollection(response.getBody(), TagResultBean.class);

        } catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
            error= e.getMessage();
        }
        return this;// Everything worked
    }

    @Override
    public ContentStructure result() {
        return result;
    }
}
