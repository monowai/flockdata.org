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

package org.flockdata.engine.track.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.company.model.FortressUserNode;
import org.flockdata.engine.schema.model.ChangeEventNode;
import org.flockdata.engine.schema.model.TxRefNode;
import org.flockdata.kv.service.KvService;
import org.flockdata.registration.model.FortressUser;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.model.*;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

/**
 * Tracks metadata about the Log event that is occurring against the Entity
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 5:57 PM
 */
@NodeEntity(useShortNames = true)
@TypeAlias("Log")
public class LogNode implements Log {
    private static final String COLON = ":";
    @GraphId
    private Long id;

    @RelatedTo(type = "CHANGED", direction = Direction.INCOMING, enforceTargetType = true)
    @Fetch
    private FortressUserNode madeBy;

    @RelatedTo(type = "AFFECTED", direction = Direction.INCOMING, enforceTargetType = true)
    private TxRefNode txRef;

    @RelatedToVia(type = "LOGGED", direction = Direction.INCOMING)
    private EntityLogRelationship entityLog;

    // DAT-344
//    @RelatedTo(elementClass = ChangeEventNode.class, type = "TRACK_EVENT", direction = Direction.OUTGOING)
//    @Fetch
//    private ChangeEventNode event;
    private String event;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String comment;
    private String storage;  // ENUMS are not serializable
    private String checkSum=null;
    private Double profileVersion = 1d;

    @Indexed (unique =  true)
    private String logKey;

    public String getContentType() {
        if ( contentType == null )
            contentType = "json";
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private String contentType ;
    private String fileName;

    private boolean compressed = false;
    private String name;

    @RelatedTo(type = "PREVIOUS_LOG", direction = Direction.OUTGOING)
    private LogNode previousLog;

    @Transient
    boolean isMocked = false;

    @Override
    public String toString() {
        return "LogNode{" +
                "id=" + id +
                ", madeBy=" + madeBy +
                ", event=" + event +
                '}';
    }

    protected LogNode() {
        this.contentType = "json";
    }

    /**
     * Creates a Mock non-persistable node
     * @param entity
     */
    public LogNode(Entity entity ){
        //DAT-349 creates a mock node when storage is disabled
        this.id = 0l;
        this.isMocked = true;
        this.madeBy = (entity.getCreatedBy()==null ? new FortressUserNode(entity.getFortress(), "Unknown") :(FortressUserNode)entity.getCreatedBy());
        this.event = (entity.getEvent() == null ? "Create":entity.getEvent());
        this.storage = KvService.KV_STORE.NONE.name();
    }

    public LogNode(FortressUser madeBy, ContentInputBean contentBean, TxRef txRef) {
        this();
        this.madeBy = (FortressUserNode) madeBy;

        event = contentBean.getEvent();

        this.name = event + COLON + (madeBy==null ? "na" : madeBy.getCode());
        this.fileName = contentBean.getFileName();
        this.contentType = contentBean.getContentType();
        setTxRef(txRef);
        this.comment = contentBean.getComment();
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    @Override
    public String getChecksum() {
        return checkSum;
    }

    @Override
    public void setChecksum(String checksum){
        this.checkSum = checksum;
    }

    public FortressUser getWho() {
        return madeBy;
    }

    @Override
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setEntityLog(EntityLogRelationship entityLog) {
        this.entityLog = entityLog;
    }

    @Override
    public void setPreviousLog(Log previousLog) {
        this.previousLog = (LogNode) previousLog;
    }

    @Override
    @JsonIgnore
    public Log getPreviousLog() {
        return previousLog;
    }

    public void setTxRef(TxRef txRef) {
        this.txRef = (TxRefNode) txRef;
    }

    public ChangeEvent getEvent() {
        // DAT-344
        if ( event== null )
            return null;

        return new ChangeEventNode(event);
    }

    public boolean isCompressed() {
        return compressed;
    }

    @JsonIgnore
    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    @Override
    public void setEvent(ChangeEvent event) {
        // DAT-344
        //this.event = (ChangeEventNode) event;
        this.event = event.getName();
    }

    public boolean equals(Object other) {
        return this == other || id != null
                && other instanceof LogNode
                && id.equals(((LogNode) other).id);

    }

    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }

    @Override
    public void setCompressed(Boolean compressed) {
        this.compressed = compressed;
    }

    @Override
    @JsonIgnore
    public EntityLog getEntityLog() {
        return entityLog;
    }

    @Transient
    private KvContent content = null;

    @Override
    @JsonIgnore
    public void setContent(KvContent kvContent) {
        this.content = kvContent;
        if ( kvContent.getContent()!=null ){
            this.profileVersion = kvContent.getContent().getProfileVersion();
        }

    }

    @Override
    public Double getProfileVersion() {
        return profileVersion;
    }


    @Override
    @JsonIgnore
    public KvContent getContent() {
        return content;
    }

    @Override
    public void setEntityLog(EntityLog entityLog) {
        // DAT-288
        // logKey assumes that an entity will have exactly one change on the FortressWhen date
        this.logKey = ""+entityLog.getEntity().getId() +"."+ entityLog.getFortressWhen();
        this.entityLog = (EntityLogRelationship) entityLog;
    }

    @Override
    public boolean isMocked() {
        return isMocked;
    }

}
