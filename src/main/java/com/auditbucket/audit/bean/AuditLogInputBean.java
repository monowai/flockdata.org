package com.auditbucket.audit.bean;

import com.auditbucket.audit.service.AuditService;
import org.joda.time.DateTime;

/**
 * User: mike
 * Date: 8/05/13
 * Time: 7:41 PM
 */
public class AuditLogInputBean {
    //'{"eventType":"change","auditKey":"c27ec2e5-2e17-4855-be18-bd8f82249157","fortressUser":"miketest","when":"2012-11-10","what":"{name: 22}"}'
    String auditKey;
    String eventType;
    String fortressUser;
    private String txRef;
    private Boolean isTransactional = false;
    String when;
    String what;
    String yourRef;
    private String comment;
    private String message;
    private AuditService.LogStatus logStatus;

    public AuditLogInputBean() {
    }

    public AuditLogInputBean(String auditKey, String fortressUser, DateTime when, String what) {
        this.auditKey = auditKey;
        this.fortressUser = fortressUser;
        this.when = when.toString();
        this.what = what;
    }

    public AuditLogInputBean(String auditKey, String fortressUser, DateTime when, String what, String event) {
        this(auditKey, fortressUser, when, what);
        this.eventType = event;
    }

    public String getAuditKey() {
        return auditKey;
    }

    public void setAuditKey(String auditKey) {
        this.auditKey = auditKey;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getFortressUser() {
        return fortressUser;
    }

    public void setFortressUser(String fortressUser) {
        this.fortressUser = fortressUser;
    }

    public String getWhen() {
        return when;
    }

    public void setWhen(String when) {
        this.when = when;
    }

    public String getWhat() {
        return what;
    }

    public void setWhat(String what) {
        this.what = what;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getYourRef() {
        return yourRef;
    }

    public void setYourRef(String yourRef) {
        this.yourRef = yourRef;
    }


    public String getTxRef() {
        return txRef;
    }

    public void setTxRef(String txRef) {
        if (txRef != null && txRef.equals(""))
            this.txRef = null;
        else {
            this.txRef = txRef;
            setTransactional(true);
        }
    }

    public void setMessage(String message) {
        this.message = message;

    }

    public String getMessage() {
        return message;
    }

    public void setTransactional(Boolean isTransactional) {
        this.isTransactional = isTransactional;
    }

    public Boolean isTransactional() {
        return isTransactional;
    }

    public void setStatus(AuditService.LogStatus logStatus) {
        this.logStatus = logStatus;

    }

    public AuditService.LogStatus getLogStatus() {
        return this.logStatus;
    }
}
