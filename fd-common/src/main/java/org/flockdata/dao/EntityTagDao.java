/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

    AbstractEntityTag save(Entity entity, Tag tag, String relationshipName);

    AbstractEntityTag save(Entity ah, Tag tag, String metaLink, boolean reverse);

    AbstractEntityTag save(Entity ah, Tag tag, String relationshipName, Boolean isReversed, Map<String, Object> propMap);

    Boolean relationshipExists(Entity entity, Tag tag, String relationshipName);

    /**
     * Track Tags that are in either direction
     *
     * @param company    validated company
     * @param entity    entity the caller is authorised to work with
     * @return           all EntityTags for the company in both directions
     */
    Set<AbstractEntityTag> getEntityTags(Company company, Entity entity);

    Set<AbstractEntityTag> getDirectedEntityTags(Company company, Entity entity, boolean outbound);

    Set<AbstractEntityTag> findLogTags(Company company, Log log) ;

    void changeType(Entity entity, AbstractEntityTag existingTag, String newType);

    Set<Entity> findEntitiesByTag(Tag tag);

    void moveTags(Entity entity, Log log, Collection<AbstractEntityTag> entityTag);

    void deleteEntityTags(Entity entity, Collection<AbstractEntityTag> entityTags) throws FlockException;

    void moveTags(Company company, Log logToMoveFrom, Entity entity);

    Collection<AbstractEntityTag> findEntityTagsByRelationship(Entity entity, String relationship) ;
}
