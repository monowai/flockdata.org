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

import com.auditbucket.engine.repo.neo4j.model.EntityNode;
import com.auditbucket.engine.repo.neo4j.model.LogNode;
import com.auditbucket.engine.repo.neo4j.model.LoggedRelationship;
import com.auditbucket.engine.repo.neo4j.model.TxRefNode;
import com.auditbucket.engine.service.TrackEventService;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.service.KeyGenService;
import com.auditbucket.track.bean.AuditTXResult;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.model.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;

/**
 * User: Mike Holdsworth
 * Date: 21/04/13
 * Time: 8:00 PM
 */
@SuppressWarnings("SpringJavaAutowiringInspection")
@Repository("auditDAO")
public class EntityDaoNeo {
    @Autowired
    EntityRepo metaRepo;

    @Autowired
    TrackLogRepo trackLogRepo;

    @Autowired
    DocumentTypeRepo documentTypeRepo;

    @Autowired
    TrackEventService trackEventService;

    @Autowired
    KvService kvService;

    @Autowired
    KeyGenService keyGenService;

    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(EntityDaoNeo.class);

    public Entity save(Entity entity) {
        return save(entity, null);
    }

    public Entity create(EntityInputBean inputBean, Fortress fortress, DocumentType documentType) throws DatagioException {
        String metaKey = ( inputBean.isTrackSuppressed()?null:keyGenService.getUniqueKey());
        Entity entity = new EntityNode(metaKey, fortress, inputBean, documentType);

        if (! inputBean.isTrackSuppressed()) {
            logger.debug("Creating {}", entity);

            entity = save(entity, documentType);
            Node node = template.getPersistentState(entity);
            node.addLabel(DynamicLabel.label(documentType.getName()));

        }
        return entity;
    }

    public Entity save(Entity entity, DocumentType documentType) {
        entity.bumpUpdate();
        return metaRepo.save((EntityNode) entity);

    }

    public TxRef save(TxRef tagRef) {
        return template.save((TxRefNode) tagRef);
    }

    //    @Cacheable(value = "metaKey", unless = "#result==null")
    private Entity getCachedEntity(String key) {
        if (key == null)
            return null;
        return metaRepo.findBySchemaPropertyValue(EntityNode.UUID_KEY, key);
    }

    public Entity findEntity(String key, boolean inflate) {
        Entity entity = getCachedEntity(key);
        if (inflate && entity != null) {
            fetch(entity);
        }
        return entity;
    }

    public Collection<Entity> findByCallerRef(Long fortressId, String callerRef) {
        return metaRepo.findByCallerRef(fortressId, callerRef);

    }

    public Entity findByCallerRefUnique(Long fortressId, String callerRef) throws DatagioException {
        int count = 0;
        Iterable<Entity> entities = findByCallerRef(fortressId, callerRef);
        Entity result = null;
        for (Entity entity : entities) {
            count++;
            result = entity;
            if (count > 1) break;
        }
        if (count > 1)
            throw new DatagioException("Unable to find exactly one record for the callerRef [" + callerRef + "]. Found " + count);

        return result;

    }

    //@Cacheable(value = "callerKey", unless = "#result==null")
    public Entity findByCallerRef(Long fortressId, Long documentId, String callerRef) {
        if (logger.isTraceEnabled())
            logger.trace("findByCallerRef fortress [" + fortressId + "] docType[" + documentId + "], callerRef[" + callerRef + "]");

        String keyToFind = "" + fortressId + "." + documentId + "." + callerRef;
        return metaRepo.findBySchemaPropertyValue("callerKeyRef", keyToFind);
    }

    //@Cacheable(value = "headerId", key = "p0.id", unless = "#result==null")
    public Entity fetch(Entity entity) {
        template.fetch(entity.getCreatedBy());
        template.fetch(entity.getLastUser());

        return entity;
    }

    public Set<Entity> findHeadersByTxRef(Long txRef) {
        return metaRepo.findHeadersByTxRef(txRef);
    }

    public Collection<Entity> findHeaders(Long fortressId, Long skipTo) {
        return metaRepo.findHeadersFrom(fortressId, skipTo);
    }

    public Collection<Entity> findHeaders(Long fortressId, String label, Long skipTo) {
        //ToDo: Should this pass in timestamp it got to??
        String cypher = "match (f:Fortress)-[:TRACKS]->(meta:`" + label + "`) where id(f)={fortress} return meta ORDER BY meta.dateCreated ASC skip {skip} limit 100 ";
        Map<String, Object> args = new HashMap<>();
        args.put("fortress", fortressId);
        args.put("skip", skipTo);
        Result<Map<String, Object>> result = template.query(cypher, args);

        Iterator<Map<String, Object>> rows = result.iterator();
        Collection<Entity> results = new ArrayList<>();

        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            results.add(template.projectTo(row.get("meta"), EntityNode.class));
        }

        return results;
    }

    public void delete(Log currentChange) {
        trackLogRepo.delete((LogNode) currentChange);
    }

    public TxRef findTxTag(@NotEmpty String txTag, @NotNull Company company, boolean fetchHeaders) {
        return metaRepo.findTxTag(txTag, company.getId());
    }


    public TxRef beginTransaction(String id, Company company) {

        TxRef txTag = findTxTag(id, company, false);
        if (txTag == null) {
            txTag = new TxRefNode(id, company);
            template.save(txTag);
        }
        return txTag;
    }

    public int getLogCount(Long id) {
        return trackLogRepo.getLogCount(id);
    }

    public Set<TrackLog> getLogs(Long auditLogID, Date from, Date to) {
        return trackLogRepo.getLogs(auditLogID, from.getTime(), to.getTime());
    }

    public Set<TrackLog> getLogs(Long auditHeaderID) {
        return trackLogRepo.findLogs(auditHeaderID);
    }

    public Map<String, Object> findByTransaction(TxRef txRef) {
        //Example showing how to use cypher and extract

        String findByTagRef = "start tag =node({txRef}) " +
                "              match tag-[:AFFECTED]->auditLog<-[logs:LOGGED]-track " +
                "             return logs, track, auditLog " +
                "           order by logs.sysWhen";
        Map<String, Object> params = new HashMap<>();
        params.put("txRef", txRef.getId());


        Result<Map<String, Object>> exResult = template.query(findByTagRef, params);

        Iterator<Map<String, Object>> rows;
        rows = exResult.iterator();

        List<AuditTXResult> simpleResult = new ArrayList<>();
        int i = 1;
        //Result<Map<String, Object>> results =
        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            TrackLog log = template.convert(row.get("logs"), LoggedRelationship.class);
            Log change = template.convert(row.get("auditLog"), LogNode.class);
            Entity audit = template.convert(row.get("track"), EntityNode.class);
            simpleResult.add(new AuditTXResult(audit, change, log));
            i++;

        }
        Map<String, Object> result = new HashMap<>(i);
        result.put("txRef", txRef.getName());
        result.put("logs", simpleResult);

        return result;
    }

    public TrackLog save(TrackLog log) {
        logger.debug("Saving track log [{}] - Log ID [{}]", log, log.getLog().getId());
        return template.save((LoggedRelationship) log);
    }

    public String ping() {


//        Map<String, Object> ab = new HashMap<>();
//        ab.put("name", "AuditBucket");
//        ArrayList labels = new ArrayList();
//        labels.add("ABPing");
//        Node abNode = template.getGraphDatabase().getOrCreateNode("system", "name", "ab", "AuditBucket", ab, labels);
//        if (abNode == null) {
//            return "Neo4J has problems";
//        }
        return "Neo4J is OK";
    }

    public Log prepareLog(FortressUser fUser, LogInputBean input, TxRef txRef, Log previousChange) throws DatagioException {
        ChangeEvent event = trackEventService.processEvent(fUser.getFortress().getCompany(), input.getEvent());
        Log changeLog = new LogNode(fUser, input, txRef);
        changeLog.setEvent(event);
        changeLog.setPreviousLog(previousChange);
        try {
            changeLog = kvService.prepareLog(changeLog, input.getWhat());
        } catch (IOException e) {
            throw new DatagioException("Unexpected error talking to What Service", e);
        }
        return changeLog;
    }

//    @Cacheable(value = "trackLog", unless = "#result==null")
    public TrackLog getLog(Long logId) {
        Relationship change = template.getRelationship(logId);
        if (change != null)
            try {
                return (TrackLog) template.getDefaultConverter().convert(change, LoggedRelationship.class);
            } catch (NotFoundException nfe) {
                // Occurs if ab-search has been down and the database is out of sync from multiple restarts
                logger.error("Error converting relationship to a LoggedRelationship");
                return null;
            }
        return null;
    }


    //    @Cacheable(value = "headerId", unless = "#result==null")
    public Entity getHeader(Long id) {
        return template.findOne(id, EntityNode.class);
    }

    public Log fetch(Log lastChange) {
        return template.fetch(lastChange);
    }

    public TrackLog addLog(Entity entity, Log newChange, DateTime fortressWhen, TrackLog existingLog) {

        newChange.setTrackLog(new LoggedRelationship(entity, newChange, fortressWhen));

        if (entity.getId() == null)// This occurs when tracking in ab-engine is suppressed and the caller is only creating search docs
            return newChange.getTrackLog();

        if ( entity.getLastChange()!=null )
            entity = template.fetch(entity);

        //template.fetch(newChange.getTrackLog());
        boolean moreRecent = (existingLog == null || existingLog.getFortressWhen() <= fortressWhen.getMillis());
        if (moreRecent) {
            if (entity.getLastUser() == null || (!entity.getLastUser().getId().equals(newChange.getWho().getId()))) {
                entity.setLastUser(newChange.getWho());
            }
            entity.setFortressLastWhen(fortressWhen.getMillis());
            entity.setLastChange(newChange);

            logger.debug("Detected "+(existingLog != null ? " a more recent change ": " the first log ")+" for entity {}. Giving this log most recent status.", entity.getId());
            try {
                template.save(entity);
            } catch (IllegalStateException e) {
                logger.error("ISE saving Entity {}", new Date(newChange.getTrackLog().getSysWhen()));
                logger.error("Unexpected", e);
            }
            logger.debug("Saved change for Entity [{}], logid [{}]", entity.getId(), newChange.getId());

        } else {
            newChange = template.save(newChange);
        }
        logger.debug("Added Log - Entity [{}], Log [{}], Change [{}]", entity.getId(), newChange.getTrackLog(), newChange.getId());
        return newChange.getTrackLog();
    }

    public void crossReference(Entity header, Collection<Entity> entities, String refName) {
        Node source = template.getPersistentState(header);
        for (Entity entity : entities) {
            Node dest = template.getPersistentState(entity);
            if ( template.getRelationshipBetween(source,entity, refName)== null )
                template.createRelationshipBetween(source, dest, refName, null);
        }
    }

    public Map<String, Collection<Entity>> getCrossReference(Company company, Entity entity, String xRefName) {
        Node n = template.getPersistentState(entity);

        RelationshipType r = DynamicRelationshipType.withName(xRefName);
        Iterable<Relationship> rlxs = n.getRelationships(r);
        Map<String, Collection<Entity>> results = new HashMap<>();
        Collection<Entity> headers = new ArrayList<>();
        results.put(xRefName, headers);
        for (Relationship rlx : rlxs) {
            headers.add(template.projectTo(rlx.getEndNode(), EntityNode.class));
        }
        return results;

    }

    public Map<String, Entity> findEntities(Company company, Collection<String> metaKeys) {
        logger.debug("Looking for {} headers for company [{}] ", metaKeys.size(), company);
        Collection<Entity> foundHeaders = metaRepo.findHeaders(company.getId(), metaKeys);
        Map<String, Entity> unsorted = new HashMap<>();
        for (Entity foundHeader : foundHeaders) {
            unsorted.put(foundHeader.getMetaKey(), foundHeader);
        }
        return unsorted;
    }

    public void purgeTagRelationships(Fortress fortress) {
        // ToDo: Check if this works with huge datasets
        trackLogRepo.purgeTagRelationships(fortress.getId());
    }

    public void purgeFortressLogs(Fortress fortress) {
        trackLogRepo.purgeFortressLogs(fortress.getId());
    }

    public void purgePeopleRelationships(Fortress fortress) {
        metaRepo.purgePeopleRelationships(fortress.getId());
    }

    public void purgeEntities(Fortress fortress) {
        metaRepo.purgeCrossReferences(fortress.getId());
        metaRepo.purgeHeaders(fortress.getId());
    }

    public void purgeFortressDocuments(Fortress fortress) {
        documentTypeRepo.purgeFortressDocuments(fortress.getId());
    }

    public TrackLog getLastLog(Long entityId) {
        Entity entity = getHeader(entityId);
        Log lastChange = entity.getLastChange();
        if (lastChange == null)
            return null;

        return trackLogRepo.getLog(lastChange.getId());
    }


}
