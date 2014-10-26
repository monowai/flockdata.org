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

package org.flockdata.track.bean;

import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityTag;
import org.flockdata.track.model.EntityLog;

import java.util.Collection;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Since: 25/08/13
 */
public class EntitySummaryBean {
    private Entity entity;
    private Set<EntityLog> changes;
    private Collection<EntityTag> tags;

    private EntitySummaryBean() {
    }

    public EntitySummaryBean(Entity entity, Set<EntityLog> changes, Collection<EntityTag> tags) {
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

    public Collection<EntityTag> getTags() {
        return tags;
    }
}
