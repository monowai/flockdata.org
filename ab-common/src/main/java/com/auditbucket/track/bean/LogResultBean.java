/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.track.bean;

import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.TrackLog;
import com.auditbucket.track.model.TxRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * User: Mike Holdsworth
 * Since: 28/08/13
 */
public class LogResultBean {
    public static final String OK = "OK";
    private String message;
    private String fortress;
    private String documentType;
    private String metaKey;
    private String callerRef;
    private LogInputBean.LogStatus status = LogInputBean.LogStatus.OK;

    private String fortressUser;
    private String txReference = null;
    private Long sysWhen;
    private TrackLog logToIndex;
    private Log whatLog;

    public LogResultBean(LogInputBean input) {
        this();
        this.metaKey = input.getMetaKey();
        this.callerRef = input.getCallerRef();
        this.documentType = input.getDocumentType();
        this.fortress = input.getFortress();
        this.fortressUser = input.getFortressUser();
    }

    private LogResultBean() {
    }


    public LogInputBean.LogStatus getStatus() {
        return status;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getMetaKey() {
        return metaKey;
    }

    public void setMetaKey(String metaKey) {
        this.metaKey = metaKey;
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

    public void setStatus(LogInputBean.LogStatus status) {
        this.status = status;
    }

    public void setSysWhen(Long sysWhe) {
        this.sysWhen = sysWhe;
    }

    public Long getSysWhen() {
        return sysWhen;
    }

    public void setLogToIndex(TrackLog logToIndex) {
        this.logToIndex = logToIndex;
    }

    @JsonIgnore
    public TrackLog getLogToIndex() {
        return logToIndex;
    }

    @JsonIgnore
    public void setWhatLog(Log whatLog) {
        this.whatLog = whatLog;
    }

    public Log getWhatLog() {
        return whatLog;
    }
}
