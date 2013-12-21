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

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditLog;


/**
 * User: Mike Holdsworth
 * Date: 16/06/13
 * Time: 6:12 PM
 */
public class AuditTXResult {

    private String auditKey;
    private String fortressName;
    private String fortressKey;
    private String documentType;
    private String callerRef;
    private Long lastSystemChange;
    private Long fortressWhen = 0l;

    private AuditLog auditLog;

    private AuditTXResult() {
    }


    public AuditTXResult(AuditHeader header, AuditChange change, AuditLog log) {
        this();
        this.fortressWhen = log.getFortressWhen();
        this.auditKey = header.getAuditKey();
        this.documentType = header.getDocumentType();
        this.callerRef = header.getCallerRef();
        this.fortressName = header.getFortress().getName();
        this.fortressKey = header.getFortress().getFortressKey();
        this.lastSystemChange = header.getLastUpdated();
        this.auditLog = log;
    }

    public Object getAuditLog() {
        return auditLog;
    }

    public String getAuditKey() {
        return auditKey;
    }

    public String getFortressName() {
        return fortressName;
    }

    public String getFortressKey() {
        return fortressKey;
    }

    public String getDocumentType() {
        return documentType;
    }

    public long getLastSystemChange() {
        return lastSystemChange;
    }

    public String getCallerRef() {
        return callerRef;
    }
}
