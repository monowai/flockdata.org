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

package org.flockdata.engine.track;

import org.flockdata.engine.track.model.EntityNode;
import org.flockdata.track.model.Entity;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 14/04/13
 * Time: 8:00 PM
 */
public interface EntityRepo extends GraphRepository<EntityNode> {

    @Query( elementClass = EntityNode.class, value = "   MATCH tx-[:AFFECTED]->change<-[:LOGGED]-(entity:Entity) where id(tx)= {0}" +
                    "return entity")
    Set<Entity> findEntitiesByTxRef(Long txRef);

    @Query( elementClass = EntityNode.class, value=" match (fortress:Fortress)-[:TRACKS]->(track:Entity) " +
                    " where id(fortress)={0} " +
                    " return track ORDER BY track.dateCreated ASC" +
                    " skip {1} limit 100 ")
    Set<Entity> findEntities(Long fortressId, Long skip);

    @Query( elementClass = EntityNode.class, value =
            " match (fortress:Fortress)-[:TRACKS]->(entity:Entity) where id(fortress)={0} " +
                    " and entity.callerRef ={1}" +
                    " return entity ")
    Collection<Entity> findByCallerRef(Long fortressId, String callerRef);

    @Query( elementClass = EntityNode.class, value = "match (company:FDCompany), (entities:Entity) " +
            " where id(company)={0} " +
            "   and entities.metaKey in {1}  " +
            "  with company, entities match (company)-[*..2]-(entities) " +
            "return entities ")
    Collection<Entity> findEntities(Long id, Collection<String> toFind);

    @Query(value = "match (f:Fortress)-[track:TRACKS]->(meta:Entity)-[other]-(:FortressUser) where id(f)={0} delete other")
    void purgePeopleRelationships(Long fortressId);

    @Query(value = "match (f:Fortress)-[track:TRACKS]->(meta:Entity)-[otherRlx]-(:Entity) where id(f)={0} delete otherRlx")
    void purgeCrossReferences(Long fortressId);

    @Query(value = "match (f:Fortress)-[track:TRACKS]->(entity:Entity) where id(f)={0} delete track, entity ")
    void purgeEntities(Long fortressId);

    @Query( elementClass = EntityNode.class,value = "match (entity:Entity) " +
            " where id(entity)in {0} " +
            "return entity ")
    Collection<Entity> getEntities(Collection<Long> entities);
}
