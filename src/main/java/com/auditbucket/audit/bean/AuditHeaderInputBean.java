package com.auditbucket.audit.bean;

import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.Date;

/**
 * User: mike
 * Date: 11/05/13
 * Time: 9:19 AM
 */
public class AuditHeaderInputBean {
    private String auditKey;
    private String callerRef;
    private String fortress;
    private String fortressUser;
    private String recordType;
    private Date when;
    private String lastMessage;
    private AuditLogInputBean auditLog;

    AuditHeaderInputBean() {
    }

    public AuditHeaderInputBean(String fortress, String fortressUser, String recordType, Date when, String callerRef) {
        this.when = when;
        this.fortress = fortress;
        this.fortressUser = fortressUser;
        this.recordType = recordType;
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

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
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

    @JsonIgnore
    public AuditLogInputBean getAuditLog() {
        return auditLog;
    }
}
