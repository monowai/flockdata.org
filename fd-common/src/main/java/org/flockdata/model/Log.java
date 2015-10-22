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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.kv.KvContent;
import org.flockdata.kv.service.KvService;
import org.flockdata.track.bean.ContentInputBean;
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
public class Log  {

    public static final String CREATE = "Create";
    public static final String UPDATE = "Update";

    private static final String COLON = ":";
    @GraphId
    private Long id;

    //@Relationship(type = "CHANGED", direction = Relationship.INCOMING)
    @RelatedTo(type = "CHANGED", direction = Direction.INCOMING, enforceTargetType = true)
    @Fetch
    private FortressUser madeBy;

    //@Relationship(type = "AFFECTED", direction = Relationship.INCOMING)
    @RelatedTo(type = "AFFECTED", direction = Direction.INCOMING, enforceTargetType = true)
    private TxRef txRef;

    @RelatedToVia(type = "LOGGED", direction = Direction.INCOMING)
    private EntityLog entityLog;

    private String event;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String comment;
    private String storage;  // ENUMS are not serializable
    private String checkSum=null;
    private Double profileVersion = 1d;

    @Indexed (unique =  true)
    private String logKey;
    private String contentType ;
    private String fileName;

    private boolean compressed = false;
    private String name;

    @RelatedTo(type = "PREVIOUS_LOG", direction = Direction.OUTGOING)
    private Log previousLog;

    @Transient
    boolean mocked = false;

    public String getContentType() {
        if ( contentType == null )
            contentType = "json";
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "Log{" +
                "id=" + id +
                ", madeBy=" + madeBy +
                ", event=" + event +
                '}';
    }

    protected Log() {
        this.contentType = "json";
    }

    /**
     * Creates a Mock non-persistent node
     * @param entity
     */
    public Log(Entity entity){
        //DAT-349 creates a mock node when storage is disabled
        this.id = 0l;
        this.mocked = true;
        this.madeBy = (entity.getCreatedBy()==null ? new FortressUser(entity.getSegment().getFortress(), null) :entity.getCreatedBy());
        this.event = (entity.getEvent() == null ? "Create":entity.getEvent());
        this.storage = KvService.KV_STORE.NONE.name();
    }

    public Log(FortressUser madeBy, ContentInputBean contentBean, TxRef txRef) {
        this();
        this.madeBy = madeBy;

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getChecksum() {
        return checkSum;
    }

    public void setChecksum(String checksum){
        this.checkSum = checksum;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public FortressUser getMadeBy() {
        return madeBy;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setPreviousLog(Log previousLog) {
        this.previousLog = previousLog;
    }

    @JsonIgnore
    public Log getPreviousLog() {
        return previousLog;
    }

    public void setTxRef(TxRef txRef) {
        this.txRef = txRef;
    }

    public ChangeEvent getEvent() {
        // DAT-344
        if ( event== null )
            return null;

        return new ChangeEvent(event);
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

    public void setEvent(ChangeEvent event) {
        // DAT-344
        //this.event = (ChangeEventNode) event;
        this.event = event.getName();
    }

    public boolean equals(Object other) {
        return this == other || id != null
                && other instanceof Log
                && id.equals(((Log) other).id);

    }

    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }

    public void setCompressed(Boolean compressed) {
        this.compressed = compressed;
    }

    @JsonIgnore
    public EntityLog getEntityLog() {
        return entityLog;
    }

    @Transient
    private KvContent content = null;

    @JsonIgnore
    public void setContent(KvContent kvContent) {
        this.content = kvContent;
        if ( kvContent.getContent()!=null ){
            this.profileVersion = kvContent.getContent().getpVer();
        }

    }

    public Double getProfileVersion() {
        return profileVersion;
    }

    @JsonIgnore
    public KvContent getContent() {
        return content;
    }

    public void setEntityLog(EntityLog entityLog) {
        // DAT-288 DAT-465
        // logKey assumes that an entity will have exactly one change on the FortressWhen date
        this.logKey = ""+entityLog.getEntity().getId() +"."+ entityLog.getFortressWhen();
        //this.entityLog = entityLog;
    }

    public void setMadeBy(FortressUser madeBy) {
        this.madeBy = madeBy;
    }

    public boolean isMocked() {
        return mocked;
    }

}
