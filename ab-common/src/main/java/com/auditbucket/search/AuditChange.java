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

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.registration.model.IFortress;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the parameters necessary to index an audit change
 * <p/>
 * User: Mike Holdsworth
 * Date: 25/04/13
 * Time: 4:33 PM
 */
public class AuditChange implements IAuditChange {
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
    private Map<String, Object> tagValues = new HashMap<String, Object>();
    private Long version;

    private String indexName;

    /**
     * extracts relevant header records to be used in indexing
     *
     * @param header auditHeader details (owner of this change)
     */
    public AuditChange(IAuditHeader header) {
        this();
        this.auditKey = header.getAuditKey();
        setDocumentType(header.getDocumentType());
        setFortress(header.getFortress());
        this.indexName = header.getIndexName();
        this.searchKey = header.getSearchKey();
        this.who = header.getLastUser().getName();
        Set<ITagValue> tags = header.getTagValues();
        if (tags != null)
            for (ITagValue tagValue : tags) {
                this.tagValues.put(tagValue.getTag().getName(), tagValue.getTagValue());
            }
    }

    public AuditChange() {
    }

    public AuditChange(IAuditHeader header, Map<String, Object> mapWhat, String event, DateTime when) {
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

    private void setFortress(IFortress fortress) {
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

    public void setTagValues(Map<String, Object> tagValues) {
        this.tagValues = tagValues;
    }

    public Map<String, Object> getTagValues() {
        return tagValues;
    }
}
