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

package com.auditbucket.engine.repo.neo4j.model;

import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
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
public class TrackLogRelationship implements TrackLog {
    @GraphId
    private Long id;

    @StartNode
    private MetaHeaderNode metaHeader;

    @EndNode
    @Fetch
    private LogNode changeLog;

    @Indexed
    private Long sysWhen = 0l;

    @Indexed
    private Long fortressWhen = 0l;

    //@Indexed
    // ToDo: Associated with a node if Not Indexed. This is for maintenance and rebuilding missing docs.
    private boolean indexed = false;

    protected TrackLogRelationship() {
        DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
        this.sysWhen = now.getMillis();
    }

    public TrackLogRelationship(MetaHeader header, Log log, DateTime fortressWhen) {
        this();
        this.metaHeader = (MetaHeaderNode) header;
        this.changeLog = (LogNode) log;
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

    public Log getChange() {
        return changeLog;
    }

    @JsonIgnore
    public MetaHeader getMetaHeader() {
        return metaHeader;
    }

    public void setChange(LogNode auditLog) {
        this.changeLog = auditLog;
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
        if (!(o instanceof TrackLogRelationship)) return false;

        TrackLogRelationship that = (TrackLogRelationship) o;

        if (changeLog != null ? !changeLog.equals(that.changeLog) : that.changeLog != null) return false;
        if (metaHeader != null ? !metaHeader.equals(that.metaHeader) : that.metaHeader != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (metaHeader != null ? metaHeader.hashCode() : 0);
        result = 31 * result + (changeLog != null ? changeLog.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TrackLogRelationship{" +
                "id=" + id +
                ", sysWhen=" + sysWhen +
                ", indexed=" + indexed +
                '}';
    }
}
