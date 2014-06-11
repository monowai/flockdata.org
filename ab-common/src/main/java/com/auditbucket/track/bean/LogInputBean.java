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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;

import java.io.IOException;
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
    private String metaKey;
    private String txRef;
    private String comment;
    private String fortressUser;
    private String event;
    private Date when;
    private String what;

    // Required to support location by Caller Ref
    private String documentType;
    private String callerRef;
    private String fortress;

    private Map<String, Object> mapWhat = null;

    private static final ObjectMapper om = new ObjectMapper();
    private boolean forceReindex;
    private Long metaId;
    private boolean status;

    protected LogInputBean() {
    }

    public LogInputBean(String metaKey, String fortressUser, DateTime when, String what) throws DatagioException {
        this(metaKey, fortressUser, when, what, false);
    }

    /**
     * @param metaKey     -guid
     * @param fortressUser -user name recognisable in the fortress
     * @param when         -fortress view of DateTime
     * @param what         -escaped JSON
     */
    public LogInputBean(String metaKey, String fortressUser, DateTime when, String what, Boolean isTransactional) throws DatagioException {
        this();
        this.metaKey = metaKey;
        this.fortressUser = fortressUser;
        if (when != null)
            this.when = when.toDate();
        setTransactional(isTransactional);
        setWhat(what);
    }

    /**
     * @param metaKey     -guid
     * @param fortressUser -user name recognisable in the fortress
     * @param when         -fortress view of DateTime
     * @param what         -escaped JSON
     * @param event        -how the caller would like to catalog this change (create, update etc)
     */
    public LogInputBean(String metaKey, String fortressUser, DateTime when, String what, String event) throws DatagioException {
        this(metaKey, fortressUser, when, what);
        this.event = event;
    }

    public LogInputBean(String metaKey, String fortressUser, DateTime when, String what, String event, String txName) throws DatagioException {
        this(metaKey, fortressUser, when, what, event);
        this.setTxRef(txName);
    }

    public LogInputBean(String fortressUser, DateTime when, String what) throws DatagioException {
        this(null, fortressUser, when, what);

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

    @JsonIgnore
    public Map<String, Object> getMapWhat() {
        return mapWhat;
    }

    public String getWhat() {
        return what;
    }

    // Will never update the map once set
    public void setWhat(String jsonWhat) throws DatagioException {
        if (jsonWhat == null || !(mapWhat == null))
            return;
        try {
            what = om.readTree(jsonWhat).toString();
            mapWhat = om.readValue(what, Map.class);
        } catch (IOException e) {

            throw new DatagioException("Error processing JSON What text", e);
        }

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
