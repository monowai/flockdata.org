package com.auditbucket.audit.bean;

import com.auditbucket.audit.service.AuditService;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private AuditService.LogStatus abStatus;
    private String abMessage;
    private Boolean isTransactional = false;
    String auditKey;
    private String txRef;
    private String comment;
    String fortressUser;
    String event;
    String when;
    String what;

    // Required to support location by Caller Ref
    String documentType;
    String callerRef;
    String fortress;

    Map<String, Object> mapWhat;

    static final ObjectMapper om = new ObjectMapper();

    protected AuditLogInputBean() {
    }

    public AuditLogInputBean(String auditKey, String fortressUser, DateTime when, String what) throws IOException {
        this(auditKey, fortressUser, when, what, false);
    }

    /**
     * @param auditKey     -guid
     * @param fortressUser -user name recognisable in the fortress
     * @param when         -fortress view of DateTime
     * @param what         -escaped JSON
     */
    public AuditLogInputBean(String auditKey, String fortressUser, DateTime when, String what, Boolean isTransactional) throws IOException {
        this.auditKey = auditKey;
        this.fortressUser = fortressUser;
        if (when != null)
            this.when = when.toString();
        setTransactional(isTransactional);
        setWhat(what);
    }

    /**
     * @param auditKey     -guid
     * @param fortressUser -user name recognisable in the fortress
     * @param when         -fortress view of DateTime
     * @param what         -escaped JSON
     * @param event        -how the caller would like to catalog this change (create, update etc)
     */
    public AuditLogInputBean(String auditKey, String fortressUser, DateTime when, String what, String event) throws IOException {
        this(auditKey, fortressUser, when, what);
        this.event = event;
    }

    public AuditLogInputBean(String auditKey, String fortressUser, DateTime when, String what, String event, String txName) throws IOException {
        this(auditKey, fortressUser, when, what, event);
        this.setTxRef(txName);
    }

    public AuditLogInputBean(String fortressUser, DateTime when, String what) throws IOException {
        this(null, fortressUser, when, what);

    }

    public String getAuditKey() {
        return auditKey;
    }

    public void setAuditKey(String auditKey) {
        this.auditKey = auditKey;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
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

    @JsonIgnore
    public Map<String, Object> getMapWhat() {
        return mapWhat;
    }

    public String getWhat() {
        return what;
    }

    public void setWhat(String jsonWhat) throws IOException {
        mapWhat = om.readValue(om.readTree(jsonWhat).toString(), Map.class);
        this.what = jsonWhat;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCallerRef() {
        return callerRef;
    }

    public void setCallerRef(String callerRef) {
        this.callerRef = callerRef;
    }

    public void setCallerRef(String fortress, String documentType, String callerRef) {
        this.callerRef = callerRef;
        this.documentType = documentType;
        this.fortress = fortress;
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

    public void setAbMessage(String abMessage) {
        this.abMessage = abMessage;

    }

    /**
     * @return auditBucket service response message
     */
    public String getAbMessage() {
        return abMessage;
    }

    public void setTransactional(Boolean isTransactional) {
        this.isTransactional = isTransactional;
    }

    public Boolean isTransactional() {
        return isTransactional;
    }

    public void setStatus(AuditService.LogStatus logStatus) {
        this.abStatus = logStatus;

    }

    public AuditService.LogStatus getAbStatus() {
        return this.abStatus;
    }

    public void setMapWhat(Map<String, Object> whatMap) {
        this.mapWhat = whatMap;

    }

    public String getDocumentType() {
        return documentType;
    }

    public String getFortress() {
        return fortress;
    }
}
