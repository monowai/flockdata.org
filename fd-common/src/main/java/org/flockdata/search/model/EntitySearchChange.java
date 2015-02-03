/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.registration.bean.FortressResultBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.model.*;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the information to make an Entity and it's log in to
 * a searchable document
 * <p/>
 * This object becomes the payload dispatch to fd-search for indexing.
 * <p/>
 * User: Mike Holdsworth
 * Date: 25/04/13
 * Time: 9:33 PM
 */
public class EntitySearchChange implements SearchChange {

    private String documentType;
    private String description;
    private String name;
    private Map<String, Object> what;
    private String attachment;
    private Date when;
    private String fortressName;
    private String companyName;
    private String who;
    private String event;
    private String metaKey;
    private String callerRef;
    private Long logId;
    private HashMap<String, Map<String,Object>> tagValues = new HashMap<>();
    private Long entityId;

    private String indexName;
    private Long sysWhen;
    private boolean replyRequired = true;
    private boolean forceReindex;
    private boolean delete;
    private Date createdDate; // Created in the fortress

    private String contentType;
    private String fileName;

    public EntitySearchChange() {
        this.sysWhen = System.currentTimeMillis();
    }
    /**
     *
     * @param entity  server side entity
     * @param content content the user wants stored
     * @deprecated use the EntityBean version of this
     */
    public EntitySearchChange(Entity entity, ContentInputBean content) {
        this ( new EntityBean(entity), content);
    }
    /**
     *
     * @param entity  server side entity
     * @deprecated use the EntityBean version of this
     */
    public EntitySearchChange(Entity entity) {
        this(new EntityBean(entity));
    }


    /**
     * extracts relevant entity records to be used in indexing
     *
     * @param entity details
     */
    public EntitySearchChange(EntityBean entity) {
        this();
        this.metaKey = entity.getMetaKey();
        this.entityId = entity.getId();
        setDocumentType(entity.getDocumentType());
        setFortress(entity.getFortress());
        this.indexName = entity.getIndexName();
        this.searchKey = entity.getSearchKey();
        this.callerRef = entity.getCallerRef();
        if (entity.getLastUser() != null)
            this.who = entity.getLastUser();
        else
            this.who = entity.getCreatedUser();
        this.sysWhen = entity.getWhenCreated();
        this.description = entity.getDescription();
        this.createdDate = entity.getFortressDateCreated().toDate(); // UTC When created in FlockData
        this.event= entity.getEvent();
        setWhen(new DateTime(entity.getWhenCreated()));
    }

    public EntitySearchChange(EntityBean entity, EntityContent content) {
        this(entity);
        if ( content != null ) {
            //ToDo: this attachment might be compressed
            this.attachment = content.getAttachment();
            this.what = content.getWhat();
        }

    }

    public EntitySearchChange(EntityBean entity, EntityContent content, Log log) {
        this(entity, content);
        if ( log !=null ) {
            this.event= log.getEvent().getCode();
            this.fileName = log.getFileName();
            this.contentType = log.getContentType();
            setWhen(new DateTime(log.getEntityLog().getFortressWhen()));
        } else {
            event = entity.getEvent();
            setWhen(entity.getFortressDateCreated());
        }
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

    private void setFortress(FortressResultBean fortress) {
        this.setFortressName(fortress.getName());
        this.setCompanyName(fortress.getCompanyName());

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

    public HashMap<String, Map<String, Object>> getTagValues() {
        return tagValues;
    }

    public void setTags(Iterable<EntityTag> tagSet) {
        tagValues = new HashMap<>();
        for (EntityTag tag : tagSet) {
            Map<String, Object> tagValues = this.tagValues.get(tag.getTagType().toLowerCase());
            if (tagValues == null) {
                tagValues = new HashMap<>();
                // ToDo: Figure out if we need the Tags label as a property
                // tag.relationship.label.code - too long?
                // -or-
                // tag.label.relationship.code
                // If label and relationship are equal then only one property is written
                this.tagValues.put(tag.getTagType().toLowerCase(), tagValues);
            }

            setTagValue("code", tag.getTag().getCode(), tagValues);
            if ( !tag.getTag().getCode().equals(tag.getTag().getName()))
                setTagValue("name", tag.getTag().getName(), tagValues);

            if (tag.getGeoData() != null) {
                setTagValue("iso", tag.getGeoData().getIsoCode().toUpperCase(), tagValues);
                setTagValue("country", tag.getGeoData().getCountry(), tagValues);
                setTagValue("state", tag.getGeoData().getState(), tagValues);
                setTagValue("city", tag.getGeoData().getCity(), tagValues);
                if ( tag.getGeoData().isValid())
                    setTagValue("geo", tag.getGeoData().getGeoPoint(),tagValues);
            }
            if (!tag.getTagProperties().isEmpty())
                tagValues.put("props", tag.getTagProperties());
        }
    }

    private String parseTagType(EntityTag tag) {
        String code = tag.getTag().getCode();
        String type = tag.getTagType();
        if ( code.equals(type))
            return code;

        return null;
    }

    private void setTagValue(String key, Object value, Map<String, Object> masterValues) {
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
     * When this log file was created in FlockData graph
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

    public Long getEntityId() {
        return entityId;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    @Override
    public boolean hasAttachment(){
        return this.attachment!=null;
    }

    public String getAttachment () {
        return this.attachment;
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
        return "EntitySearchChange{" +
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
     * Flags to fd-search to delete the SearchDocument
     *
     * @param delete shall I?
     */
    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public Boolean isDelete() {
        return delete;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileName() {
        return fileName;
    }



}
