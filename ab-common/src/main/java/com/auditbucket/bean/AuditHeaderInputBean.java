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

package com.auditbucket.bean;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 11/05/13
 * Time: 9:19 AM
 */
public class AuditHeaderInputBean {
    private String auditKey;
    private String callerRef;
    private String fortress;
    private String fortressUser;
    private String documentType;
    private Date when;
    private String lastMessage;
    private AuditLogInputBean auditLog;
    private Map<String, Object> tagValues = new HashMap<String, Object>();

    AuditHeaderInputBean() {
    }

    public AuditHeaderInputBean(String fortress, String fortressUser, String documentType, Date when, String callerRef) {
        this.when = when;
        this.fortress = fortress;
        this.fortressUser = fortressUser;
        this.documentType = documentType;
        this.callerRef = callerRef;

    }

    public void setAuditKey(String auditKey) {
        this.auditKey = auditKey;
    }

    public String getAuditKey() {
        return this.auditKey;
    }

    public Date getWhen() {
        return when;
    }

    public void setWhen(Date when) {
        this.when = when;
    }

    public String getFortress() {
        return fortress;
    }

    public void setFortress(String fortress) {
        this.fortress = fortress;
    }

    public String getFortressUser() {
        return fortressUser;
    }

    public void setFortressUser(String fortressUser) {
        this.fortressUser = fortressUser;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }


    public String getCallerRef() {
        return callerRef;
    }

    public void setCallerRef(String callerRef) {
        this.callerRef = callerRef;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    protected void setIsTransactional(Boolean transactional) {
    }

    public void setAuditLog(AuditLogInputBean auditLog) {
        this.auditLog = auditLog;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public AuditLogInputBean getAuditLog() {
        return auditLog;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getTagValues() {
        return tagValues;
    }

    public void setTagValues(Map<String, Object> tagValues) {
        this.tagValues = tagValues;
    }
}
