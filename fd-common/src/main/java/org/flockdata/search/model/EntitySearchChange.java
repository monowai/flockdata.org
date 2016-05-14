/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
    private String type;
    private String code;
    private String name;
    private String description;
    private Map<String, Object> data;
    private Map<String, Object> props;
    private String attachment;
    private Date when;
    private String fortressName;
    private String companyName;
    private String who;
    private String event;
    private String key;
    private Long logId;
    private HashMap<String, Map<String,ArrayList<SearchTag>>> tagValues = new HashMap<>();
    private Long id;

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
    private Collection<EntityKeyBean> entityLinks = new ArrayList<>();

    public EntitySearchChange() {
        this.sysWhen = System.currentTimeMillis();
    }

    /**
     * extracts relevant entity properties to index
     *
     * @param entity details
     */
    public EntitySearchChange(Entity entity, String indexName) {
        this();
        this.key = entity.getKey();
        this.id = entity.getId();
        this.searchKey = entity.getSearchKey();
        assert entity.getType() !=null;
        setDocumentType(entity.getType().toLowerCase());
        setFortress(entity.getSegment().getFortress());
        if (!entity.getSegment().isDefault())
            setSegment(entity.getSegment().getCode());
        this.indexName = indexName;

        this.searchKey = entity.getSearchKey();
        this.code = entity.getCode();
        if (entity.getLastUser() != null)
            this.who = entity.getLastUser().getCode();
        else
            this.who = (entity.getCreatedBy() != null ? entity.getCreatedBy().getCode() : null);
        this.sysWhen = entity.getDateCreated();
        this.props = entity.getProperties(); // Userdefined entity properties
        this.createdDate = entity.getFortressCreatedTz().toDate(); // UTC When created in the Fortress
        if (entity.getFortressUpdatedTz() != null)
            this.updatedDate = entity.getFortressUpdatedTz().toDate();
        this.event = entity.getEvent();
    }

    public EntitySearchChange(Entity entity, ContentInputBean content, String indexName) {
        this(entity,indexName);
        if ( content != null ) {
            //ToDo: this attachment might be compressed
            this.attachment = content.getAttachment();
            this.data = content.getData();
        }

    }

    public EntitySearchChange(Entity entity, EntityLog entityLog, ContentInputBean content, String indexName) {
        this(entity, content, indexName);
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

    public Map<String, Object> getData() {
        return data;
    }

    public EntitySearchChange setData(Map<String, Object> data) {
        this.data = data;
        return this;
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

    public void setWho(String name) {
        this.who = name;
    }

    public String getFortressName() {
        return fortressName;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getIndexName() {
        if ( parent!=null )
            return parent.getIndex();

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

    public String getKey() {
        return key;
    }

    public HashMap<String, Map<String, ArrayList<SearchTag>>> getTagValues() {
        return tagValues;
    }

    public EntitySearchChange setStructuredTags(Collection<EntityTag> tags) {
        setStructuredTags(EntityService.TAG_STRUCTURE.DEFAULT, tags);
        return this;
    }

    @JsonIgnore
    public EntitySearchChange setStructuredTags(EntityService.TAG_STRUCTURE tagStructure, Iterable<EntityTag> entityTags) {
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
        return this;
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

    public void setLogId(Long id) {
        this.logId = id;

    }

    @Override
    public Long getLogId() {
        return logId;
    }

    public Long getId() {
        return id;
    }

    @Override
    public EntitySearchChange setDescription(String description) {
        this.description = description;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

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

    public Date getCreatedDate() {
        return createdDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    @Override
    public String toString() {
        return "EntitySearchChange{" +
                "indexName='" + indexName + '\'' +
                ", code='" + code + '\'' +
                ", key='" + key + '\'' +
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

    public EntitySearchChange setParent(EntityKeyBean parent) {
        this.parent = parent;
        return this;
    }

    public EntityKeyBean getParent() {
        return parent;
    }

    @Override
    @JsonIgnore
    public boolean isType(Type type) {
        return getType().equals(type.name());
    }

    @Override
    public String getType() {
        return Type.ENTITY.name();
    }

    public EntitySearchChange setType(String type){
        this.type = type;
        return this;
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
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (logId != null ? !logId.equals(that.logId) : that.logId != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (indexName != null ? !indexName.equals(that.indexName) : that.indexName != null) return false;
        return !(searchKey != null ? !searchKey.equals(that.searchKey) : that.searchKey != null);

    }

    @Override
    public int hashCode() {
        int result = documentType != null ? documentType.hashCode() : 0;
        result = 31 * result + (fortressName != null ? fortressName.hashCode() : 0);
        result = 31 * result + (companyName != null ? companyName.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (logId != null ? logId.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (indexName != null ? indexName.hashCode() : 0);
        result = 31 * result + (searchKey != null ? searchKey.hashCode() : 0);
        return result;
    }

    public SearchChange setSegment(String segment) {
        this.segment = segment;
        return this;
    }

    public SearchChange addEntityLinks(Collection<EntityKeyBean> inboundEntities) {
        this.entityLinks = inboundEntities;
        return this;
    }

    public Collection<EntityKeyBean> getEntityLinks() {
        return this.entityLinks;
    }

    public String getName() {
        return name;
    }
}
