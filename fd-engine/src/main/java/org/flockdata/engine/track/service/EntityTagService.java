/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.engine.track.service;

import java.util.Collection;
import java.util.Set;
import org.flockdata.data.Company;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Log;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.LogNode;
import org.flockdata.helper.FlockException;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityTagInputBean;
import org.flockdata.track.bean.EntityTagResult;

/**
 * @author mholdsworth
 * @tag Entity, Tag, EntityTag
 * @since 6/09/2014
 */
public interface EntityTagService {
  void processTag(EntityNode entity, EntityTagInputBean tagInput);

  Boolean relationshipExists(EntityNode entity, String name, String relationshipType);

  Boolean relationshipExists(EntityNode entity, String keyPrefix, String tagCode, String relationshipType);

  Collection<EntityTag> associateTags(Company company, Entity entity, EntityLog lastLog, EntityInputBean entityInputBean) throws FlockException;

  Collection<EntityTag> findEntityTags(Entity entity);

  Collection<EntityTagResult> findEntityTagResults(Entity entity);

  Collection<EntityTagResult> findOutboundTagResults(Entity entity);

  Collection<EntityTagResult> findOutboundTagResults(Company company, Entity entity);

  Collection<EntityTagResult> findInboundTagResults(Company company, Entity entity);

  Collection<EntityTagResult> findInboundTagResults(Entity entity);

  Collection<EntityTag> findEntityTagsWithGeo(Entity entity);

//    Set<Entity> findEntityTags(Company company, String tagName) throws FlockException;

  Set<Entity> findEntityTagResults(Company company, String tagName) throws FlockException;

  void deleteEntityTags(Entity entity, Collection<EntityTag> entityTags) throws FlockException;

  void deleteEntityTags(Entity entity, EntityTag value) throws FlockException;

  void changeType(EntityNode entity, EntityTag existingTag, String newType) throws FlockException;

  Collection<EntityTag> findLogTags(Company company, Log log);

  EntityNode moveTags(Company company, LogNode previousLog, EntityNode entity);

  Collection<Long> mergeTags(Long fromTag, Long toTag);


  void purgeUnusedTags(String label);

  Collection<EntityTag> findEntityTagsByRelationship(EntityNode entity, String relationship);
}
