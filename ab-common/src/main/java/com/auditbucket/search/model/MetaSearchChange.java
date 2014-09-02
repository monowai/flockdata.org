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

package com.auditbucket.search.model;

import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.SearchChange;
import com.auditbucket.track.model.TrackTag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the information to make an track header and log a searchable document
 * according to the way AuditBucket deals with search docs.
 * <p/>
 * This object becomes the payload dispatch to ab-search for indexing.
 * <p/>
 * User: Mike Holdsworth
 * Date: 25/04/13
 * Time: 9:33 PM
 */
public class MetaSearchChange implements SearchChange {

    private String documentType;
    private String description;
    private String name;
    private Map<String, Object> what;
    private Date when;
    private String fortressName;
    private String companyName;
    private String who;
    private String event;
    private String metaKey;
    private String callerRef;
    private Long logId;
    // String, Object?
    private HashMap<String, Object> tagValues = new HashMap<>();
    private Long version;
    private Long metaId;

    private String indexName;
    private long sysWhen;
    private boolean replyRequired = true;
    private boolean forceReindex;
    private boolean delete;
    private Date createdDate; // Created in the fortress

    /**
     * extracts relevant header records to be used in indexing
     *
     * @param header details
     */
    public MetaSearchChange(MetaHeader header) {
        this();
        this.metaKey = header.getMetaKey();
        this.metaId = header.getId();
        setDocumentType(header.getDocumentType());
        setFortress(header.getFortress());
        this.indexName = header.getIndexName();
        this.searchKey = header.getSearchKey();
        this.callerRef = header.getCallerRef();
        if (header.getLastUser() != null)
            this.who = header.getLastUser().getCode();
        this.description = header.getDescription();
        this.createdDate = header.getFortressDateCreated().toDate(); // UTC When created in AuditBucket

    }

    public MetaSearchChange() {
    }

    public MetaSearchChange(MetaHeader header, HashMap<String, Object> mapWhat, String event, DateTime when) {
        this(header);
        this.what = mapWhat;
        this.event = event;
        setWhen(when);
    }

    public MetaSearchChange(MetaHeader header, Map<String, Object> json) {
        this(header);
        this.what = json;
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

    public Date getWhen() {
        return when;
    }

    public String getEvent() {
        return event;
    }

    public void setWhen(DateTime when) {
        if ((when != null) && (when.getMillis() != 0))
            this.when = when.toDate();
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

    void setFortressName(String fortressName) {
        this.fortressName = fortressName;
    }

    @JsonIgnore
    public String getCompanyName() {
        return companyName;
    }

    void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDocumentType() {
        return documentType;
    }

    void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getMetaKey() {
        return metaKey;
    }

    public Map<String, Object> getTagValues() {
        return tagValues;
    }

    public void setTags(Iterable<TrackTag> tagSet) {
        tagValues = new HashMap<>();
        for (TrackTag tag : tagSet) {
            HashMap<String, Object> tagValues = (HashMap<String, Object>) this.tagValues.get(tag.getTagType());
            if (tagValues == null) {
                tagValues = new HashMap<>();
                this.tagValues.put(tag.getTagType().toLowerCase(), tagValues);
            }

            setTagValue("name", tag.getTag().getName(), tagValues);
            setTagValue("code", tag.getTag().getCode().toLowerCase(), tagValues);

            if (tag.getGeoData() != null) {
                setTagValue("iso", tag.getGeoData().getIsoCode(), tagValues);
                setTagValue("country", tag.getGeoData().getCountry(), tagValues);
                setTagValue("state", tag.getGeoData().getState(), tagValues);
                setTagValue("city", tag.getGeoData().getCity(), tagValues);
            }
            if (!tag.getTagProperties().isEmpty())
                tagValues.put("props", tag.getTagProperties());
        }
    }

    private void setTagValue(String key, Object value, HashMap<String, Object> masterValues) {
        if (value != null) {
            Object object = masterValues.get(key);
            ArrayList values;
            if (object == null) {
                values = new ArrayList();
            } else
                values = (ArrayList) object;

            values.add(value);
            masterValues.put(key, values);
        }
    }

    public String getCallerRef() {
        return callerRef;
    }

    /**
     * When this log file was created in AuditBucket graph
     */
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

    public Long getMetaId() {
        return metaId;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public Long getSysWhen() {
        return sysWhen;
    }

    @Override
    public Date getCreatedDate() {
        return createdDate;
    }

    @Override
    public String toString() {
        return "MetaSearchChange{" +
                "fortressName='" + fortressName + '\'' +
                ", documentType='" + documentType + '\'' +
                ", callerRef='" + callerRef + '\'' +
                ", metaKey='" + metaKey + '\'' +
                '}';
    }

    /**
     * @param replyRequired do we require the search service to acknowledge this request
     */
    public void setReplyRequired(boolean replyRequired) {
        this.replyRequired = replyRequired;
    }

    public boolean isReplyRequired() {
        return replyRequired;
    }

    public void setForceReindex(boolean forceReindex) {
        this.forceReindex = forceReindex;
    }

    public boolean isForceReindex() {
        return forceReindex;
    }

    /**
     * Flags to ab-search to delete the SearchDocument
     *
     * @param delete shall I?
     */
    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public boolean isDelete() {
        return delete;
    }

}
