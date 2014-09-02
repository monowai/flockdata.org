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
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackLog;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.data.neo4j.annotation.*;

import java.util.TimeZone;

/**
 * User: Mike Holdsworth
 * Date: 26/05/13
 * Time: 4:12 PM
 */
@RelationshipEntity(type = "LOGGED")
public class LoggedRelationship implements TrackLog {
    @GraphId
    private Long id;

    @StartNode
    private MetaHeaderNode metaHeader;

    @EndNode
    @Fetch
    private LogNode log;

    @Indexed
    private Long sysWhen = 0l;

    @Indexed
    @Fetch
    private Long fortressWhen = 0l;

    private String tz = null;

    //@Indexed
    // ToDo: Associated with a node if Not Indexed. This is for maintenance and rebuilding missing docs.
    private boolean indexed = false;

    protected LoggedRelationship() {
        DateTime utcNow = new DateTime().toDateTime(DateTimeZone.UTC);
        setSysWhen(utcNow.getMillis());
    }

    public LoggedRelationship(MetaHeader header, Log log, DateTime fortressWhen) {
        this();
        this.metaHeader = (MetaHeaderNode) header;
        this.log = (LogNode) log;
        this.tz = header.getFortress().getTimeZone();
        if (fortressWhen != null && fortressWhen.getMillis() != 0) {
            setFortressWhen(fortressWhen);
        } else {
            // "now" in the fortress default timezone
            setFortressWhen(new DateTime(sysWhen, DateTimeZone.forTimeZone(TimeZone.getTimeZone(header.getFortress().getTimeZone()))));
        }
    }


    public Long getSysWhen() {
        return sysWhen;
    }
    @Override
    public DateTime getFortressWhen(DateTimeZone tz) {
        return new DateTime(fortressWhen, tz);
    }

    public DateTime getFortressWhen() {
        return new DateTime(fortressWhen);
    }

    void setFortressWhen(DateTime fortressWhen){
        this.fortressWhen = fortressWhen.getMillis();
    }

    void setSysWhen(Long sysWhen) {
        this.sysWhen = sysWhen;
    }

    public Log getLog() {
        return log;
    }

    @JsonIgnore
    public MetaHeader getMetaHeader() {
        return metaHeader;
    }

    public void setMetaHeader(MetaHeader metaHeader){
        this.metaHeader = (MetaHeaderNode)metaHeader;
    }

    public void setChange(LogNode auditLog) {
        this.log = auditLog;
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
        if (!(o instanceof LoggedRelationship)) return false;

        LoggedRelationship that = (LoggedRelationship) o;

        if (log != null ? !log.equals(that.log) : that.log != null) return false;
        if (metaHeader != null ? !metaHeader.equals(that.metaHeader) : that.metaHeader != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (metaHeader != null ? metaHeader.hashCode() : 0);
        result = 31 * result + (log != null ? log.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LoggedRelationship{" +
                "id=" + id +
                ", sysWhen=" + sysWhen +
                ", indexed=" + indexed +
                '}';
    }
}
