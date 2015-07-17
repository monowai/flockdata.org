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

package org.flockdata.track;

import org.flockdata.track.bean.EntityTagInputBean;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by mike on 15/07/15.
 */
public class EntityTagPayload {
    Collection<EntityTagInputBean>entityTags = new ArrayList<>();
    private Long entityId;

    EntityTagPayload () {}

    public EntityTagPayload (Long entityId, EntityTagInputBean entityTag){
        this();
        this.entityId = entityId;
        addEntityTag(entityTag);
    }

    public Long getEntityId() {
        return entityId;
    }

    public Collection<EntityTagInputBean> getEntityTags() {
        return entityTags;
    }

    public void addEntityTag(EntityTagInputBean entityTag){
        entityTags.add(entityTag);
    }
}
