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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
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
        this.id = System.currentTimeMillis();
        this.mocked = true;
        this.madeBy = (entity.getCreatedBy()==null ? new FortressUser(entity.getSegment().getFortress(), null) :entity.getCreatedBy());
        this.event = (entity.getEvent() == null ? "Create":entity.getEvent());
        this.storage = Store.NONE.name();
    }

    public Log(FortressUser madeBy, ContentInputBean contentBean, TxRef txRef) {
        this();
        this.madeBy = madeBy;

        event = contentBean.getEvent();

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
    private StoredContent content = null;

    @JsonIgnore
    public void setContent(StoredContent storedContent) {
        this.content = storedContent;
        if ( storedContent.getContent()!=null ){
            this.profileVersion = storedContent.getContent().getpVer();
        }

    }

    public Double getProfileVersion() {
        return profileVersion;
    }

    @JsonIgnore
    public StoredContent getContent() {
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

    /**
     * If we don't store the log for an entity, then we may still need to display
     * the data for it as resolved by fd-store.
     *
     * @return true if the log is not physically stored in the Graph
     */
    public boolean isMocked() {
        return mocked;
    }

}
