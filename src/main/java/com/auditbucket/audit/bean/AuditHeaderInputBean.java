package com.auditbucket.audit.bean;

import java.util.Date;

/**
 * User: mike
 * Date: 11/05/13
 * Time: 9:19 AM
 */
public class AuditHeaderInputBean {
    private String fortress;
    private String fortressUser;
    private String recordType;
    private String callerRef;
    private String txRef;
    private Date when;
    private String[] tags;

    AuditHeaderInputBean() {
    }

    public AuditHeaderInputBean(String fortress, String fortressUser, String recordType, Date when, String callerRef) {
        this(fortress, fortressUser, recordType, when, callerRef, null);
    }

    public AuditHeaderInputBean(String fortress, String fortressUser, String recordType, Date when, String callerRef, String txRef) {
        this.when = when;
        this.fortress = fortress;
        this.fortressUser = fortressUser;
        this.recordType = recordType;
        this.callerRef = callerRef;
        this.txRef = txRef;

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
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public String[] getTags() {
        return this.tags;
    }
}
