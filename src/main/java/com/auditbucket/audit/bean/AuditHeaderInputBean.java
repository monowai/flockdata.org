package com.auditbucket.audit.bean;

import java.util.Date;

/**
 * User: mike
 * Date: 11/05/13
 * Time: 9:19 AM
 */
public class AuditHeaderInputBean {
    private String auditKey;
    private String txRef;
    private String callerRef;
    private String fortress;
    private String fortressUser;
    private String recordType;
    private Date when;
    private Boolean isTransactional = false;
    private String lastMessage;

    AuditHeaderInputBean() {
    }

    public AuditHeaderInputBean(String fortress, String fortressUser, String recordType, Date when, String callerRef) {
        this(fortress, fortressUser, recordType, when, callerRef, false);
    }

    public AuditHeaderInputBean(String fortress, String fortressUser, String recordType, Date when, String callerRef, Boolean transactional) {
        this(fortress, fortressUser, recordType, when, callerRef, (String) null);
        isTransactional = transactional;
    }

    public AuditHeaderInputBean(String fortress, String fortressUser, String recordType, Date when, String callerRef, String txRef) {
        this.when = when;
        this.fortress = fortress;
        this.fortressUser = fortressUser;
        this.recordType = recordType;
        this.callerRef = callerRef;
        setTxRef(txRef);

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

    public String getTxRef() {
        return txRef;
    }

    public void setTxRef(String txRef) {
        this.txRef = txRef;
        if (txRef != null && !txRef.isEmpty())
            setIsTransactional(true);
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public boolean isTransactional() {
        return isTransactional;
    }

    protected void setIsTransactional(Boolean transactional) {
        this.isTransactional = transactional;
    }
}
