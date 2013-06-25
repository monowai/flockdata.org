package com.auditbucket.audit.bean;

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
