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

import org.flockdata.model.ChangeEvent;
import org.flockdata.model.EntityLog;
import org.flockdata.model.FortressUser;
import org.flockdata.model.Log;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;

import java.util.Map;

/**
 * Returns a view on to a tracked Entity log and it's content
 * Created by mike on 21/04/16.
 */
public class EntityLogResult {


    private Long logId;
    private Store store;
    private EntityBean entity;
    private String contentType;
    private String checkSum;
    private FortressUser madeBy;
    private String comment;
    private Long when;
    private Boolean indexed;
    private ChangeEvent event;
    private Map<String, Object> data;
    private boolean mocked;
    private Object checksum;

    EntityLogResult(){}

    public EntityLogResult(EntityLog entityLog) {
        this();
        Log log = entityLog.getLog();
        this.logId = entityLog.getId();
        this.store = Store.valueOf(log.getStorage());
        this.entity = new EntityBean(entityLog.getEntity());
        this.contentType = log.getContentType();
        this.checkSum = log.getChecksum();
        this.mocked = log.isMocked();
        this.event = log.getEvent();
        if (log.getContent()!=null )
            this.data = log.getContent().getData();
        if ( log.getMadeBy()!=null)
            this.madeBy = log.getMadeBy();
        this.comment= log.getComment();
        this.when = entityLog.getFortressWhen();
        this.indexed = entityLog.isIndexed();

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

    public EntityBean getEntity() {
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

    public FortressUser getMadeBy() {
        return madeBy;
    }

    public String getComment() {
        return comment;
    }

    public Long getWhen() {
        return when;
    }

    public Boolean getIndexed() {
        return indexed;
    }

    public ChangeEvent getEvent() {
        return event;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public boolean isMocked() {
        return mocked;
    }

    public void setMocked(boolean mocked) {
        this.mocked = mocked;
    }

    public Object getChecksum() {
        return checksum;
    }

    public void setChecksum(Object checksum) {
        this.checksum = checksum;
    }
}
