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

import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackRequestResult;
import org.flockdata.transform.FdIoInterface;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.core.CommandMarker;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Track an EntityInputBean into FlockData
 *
 * @tag Command, Entity, Track
 * @author mholdsworth
 * @since 17/04/2016
 */
@Component
public class TrackEntityPost implements CommandMarker {

    public CommandResponse<TrackRequestResult > exec(FdIoInterface fdIoInterface, EntityInputBean entityInputBean) {
        TrackRequestResult result=null;
        String error =null;
        HttpEntity<EntityInputBean> requestEntity = new HttpEntity<>(entityInputBean, fdIoInterface.getHeaders());

        try {
            ResponseEntity<TrackRequestResult> restResult = fdIoInterface.getRestTemplate().exchange(fdIoInterface.getUrl()+"/api/v1/track/", HttpMethod.POST, requestEntity, TrackRequestResult.class);
            result = restResult.getBody();
        }catch (HttpClientErrorException | ResourceAccessException | HttpServerErrorException e) {
            error= e.getMessage();
        }
        return new CommandResponse<>(error, result);
    }
}
