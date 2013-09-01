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

import com.auditbucket.audit.model.SearchChange;
import com.auditbucket.audit.model.TxRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * User: Mike Holdsworth
 * Since: 28/08/13
 */
public class AuditLogResultBean {
    public static final String OK = "OK";
    private String message;
    private String fortress;
    private String documentType;
    private String auditKey;
    private String callerRef;
    private AuditLogInputBean.LogStatus status = AuditLogInputBean.LogStatus.OK;

    private String fortressUser;
    private String txReference = null;
    private SearchChange searchDocument;

    public AuditLogResultBean(AuditLogInputBean input) {
        this.auditKey = input.getAuditKey();
        this.callerRef = input.getCallerRef();
        this.documentType = input.getDocumentType();
        this.fortress = input.getFortress();
        this.fortressUser = input.getFortressUser();
    }

    public AuditLogResultBean() {
    }


    public AuditLogInputBean.LogStatus getStatus() {
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
    public String getAuditKey() {
        return auditKey;
    }

    public void setAuditKey(String auditKey) {
        this.auditKey = auditKey;
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

    public void setStatus(AuditLogInputBean.LogStatus status) {
        this.status = status;
    }

    public void setSearchDocument(SearchChange searchDocument) {
        this.searchDocument = searchDocument;
    }

    @JsonIgnore
    public SearchChange getSearchDocument() {
        return searchDocument;
    }
}
