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

package com.auditbucket.track.bean;

import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.track.model.EntityKey;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

import java.util.*;

/**
 * User: Mike Holdsworth
 * Date: 11/05/13
 * Time: 9:19 AM
 */
public class EntityInputBean {
    private String metaKey;
    private String callerRef;
    private String fortress;
    private String fortressUser;
    private String documentType;
    private Date when = null;
    private ContentInputBean log;
    private List<TagInputBean> tags = new ArrayList<>();
    private Map<String,List<EntityKey>> crossReferences = new HashMap<>();

    private String event = "Create";
    private String description;
    private String name;
    private boolean searchSuppressed;
    private boolean trackSuppressed = false;
    private boolean metaOnly = false;
    private String timezone;


    public EntityInputBean() {
    }

    /**
     *
     * @param fortress      Application/Division or System that owns this information
     * @param fortressUser  who in the fortress created it
     * @param documentType  within the fortress, this is a document of this unique type
     * @param fortressWhen  when did this occur in the fortress
     * @param callerRef     case sensitive unique key. If not supplied, then the service will generate one
     */
    public EntityInputBean(String fortress, String fortressUser, String documentType, DateTime fortressWhen, String callerRef) {
        this();
        if (fortressWhen != null)
            setWhen(fortressWhen);
        setFortress(fortress);
        setFortressUser( fortressUser);
        setDocumentType(documentType);
        setCallerRef(callerRef);
    }

    public EntityInputBean(String description, String fortressUser, String companyNode, DateTime fortressWhen) {
        this(description, fortressUser, companyNode, fortressWhen, null);

    }

    public void setMetaKey(String metaKey) {
        this.metaKey = metaKey;
    }

    public String getMetaKey() {
        return this.metaKey;
    }

    /**
     * Fortress Timezone when
     * Defers to the AuditLogInput if present with a valid date
     *
     * @return when in the fortress this was created
     */
    public Date getWhen() {
        if (when !=null )
            return when;
        // Default to the log date
        if (log != null && log.getWhen() != null && log.getWhen().getTime() > 0)
            return log.getWhen();
        return null;
    }

    /**
     * This date is ignored if a valid one is set in a present log
     *
     * @param when when the caller says this occurred
     */
    public void setWhen(DateTime when) {
        //if (!(log != null && log.getWhen() != null && log.getWhen().getTime() > 0))
        this.when = when.toDate();
        //this.metaTZ = when.getZone().getID();

    }

    public String getFortress() {
        return fortress;
    }

    /**
     * Fortress is a computer application/service in the callers environment, i.e. Payroll, HR, AR.
     * This could also be thought of as a Database in an DBMS
     *
     * The Fortress relationshipName is unique for the Company.
     *
     * @param fortress unique fortress relationshipName
     */
    public void setFortress(String fortress) {
        this.fortress = fortress;
    }

    /**
     * This is unused in the Entity
     * Obtain this from the InputBean that you are logging, not the
     * @return name
     */
    @Deprecated
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
     * @param documentType relationshipName of the document
     */
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }


    public String getCallerRef() {
        return callerRef;
    }

    /**
     * Optional case sensitive & unique for the Fortress & Document Type combination. If you do not have
     * a primary key, then to update "this" instance of the Entity you will need to use
     * the generated AuditKey returned by AuditBucket in the TrackResultBean
     *
     * @see TrackResultBean
     *
     * @param callerRef case sensitive primary key generated by the calling fortress
     */
    public void setCallerRef(String callerRef) {
        this.callerRef = callerRef;
    }

    public void setContent(ContentInputBean content) {
        this.log = content;
        if (content != null) {
            this.metaOnly = false;
            //this.when = log.getWhen();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ContentInputBean getLog() {
        return log;
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
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * Single tag
     *
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

    public void setTags(Collection<TagInputBean>tags){
        for (TagInputBean next : tags) {
            this.tags.add(next);

        }

    }
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description User definable note describing the entity
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

    public void addCrossReference(String relationshipName, EntityKey entityKey){
        //new CrossReferenceInputBean(getFortresses(), callerRef, c)
        List<EntityKey>refs = crossReferences.get(relationshipName);
        if ( refs == null ){
            refs = new ArrayList<>();
            crossReferences.put(relationshipName, refs);
        }
        refs.add(entityKey);
    }

    /**
     * Format is "referenceName", Collection<callerRef>
     * All callerRefs are assumed to belong to this same fortress
     * "This" callerRef is assume to be the starting point for the CrossReferences to link to
     *
     * @return crossReferences
     */
    public Map<String,List<EntityKey>> getCrossReferences(){
        return crossReferences;
    }

    @Override
    public String toString() {
        return "MetaInputBean{" +
                "fortress='" + getFortress() + '\'' +
                ", documentType='" + getDocumentType() + '\'' +
                ", name='" + getName() + '\'' +
                ", callerRef='" + getCallerRef() + '\'' +
                ", metaKey='" + getMetaKey() + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (name!=null? name.trim():name);
    }

    /**
     * Flags that this Entity will never have a log. It will still be tracked through
     * in to the Search Service.
     *
     * @param metaOnly if false then the entity will not be indexed in search until a log is added
     */
    public void setMetaOnly(boolean metaOnly) {
        this.metaOnly = metaOnly;
    }

    public boolean isMetaOnly() {
        return metaOnly;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    /**
     * Only used if the fortress is being created for the first time.
     * This configures the default TZ used by the fortress for dates
     *
     * @return TimeZone.getTimeZone(fortressTz).getID();
     */
    public String getTimezone() {
        if (timezone !=null )
            return timezone;
        return TimeZone.getDefault().getID();
    }
}
