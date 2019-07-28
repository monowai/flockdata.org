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
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.LogNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author mholdsworth
 * @tag GraphRepository, Neo4j, Log
 * @since 14/04/2013
 */
public interface TrackLogRepo extends GraphRepository<LogNode> {

  @Query(value = "match (entity:Entity)-[cw:LOGGED]->(log:Log) where id(entity)={0} return count(cw)")
  int getLogCount(Long entityId);

  @Query(value = "match (change:Log)<-[log:LOGGED]-() where id(change)={0} " +
      "   return log")
  EntityLog getLog(Long logId);

  @Query(value = "match (change:Log)<-[log:LAST_CHANGE]-(entity:Entity) where id(entity)={0} " +
      "   return log")
  EntityLog getLastChange(Long entityId);


  @Query(elementClass = EntityLog.class, value = "match (entity)-[log:LOGGED]->(entityLog) where id(entity)={0} and log.fortressWhen >= {1} and log.fortressWhen <= {2}  return log ")
  Set<EntityLog> getLogs(Long entityId, Long from, Long to);

  @Query(elementClass = EntityLog.class, value = "match (entity)-[log:LOGGED]->(entityLog) where id(entity)={0} and log.fortressWhen <= {1} return log limit 5")
  Set<EntityLog> getLogs(Long entityId, Long from);

  @Query(elementClass = EntityLog.class, value = "  MATCH (entity:Entity)-[log:LOGGED]->(change) where id(entity) = {0} " +
      " return log order by log.fortressWhen desc")
  Set<EntityLog> findLogs(Long entityId);

  @Query(value = "match (m:Entity)-[l:LOGGED]-(log:Log) where m.key in {0} delete l,log;")
  void purgeFortressLogs(Collection<String> entities);

  @Query(value = "match (m:Entity)-[l:LOGGED]-(log:Log)-[people]-() where m.key in {0} delete l, people, log")
  void purgeLogsWithUsers(Collection<String> entities);

  //match (f:Fortress)-[track:TRACKS]->(entity:Entity)-[r]-(o:Tag) where id(f)=514705 delete r;
  @Query(value = "match (m:Entity)-[tagRlx]-(:Tag) where m.key  in {0} delete tagRlx")
  void purgeTagRelationships(Collection<String> entities);


}
