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

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.IAuditWhen;


/**
 * User: mike
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

    private IAuditLog auditLog;

    private AuditTXResult() {
    }


    public AuditTXResult(IAuditHeader header, IAuditLog log, IAuditWhen when) {
        this.fortressWhen = when.getFortressWhen();
        if (header == null)
            header = log.getHeader();
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
