/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.data.ChangeEvent;
import org.flockdata.data.EntityLog;
import org.flockdata.data.Log;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;

import java.util.Map;

/**
 * Returns a view on to a tracked Entity log and it's content
 * @author mholdsworth
 * @since 21/04/2016
 * @tag Contract, EntityLog, Query
 */
public class EntityLogResult {


    private Long logId;
    private Store store;
//    private EntityBean entity;
    private String entityKey;
    private String contentType;
    private String checkSum;
    private String madeBy;
    private String comment;
    private Long when;
//    private Boolean indexed;
    private ChangeEventResultBean event;
    private Map<String, Object> data;
    private boolean versioned;
    private String checksum;

    EntityLogResult(){}

    public EntityLogResult(EntityLog entityLog) {
        this();
        Log log = entityLog.getLog();
        this.logId = entityLog.getId();
        this.store = Store.valueOf(log.getStorage());
        this.entityKey = entityLog.getEntity().getKey();
        this.contentType = log.getContentType();
        this.checkSum = log.getChecksum();
        this.versioned = !log.isMocked();
        this.event = new ChangeEventResultBean(log.getEvent());
        if (log.getContent()!=null )
            this.data = log.getContent().getData();
        if ( log.getMadeBy()!=null)
            this.madeBy = log.getMadeBy().getCode();
        this.comment= log.getComment();
        this.when = entityLog.getFortressWhen();

    }

    public EntityLogResult(EntityLog log, StoredContent storedContent) {
        this(log);
        if ( storedContent!=null )
            this.data = storedContent.getData();
    }

    public Long getLogId() {
        return logId;
    }

    public Store getStore() {
        return store;
    }

    @Override
    public String toString() {
        return "LogRequest{" +
                "store=" + store +
                ", logId=" + logId +
                ", entity=" + entityKey +
                '}';
    }

    public String getContentType() {
        return contentType;
    }

    public String getMadeBy() {
        return madeBy;
    }

    public String getEntityKey() {
        return entityKey;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getComment() {
        return comment;
    }

    public Long getWhen() {
        return when;
    }

    public ChangeEvent getEvent() {
        return event;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getData() {
        return data;
    }

    public boolean isVersioned() {
        return versioned;
    }

    public void setMocked(boolean mocked) {
        this.versioned = mocked;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
