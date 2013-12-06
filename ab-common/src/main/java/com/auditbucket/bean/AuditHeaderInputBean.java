/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

package com.auditbucket.bean;

import com.auditbucket.audit.model.AuditEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

import java.util.*;

/**
 * User: Mike Holdsworth
 * Date: 11/05/13
 * Time: 9:19 AM
 */
public class AuditHeaderInputBean {
    private String auditKey;
    private String callerRef;
    private String fortress;
    private String fortressUser;
    private String documentType;
    private Date when = null;
    private String lastMessage;
    private AuditLogInputBean auditLog;
    private Map<String, Object> tagValues = new HashMap<>();
    private List<TagInputBean> associatedTags = new ArrayList<>();
    private boolean searchSuppressed;
    private String event;
    private AuditEvent eventObject;
    private String apiKey;
    private String description;
    private boolean trackSuppressed = false;


    public AuditHeaderInputBean() {
    }

    public AuditHeaderInputBean(String fortress, String fortressUser, String documentType, DateTime fortressWhen, String callerRef) {
        if (fortressWhen != null)
            this.when = fortressWhen.toDate();
        this.fortress = fortress;
        this.fortressUser = fortressUser;
        this.documentType = documentType;
        this.callerRef = callerRef;
    }

    public AuditHeaderInputBean(String description, String s, String companyNode, DateTime fortressWhen) {
        this(description, s, companyNode, fortressWhen, null);

    }

    public void setAuditKey(String auditKey) {
        this.auditKey = auditKey;
    }

    public String getAuditKey() {
        return this.auditKey;
    }

    /**
     * Fortress Timezone when
     * Defers to the AuditLogInput if present with a valid date
     *
     * @return when in the fortress this was created
     */
    public Date getWhen() {
        if (auditLog != null && auditLog.getWhen() != null && auditLog.getWhen().getTime() > 0)
            return auditLog.getWhen();
        return when;
    }

    /**
     * If there is an auditLog with a valid Date, this variable will not set
     *
     * @param when
     */
    public void setWhen(Date when) {
        if (!(auditLog != null && auditLog.getWhen() != null && auditLog.getWhen().getTime() > 0))
            this.when = when;
        // We ignore the incoming date if a valid one is set in a present auditLog
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
        if (auditLog != null) {
            this.when = auditLog.getWhen();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public AuditLogInputBean getAuditLog() {
        return auditLog;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getTagValues() {
        return tagValues;
    }

    /**
     * @param tagValues <relationship, object>
     */
    public void setTagValues(Map<String, Object> tagValues) {
        this.tagValues = tagValues;
    }

    public void setSearchSuppressed(boolean searchSuppressed) {
        this.searchSuppressed = searchSuppressed;
    }

    public boolean isSearchSuppressed() {
        return searchSuppressed;
    }

    public String getEvent() {
        return event;
    }

    /**
     * only used if the header is a one off immutable event
     *
     * @param event user definable event for an immutable header
     */
    public void setEvent(String event) {
        this.event = event;
    }

    @JsonIgnore
    public AuditEvent getEventObject() {
        return eventObject;
    }

    public void setEventObject(AuditEvent eventObject) {
        this.eventObject = eventObject;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setAssociatedTag(TagInputBean tag) {
        associatedTags.add(tag);
    }

    public List<TagInputBean> getAssociatedTags() {
        return associatedTags;
    }

    public void addTagValue(String relationship, TagInputBean tag) {
        getTagValues().put(relationship, tag);

    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isTrackSuppressed() {
        return trackSuppressed;
    }

    public void setTrackSuppressed(boolean trackSuppressed) {
        this.trackSuppressed = trackSuppressed;
    }
}
