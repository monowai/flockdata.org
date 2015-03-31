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

package org.flockdata.engine.track;

import org.flockdata.engine.track.model.EntityLogRelationship;
import org.flockdata.engine.track.model.LogNode;
import org.flockdata.track.model.EntityLog;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 14/04/13
 * Time: 8:00 PM
 */
public interface TrackLogRepo extends GraphRepository<LogNode> {

    @Query(value = "match (entity:Entity)-[cw:LOGGED]->(log:Log) where id(entity)={0} return count(cw)")
    int getLogCount(Long entityId);

    @Query( value = "match (change:Log)<-[log:LOGGED]-() where id(change)={0} " +
                    "   return log")
    EntityLogRelationship getLog(Long logId);

    @Query(  value = "match (change:Log)<-[log:LAST_CHANGE]-(entity:_Entity) where id(entity)={0} " +
                    "   return log")
    EntityLogRelationship getLastChange(Long entityId);


    @Query(elementClass = EntityLogRelationship.class,  value = "match (entity)-[log:LOGGED]->(entityLog) where id(entity)={0} and log.fortressWhen >= {1} and log.fortressWhen <= {2}  return log ")
    Set<EntityLog> getLogs(Long entityId, Long from, Long to);

    @Query(elementClass = EntityLogRelationship.class, value = "match (entity)-[log:LOGGED]->(entityLog) where id(entity)={0} and log.fortressWhen <= {1} return log limit 5")
    Set<EntityLog> getLogs(Long entityId, Long from );

    @Query( elementClass = EntityLogRelationship.class, value = "  MATCH (entity:Entity)-[log:LOGGED]->(change) where id(entity) = {0} " +
                    " return log order by log.fortressWhen desc")
    Set<EntityLog> findLogs(Long entityId);

    @Query (value = "match (f:Fortress)-[:TRACKS]->(m:Entity)-[l:LOGGED]-(log:Log)-[people]-() where id(f)={0} delete l, people, log")
    void purgeFortressLogs(Long fortressId);

    @Query (value = "match (f:Fortress)-[track:TRACKS]->(m:Entity)-[tagRlx]-(:Tag) where id(f)={0} delete tagRlx")
    void purgeTagRelationships(Long fortressId);



}
