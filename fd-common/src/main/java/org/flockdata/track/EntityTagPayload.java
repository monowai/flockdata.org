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
