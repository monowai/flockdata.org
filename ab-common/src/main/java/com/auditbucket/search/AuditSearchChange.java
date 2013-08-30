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

package com.auditbucket.search;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.TagValue;
import com.auditbucket.registration.model.Fortress;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the parameters necessary to make an audit event searchable
 * <p/>
 * User: Mike Holdsworth
 * Date: 25/04/13
 * Time: 9:33 PM
 */
public class AuditSearchChange implements com.auditbucket.audit.model.SearchChange {
    // ToDo: Figure out naming standard for system variables
    private String id;
    private String documentType;
    private Map<String, Object> what;
    private Long when;
    private String fortressName;
    private String companyName;
    private String who;
    private String event;
    private String auditKey;
    private String callerRef;
    private Long logId;
    // String, Object?
    private Map<String, Object> tagValues = new HashMap<>();
    private Long version;

    private String indexName;
    private long sysWhen;

    /**
     * extracts relevant header records to be used in indexing
     *
     * @param header auditHeader details (owner of this change)
     */
    public AuditSearchChange(AuditHeader header) {
        this();
        this.auditKey = header.getAuditKey();
        setDocumentType(header.getDocumentType());
        setFortress(header.getFortress());
        this.indexName = header.getIndexName();
        this.searchKey = header.getSearchKey();
        this.callerRef = header.getCallerRef();
        this.who = header.getLastUser().getName();
        setTags(header.getTagValues());
    }

    public AuditSearchChange() {
    }

    public AuditSearchChange(AuditHeader header, Map<String, Object> mapWhat, String event, DateTime when) {
        this(header);
        this.what = mapWhat;
        this.event = event;

        setWhen(when);
    }

    @Override
    public Map<String, Object> getWhat() {
        return what;
    }

    @Override
    public void setWhat(Map<String, Object> what) {
        this.what = what;
    }

    private String searchKey;

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public String getSearchKey() {
        return searchKey;
    }

    private void setFortress(Fortress fortress) {
        this.setFortressName(fortress.getName());
        this.setCompanyName(fortress.getCompany().getName());

    }

    @Override
    public String getWho() {
        return this.who;
    }

    public Long getWhen() {
        return when;
    }

    public String getEvent() {
        return event;
    }

    public void setWhen(DateTime when) {
        if (when != null)
            this.when = when.getMillis();
    }

    @Override
    public void setWho(String name) {
        this.who = name;
    }

    public String getFortressName() {
        return fortressName;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getIndexName() {
        return indexName;
    }

    public void setFortressName(String fortressName) {
        this.fortressName = fortressName;
    }

    @JsonIgnore
    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDocumentType() {
        return documentType;
    }

    protected void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getAuditKey() {
        return auditKey;
    }

    public Map<String, Object> getTagValues() {
        return tagValues;
    }

    private void setTags(Set<TagValue> tagSet) {
        tagValues = new HashMap<>();
        for (TagValue tag : tagSet) {
            tagValues.put(tag.getTagType(), tag.getTag().getName());
        }
    }

    public String getCallerRef() {
        return callerRef;
    }

    public void setSysWhen(Long sysWhen) {
        this.sysWhen = sysWhen;
    }

    @Override
    public void setLogId(Long id) {
        this.logId = id;

    }

    @Override
    public Long getLogId() {
        return logId;
    }

    public Long getSysWhen() {
        return sysWhen;
    }

    @Override
    public String toString() {
        return "AuditSearchChange{" +
                "fortressName='" + fortressName + '\'' +
                ", documentType='" + documentType + '\'' +
                ", auditKey='" + auditKey + '\'' +
                '}';
    }
}
