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
public class GetTag extends AbstractRestCommand  {

    private String label;
    private String code;

    private TagResultBean results;

    public GetTag(ClientConfiguration clientConfiguration, FdRestWriter fdRestWriter, String label, String code) {
        super(clientConfiguration, fdRestWriter);
        this.label = label;
        this.code = code;
    }

    public TagResultBean getResult() {
        return results;
    }

    @Override
    public String exec() {
        HttpEntity requestEntity = new HttpEntity<>(httpHeaders);

        try {
            ResponseEntity<TagResultBean> response ;
                response = restTemplate.exchange(url+"/api/v1/tag/{label}/{code}", HttpMethod.GET, requestEntity, TagResultBean.class, label,code );

            results = response.getBody();//JsonUtils.toCollection(response.getBody(), TagResultBean.class);
        } catch (HttpClientErrorException e) {
            return e.getMessage();
        } catch (HttpServerErrorException e) {
            return e.getMessage();
        } catch (ResourceAccessException e) {
            return e.getMessage();
        }
        return null;// Everything worked
    }
}
