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

package org.flockdata.engine.dao;

import org.flockdata.model.Entity;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 14/04/13
 * Time: 8:00 PM
 */
public interface EntityRepo extends GraphRepository<Entity> {

    @Query( elementClass = Entity.class, value = "   MATCH tx-[:AFFECTED]->change<-[:LOGGED]-(entity:Entity) where id(tx)= {0}" +
                    "return entity")
    Set<Entity> findEntitiesByTxRef(Long txRef);

    @Query( elementClass = Entity.class, value=" match (fortress:Fortress)-[:TRACKS]->(track:Entity) " +
                    " where id(fortress)={0} " +
                    " return track ORDER BY track.dateCreated ASC" +
                    " skip {1} limit 100 ")
    Set<Entity> findEntities(Long fortressId, Long skip);

    @Query(  value=" match (fortress:Fortress)-[:TRACKS]->(track:Entity) " +
            " where id(fortress)={0} " +
            " return track.metaKey " +
            " limit {1} ")
    Collection<String> findEntitiesWithLimit(Long id, int limit);

    @Query( elementClass = Entity.class, value =
            " match (fortress:Fortress)-[:TRACKS]->(entity:Entity) where id(fortress)={0} " +
                    " and entity.callerRef ={1}" +
                    " return entity ")
    Collection<Entity> findByCallerRef(Long fortressId, String callerRef);

//    @Query( elementClass = EntityNode.class, value = "match (c:FDCompany) where id(c)={0} with c match (entities:Entity)-[]-(:Fortress)-[]-(c) " +
//            " where  entities.metaKey in {1}  " +
//            "return entities ")

    @Query( elementClass = Entity.class, value = "match  (entities:Entity) " +
            " where  entities.metaKey in {1}  " +
            "return entities ")
    Collection<Entity> findEntities(Long id, Collection<String> toFind);

    @Query(value = "match (meta:Entity)-[other]-(:FortressUser) where meta.metaKey in{0} delete other")
    void purgePeopleRelationships(Collection<String> entities);

    @Query(value = "match (meta:Entity)-[otherRlx]-(:Entity) where meta.metaKey in {0} delete otherRlx")
    void purgeCrossReferences(Collection<String> entities);

    @Query(value = "match (f:Fortress)-[track:TRACKS]->(entity:Entity) where entity.metaKey in {0} delete track, entity ")
    void purgeEntities(Collection<String> entities);

    @Query( elementClass = Entity.class,value = "match (entity:Entity) " +
            " where id(entity)in {0} " +
            "return entity ")
    Collection<Entity> getEntities(Collection<Long> entities);

}
