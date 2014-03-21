/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

package com.auditbucket.audit.bean;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditTag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Since: 11/05/13
 */
public class AuditResultBean {
    private Long auditId = null;
    private String status;
    private String fortressName;
    private String documentType;
    private String callerRef;
    private String auditKey;
    private AuditLogResultBean logResult;
    private AuditLogInputBean auditLog;
    private com.auditbucket.audit.model.AuditHeader auditHeader;
    private Set<AuditTag> tags;
    private AuditHeaderInputBean auditInputBean;

    protected AuditResultBean() {
    }

    public AuditResultBean(String statusMessage) {
        this();
        this.status = statusMessage;
    }

    public AuditResultBean(String fortressName, String documentType, String callerRef, String auditKey) {
        this();
        this.fortressName = fortressName;
        this.documentType = documentType;
        this.callerRef = callerRef;
        this.auditKey = auditKey;

    }

    public AuditResultBean(com.auditbucket.audit.model.AuditHeader input) {
        this(input.getFortress().getName(), input.getDocumentType(), input.getCallerRef(), input.getAuditKey());
        this.auditId = input.getId();
        this.auditHeader = input;
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

    public String getAuditKey() {
        return auditKey;
    }

    public void setAuditKey(String auditKey) {
        this.auditKey = auditKey;
    }

    public String getStatus() {
        if (wasDuplicate)
            return "Existing audit record found was found and returned";
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonIgnore
    public Long getAuditId() {
        return auditId;
    }

    @JsonIgnore
    public AuditHeader getAuditHeader() {
        return auditHeader;
    }

    public void setLogResult(AuditLogResultBean logResult) {
        this.logResult = logResult;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public AuditLogResultBean getLogResult() {
        return logResult;
    }

    boolean wasDuplicate = false;

    public void setWasDuplicate() {
        this.wasDuplicate = true;
    }

    public boolean isDuplicate() {
        return wasDuplicate;
    }

    public void setTags(Set<AuditTag> tags) {
        this.tags = tags;
    }

    @JsonIgnore
    /**
     * Only used when creating  relationships for the purpose of search
     * that bypass the graph
     */
    public Set<AuditTag> getTags() {
        return tags;
    }

    @JsonIgnore
    public AuditLogInputBean getAuditLog() {
        return auditLog;
    }

    public void setAuditLogInput(AuditLogInputBean logInputBean) {
        this.auditLog = logInputBean;
    }

    public void setAuditInputBean(AuditHeaderInputBean auditInputBean) {
        this.auditInputBean = auditInputBean;
    }

    @JsonIgnore
    public AuditHeaderInputBean getAuditInputBean() {
        return auditInputBean;
    }
}
