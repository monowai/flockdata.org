/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.model.Entity;
import org.flockdata.model.Log;

/**
 * Used to talk with the fd-store about entity content
 * @author mholdsworth
 * @since 17/02/2016
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
