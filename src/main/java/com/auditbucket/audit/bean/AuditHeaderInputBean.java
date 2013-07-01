package com.auditbucket.audit.bean;

import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
    private String documentType;
    private Date when;
    private String lastMessage;
    private AuditLogInputBean auditLog;
    private Map<String, String> tagValues = new HashMap<String, String>();

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

    @JsonIgnore
    public AuditLogInputBean getAuditLog() {
        return auditLog;
    }

    public Map<String, String> getTagValues() {
        return tagValues;
    }

    public void setTagValues(Map<String, String> tagValues) {
        this.tagValues = tagValues;
    }
}
