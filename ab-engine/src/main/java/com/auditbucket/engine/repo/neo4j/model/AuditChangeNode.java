/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

package com.auditbucket.engine.repo.neo4j.model;

import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.model.*;
import com.auditbucket.engine.repo.AuditWhatData;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.repo.neo4j.model.FortressUserNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 5:57 PM
 */
@NodeEntity(useShortNames = true)
@TypeAlias("Change")
public class AuditChangeNode implements AuditChange {
    private static final String COLON = ":";
    @GraphId
    private Long id;

    @RelatedTo(elementClass = FortressUserNode.class, type = "CHANGED", direction = Direction.INCOMING, enforceTargetType = true)
    @Fetch
    private FortressUserNode madeBy;

    @RelatedTo(elementClass = TxRefNode.class, type = "AFFECTED", direction = Direction.INCOMING, enforceTargetType = true)
    private TxRef txRef;

    @RelatedTo(elementClass = AuditEventNode.class, type = "AUDIT_EVENT", direction = Direction.OUTGOING)
    @Fetch
    private AuditEventNode event;

    private String comment;
    private String storage ;

    // Neo4J will not persist a byte[] over it's http interface. Probably fixed in V2, but not in our version
    @JsonIgnore
    private boolean compressed = false;
    private String name;

    @RelatedTo(type = "PREVIOUS_CHANGE", direction = Direction.OUTGOING)
    private AuditChangeNode previousChange;

    @Fetch
    @RelatedToVia(type = "LOGGED", direction = Direction.INCOMING)
    AuditLogRelationship auditLog;

//    @RelatedTo(type = "auditWhat")
    private AuditWhatData auditWhat;

    protected AuditChangeNode() {

    }

    public AuditChangeNode(FortressUser madeBy, AuditLogInputBean inputBean, TxRef txRef) {
        this();
        this.madeBy = (FortressUserNode) madeBy;

        String event = inputBean.getEvent();
        this.name = event + COLON + madeBy.getCode();
        setTxRef(txRef);
        this.comment = inputBean.getComment();
    }

    @JsonIgnore
    public long getId() {
        return id;
    }

    @Override
    @JsonIgnore
    public AuditWhat getWhat() {
        return auditWhat;
    }

    public void setWhat(AuditWhat what) {
        this.auditWhat = (AuditWhatData) what;
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

    /**
     * @return the name of the event that caused this change
     */
    @JsonIgnore
    public String getName() {
        return name;
    }

    @Override
    public void setPreviousChange(AuditChange previousChange) {
        this.previousChange = (AuditChangeNode) previousChange;
    }

    @Override
    @JsonIgnore
    public AuditChange getPreviousChange() {
        return previousChange;
    }

    @Override
    @JsonIgnore
    public AuditLog getAuditLog() {
        return auditLog;
    }

    @Transient
    private Map<String, Object> what;

    public void setTxRef(TxRef txRef) {
        this.txRef = txRef;
    }

    public AuditEvent getEvent() {
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
    public void setEvent(AuditEvent event) {
        this.event = (AuditEventNode) event;

    }
}
