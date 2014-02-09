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

package com.auditbucket.audit.bean;

import com.auditbucket.registration.bean.TagInputBean;
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
    private AuditLogInputBean auditLog;
    private Map<String, Object> tagValues = new HashMap<>();
    private List<TagInputBean> associatedTags = new ArrayList<>();
    private String event;
    private String apiKey;
    private String description;
    private boolean searchSuppressed;
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
     * This date is ignored if a valid one is set in a present auditLog
     *
     * @param when when the caller says this occurred
     */
    public void setWhen(Date when) {
        if (!(auditLog != null && auditLog.getWhen() != null && auditLog.getWhen().getTime() > 0))
            this.when = when;
        //
    }

    public String getFortress() {
        return fortress;
    }

    /**
     * Fortress is a computer application/service in the callers environment, i.e. Payroll, HR, AR.
     * This could also be thought of as a Database in an DBMS
     *
     * The Fortress name is unique for the Company.
     *
     * @param fortress unique fortress name
     */
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

    /**
     * Fortress unique type of document that categorizes this type of change.
     *
     * @param documentType name of the document
     */
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }


    public String getCallerRef() {
        return callerRef;
    }

    /**
     * Must be unique for the Fortress and Document Type. It is also optional. If you do not have
     * a primary key, then to update "this" instance of the AuditHeader you will need to use
     * the generated AuditKey returned by AuditBucket in the AuditResultBean
     *
     * @see AuditResultBean
     *
     * @param callerRef primary key in the calling system datastore
     */
    public void setCallerRef(String callerRef) {
        this.callerRef = callerRef;
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
     * Tag values to associate with the header
     *
     * @param tagValues <relationship, object>
     * @see AuditHeaderInputBean#getAssociatedTags()
     */
    public void setTagValues(Map<String, Object> tagValues) {
        this.tagValues = tagValues;
    }

    public String getEvent() {
        return event;
    }

    /**
     * only used if the header is a one off immutable event
     * is supplied, then the event is logged against the header. Typically events are logged
     * against AuditLogs
     *
     * @param event user definable event for an immutable header
     */
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * @return secret know only by the caller and AuditBucket
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Reserved for future use. Looking to set the Company API key as a secret
     * @param apiKey company secret
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Single tag
     *
     * @param tag tag to add
     * @see AuditHeaderInputBean#getAssociatedTags()
     */
    public void setAssociatedTag(TagInputBean tag) {
        associatedTags.add(tag);
    }

    /**
     * Tag structure to create. This is a short hand way of ensuring an
     * associative structure will exist. Perhaps you can only identify this while processing
     * a large file set.
     * <p/>
     * This will not associate the header with the tag structure. To do that
     *
     * @return Tag values to created
     * @see AuditHeaderInputBean#setTagValues(java.util.Map)
     */
    public List<TagInputBean> getAssociatedTags() {
        return associatedTags;
    }

    public void addTagValue(String relationship, TagInputBean tag) {
        getTagValues().put(relationship, tag);

    }

    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description User definable note describing the header
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return do not index in the search service
     */
    public boolean isSearchSuppressed() {
        return searchSuppressed;
    }

    /**
     * Graph the change only. Do not write to the search service
     *
     * @param searchSuppressed true/false
     */
    public void setSearchSuppressed(boolean searchSuppressed) {
        this.searchSuppressed = searchSuppressed;
    }

    /**
     * do not index in the graph - search only
     * @return graphable?
     */
    public boolean isTrackSuppressed() {
        return trackSuppressed;
    }

    /**
     * Write the change as a search event only. Do not write to the graph service
     *
     * @param trackSuppressed true/false
     */
    public void setTrackSuppressed(boolean trackSuppressed) {
        this.trackSuppressed = trackSuppressed;
    }
}
