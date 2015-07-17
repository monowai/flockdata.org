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

package org.flockdata.neo4j.service;

import org.flockdata.helper.FlockException;
import org.flockdata.model.Entity;
import org.flockdata.model.EntityLog;
import org.flockdata.model.FortressUser;
import org.flockdata.model.Log;
import org.flockdata.track.EntityLogs;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by mike on 12/07/15.
 */
public class LogService {

    public static final DynamicRelationshipType LAST_CHANGE = DynamicRelationshipType.withName("LAST_CHANGE");
    public static final DynamicRelationshipType LASTCHANGED_BY = DynamicRelationshipType.withName("LASTCHANGED_BY");
    public static final DynamicRelationshipType PREVIOUS_LOG = DynamicRelationshipType.withName("PREVIOUS_LOG");

    private Logger logger = LoggerFactory.getLogger(LogService.class);

    private GraphDatabaseService database;

    LogService () {}

    public LogService(@Context GraphDatabaseService database) {
        this();
        this.database = database;
    }

    public EntityLogs getLogs(Long entityId){
        return getLogs(entityId, (Long)null);
    }

    public EntityLogs getLogs(Long entityId, Long since) {
        String cypher = "match (e:Entity)-[lc:LOGGED]->(l:Log) where id(e) = {entity} "+(since!=null? " and lc.fortressWhen <= {before}":"")+" with e, lc,l " +
                "optional match (l)-[:CHANGED]-(fu) return e,lc,l,fu";
        Map<String, Object> args = new HashMap<>();
        args.put("entity", entityId);
        if ( since!=null )
            args.put("before", since);
        Result result = database.execute(cypher, args);
        EntityLogs entityLogs = new EntityLogs();
        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            Relationship r = (Relationship) row.get("lc");
            Node lc = (Node) row.get("l");
            Entity e = new Entity((Node) row.get("e"));
            EntityLog entityLog = new EntityLog(e, r, lc);
            entityLog.getLog().setMadeBy(new FortressUser((Node)row.get("fu")));
            entityLogs.add(entityLog);
        }
        result.close();

        return entityLogs;
    }

    public EntityLog writeLog(Entity entity, Log newLog, Long fortressWhen) throws FlockException {

        if (entity.getId() == null)// Graph tracking is suppressed; caller is only creating search docs
            return null;

        if (entity.getFortress().isStoreDisabled())
            return null;

        if (entity.getMetaKey() == null)
            throw new FlockException("Where has the metaKey gone?");


        EntityLog entityLog = new EntityLog(entity, newLog, fortressWhen);
        entityLog = createLog(entityLog);
        Node entityNode = database.getNodeById(entity.getId());

        // Identify most recent log
        EntityLog historicLog = resolveHistoricLog(entity, entityLog, fortressWhen);
        setLastChange(entityNode, historicLog.getLog(), fortressWhen);
        entity.setLastChange(historicLog.getLog());

        return entityLog;
    }

    EntityLog resolveHistoricLog(Entity entity, EntityLog incomingLog, Long contentWhen) {
        // ToDo re-write log pointers if necessary to handle a sequence insertion use case
        if (incomingLog == null || incomingLog.isMocked())
            return null;

        boolean historicIncomingLog = (entity.getLastUpdate() != null && contentWhen< entity.getLastUpdate());

        logger.debug("Historic {}, {}, log {}, contentWhen {}",
                new DateTime(entity.getFortressUpdatedTz()),
                historicIncomingLog,
                new DateTime(incomingLog.getFortressWhen()),
                contentWhen);

        if (historicIncomingLog) {
            // ToTo: Optimize the number of logs we will look at
            EntityLogs logResult =getLogs(entity.getId());
            Set<EntityLog> entityLogs = logResult.getEntityLogs();
            if (entityLogs.isEmpty()) {
                logger.debug("No logs prior to {}. Returning existing log", contentWhen);
                return incomingLog;
            } else {
                logger.debug("Found {} historic logs", entityLogs.size());
                EntityLog mostRecent = null;

                for (EntityLog entityLog : entityLogs) {
                    if (mostRecent == null)
                        mostRecent = entityLog;
                    else if (entityLog.getFortressWhen() > mostRecent.getFortressWhen())
                        mostRecent = entityLog;
//                    if (entityLog.getFortressWhen().equals(contentWhen))
//                        return entityLog; // Exact match to the millis
                }

                logger.debug("return closestLog {}", mostRecent == null ? "[null]" : mostRecent.getFortressWhen());
                return mostRecent;
            }

        }
        logger.debug("return incomingLog");
        return incomingLog;
    }

    private void setLastChange(Node entityNode, Log log, Long fortressWhen) {
        Relationship r = entityNode.getSingleRelationship(LAST_CHANGE, Direction.OUTGOING);
        Node previousLog = null;
        if (r != null && r.getId() != log.getId()) {
            previousLog = r.getOtherNode(entityNode);
            r.delete();

        }

        if ( fortressWhen == null ||fortressWhen == 0l )
            entityNode.removeProperty("fortressLastWhen");
        else
            entityNode.setProperty("fortressLastWhen", fortressWhen);

        String cypher = "match (e:Entity), (log:Log)  where id(e) = {entity} and id(log) ={log} with e,log create unique (e)-[lc:LAST_CHANGE]->(log) return log  ";

        Map<String, Object> args = new HashMap<>();
        args.put("entity", entityNode.getId());
        args.put("log", log.getId());
        Result result = database.execute(cypher, args);

        if ( previousLog!=null && result.hasNext() ){
            Node logNode = (Node)result.next().get("log");
            logNode.createRelationshipTo(previousLog, DynamicRelationshipType.withName("PREVIOUS_LOG"));
            log.setPreviousLog( new Log(previousLog));
        }

        result.close();

        if (log.getMadeBy() != null) {
            Relationship lastChangedBy = entityNode.getSingleRelationship(LASTCHANGED_BY, Direction.OUTGOING);
            if (lastChangedBy != null && lastChangedBy.getEndNode().getId() == log.getMadeBy().getId())
                return;

            if (lastChangedBy != null)
                lastChangedBy.delete();

            // Set the lastChange FortressUser against the entity
            cypher = "match (e:Entity), (fu:FortressUser)  where id(fu) = {fu} and id(e) = {entity}  " +
                    "create (fu)<-[:LASTCHANGED_BY]-(e)";
            args.put("fu", log.getMadeBy().getId());
            database.execute(cypher, args);
        }
    }

    public EntityLog getLastLog(Long entityId) {
        return getLastLog(entityId, false);
    }

    private EntityLog getLastLog(Long entityId, boolean includePrevious) {

        String cypher = "match (entity:Entity)-[:LAST_CHANGE]->(log:Log)-[entityLog:LOGGED]-() where id(entity) = {entity} with entity,entityLog,log " +
                " optional match (log)<-[:CHANGED]-(fu:FortressUser) return entity,entityLog,log,fu ";
        Map<String, Object> args = new HashMap<>();
        args.put("entity", entityId);
        Result result = database.execute(cypher, args);
        EntityLog entityLog = null;

        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            Relationship logged = (Relationship) row.get("entityLog");
            Node logNode = (Node) row.get("log");
            Entity entity = new Entity((Node) row.get("entity"));

            entityLog = new EntityLog(entity, logged, logNode);
            entityLog.getLog().setMadeBy(new FortressUser((Node)row.get("fu")));
            if ( includePrevious && logNode!=null  ){
                Relationship logRlx = logNode.getSingleRelationship(PREVIOUS_LOG, Direction.OUTGOING);
                if ( logRlx !=null ) {
                    Node previousLog = logRlx.getEndNode();
                    entityLog.getLog().setPreviousLog(new Log(previousLog));
                }
            }
        }
        result.close();
        return entityLog;
    }

    private EntityLog createLog(EntityLog entityLog) {
        Map<String, Object> props = new HashMap<>();
        props.put("entityId", entityLog.getEntity().getId());
        props.put("checkSum", entityLog.getLog().getChecksum());
        props.put("comment", entityLog.getLog().getComment());
        props.put("contentType", entityLog.getLog().getContentType());
        props.put("event", entityLog.getLog().getEvent().getName());
        props.put("profileVersion", entityLog.getLog().getProfileVersion());
        props.put("storage", entityLog.getLog().getStorage());
        props.put("fortressWhen", entityLog.getFortressWhen());
        props.put("sysWhen", entityLog.getSysWhen());
        boolean madeBy = entityLog.getLog().getMadeBy() != null;
        if (madeBy)
            props.put("fu", entityLog.getLog().getMadeBy().getKey());

        String cypher = "match (e:Entity)  where id(e)= {entityId} " +
                "with e " +
                "merge  (e )-[l:LOGGED {fortressWhen:{fortressWhen}, sysWhen:{sysWhen}}]" +
                "->(log:Log {checkSum:{checkSum}, event:{event}, comment:{comment}, contentType:{contentType}, profileVersion:{profileVersion}, storage:{storage}}) " +
                "return l,log";

        Result rows = database.execute(cypher, props);
        Map<String, Object> row = rows.next();
        Relationship r = (Relationship) row.get("l");
        Node log = (Node) row.get("log");
        rows.close();
        EntityLog logResult = new EntityLog(entityLog.getEntity(), r, log);
        logResult.getLog().setMadeBy(entityLog.getLog().getMadeBy());
        if (entityLog.getLog().getMadeBy() != null) {
//            Node fu = database.getNodeById(entityLog.getLog().getMadeBy().getId());
            props.put("fu", entityLog.getLog().getMadeBy().getId());
            props.put("log", log.getId());
            cypher = "match (l:Log), (fu:FortressUser ) where id(l)={log} and id(fu) = {fu} " +
                    "  merge (fu)-[:CHANGED]->(l)";
            database.execute(cypher, props);

            //log.createRelationshipTo(fu, DynamicRelationshipType.withName("CHANGED"));
        }

        return logResult;
    }

    public TrackResultBean cancelLastLog(Entity entity){

        TrackResultBean trackResultBean = new TrackResultBean(entity);
        Node entityNode = database.getNodeById(entity.getId());


        EntityLog lastLog = getLastLog(entity.getId(), true);
        if (lastLog == null)
            return new TrackResultBean("No log found");

        Log currentLog = lastLog.getLog();


        //Log fromLog = lastLog.getLog().getPreviousLog();


        if (lastLog.getLog().getPreviousLog() != null) {
            EntityLog previousEl = getEntityLog(entity.getId(), currentLog.getPreviousLog().getId());
            trackResultBean.setDeletedLog( previousEl);

            //entityTagService.findEntityTags(company, entity);
            Node toDelete = database.getNodeById(currentLog.getId());
            for (Relationship relationship : toDelete.getRelationships()) {
                relationship.delete();
            }

            toDelete.delete();
            entity.setLastChange(previousEl.getLog());
            setLastChange(entityNode, previousEl.getLog(), previousEl.getFortressWhen());

            // FixMe
            //entityTagService.moveTags(company, fromLog, entity);

        } else {
            // No changes left, there is now just an entity
            // ToDo: What to to with the entity? Delete it? Store the "canceled By" User? Assign the log to a Cancelled RLX?
            // Delete from ElasticSearch??

            // FixMe - create a Set Last User routine
            entity.setLastUser(entity.getCreatedBy());

            entity.setFortressLastWhen(0l);
            entity.setSearchKey(null);
            entityNode.removeProperty("fortressLastWhen");
            entityNode.removeProperty("searchKey");


            Node toDelete = database.getNodeById(currentLog.getId());
            EntityLog deleteLog = new EntityLog(entity, new Log(toDelete), 0l);
            for (Relationship relationship : toDelete.getRelationships()) {
                relationship.delete();
            }

            toDelete.delete();
            trackResultBean.setDeletedLog(deleteLog);
        }
        return trackResultBean;
    }

    public EntityLog getEntityLog(Long entityId, Long logId) {
        String cypher = "match (entity:Entity)-[entityLog:LOGGED]->(log:Log) where id(log) = {logId} with entity,entityLog,log " +
                " optional match (log)<-[:CHANGED]-(fu:FortressUser) return entity,entityLog,log,fu ";

        Map<String, Object> args = new HashMap<>();
        args.put("logId", logId);
        args.put("entityId", entityId);
        Result result = database.execute(cypher, args);
        EntityLog entityLog = null;

        while (result.hasNext()) {
            Map<String, Object> row = result.next();
            Relationship logged = (Relationship) row.get("entityLog");
            Node logNode = (Node) row.get("log");
            Entity entity = new Entity((Node) row.get("entity"));

            entityLog = new EntityLog(entity, logged, logNode);
            entityLog.getLog().setMadeBy(new FortressUser((Node)row.get("fu")));
            if ( logNode!=null  ){
                Relationship logRlx = logNode.getSingleRelationship(PREVIOUS_LOG, Direction.OUTGOING);
                if ( logRlx !=null ) {
                    Node previousLog = logRlx.getEndNode();
                    entityLog.getLog().setPreviousLog(new Log(previousLog));
                }
            }
        }
        result.close();
        return entityLog;
    }

}
