/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.repo.neo4j;

import com.auditbucket.engine.repo.neo4j.model.LogNode;
import com.auditbucket.engine.repo.neo4j.model.LoggedRelationship;
import com.auditbucket.track.model.TrackLog;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 14/04/13
 * Time: 8:00 PM
 */
public interface TrackLogRepo extends GraphRepository<LogNode> {

    @Query(value = "start metaHeader=node({0}) match metaHeader-[cw:LOGGED]->log return count(cw)")
    int getLogCount(Long metaHeaderId);

    @Query(elementClass = LoggedRelationship.class,
            value = "match (change:Log)<-[log:LOGGED]-() where id(change)={0} " +
                    "   return log")
    LoggedRelationship getLastLog(Long metaHeaderId);

    @Query(elementClass = LoggedRelationship.class, value = "match (header)-[log:LOGGED]->(auditLog) where id(header)={0} and log.fortressWhen >= {1} and log.fortressWhen <= {2} return log ")
    Set<TrackLog> getLogs(Long metaHeaderId, Long from, Long to);

    @Query(elementClass = LoggedRelationship.class, value =
            "   MATCH track-[log:LOGGED]->change where id(track) = {0} " +
            "return log order by log.fortressWhen desc")
    Set<TrackLog> findLogs(Long metaHeaderId);

    @Query (value = "match (f:_Fortress)-[:TRACKS]->(m:MetaHeader)-[l:LOGGED]-(log:Log)-[people]-() where id(f)={0} delete l, people, log")
    void purgeFortressLogs(Long fortressId);

    @Query (value = "match (f:_Fortress)-[track:TRACKS]->(m:MetaHeader)-[tagRlx]-(:_Tag) where id(f)={0} delete tagRlx")
    void purgeTagRelationships(Long fortressId);


}
