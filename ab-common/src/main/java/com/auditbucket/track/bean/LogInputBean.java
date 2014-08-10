/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
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

package com.auditbucket.track.bean;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.track.model.ChangeEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 8/05/13
 * Time: 7:41 PM
 */
public class LogInputBean {
    private LogStatus abStatus;
    private String abMessage;
    private Boolean isTransactional = false;

    // Use either metaKey or CallerRef strategy
    // Required if not updating via a MetaHeader
    private String metaKey;

    // For tracking by Caller Ref
    private String documentType;
    private String callerRef;
    private String fortress;

    private String txRef;
    private String comment;
    private String fortressUser;
    private String event;
    private Date when;

    private Map<String, Object> what = null;

    private boolean forceReindex;
    private Long metaId;
    private boolean status;


    protected LogInputBean() {
    }

    public LogInputBean(String fortressUser, DateTime when,  Map<String, Object> what) throws DatagioException {
        this(fortressUser, null, when, what);
    }

    public LogInputBean(String fortressUser, String metaKey, DateTime when, Map<String, Object> what) throws DatagioException {
        this(fortressUser, metaKey, when, what, false);
    }

    /**
     * @param fortressUser -user name recognisable in the fortress
     * @param metaKey     -guid
     * @param when         -fortress view of DateTime
     * @param what         -escaped JSON
     */
    public LogInputBean(String fortressUser, String metaKey, DateTime when, Map<String, Object> what, Boolean isTransactional) throws DatagioException {
        this();
        this.metaKey = metaKey;
        this.fortressUser = fortressUser;
        if (when != null)
            this.when = when.toDate();
        setTransactional(isTransactional);
        setWhat(what);
    }

    /**
     * @param fortressUser -user name recognisable in the fortress
     * @param metaKey     -guid
     * @param when         -fortress view of DateTime
     * @param what         -escaped JSON
     * @param event        -how the caller would like to catalog this change (create, update etc)
     */
    public LogInputBean(String fortressUser, String metaKey, DateTime when, Map<String, Object> what, String event) throws DatagioException {
        this(fortressUser, metaKey, when, what);
        this.event = event;
    }

    public LogInputBean(String fortressUser, String metaKey, DateTime when, Map<String, Object> what, String event, String txName) throws DatagioException {
        this(fortressUser, metaKey, when, what, event);
        this.setTxRef(txName);
    }


    public String getMetaKey() {
        return metaKey;
    }

    public void setMetaKey(String metaKey) {
        this.metaKey = metaKey;
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

    public Date getWhen() {
        return when;
    }

    public void setWhen(Date when) {
        this.when = when;
    }

    public Map<String, Object> getWhat() {
        return what;
    }


    public void setWhat( Map<String, Object> what) throws DatagioException {
        this.what = what;

    }

    public String getComment() {
        return comment;
    }

    public String getCallerRef() {
        return callerRef;
    }

    /**
     * The caller ref must be unique for the Document Type in the Fortress
     * If you do not have a unique ref, then you must pass the AuditKey instead.
     *
     * @param callerRef fortress primary key
     */
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

    /**
     * This LogInput will be tracked against the supplied TxRef
     *
     * @param txRef TX Key Reference to use
     */
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
     * event sourcing type functionality. If true, AB will create a transaction identifier
     * that the caller can supply in subsequent updates
     *
     * @param isTransactional track
     */
    public void setTransactional(Boolean isTransactional) {
        this.isTransactional = isTransactional;
    }

    public Boolean isTransactional() {
        return isTransactional;
    }

    public void setStatus(LogStatus logStatus) {
        this.abStatus = logStatus;

    }

    public String getDocumentType() {
        return documentType;
    }

    public String getFortress() {
        return fortress;
    }

    public boolean isForceReindex() {
        return forceReindex;
    }

    public void setForceReindex(boolean forceReindex) {
        this.forceReindex = forceReindex;
    }

    @JsonIgnore
    public Long getMetaId() {
        return metaId;
    }

    public void setMetaId(Long metaId) {
        this.metaId = metaId;
    }

    private ChangeEvent changeEvent;

    public ChangeEvent getChangeEvent() {
        return changeEvent;
    }

    public void setChangeEvent(ChangeEvent changeEvent) {
        this.changeEvent = changeEvent;
    }

    public LogStatus getStatus() {
        return abStatus;
    }

    public enum LogStatus {
        IGNORE, OK, FORBIDDEN, NOT_FOUND, REINDEX, ILLEGAL_ARGUMENT, TRACK_ONLY
    }

    @Override
    public String toString() {
        return "LogInputBean{" +
                "event='" + event + '\'' +
                ", documentType='" + documentType + '\'' +
                ", callerRef='" + callerRef + '\'' +
                ", metaKey='" + metaKey + '\'' +
                '}';
    }
}
