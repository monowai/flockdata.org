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

package org.flockdata.track.service;

import org.flockdata.helper.FlockException;
import org.flockdata.model.*;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityTagInputBean;

import java.util.Collection;
import java.util.Set;

/**
 * User: mike
 * Date: 6/09/14
 * Time: 5:13 PM
 */
public interface EntityTagService {
    void processTag(Entity entity, EntityTagInputBean tagInput);

    Boolean relationshipExists(Entity entity, String name, String relationshipType);

    Boolean relationshipExists(Entity entity, String keyPrefix, String tagCode, String relationshipType);

    Collection<EntityTag> associateTags(Company company, Entity entity, EntityLog lastLog, EntityInputBean entityInputBean) throws FlockException;

    Collection<EntityTag> findEntityTags(Company company, Entity entity);

    Collection<EntityTag> findEntityTags(Entity entity);

    Collection<EntityTag> findOutboundTags(Entity entity);

    Collection<EntityTag> findOutboundTags(Company company, Entity entity);

    Collection<EntityTag> findInboundTags(Company company, Entity entity);

    Collection<EntityTag> findInboundTags(Entity entity);

    Collection<EntityTag> getEntityTags(Entity entity);

    Iterable<EntityTag> getEntityTagsWithGeo(Entity entity);

    void deleteEntityTags(Entity entity, Collection<EntityTag> entityTags) throws FlockException;

    void deleteEntityTags(Entity entity, EntityTag value) throws FlockException;

    void changeType(Entity entity, EntityTag existingTag, String newType) throws FlockException;

    Set<Entity> findEntityTags(Company company, String tagName) throws FlockException;

    Collection<EntityTag> findLogTags(Company company, Log log);

    Entity moveTags(Company company, Log previousLog, Entity entity);

    Collection<Long> mergeTags(Long fromTag, Long toTag);

    void purgeUnusedTags(String label);


    Collection<EntityTag> findEntityTagsByRelationship(Entity entity, String relationship);
}
