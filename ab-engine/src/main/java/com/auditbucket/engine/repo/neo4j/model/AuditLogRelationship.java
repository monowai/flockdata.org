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

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditLog;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

import java.util.TimeZone;

/**
 * User: Mike Holdsworth
 * Date: 26/05/13
 * Time: 4:12 PM
 */
@RelationshipEntity(type = "LOGGED")
@TypeAlias("ab.Log")
public class AuditLogRelationship implements AuditLog {
    @GraphId
    private Long id;

    @StartNode
    private AuditHeaderNode auditHeader;

    @EndNode
    @Fetch
    private AuditChangeNode auditChange;

    @Indexed
    private Long sysWhen = 0l;

    @Indexed
    private Long fortressWhen = 0l;

    //@Indexed
    // ToDo: Associated with a node if Not Indexed. This is for maintenance and rebuilding missing docs.
    private boolean indexed = false;

    protected AuditLogRelationship() {
        DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
        this.sysWhen = now.getMillis();
    }

    public AuditLogRelationship(AuditHeader header, AuditChange log, DateTime fortressWhen) {
        this();
        this.auditHeader = (AuditHeaderNode) header;
        this.auditChange = (AuditChangeNode) log;
        if (fortressWhen != null && fortressWhen.getMillis() != 0) {
            this.fortressWhen = fortressWhen.getMillis();
        } else {
            // "now" in the fortress default timezone
            this.fortressWhen = new DateTime(sysWhen, DateTimeZone.forTimeZone(TimeZone.getTimeZone(header.getFortress().getTimeZone()))).getMillis();
        }
    }


    public Long getSysWhen() {
        return sysWhen;
    }

    public Long getFortressWhen() {
        return fortressWhen;
    }

    void setSysWhen(Long sysWhen) {
        this.sysWhen = sysWhen;
    }

    public AuditChange getAuditChange() {
        return auditChange;
    }

    @JsonIgnore
    public AuditHeader getAuditHeader() {
        return auditHeader;
    }

    public void setChange(AuditChangeNode auditLog) {
        this.auditChange = auditLog;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIsIndexed() {
        this.indexed = true;
    }

    public Long getId() {
        return id;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLogRelationship)) return false;

        AuditLogRelationship that = (AuditLogRelationship) o;

        if (auditChange != null ? !auditChange.equals(that.auditChange) : that.auditChange != null) return false;
        if (auditHeader != null ? !auditHeader.equals(that.auditHeader) : that.auditHeader != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (auditHeader != null ? auditHeader.hashCode() : 0);
        result = 31 * result + (auditChange != null ? auditChange.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AuditLogRelationship{" +
                "id=" + id +
                ", sysWhen=" + sysWhen +
                ", indexed=" + indexed +
                '}';
    }
}
