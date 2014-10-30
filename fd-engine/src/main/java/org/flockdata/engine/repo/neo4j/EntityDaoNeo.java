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

package org.flockdata.engine.repo.neo4j;

import org.flockdata.engine.repo.neo4j.model.EntityNode;
import org.flockdata.engine.repo.neo4j.model.LogNode;
import org.flockdata.engine.repo.neo4j.model.TxRefNode;
import org.flockdata.engine.service.TrackEventService;
import org.flockdata.kv.service.KvService;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.FortressUser;
import org.flockdata.registration.service.KeyGenService;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityTXResult;
import org.flockdata.engine.repo.neo4j.model.LoggedRelationship;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Company;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.*;
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
@Repository("entityDao")
public class EntityDaoNeo {
    @Autowired
    EntityRepo entityRepo;

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

    public Entity create(EntityInputBean inputBean, FortressUser fortressUser, DocumentType documentType) throws FlockException {
        String metaKey = ( inputBean.isTrackSuppressed()?null:keyGenService.getUniqueKey());
        Entity entity = new EntityNode(metaKey, fortressUser.getFortress(), inputBean, documentType);
        entity.setCreatedBy(fortressUser);
        entity.addLabel(documentType.getName());
        if (! inputBean.isTrackSuppressed()) {
            logger.debug("Creating {}", entity);
            entity = save(entity);
        }
        return entity;
    }

    public Entity save(Entity entity) {
        return save(entity, false);
    }

    /**
     *
     * @param entity   to save
     * @param quietly  if we're doing this quietly then lastUpdate is not changed
     * @return saved entity
     */
    public Entity save(Entity entity, boolean quietly) {
        if (!quietly)
            entity.bumpUpdate();
        return entityRepo.save((EntityNode) entity);

    }

    public TxRef save(TxRef tagRef) {
        return template.save((TxRefNode) tagRef);
    }

    //    @Cacheable(value = "metaKey", unless = "#result==null")
    private Entity getCachedEntity(String key) {
        if (key == null)
            return null;
        return entityRepo.findBySchemaPropertyValue(EntityNode.UUID_KEY, key);
    }

    public Entity findEntity(String key, boolean inflate) {
        Entity entity = getCachedEntity(key);
        if (inflate && entity != null) {
            fetch(entity);
        }
        return entity;
    }

    public Collection<Entity> findByCallerRef(Long fortressId, String callerRef) {
        return entityRepo.findByCallerRef(fortressId, callerRef);

    }

    public Entity findByCallerRefUnique(Long fortressId, String callerRef) throws FlockException {
        int count = 0;
        Iterable<Entity> entities = findByCallerRef(fortressId, callerRef);
        Entity result = null;
        for (Entity entity : entities) {
            count++;
            result = entity;
            if (count > 1) break;
        }
        if (count > 1)
            throw new FlockException("Unable to find exactly one record for the callerRef [" + callerRef + "]. Found " + count);

        return result;

    }

    //@Cacheable(value = "callerKey", unless = "#result==null")
    public Entity findByCallerRef(Long fortressId, Long documentId, String callerRef) {
        if (logger.isTraceEnabled())
            logger.trace("findByCallerRef fortressUser [" + fortressId + "] docType[" + documentId + "], callerRef[" + callerRef + "]");

        String keyToFind = "" + fortressId + "." + documentId + "." + callerRef;
        return entityRepo.findBySchemaPropertyValue("callerKeyRef", keyToFind);
    }

    //@Cacheable(value = "entityId", key = "p0.id", unless = "#result==null")
    public Entity fetch(Entity entity) {
        template.fetch(entity.getCreatedBy());
        template.fetch(entity.getLastUser());

        return entity;
    }

    public Set<Entity> findEntitiesByTxRef(Long txRef) {
        return entityRepo.findEntitiesByTxRef(txRef);
    }

    public Collection<Entity> findEntities(Long fortressId, Long skipTo) {
        return entityRepo.findEntities(fortressId, skipTo);
    }

    public Collection<Entity> findEntities(Long fortressId, String label, Long skipTo) {
        //ToDo: Should this pass in timestamp it got to??
        String cypher = "match (f:Fortress)-[:TRACKS]->(meta:`" + label + "`) where id(f)={fortressUser} return meta ORDER BY meta.dateCreated ASC skip {skip} limit 100 ";
        Map<String, Object> args = new HashMap<>();
        args.put("fortressUser", fortressId);
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

    public TxRef findTxTag(@NotEmpty String txTag, @NotNull Company company) {
        return entityRepo.findTxTag(txTag, company.getId());
    }


    public TxRef beginTransaction(String id, Company company) {

        TxRef txTag = findTxTag(id, company);
        if (txTag == null) {
            txTag = new TxRefNode(id, company);
            template.save(txTag);
        }
        return txTag;
    }

    public int getLogCount(Long id) {
        return trackLogRepo.getLogCount(id);
    }

    public Set<EntityLog> getLogs(Long entityId, Date from, Date to) {
        return trackLogRepo.getLogs(entityId, from.getTime(), to.getTime());
    }

    public Set<EntityLog> getLogs(Long entityId) {
        return trackLogRepo.findLogs(entityId);
    }

    public Map<String, Object> findByTransaction(TxRef txRef) {
        //Example showing how to use cypher and extract

        String findByTagRef = "start tag =node({txRef}) " +
                "              match tag-[:AFFECTED]->log<-[logs:LOGGED]-track " +
                "             return logs, track, log " +
                "           order by logs.sysWhen";
        Map<String, Object> params = new HashMap<>();
        params.put("txRef", txRef.getId());


        Result<Map<String, Object>> exResult = template.query(findByTagRef, params);

        Iterator<Map<String, Object>> rows;
        rows = exResult.iterator();

        List<EntityTXResult> simpleResult = new ArrayList<>();
        int i = 1;
        //Result<Map<String, Object>> results =
        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            EntityLog log = template.convert(row.get("logs"), LoggedRelationship.class);
            Log change = template.convert(row.get("log"), LogNode.class);
            Entity entity = template.convert(row.get("track"), EntityNode.class);
            simpleResult.add(new EntityTXResult(entity, change, log));
            i++;

        }
        Map<String, Object> result = new HashMap<>(i);
        result.put("txRef", txRef.getName());
        result.put("logs", simpleResult);

        return result;
    }

    public EntityLog save(EntityLog log) {
        logger.debug("Saving track log [{}] - Log ID [{}]", log, log.getLog().getId());
        return template.save((LoggedRelationship) log);
    }

    public String ping() {


        return "Neo4J is OK";
    }

    public Log prepareLog(FortressUser fUser, ContentInputBean entityContent, TxRef txRef, Log previousChange) throws FlockException {
        ChangeEvent event = trackEventService.processEvent(fUser.getFortress().getCompany(), entityContent.getEvent());
        Log changeLog = new LogNode(fUser, entityContent, txRef);
        changeLog.setEvent(event);
        changeLog.setPreviousLog(previousChange);
        try {
            changeLog = kvService.prepareLog(changeLog, entityContent);
        } catch (IOException e) {
            throw new FlockException("Unexpected error talking to What Service", e);
        }
        return changeLog;
    }

//    @Cacheable(value = "trackLog", unless = "#result==null")
    public EntityLog getLog(Long logId) {
        Relationship change = template.getRelationship(logId);
        if (change != null)
            try {
                return (EntityLog) template.getDefaultConverter().convert(change, LoggedRelationship.class);
            } catch (NotFoundException nfe) {
                // Occurs if fd-search has been down and the database is out of sync from multiple restarts
                logger.error("Error converting relationship to a LoggedRelationship");
                return null;
            }
        return null;
    }


    //    @Cacheable(value = "entityId", unless = "#result==null")
    public Entity getEntity(Long pk) {
        return entityRepo.findOne(pk);
        //return template.findOne(pk, EntityNode.class);
    }

    public Log fetch(Log lastChange) {
        return template.fetch(lastChange);
    }

    public EntityLog addLog(Entity entity, Log newChange, DateTime fortressWhen, EntityLog existingLog) {

        newChange.setTrackLog(new LoggedRelationship(entity, newChange, fortressWhen));

        if (entity.getId() == null)// This occurs when tracking in fd-engine is suppressed and the caller is only creating search docs
            return newChange.getEntityLog();

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
                logger.error("ISE saving Entity {}", new Date(newChange.getEntityLog().getSysWhen()));
                logger.error("Unexpected", e);
            }
            logger.debug("Saved change for Entity [{}], logid [{}]", entity.getId(), newChange.getId());

        } else {
            newChange = template.save(newChange);
        }
        logger.debug("Added Log - Entity [{}], Log [{}], Change [{}]", entity.getId(), newChange.getEntityLog(), newChange.getId());
        return newChange.getEntityLog();
    }

    public void crossReference(Entity entity, Collection<Entity> entities, String refName) {
        Node source = template.getPersistentState(entity);
        for (Entity sourceEntity : entities) {
            Node dest = template.getPersistentState(sourceEntity);
            if ( template.getRelationshipBetween(source, sourceEntity, refName)== null )
                template.createRelationshipBetween(source, dest, refName, null);
        }
    }

    public Map<String, Collection<Entity>> getCrossReference(Entity entity, String xRefName) {
        Node n = template.getPersistentState(entity);

        RelationshipType r = DynamicRelationshipType.withName(xRefName);
        Iterable<Relationship> rlxs = n.getRelationships(r);
        Map<String, Collection<Entity>> results = new HashMap<>();
        Collection<Entity> entities = new ArrayList<>();
        results.put(xRefName, entities);
        for (Relationship rlx : rlxs) {
            entities.add(template.projectTo(rlx.getEndNode(), EntityNode.class));
        }
        return results;

    }

    public Map<String, Entity> findEntities(Company company, Collection<String> metaKeys) {
        logger.debug("Looking for {} entities for company [{}] ", metaKeys.size(), company);
        Collection<Entity> foundEntities = entityRepo.findEntities(company.getId(), metaKeys);
        Map<String, Entity> unsorted = new HashMap<>();
        for (Entity foundEntity : foundEntities) {
            unsorted.put(foundEntity.getMetaKey(), foundEntity);
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
        entityRepo.purgePeopleRelationships(fortress.getId());
    }

    public void purgeEntities(Fortress fortress) {
        entityRepo.purgeCrossReferences(fortress.getId());
        entityRepo.purgeEntities(fortress.getId());
    }

    public void purgeFortressDocuments(Fortress fortress) {
        documentTypeRepo.purgeFortressDocuments(fortress.getId());
    }

    public EntityLog getLastLog(Long entityId) {
        Entity entity = getEntity(entityId);
        Log lastChange = entity.getLastChange();
        if (lastChange == null)
            return null;

        return trackLogRepo.getLog(lastChange.getId());
    }


    public Set<EntityLog> getLogs(Long id, Date date) {
        return trackLogRepo.getLogs(id, date.getTime());
    }
}
