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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Collection;

/**
 * Created by mike on 20/07/15.
 */
public class TrackRequestResult implements Serializable {

    private String company;
    private String fortressCode;
    private String code;
    private String key;

    private boolean newEntity;
    private ContentInputBean.LogStatus logStatus;

    private Collection<String> serviceMessages = null;

    TrackRequestResult() {
    }

    public TrackRequestResult(TrackResultBean resultBean) {
        this();
        this.key = resultBean.getKey();
        this.code = resultBean.getEntity().getCode();
        this.fortressCode = resultBean.getEntity().getSegment().getFortress().getCode();
        logStatus = resultBean.getLogStatus();
        this.company = resultBean.getCompany().getCode();
        serviceMessages = resultBean.getServiceMessages();
        newEntity = resultBean.isNewEntity();
    }

    public String getKey() {
        return key;
    }

    public String getCode() {
        return code;
    }

    public String getFortressCode() {
        return fortressCode;
    }

    public String getCompany() {
        return company;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ContentInputBean.LogStatus getLogStatus() {
        return logStatus;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Collection<String> getServiceMessages() {
        return serviceMessages;
    }

    public boolean isNewEntity() {
        return newEntity;
    }


}
