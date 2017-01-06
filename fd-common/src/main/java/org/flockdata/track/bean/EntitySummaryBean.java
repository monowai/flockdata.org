/*
 *  Copyright 2012-2017 the original author or authors.
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

package org.flockdata.track.bean;

import org.flockdata.data.Entity;
import org.flockdata.data.EntityLog;
import org.flockdata.data.EntityTag;

import java.util.Collection;

/**
 * @author mholdsworth
 * @since 25/08/2013
 * @tag Contract, Track, Entity, Query
 */
public class EntitySummaryBean {
    private EntityResultBean entity;
    private String type;
    private Collection<EntityLog> changes;
    private Collection<EntityTag> tags;
    private String index;

    private EntitySummaryBean() {
    }

    public EntitySummaryBean(Entity entity, Collection<EntityLog> changes, Collection<EntityTag> tags) {
        this();
        this.entity = new EntityResultBean(entity);
        this.type = entity.getType().toLowerCase();
        this.changes = changes;
        this.tags = tags;
    }

    public Entity getEntity() {
        return entity;
    }

    public Collection<EntityLog> getChanges() {
        return changes;
    }

    public Collection<EntityTag> getTags() {
        return tags;
    }

    public String getType() {
        return type;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
}
