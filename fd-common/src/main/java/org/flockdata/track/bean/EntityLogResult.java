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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import org.flockdata.data.ChangeEvent;
import org.flockdata.data.EntityLog;
import org.flockdata.data.Log;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;

/**
 * Returns a view on to a tracked Entity log and it's content
 *
 * @author mholdsworth
 * @tag Contract, EntityLog, Query
 * @since 21/04/2016
 */
public class EntityLogResult {


  private Long id;
  private Store store;
  private String entityKey;
  private String contentType;
  private String checkSum;
  private String madeBy;
  private String comment;
  private Long when;
  private ChangeEventResultBean event;
  private Map<String, Object> data;
  private boolean versioned;
  private String checksum;
  private Log log;
  private boolean mocked = false;

  EntityLogResult() {
  }

  public EntityLogResult(EntityLog entityLog) {
    this();
    this.log = entityLog.getLog();
    this.mocked = log.isMocked();
    this.checkSum = log.getChecksum();
    this.id = entityLog.getId();
    this.store = Store.valueOf(log.getStorage());
    this.entityKey = entityLog.getEntity().getKey();
    this.contentType = log.getContentType();
    this.checkSum = log.getChecksum();
    this.versioned = !log.isMocked();
    this.event = new ChangeEventResultBean(log.getEvent());
    if (log.getContent() != null) {
      this.data = log.getContent().getData();
    }
    if (log.getMadeBy() != null) {
      this.madeBy = log.getMadeBy().getCode();
    }
    this.comment = log.getComment();
    this.when = entityLog.getFortressWhen();

  }

  public EntityLogResult(EntityLog log, StoredContent storedContent) {
    this(log);
    if (storedContent != null) {
      this.data = storedContent.getData();
    }
  }

  public Long getId() {
    return id;
  }

  public Store getStore() {
    return store;
  }

  @Override
  public String toString() {
    return "LogRequest{" +
        "store=" + store +
        ", id=" + id +
        ", entity=" + entityKey +
        '}';
  }

  public String getContentType() {
    return contentType;
  }

  public String getMadeBy() {
    return madeBy;
  }

//    public String getEntityKey() {
//        return entityKey;
//    }

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

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getChecksum() {
    return checksum;
  }

  @JsonIgnore
  public Log getLog() {
    return log;
  }

  public String getCheckSum() {
    return checkSum;
  }

  public boolean isMocked() {
    return mocked;
  }
}
