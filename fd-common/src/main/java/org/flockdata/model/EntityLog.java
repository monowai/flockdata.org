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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.*;

import java.util.TimeZone;

/**
 * User: Mike Holdsworth
 * Date: 21/06/13
 * Time: 1:21 PM
 */
@RelationshipEntity(type = "LOGGED")
public class EntityLog {

    @GraphId
    private Long id;

    @StartNode
    private Entity entity;

    @EndNode
    @Fetch
    private Log log;

    @Indexed
    private Long sysWhen = 0l;

    @Indexed
    @Fetch
    private Long fortressWhen = 0l;

    private String timezone = null;

    @Transient
    private boolean isMock;

    //@Indexed
    // ToDo: Associated with a node if Not Indexed. This is for maintenance and rebuilding missing docs.
    private boolean indexed = false;

    protected EntityLog() {
        DateTime utcNow = new DateTime().toDateTime(DateTimeZone.UTC);
        setSysWhen(utcNow.getMillis());
    }

    public EntityLog(Entity entity, Log log, DateTime fortressWhen) {
        this();
        this.entity = entity;
        this.log = log;
        this.timezone = entity.getSegment().getFortress().getTimeZone();
        if (entity.getSegment().getFortress().isStoreDisabled()) {
            id = System.currentTimeMillis();
            isMock = log.isMocked();
        }
        if (fortressWhen != null && fortressWhen.getMillis() != 0) {
            setFortressWhen(fortressWhen);
        } else {
            // "now" in the fortress default timezone
            setFortressWhen(new DateTime(sysWhen, DateTimeZone.forTimeZone(TimeZone.getTimeZone(entity.getSegment().getFortress().getTimeZone()))));
        }
        log.setEntityLog(this);
    }


    public Long getSysWhen() {
        return sysWhen;
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

    void setSysWhen(Long sysWhen) {
        this.sysWhen = sysWhen;
    }

    public Log getLog() {
        return log;
    }

    @JsonIgnore
    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity){
        this.entity = entity;
    }

    public void setChange(Log auditLog) {
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
        if (!(o instanceof EntityLog)) return false;

        EntityLog that = (EntityLog) o;

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
