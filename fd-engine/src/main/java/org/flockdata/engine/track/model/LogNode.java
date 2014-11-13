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

import org.flockdata.engine.schema.model.ChangeEventNode;
import org.flockdata.engine.schema.model.TxRefNode;
import org.flockdata.company.model.FortressUserNode;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.registration.model.FortressUser;
import org.flockdata.track.model.ChangeEvent;
import org.flockdata.track.model.EntityLog;
import org.flockdata.track.model.Log;
import org.flockdata.track.model.TxRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
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

    @RelatedTo(elementClass = FortressUserNode.class, type = "CHANGED", direction = Direction.INCOMING, enforceTargetType = true)
    @Fetch
    private FortressUserNode madeBy;

    @RelatedTo(elementClass = TxRefNode.class, type = "AFFECTED", direction = Direction.INCOMING, enforceTargetType = true)
    private TxRef txRef;

    @RelatedToVia(elementClass = EntityLogRelationship.class, type = "LOGGED", direction = Direction.INCOMING)
    private EntityLogRelationship entityLog;

    @RelatedTo(elementClass = ChangeEventNode.class, type = "TRACK_EVENT", direction = Direction.OUTGOING)
    @Fetch
    private ChangeEventNode event;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String comment;
    private String storage;
    private String checkSum=null;
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

    public LogNode(FortressUser madeBy, ContentInputBean contentBean, TxRef txRef) {
        this();
        this.madeBy = (FortressUserNode) madeBy;

        String event = contentBean.getEvent();
        this.name = event + COLON + madeBy.getCode();
        this.fileName = contentBean.getFileName();
        this.contentType = contentBean.getContentType();
        setTxRef(txRef);
        this.comment = contentBean.getComment();
    }

    @JsonIgnore
    public long getId() {
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
        this.txRef = txRef;
    }

    public ChangeEvent getEvent() {
        return event;
    }

    public boolean isCompressed() {
        return compressed;
    }

    @JsonIgnore
    public String getWhatStore() {
        return storage;
    }

    public void setWhatStore(String storage) {
        this.storage = storage;
    }

    @Override
    public void setEvent(ChangeEvent event) {
        this.event = (ChangeEventNode) event;

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
    private byte[] entityContent = null;

    @Override
    @JsonIgnore
    public void setEntityContent(byte[] entityContent) {
        this.entityContent = entityContent;

    }

    @Override
    @JsonIgnore
    public byte[] getEntityContent() {
        return entityContent;
    }

    @Override
    public void setTrackLog(EntityLog entityLog) {
        this.logKey = ""+entityLog.getEntity().getId() +"."+ entityLog.getFortressWhen();
        this.entityLog = (EntityLogRelationship) entityLog;
    }


}
