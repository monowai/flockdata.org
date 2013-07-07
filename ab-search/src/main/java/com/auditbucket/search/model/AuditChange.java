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

package com.auditbucket.search.model;

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.registration.model.IFortress;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * User: mike
 * Date: 25/04/13
 * Time: 4:33 PM
 */
public class AuditChange implements IAuditChange {
    // ToDo: Figure out naming standard for system variables
    private String id;
    private String documentType;
    private Map<String, Object> what;
    private Date when;
    private String fortressName;
    private String companyName;
    private String name;
    private String who;
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
        setName(header.getAuditKey());
        this.auditKey = header.getAuditKey();
        this.documentType = header.getDocumentType();
        setFortress(header.getFortress());
        this.indexName = header.getIndexName();
        this.searchKey = header.getSearchKey();
        if (header.getTagValues() != null)
            for (ITagValue tagValue : header.getTagValues()) {
                tagValues.put(tagValue.getTag().getName(), tagValue.getTagValue());
            }
    }

    public AuditChange() {
    }

    public AuditChange(IAuditHeader header, String event, Map<String, Object> what) {
        this(header);
        this.name = event;
        this.what = what;
    }

    @Override
    @JsonIgnore
    public Map<String, Object> getWhat() {
        return what;
    }

    @Override
    public void setWhat(Map<String, Object> what) {
        this.what = what;
    }

    /**
     * @return Unique key in the index
     */
    @JsonIgnore
    public String getId() {
        return id;
    }

    private String searchKey;

    @JsonIgnore
    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public String getSearchKey() {
        return searchKey;
    }


    public void setId(String id) {
        this.id = id;
    }


    private void setFortress(IFortress fortress) {
        this.setFortressName(fortress.getName());
        this.setCompanyName(fortress.getCompany().getName());

    }

    public String getName() {
        return name;
    }

    @Override
    public String getWho() {
        return this.who;
    }

    public void setName(String who) {
        this.name = who;
    }

    public Date getWhen() {
        return when;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    public void setWhen(Date when) {
        this.when = when;
    }

    @Override
    public void setWho(String name) {
        this.who = name;
    }

    public String getFortressName() {
        return fortressName;
    }

    @JsonIgnore
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

    @JsonIgnore
    public Long getVersion() {
        return version;
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

    @JsonIgnore
    public String getRoutingKey() {
        return getAuditKey();
    }

    public Map<String, Object> getTagValues() {
        return tagValues;
    }
}
