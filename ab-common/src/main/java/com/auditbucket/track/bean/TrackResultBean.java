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

import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackTag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Since: 11/05/13
 */
public class TrackResultBean {
    private Long metaId = null;
    private String serviceMessage;
    private String fortressName;
    private String documentType;
    private String callerRef;
    private String metaKey;
    private LogResultBean logResult;
    private LogInputBean log;
    private MetaHeader metaHeader;
    private Collection<TrackTag> tags;
    private MetaInputBean metaInputBean;

    protected TrackResultBean() {
    }

    /**
     * @param serviceMessage server side error messgae to return to the caller
     */
    public TrackResultBean(String serviceMessage) {
        this();
        this.serviceMessage = serviceMessage;
    }

    private TrackResultBean(String fortressName, String documentType, String callerRef, String metaKey) {
        this();
        this.fortressName = fortressName;
        this.documentType = documentType;
        this.callerRef = callerRef;
        this.metaKey = metaKey;

    }

    public TrackResultBean(MetaHeader input) {
        this(input.getFortress().getName(), input.getDocumentType(), input.getCallerRef(), input.getMetaKey());
        this.metaId = input.getId();
        this.metaHeader = input;
    }

    public TrackResultBean(LogResultBean logResultBean, LogInputBean input) {
        this.logResult = logResultBean;
        this.log = input;
        this.metaHeader = logResultBean.getMetaHeader();
        // ToDo: Do we need these instance variables or just get straight from the header?
        if (metaHeader != null) {
            this.fortressName = metaHeader.getFortress().getName();
            this.documentType = metaHeader.getDocumentType();
            this.callerRef = metaHeader.getCallerRef();
            this.metaKey = metaHeader.getMetaKey();
        }
    }

    public String getFortressName() {
        return fortressName;
    }

    public String getDocumentType() {
        return documentType;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCallerRef() {
        return callerRef;
    }

    public String getMetaKey() {
        if (metaHeader != null)
            return metaHeader.getMetaKey();
        return metaKey;
    }

    public void setMetaKey(String metaKey) {
        this.metaKey = metaKey;
    }

    public String getServiceMessage() {
        return serviceMessage;
    }

    public void setServiceMessage(String serviceMessage) {
        this.serviceMessage = serviceMessage;
    }

    @JsonIgnore
    public Long getMetaId() {
        return metaId;
    }

    @JsonIgnore
    public MetaHeader getMetaHeader() {
        return metaHeader;
    }

    public void setLogResult(LogResultBean logResult) {
        this.logResult = logResult;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public LogResultBean getLogResult() {
        return logResult;
    }

    boolean wasDuplicate = false;

    public void setWasDuplicate() {
        this.wasDuplicate = true;
    }

    public boolean isDuplicate() {
        return wasDuplicate;
    }

    public void setTags(Collection<TrackTag> tags) {
        this.tags = tags;
    }

    @JsonIgnore
    /**
     * Only used when creating  relationships for the purpose of search
     * that bypass the graph
     */
    public Collection<TrackTag> getTags() {
        return tags;
    }

    @JsonIgnore
    public LogInputBean getLog() {
        return log;
    }

    public void setLogInput(LogInputBean logInputBean) {
        this.log = logInputBean;
    }

    public void setMetaInputBean(MetaInputBean metaInputBean) {
        this.metaInputBean = metaInputBean;
    }

    @JsonIgnore
    public MetaInputBean getMetaInputBean() {
        return metaInputBean;
    }

    public boolean processLog() {
        return getLog() != null && log.getStatus() != LogInputBean.LogStatus.IGNORE;
    }
}
