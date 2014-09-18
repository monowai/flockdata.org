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

package com.auditbucket.track.bean;

import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.EntityLog;
import com.auditbucket.track.model.TrackTag;

import java.util.Collection;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Since: 25/08/13
 */
public class EntitySummaryBean {
    private Entity entity;
    private Set<EntityLog> changes;
    private Collection<TrackTag> tags;

    private EntitySummaryBean() {
    }

    public EntitySummaryBean(Entity entity, Set<EntityLog> changes, Collection<TrackTag> tags) {
        this();
        this.entity = entity;
        this.changes = changes;
        this.tags = tags;
    }

    public Entity getEntity() {
        return entity;
    }

    public Set<EntityLog> getChanges() {
        return changes;
    }

    public Collection<TrackTag> getTags() {
        return tags;
    }
}
