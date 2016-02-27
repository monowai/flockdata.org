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

package org.flockdata.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.model.Entity;
import org.flockdata.model.Log;

/**
 * Used to talk with the fd-store about entity content
 * Created by mike on 17/02/16.
 */
public class LogRequest {

    private Long logId;
    private Store store;
    private Entity entity;
    private String contentType;
    private String checkSum;
    private String type;

    public LogRequest(){}

    public LogRequest(Entity entity, Log log ){
        this();
        this.logId = log.getId();
        this.store = Store.valueOf(log.getStorage());
        this.entity = entity;
        this.contentType = log.getContentType();
        this.checkSum = log.getChecksum();
    }

    public Long getLogId() {
        return logId;
    }

    public Store getStore() {
        return store;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return "LogRequest{" +
                "store=" + store +
                ", logId=" + logId +
                ", entity=" + entity.getKey() +
                '}';
    }

    public String getContentType() {
        return contentType;
    }

    public String getCheckSum() {
        return checkSum;
    }

    @JsonIgnore
    public String getType() {
        return getEntity().getType();
    }
}
