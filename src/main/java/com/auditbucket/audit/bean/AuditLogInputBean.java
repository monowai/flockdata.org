package com.auditbucket.audit.bean;

import com.auditbucket.audit.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Map;

/**
 * User: mike
 * Date: 8/05/13
 * Time: 7:41 PM
 */
public class AuditLogInputBean {
    //'{"eventType":"change","auditKey":"c27ec2e5-2e17-4855-be18-bd8f82249157","fortressUser":"miketest","when":"2012-11-10","jsonWhat":"{name: 22}"}'
    String auditKey;
    private String txRef;
    private Boolean isTransactional = false;
    String fortressUser;
    String eventType;
    String when;
    String what;
    Map<String, Object> mWhat;
    String yourRef;
    private String comment;
    private String message;
    private AuditService.LogStatus logStatus;

    static final ObjectMapper om = new ObjectMapper();

    protected AuditLogInputBean() {
    }

    public AuditLogInputBean(String auditKey, String fortressUser, DateTime when, String jsonWhat) throws IOException {
        this(auditKey, fortressUser, when, jsonWhat, false);
    }

    /**
     * @param auditKey     -guid
     * @param fortressUser -user name recognisable in the fortress
     * @param when         -fortress view of DateTime
     * @param jsonWhat     -escaped JSON
     */
    public AuditLogInputBean(String auditKey, String fortressUser, DateTime when, String jsonWhat, Boolean isTransactional) throws IOException {
        this.auditKey = auditKey;
        this.fortressUser = fortressUser;
        if (when != null)
            this.when = when.toString();
        setTransactional(isTransactional);
        setWhat(jsonWhat);
    }

    /**
     * @param auditKey     -guid
     * @param fortressUser -user name recognisable in the fortress
     * @param when         -fortress view of DateTime
     * @param jsonWhat     -escaped JSON
     * @param event        -how the caller would like to catalog this change (create, update etc)
     */
    public AuditLogInputBean(String auditKey, String fortressUser, DateTime when, String jsonWhat, String event) throws IOException {
        this(auditKey, fortressUser, when, jsonWhat);
        this.eventType = event;
    }

    public AuditLogInputBean(String auditKey, String fortressUser, DateTime when, String jsonWhat, String event, String txName) throws IOException {
        this(auditKey, fortressUser, when, jsonWhat, event);
        this.setTxRef(txName);
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

    public Map<String, Object> getWhat() {
        return mWhat;
    }

    public String getWhatAsText() {
        return what;
    }

    public void setWhat(String jsonWhat) throws IOException {
        mWhat = om.readValue(om.readTree(jsonWhat).toString(), Map.class);
        this.what = jsonWhat;
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
