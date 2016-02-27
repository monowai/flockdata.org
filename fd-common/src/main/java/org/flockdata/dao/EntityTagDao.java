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

package org.flockdata.dao;

import org.flockdata.helper.FlockException;
import org.flockdata.model.*;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 9:55 PM
 */
public interface EntityTagDao {

    // Property that refers to when this relationship was introduced to FD
    String FD_WHEN = "fdWhen";

    EntityTag save(Entity entity, Tag tag, String relationshipName);

    EntityTag save(Entity ah, Tag tag, String metaLink, boolean reverse);

    EntityTag save(Entity ah, Tag tag, String relationshipName, Boolean isReversed, Map<String, Object> propMap);

    Boolean relationshipExists(Entity entity, Tag tag, String relationshipName);

    /**
     * Track Tags that are in either direction
     *
     * @param company    validated company
     * @param entity    entity the caller is authorised to work with
     * @return           all EntityTags for the company in both directions
     */
    Set<EntityTag> getEntityTags(Company company, Entity entity);

    Set<EntityTag> getDirectedEntityTags(Company company, Entity entity, boolean outbound);

    Set<EntityTag> findLogTags(Company company, Log log) ;

    void changeType(Entity entity, EntityTag existingTag, String newType);

    Set<Entity> findEntitiesByTag(Tag tag);

    void moveTags(Entity entity, Log log, Collection<EntityTag> entityTag);

    void deleteEntityTags(Entity entity, Collection<EntityTag> entityTags) throws FlockException;

    void moveTags(Company company, Log logToMoveFrom, Entity entity);

    Collection<EntityTag> findEntityTagsByRelationship(Entity entity, String relationship) ;
}
