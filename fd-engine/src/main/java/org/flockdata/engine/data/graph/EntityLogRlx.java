/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.engine.data.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityLog;
import org.flockdata.data.Log;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.*;

import java.util.TimeZone;

/**
 * @author mholdsworth
 * @since 21/06/2013
 * @tag Relationship, Log
 */
@RelationshipEntity(type = "LOGGED")
public class EntityLogRlx implements EntityLog {

    @GraphId
    private Long id;

    @StartNode
    private EntityNode entity;

    @EndNode
    @Fetch
    private LogNode log;

    @Indexed
    private Long sysWhen = 0l;

    @Indexed
    @Fetch
    private Long fortressWhen = 0l;

    @Transient
    private boolean isMock;

    //@Indexed
    // ToDo: Associated with a node if Not Indexed. This is for maintenance and rebuilding missing docs.
    private boolean indexed = false;

    protected EntityLogRlx() {
        DateTime utcNow = new DateTime().toDateTime(DateTimeZone.UTC);
        setSysWhen(utcNow.getMillis());
    }

    public EntityLogRlx(Entity entity, Log log, DateTime fortressWhen) {
        this();
        this.entity = (EntityNode)entity;
        this.log = (LogNode)log;
        if (!entity.getSegment().getFortress().isStoreEnabled()) {
            id = System.currentTimeMillis();
            isMock = log.isMocked();
        }
        if (fortressWhen != null && fortressWhen.getMillis() != 0) {
            setFortressWhen(fortressWhen);
        } else {
            // "now" in the fortress default timezone
            setFortressWhen(new DateTime(sysWhen, DateTimeZone.forTimeZone(TimeZone.getTimeZone(entity.getSegment().getFortress().getTimeZone()))));
        }
        this.log.setEntityLog(this);
    }


    @Override
    public Long getSysWhen() {
        return sysWhen;
    }

    void setSysWhen(Long sysWhen) {
        this.sysWhen = sysWhen;
    }

    public DateTime getFortressWhen(DateTimeZone tz) {
        return new DateTime(fortressWhen, tz);
    }

    public Long getFortressWhen() {
        return fortressWhen;
    }

    void setFortressWhen(DateTime fortressWhen){
        this.fortressWhen = fortressWhen.getMillis();
    }

    @Override
    public Log getLog() {
        return log;
    }

    @Override
    @JsonIgnore
    public Entity getEntity() {
        return entity;
    }

    public void setEntity(EntityNode entity){
        this.entity = entity;
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

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityLogRlx)) return false;

        EntityLogRlx that = (EntityLogRlx) o;

        if (log != null ? !log.equals(that.log) : that.log != null) return false;
        if (entity != null ? !entity.getId().equals(that.entity.getId()) : that.entity != null) return false;
        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (entity != null ? entity.hashCode() : 0);
        result = 31 * result + (log != null ? log.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EntityLog{" +
                "id=" + id +
                ", fortressWhen=" + new DateTime(fortressWhen) +
                ", sysWhen=" + new DateTime(sysWhen) +
                ", indexed=" + indexed +
                '}';
    }

    @JsonIgnore
    public boolean isMocked() {
        return isMock;
    }
}
