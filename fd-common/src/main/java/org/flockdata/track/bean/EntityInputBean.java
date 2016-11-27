/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.flockdata.model.FortressUser;
import org.flockdata.model.MetaDocument;
import org.flockdata.model.MetaFortress;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.transform.UserProperties;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.*;

/**
 * @author mholdsworth
 * @since 11/05/2013
 * @tag Payload, Entity
 */
public class EntityInputBean implements Serializable, UserProperties {
    Map<String, Object> properties = new HashMap<>();  // Set into the Entity
    private String key;
    private String code;
    private FortressInputBean fortress;
    private String fortressUser;
    @JsonDeserialize(as = DocumentTypeInputBean.class)
    private MetaDocument documentType;
    private Date when = null; // Created Date
    private Date lastChange = null;
    private ContentInputBean content;
    private transient List<TagInputBean> tags = new ArrayList<>();
    // String is the relationship name
    private transient Map<String, List<EntityKeyBean>> entityLinks = new HashMap<>();
    private String event = null;
    private String description;
    private String name;
    private boolean searchSuppressed;
    private boolean trackSuppressed = false;
    private boolean entityOnly = true;
    private String timezone;
    private boolean archiveTags = true;
    private String updateUser;
    private FortressUser user;
    private String segment;

    public EntityInputBean() {
        setEntityOnly(true);
    }

    public EntityInputBean(EntityKeyBean entityKeyBean) {
        setFortress(new FortressInputBean(entityKeyBean.getFortressName()));
        setDocumentType(new DocumentTypeInputBean(entityKeyBean.getDocumentType()));
        setCode(entityKeyBean.getCode());
    }

    /**
     * Constructor is for testing purposes
     *
     * @param fortress     Application/Division or System that owns this information
     * @param fortressUser who in the fortressName created it
     * @param documentName within the fortressName, this is a document of this unique type
     * @param fortressWhen when did this occur in the fortressName
     * @param code         case sensitive unique key. If not supplied, then the service will generate one
     */
    public EntityInputBean(MetaFortress fortress, String fortressUser, String documentName, DateTime fortressWhen, String code) {
        this();
        if (fortressWhen != null) {
            setWhen(fortressWhen.toDate());
        }
        setFortress(new FortressInputBean(fortress.getName()));
        setFortressUser(fortressUser);
        setDocumentType(new DocumentTypeInputBean(documentName));
        setCode(code);
    }

    public EntityInputBean(MetaFortress fortress, String fortressUser, String documentName, DateTime fortressWhen) {
        this(fortress, fortressUser, documentName, fortressWhen, null);
    }

    public EntityInputBean(MetaFortress fortress, MetaDocument documentType) {
        this.fortress = new FortressInputBean(fortress.getName());
        setDocumentType(documentType);
    }

    public EntityInputBean(MetaFortress fortress, MetaDocument docType, String entityCode) {
        this(fortress, docType);
        this.code = entityCode;

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getKey() {
        return this.key;
    }

    public EntityInputBean setKey(final String key) {
        this.key = key;
        return this;
    }

    /**
     * Fortress Timezone when
     * Defers to the ContentInput if it has a valid date
     *
     * @return when created in the owning fortress
     */
    public Date getWhen() {
        if (when != null)
            return when;
        // Default to the content date
        if (content != null && content.getWhen() != null && content.getWhen().getTime() > 0)
            return content.getWhen();
        return null;
    }

    /**
     * This date is ignored if a valid one is in the Content
     *
     * @param when when the caller says this occurred
     */
    public EntityInputBean setWhen(final Date when) {
        this.when = when;
        return this;
    }

    public FortressInputBean getFortress() {
        return fortress;
    }

    public EntityInputBean setFortress(FortressInputBean fortress) {
        if ( fortress!=null && fortress.getName().equals(""))
            throw new IllegalArgumentException("Fortress name cannot be blank or null");
        this.fortress = fortress;
        return this;
    }

    /**
     * @return name
     */
    public String getFortressUser() {
        return fortressUser;
    }

    public EntityInputBean setFortressUser(final String fortressUser) {
        this.fortressUser = fortressUser;
        return this;
    }

    /**
     * Fortress unique type of document that categorizes this type of change.
     *
     * @param documentName relationshipName of the document
     */
    public EntityInputBean setDocumentName(final String documentName) {
        this.documentType = new DocumentTypeInputBean(documentName);
        return this;
    }

    public EntityInputBean setFortressName(String fortressName) {
        this.fortress = new FortressInputBean(fortressName);
        return this;
    }

    public String getCode() {
        return code;
    }

    /**
     * Optional case sensitive & unique for the Fortress & Document Type combination. If you do not have
     * a primary key, then to update "this" instance of the Entity you will need to use
     * the generated AuditKey returned by FlockData in the TrackResultBean
     *
     * @param code case sensitive primary key generated by the calling fortressName
     * @see TrackResultBean
     */
    public EntityInputBean setCode(String code) {
        this.code = code;
        return this;
    }

    @Deprecated
    public void setLog(ContentInputBean content) {
        setContent(content);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ContentInputBean getContent() {
        return content;
    }

    public EntityInputBean setContent(ContentInputBean content) {
        this.content = content;
        if (content != null) {
            this.entityOnly = false;
            //this.when = content.getWhen();
        }
        return this;
    }

    @Override
    public Object getProperty(String key) {
        if (properties == null)
            return null;
        return properties.get(key);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getProperties() {
        return properties;
    }

    public EntityInputBean setProperties(final Map<String, Object> properties) {
        this.properties = properties;
        return this;
    }

    public EntityInputBean setProperty(String key, Object value) {
        if ( value == null || key == null ) // DAT-568
            return this; // We don't accept NULL values for a map

        if (properties == null)
            properties = new HashMap<>();
        properties.put(key, value);
        return this;
    }

    public String getEvent() {
        return event;
    }

    /**
     * only used if the entity is a one off immutable event
     * is supplied, then the event is logged against the entity. Typically events are logged
     * against AuditLogs
     *
     * @param event user definable event for an immutable entity
     */
    public EntityInputBean setEvent(final String event) {
        this.event = event;
        return this;
    }

    /**
     * Single tag
     *
     * @param tag tag to add
     * @see EntityInputBean#getTags()
     */
    public EntityInputBean addTag(TagInputBean tag) {
        tags.add(tag);
        return this;
    }

    /**
     * Tag structure to create. This is a short hand way of ensuring an
     * associative structure will exist. Perhaps you can only identify this while processing
     * a large file set.
     * <p/>
     * This will not associate the entity with the tag structure. To do that
     *
     * @return Tag values to created
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<TagInputBean> getTags() {
        return tags;
    }

    public EntityInputBean setTags(Collection<TagInputBean> tags) {
        for (TagInputBean next : tags) {
            this.tags.add(next);

        }
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDescription() {
        return description;
    }

    /**
     * @param description User definable note describing the entity
     */
    public EntityInputBean setDescription(String description) {
        this.description = description;
        return this;
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
    public EntityInputBean setSearchSuppressed(final boolean searchSuppressed) {
        this.searchSuppressed = searchSuppressed;
        return this;
    }

    /**
     * do not index in the graph - search only
     *
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
    public EntityInputBean setTrackSuppressed(final boolean trackSuppressed) {
        this.trackSuppressed = trackSuppressed;
        return this;
    }

    public EntityInputBean addEntityLink(String relationshipName, EntityKeyBean entityKey) {
        if ( entityKey.getRelationshipName()== null)
            entityKey.setRelationshipName(relationshipName);
        List<EntityKeyBean> refs = entityLinks.get(relationshipName);
        if (refs == null) {
            refs = new ArrayList<>();
            entityLinks.put(relationshipName, refs);
        }
        refs.add(entityKey);
        return this;
    }

    /**
     * Format is "referenceName", Collection<code>
     * All callerRefs are assumed to belong to this same fortressName
     * "This" code is assume to be the starting point for the EntityLinks to link to
     *
     * @return entityLinks
     */
    public Map<String, List<EntityKeyBean>> getEntityLinks() {
        return entityLinks;
    }

    public EntityInputBean setEntityLinks(final Map<String, List<EntityKeyBean>> entityLinks) {
        this.entityLinks = entityLinks;
        return this;
    }

    @Override
    public String toString() {
        return "EntityInputBean{" +
                "for='" + getFortress() + '\'' +
                ", doc='" + getDocumentType() + '\'' +
                ", seg='" + getSegment() + '\'' +
                ", mek='" + getKey() + '\'' +
                ", cod='" + getCode() + '\'' +
                ", nam='" + getName() + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public EntityInputBean setName(String name) {
        this.name = (name != null ? name.trim() : null);
        return this;
    }

    public boolean isEntityOnly() {
        return entityOnly;
    }

    /**
     * Flags that this Entity will never have a content. It will still be tracked through
     * in to the Search Service.
     *
     * @param entityOnly if false then the entity will not be indexed in search until a content is added
     */
    public EntityInputBean setEntityOnly(boolean entityOnly) {
        this.entityOnly = entityOnly;
        return this;
    }

    /**
     * Only used if the fortressName is being created for the first time.
     * This configures the default TZ used by the fortressName for dates
     *
     * @return TimeZone.getTimeZone(fortressTz).getID();
     */
    public String getTimezone() {
        if (timezone != null)
            return timezone;
        return TimeZone.getDefault().getID();
    }

    public EntityInputBean setTimezone(String timezone) {
        this.timezone = timezone;
        return this;
    }

    public boolean isArchiveTags() {
        return archiveTags;
    }

    /**
     * Instructs FlockData to Move tags already associated with an entity to the content
     * if they are NOT present in this track request.
     * <p/>
     * Only applies to updating existing entities.
     *
     * @param archiveTags default False - tags not present in this request but are recorded
     *                    against the entity will be MOVED to the content
     */
    public EntityInputBean setArchiveTags(final boolean archiveTags) {
        this.archiveTags = archiveTags;
        return this;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    /**
     * Supports the situation where an entity and it's content are being created and parsed from a single row.
     * The mapping process is responsible for mapping the value to the Log as the entity does not have it
     *
     * @param updateUser fortressUser
     */
    public EntityInputBean setUpdateUser(final String updateUser) {
        this.updateUser = updateUser;
        return this;
    }

    public Date getLastChange() {
        return lastChange;
    }

    public EntityInputBean setLastChange(final Date lastChange) {
        this.lastChange = lastChange;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityInputBean)) return false;

        EntityInputBean that = (EntityInputBean) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (!getDocumentType().getName().equals(that.getDocumentType().getName())) return false;
        if (!fortress.equals(that.fortress)) return false;
        return !(key != null ? !key.equals(that.key) : that.key != null);

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + fortress.hashCode();
        result = 31 * result + getDocumentType().getName().hashCode();
        return result;
    }

    public FortressUser getUser() {
        return user;
    }

    public EntityInputBean setUser(final FortressUser user) {
        this.user = user;
        return this;
    }

    public MetaDocument getDocumentType() {
        return documentType;
    }

    public EntityInputBean setDocumentType(final MetaDocument documentType) {
        this.documentType = documentType;
        return this;
    }

    public String getSegment() {
        return segment;
    }

    public EntityInputBean setSegment(String segment) {
        this.segment = segment;
        return this;
    }

    /**
     * Merges selected properties of the EntityInputBean into this one
     *
     * @param source eib to merge from
     * @return fluent
     */
    public EntityInputBean merge(EntityInputBean... source) {
        for (EntityInputBean entityInputBean : source) {
            for (TagInputBean tagInputBean : entityInputBean.getTags()) {
                int index = tags.indexOf(tagInputBean);
                if (index != -1) {
                    // Tag exists, but do the relationships?
                    TagInputBean existingTag = tags.get(index);
                    tagInputBean.getEntityTagLinks().keySet().stream().filter(key -> !existingTag.hasEntityRelationship(key)).forEach(key -> {
                        existingTag.addEntityTagLink(key, tagInputBean.getEntityTagLinks().get(key));
                    });
                } else {
                    addTag(tagInputBean);
                }
            }
            for (String key : entityInputBean.getEntityLinks().keySet()) {
                List<EntityKeyBean> links = entityInputBean.getEntityLinks().get(key);
                if ( getEntityLinks().containsKey(key))
                    for (EntityKeyBean link : links) {
                        getEntityLinks().get(key).add(link);
                    } else {
                        getEntityLinks().put(key, links);
                }
            }

        }

        return this;
    }
}
