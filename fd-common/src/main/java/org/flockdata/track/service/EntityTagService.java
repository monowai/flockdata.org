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

package org.flockdata.track.service;

import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.bean.EntityTagInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityTag;
import org.flockdata.track.model.Log;
import org.flockdata.registration.model.Company;
import org.flockdata.track.model.EntityLog;

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

    Collection<EntityTag> associateTags(Company company, Entity ah, EntityLog lastLog, Collection<TagInputBean> userTags, Boolean archiveRemovedTags);

    Collection<EntityTag> findEntityTags(Company company, Entity entity);

    Collection<EntityTag> findEntityTags(Entity entity);

    Collection<EntityTag> findOutboundTags(Entity entity);

    Collection<EntityTag> findOutboundTags(Company company, Entity entity);

    Collection<EntityTag> findInboundTags(Company company, Entity entity);

    Collection<EntityTag> getEntityTags(Company company, Entity entity);

    Collection<EntityTag> getEntityTags(Company company, Long entity);

    void deleteEntityTags(Entity entity, Collection<EntityTag> entityTags) throws FlockException;

    void deleteEntityTags(Entity entity, EntityTag value) throws FlockException;

    void changeType(Entity entity, EntityTag existingTag, String newType) throws FlockException;

    Set<Entity> findEntityTags(Company company, String tagName) throws FlockException;

    Collection<EntityTag> findLogTags(Company company, Log log);

    void moveTags(Company company, Log previousLog, Entity entity);

    Collection<Long> mergeTags(Tag fromTag, Tag toTag);
}
