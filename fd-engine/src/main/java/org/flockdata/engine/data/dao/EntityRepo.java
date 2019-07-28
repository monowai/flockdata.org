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

package org.flockdata.engine.data.dao;

import java.util.Collection;
import java.util.Set;
import org.flockdata.data.Entity;
import org.flockdata.engine.data.graph.EntityNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author mholdsworth
 * @tag neo4j, Entity, GraphRepository, Query
 * @since 14/04/2013
 */
public interface EntityRepo extends GraphRepository<EntityNode> {

  @Query(elementClass = EntityNode.class, value = "   MATCH tx-[:AFFECTED]->change<-[:LOGGED]-(entity:Entity) where id(tx)= {0}" +
      "return entity")
  Set<EntityNode> findEntitiesByTxRef(Long txRef);

  @Query(elementClass = EntityNode.class, value = " match (fortress:Fortress)-[:DEFINES]-(segment:FortressSegment)-[:TRACKS]->(entity:Entity) " +
      " where id(fortress)={0} and id(entity) > {1}" +
      " return entity ORDER BY id(entity) ASC" +
      " limit 100 ")
  Set<Entity> findEntities(Long fortressId, Long lastEntity);

  @Query(value = " match (fortress:Fortress)-[:DEFINES]-(FortressSegment)-[:TRACKS]->(track:Entity) " +
      " where id(fortress)={0} " +
      " return track.key " +
      " limit {1} ")
  Collection<String> findEntitiesWithLimit(Long id, int limit);

  @Query(value = " match (fortress:Fortress)-[:DEFINES]-(fs:FortressSegment)-[:TRACKS]->(track:Entity) " +
      " where id(fortress)={0} and id(fs)={1}" +
      " return track.key " +
      " limit {2} ")
  Collection<String> findEntitiesWithLimit(Long id, Long segmentId, int limit);

  @Query(elementClass = EntityNode.class, value =
      " match (fortress:Fortress) where id(fortress) = {0} " +
          "match (entity:Entity) where entity.code ={1} " +
          "with fortress,entity " +
          "match p=shortestpath ((entity)<-[*1..2]-(fortress)) " +
          "return entity")
  Collection<Entity> findByCode(Long fortressId, String code);

  @Query(elementClass = EntityNode.class, value = "match  (entities:Entity) " +
      " where  entities.key in {1}  " +
      "return entities ")
  Collection<EntityNode> findEntities(Long id, Collection<String> toFind);

  @Query(value = "match (meta:Entity)-[other]-(:FortressUser) where meta.key in{0} delete other")
  void purgePeopleRelationships(Collection<String> entities);

  @Query(value = "match (meta:Entity)-[otherRlx]-(:Entity) where meta.key in {0} delete otherRlx")
  void purgeEntityLinks(Collection<String> entities);

  @Query(value = "match (f:FortressSegment)-[track:TRACKS]-(entity:Entity) where entity.key in {0} delete track, entity")
  void purgeEntities(Collection<String> entities);

  @Query(elementClass = EntityNode.class, value = "match (entity:Entity) " +
      " where id(entity)in {0} " +
      "return entity ")
  Collection<EntityNode> getEntities(Collection<Long> entities);

  @Query(value = "match (child:Entity)<-[p:parent]-(parent:Entity) where id(child) = {0} return parent")
  EntityNode findParent(Long childId);

  @Query(value = "match (child:Entity)<-[p]-(parent:Entity) where id(child) = {0} return parent")
  Collection<EntityNode> findInboundEntities(Long id);

  @Query(value = "match (entity:Entity {extKey:{0}}) return entity")
  EntityNode findByExtKey(String extKey);

  @Query(value = "match (entity:Entity {key:{0}}) return entity")
  EntityNode findByKey(String key);

}
