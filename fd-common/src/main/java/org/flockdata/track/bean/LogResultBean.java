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
import org.flockdata.model.EntityLog;
import org.flockdata.model.TxRef;

import java.io.Serializable;

/**
 * User: Mike Holdsworth
 * Since: 28/08/13
 */
public class LogResultBean implements Serializable {
    public static final String OK = "OK";
    private String fortress;
    private String documentType;
    private String callerRef;
    private ContentInputBean.LogStatus status = ContentInputBean.LogStatus.OK;

    private String fortressUser;
    private String txReference = null;
    private Long sysWhen;
    private EntityLog entityLog;
    private boolean logIgnored = false;

    private LogResultBean() {
    }

    public LogResultBean(ContentInputBean content) {
        this();
        setFortressUser(content.getFortressUser());

    }

    public ContentInputBean.LogStatus getStatus() {
        return status;
    }

    public void setTxReference(TxRef txReference) {
        if (txReference != null)
            this.txReference = txReference.getName();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getTxReference() {
        return txReference;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getFortressUser() {
        return fortressUser;
    }

    public void setFortressUser(String fortressUser) {
        this.fortressUser = fortressUser;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCallerRef() {
        return callerRef;
    }

    public void setCallerRef(String callerRef) {
        this.callerRef = callerRef;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getFortress() {
        return fortress;
    }

    public void setFortress(String fortress) {
        this.fortress = fortress;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

//    public void setLogStatus(ContentInputBean.LogStatus status) {
//        this.status = status;
//    }

    public void setSysWhen(Long sysWhen) {
        this.sysWhen = sysWhen;
    }

    public Long getSysWhen() {
        return sysWhen;
    }

    public void setLogToIndex(EntityLog logToIndex) {
        this.entityLog = logToIndex;
    }

//    @JsonIgnore
//    public EntityLog getLogToIndex() {
//        return entityLog;
//    }


}
