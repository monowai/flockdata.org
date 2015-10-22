/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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
import org.flockdata.model.Entity;
import org.flockdata.model.EntityLog;
import org.flockdata.model.EntityTag;
import org.flockdata.model.Fortress;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.SearchChange;
import org.flockdata.track.service.EntityService;
import org.joda.time.DateTime;

import java.util.*;

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
    private Map<String, Object> props;
    private String attachment;
    private Date when;
    private String fortressName;
    private String companyName;
    private String who;
    private String event;
    private String metaKey;
    private String code;
    private Long logId;
    private HashMap<String, Map<String,ArrayList<SearchTag>>> tagValues = new HashMap<>();
    private Long entityId;

    private String indexName;
    private Long sysWhen;
    private boolean replyRequired = true;
    private boolean forceReindex;
    private boolean delete;
    private Date createdDate; // Created in the fortress

    private Date updatedDate; // Last updated in the fortress;

    private String contentType;
    private String fileName;
    private EntityService.TAG_STRUCTURE tagStructure;
    private EntityKeyBean parent;
    private String segment;

    public EntitySearchChange() {
        this.sysWhen = System.currentTimeMillis();
    }

    /**
     * extracts relevant entity properties to index
     *
     * @param entity details
     */
    public EntitySearchChange(Entity entity) {
        this();
        this.metaKey = entity.getMetaKey();
        this.entityId = entity.getId();
        this.searchKey = entity.getSearchKey();
        setDocumentType(entity.getType().toLowerCase());
        setFortress(entity.getSegment().getFortress());
        if (!entity.getSegment().isDefault())
            setSegment(entity.getSegment().getCode());
        this.indexName = entity.getSegment().getFortress().getRootIndex();
        this.searchKey = entity.getSearchKey();
        this.code = entity.getCode();
        if (entity.getLastUser() != null)
            this.who = entity.getLastUser().getCode();
        else
            this.who = (entity.getCreatedBy() != null ? entity.getCreatedBy().getCode() : null);
        this.sysWhen = entity.getDateCreated();
        this.description = entity.getDescription();
        this.props = entity.getProperties(); // Userdefined entity properties
        this.createdDate = entity.getFortressCreatedTz().toDate(); // UTC When created in the Fortress
        if (entity.getFortressUpdatedTz() != null)
            this.updatedDate = entity.getFortressUpdatedTz().toDate();
        this.event = entity.getEvent();
        //setWhen(new DateTime(entity.getFortressDateUpdated()));
    }

    public EntitySearchChange(Entity entity, ContentInputBean content) {
        this(entity);
        if ( content != null ) {
            //ToDo: this attachment might be compressed
            this.attachment = content.getAttachment();
            this.what = content.getWhat();
        }

    }

    public EntitySearchChange(Entity entity, EntityLog entityLog, ContentInputBean content) {
        this(entity, content);
        if ( entityLog !=null ) {
            this.event= entityLog.getLog().getEvent().getCode();
            this.fileName = entityLog.getLog().getFileName();
            this.contentType = entityLog.getLog().getContentType();
            if ( entityLog.getFortressWhen()!=null)
                this.updatedDate = new Date(entityLog.getFortressWhen());
            this.createdDate = entity.getFortressCreatedTz().toDate();
        } else {
            event = entity.getEvent();
            this.createdDate = entity.getFortressCreatedTz().toDate();
            if (entity.getFortressUpdatedTz()!=null)
                this.updatedDate = entity.getFortressUpdatedTz().toDate();
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

    private void setFortress(Fortress fortress) {
        this.setFortressName(fortress.getName());
        this.setCompanyName(fortress.getCompany().getName());

    }

    @Override
    public String getWho() {
        return this.who;
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
        if ( indexName== null )
            return indexName;
        return indexName.toLowerCase();
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

    public HashMap<String, Map<String, ArrayList<SearchTag>>> getTagValues() {
        return tagValues;
    }

    @JsonIgnore
    public void setTags(EntityService.TAG_STRUCTURE tagStructure, Iterable<EntityTag> entityTags) {
        this.tagStructure = tagStructure;
        tagValues = new HashMap<>();
        for (EntityTag entityTag : entityTags) {
            Map<String, ArrayList<SearchTag>> tagValues = this.tagValues.get(entityTag.getRelationship().toLowerCase());
            if (tagValues == null) {
                tagValues = new HashMap<>();
                // ToDo: Figure out if we need the Tags label as a property
                // tag.relationship.label.code
                // -or-
                // tag.label.relationship.code
                // If label and relationship are equal then only one property is written
                this.tagValues.put(entityTag.getRelationship().toLowerCase(), tagValues);
            }
            mapTag(entityTag, tagValues);
        }
    }

    private void mapTag(EntityTag entityTag, Map<String, ArrayList<SearchTag>> masterValues) {

        if (entityTag != null) {
            ArrayList<SearchTag> object = masterValues.get(entityTag.getTag().getLabel().toLowerCase());
            ArrayList<SearchTag> values;
            if (object == null) {
                values = new ArrayList<>();
            } else
                values = object;

            values.add(new SearchTag(entityTag));
            // ToDo: Convert to a "search tag"
            masterValues.put(entityTag.getTag().getLabel().toLowerCase(), values);
        }
    }

    private String parseTagType(EntityTag tag) {
        String code = tag.getTag().getCode();
        String type = tag.getRelationship();
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

    public String getCode() {
        return code;
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
    public Date getUpdatedDate() {
        return updatedDate;
    }

    @Override
    public String toString() {
        return "EntitySearchChange{" +
                "indexName='" + indexName + '\'' +
                ", code='" + code + '\'' +
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

    @Override
    public Map<String, Object> getProps() {
        return props;
    }

    @Override
    public EntityService.TAG_STRUCTURE getTagStructure() {
        return tagStructure;
    }

    @Override
    public void setTags(ArrayList<EntityTag> tags) {
        setTags(EntityService.TAG_STRUCTURE.DEFAULT, tags);
    }

    @Override
    public void setParent(EntityKeyBean parent) {
        this.parent = parent;
    }

    public EntityKeyBean getParent() {
        return parent;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntitySearchChange)) return false;

        EntitySearchChange that = (EntitySearchChange) o;

        if (documentType != null ? !documentType.equals(that.documentType) : that.documentType != null) return false;
        if (fortressName != null ? !fortressName.equals(that.fortressName) : that.fortressName != null) return false;
        if (companyName != null ? !companyName.equals(that.companyName) : that.companyName != null) return false;
        if (metaKey != null ? !metaKey.equals(that.metaKey) : that.metaKey != null) return false;
        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (logId != null ? !logId.equals(that.logId) : that.logId != null) return false;
        if (entityId != null ? !entityId.equals(that.entityId) : that.entityId != null) return false;
        if (indexName != null ? !indexName.equals(that.indexName) : that.indexName != null) return false;
        return !(searchKey != null ? !searchKey.equals(that.searchKey) : that.searchKey != null);

    }

    @Override
    public int hashCode() {
        int result = documentType != null ? documentType.hashCode() : 0;
        result = 31 * result + (fortressName != null ? fortressName.hashCode() : 0);
        result = 31 * result + (companyName != null ? companyName.hashCode() : 0);
        result = 31 * result + (metaKey != null ? metaKey.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (logId != null ? logId.hashCode() : 0);
        result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
        result = 31 * result + (indexName != null ? indexName.hashCode() : 0);
        result = 31 * result + (searchKey != null ? searchKey.hashCode() : 0);
        return result;
    }

    public void setTags(Collection<EntityTag> tags) {
        setTags(EntityService.TAG_STRUCTURE.DEFAULT, tags);
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getSegment() {
        return segment;
    }
}
