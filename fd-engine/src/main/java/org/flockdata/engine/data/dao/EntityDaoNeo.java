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

import static org.flockdata.store.StoreHelper.isMockable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.flockdata.data.Company;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.data.Log;
import org.flockdata.data.Segment;
import org.flockdata.data.TxRef;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.FortressUserNode;
import org.flockdata.engine.data.graph.LogNode;
import org.flockdata.engine.data.graph.TxRefNode;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.engine.track.service.EntityTagService;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.engine.track.service.TrackEventService;
import org.flockdata.helper.FlockException;
import org.flockdata.integration.IndexManager;
import org.flockdata.integration.KeyGenService;
import org.flockdata.store.Store;
import org.flockdata.store.StoreHelper;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.EntityTXResult;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Access to Entity objects stored in Neo4j
 *
 * @author mholdsworth
 * @tag neo4j, Entity, Track
 * @since 21/04/2013
 */
@Repository("entityDao")
@Transactional
public class EntityDaoNeo {

  private final EntityRepo entityRepo;

  private final PlatformConfig engineConfig;

  private final TxRepo txRepo;

  private final TrackLogRepo trackLogRepo;

  private final DocumentTypeRepo documentTypeRepo;

  private final TrackEventService trackEventService;

  private final KeyGenService keyGenService;

  private final EntityTagService entityTagService;

  private final IndexManager indexManager;
  private final Neo4jTemplate template;
  private final EntityTagInRepo entityTagInRepo;
  private final EntityTagOutRepo entityTagOutRepo;
  private FortressService fortressService;
  private ConceptService conceptService;

  private Logger logger = LoggerFactory.getLogger(EntityDaoNeo.class);

  @Autowired
  public EntityDaoNeo(EntityRepo entityRepo, KeyGenService keyGenService, TrackEventService trackEventService, DocumentTypeRepo documentTypeRepo, EntityTagService entityTagService, TxRepo txRepo,
                      TrackLogRepo trackLogRepo, IndexManager indexManager, @Qualifier("engineConfig") PlatformConfig engineConfig, Neo4jTemplate template, EntityTagInRepo entityTagInRepo, EntityTagOutRepo entityTagOutRepo) {
    this.entityRepo = entityRepo;
    this.keyGenService = keyGenService;
    this.trackEventService = trackEventService;
    this.documentTypeRepo = documentTypeRepo;
    this.entityTagService = entityTagService;
    this.txRepo = txRepo;
    this.trackLogRepo = trackLogRepo;
    this.indexManager = indexManager;
    this.engineConfig = engineConfig;
    this.template = template;
    this.entityTagInRepo = entityTagInRepo;
    this.entityTagOutRepo = entityTagOutRepo;
  }

  @Autowired
  private void setFortressService(FortressService fortressService) {
    this.fortressService = fortressService;
  }

  @Autowired
  private void setConceptService(ConceptService conceptService) {
    this.conceptService = conceptService;
  }

  public EntityNode create(EntityInputBean inputBean, Segment segment, FortressUserNode fortressUser, DocumentNode documentType) throws FlockException {
    Boolean trackEnabled = isTrackEnabled(documentType, inputBean);
    String key = (trackEnabled ? keyGenService.getUniqueKey() : null);
    EntityNode entity = new EntityNode(key, segment, inputBean, documentType);

    entity.setIndexName(indexManager.toIndex(entity));
    entity.setCreatedBy(fortressUser);
    entity.addLabel(documentType.getName());

    if (trackEnabled) {
      logger.debug("Creating {}", entity);
      entity = save(entity);
      // ToDo: Track the meta structure!
    }
    return entity;
  }

  private Boolean isTrackEnabled(DocumentNode documentType, EntityInputBean inputBean) {
    return (documentType.isTrackEnabled() != null && documentType.isTrackEnabled()) || !inputBean.isTrackSuppressed();
  }

  public EntityNode save(EntityNode entity) {
    return save(entity, false);
  }

  /**
   * @param entity  to saveFortressContentType
   * @param quietly if we're doing this quietly then lastUpdate is not changed
   * @return saved entity
   */
  public EntityNode save(EntityNode entity, boolean quietly) {
    if (!quietly) {
      entity.bumpUpdate();
    }
    return entityRepo.save(entity);

  }

  public TxRefNode save(TxRefNode tagRef) {
    return txRepo.save(tagRef);
  }

  public EntityNode findEntity(String key, boolean inflate) {
    if (key == null) {
      return null;
    }

    EntityNode entity = entityRepo.findByKey(key);
    if (inflate && entity != null) {
      fetch(entity);

    }
    return entity;
  }

  public Collection<Entity> findByCode(Long fortressId, String code) {
    return entityRepo.findByCode(fortressId, code);

  }

  public Entity findByCodeUnique(Long fortressId, String code) throws FlockException {
    int count = 0;
    Iterable<Entity> entities = findByCode(fortressId, code);
    Entity result = null;
    for (Entity entity : entities) {
      count++;
      result = entity;
      if (count > 1) {
        break;
      }
    }
    if (count > 1) {
      throw new FlockException("Unable to find exactly one record for the code [" + code + "]. Found " + count);
    }

    return result;

  }

  //    @Cacheable (value = "entityByCode", unless = "#result == null")
  public Entity findByCode(Long fortressId, Document document, String code) {
    if (logger.isTraceEnabled()) {
      logger.trace("findByCode fortressUser [" + fortressId + "] docType[" + document + "], code[" + code + "]");
    }

    Entity result;
    if (document == null || document.getName().equalsIgnoreCase("entity")) {
      // Locating an entity with a caller key across doctypes. This can return multiples of course
      // so caller should be aware of this. Current default action is to use the first found
      logger.debug("Locating by code {} in fortress {}", code, fortressId);
      Collection<Entity> entities = entityRepo.findByCode(fortressId, code);
      logger.debug("found {} results", entities.size());

      if (entities.size() == 1) {
        result = entities.iterator().next();
      } else if (entities.size() > 1) {
        logger.error("Multiple Entity objects found in the fortress {} for code {}. Returning the first", fortressId, code);
        result = entities.iterator().next();
      } else {
        // nothing found;
        result = null;
      }

    } else {
      String extKey = "" + fortressId + "." + document.getId() + "." + code;
      //Entity result= entityRepo.findBySchemaPropertyValue(EXT_KEY, keyToFind);
      result = entityRepo.findByExtKey(extKey);

    }
    return fetch(result);
  }

  public Entity fetch(Entity entity) {
    if (entity != null) {
      if (entity.getCreatedBy() != null) {
        template.fetch(entity.getCreatedBy());
      }
      if (entity.getLastUpdate() != null) {
        template.fetch(entity.getLastUser());
      }
    }

    return entity;
  }

  public Set<EntityNode> findEntitiesByTxRef(Long txRef) {
    return entityRepo.findEntitiesByTxRef(txRef);
  }

  public Collection<Entity> findEntities(Long fortressId, Long lastEntityId) {
    return entityRepo.findEntities(fortressId, lastEntityId);
  }

  public Collection<Entity> findEntities(Long fortressId, String label, Long skipTo) {
    //ToDo: Should this pass in timestamp it got to??
    String cypher = "match (f:Fortress)-[:DEFINES]-(s:FortressSegment)-[:TRACKS]->(meta:`" + label + "`) where id(f)={fortress} return meta ORDER BY meta.dateCreated ASC skip {skip} limit 100 ";
    Map<String, Object> args = new HashMap<>();
    args.put("fortress", fortressId);
    args.put("skip", skipTo);
    Iterable<Map<String, Object>> result = template.query(cypher, args);

    Iterator<Map<String, Object>> rows = result.iterator();
    Collection<Entity> results = new ArrayList<>();

    while (rows.hasNext()) {
      Map<String, Object> row = rows.next();
      results.add(template.projectTo(row.get("meta"), EntityNode.class));
    }

    return results;
  }

  public void delete(LogNode currentChange) {
    trackLogRepo.delete(currentChange);
  }

  public TxRefNode findTxTag(String txTag, Company company) {
    return txRepo.findTxTag(txTag, company.getId());
  }

  public TxRef beginTransaction(String id, CompanyNode company) {

    TxRefNode txTag = findTxTag(id, company);
    if (txTag == null) {
      txTag = new TxRefNode(id, company);
      txRepo.save(txTag);
    }
    return txTag;
  }

  public Set<EntityLog> getLogs(Long entityId, Date from, Date to) {
    return trackLogRepo.getLogs(entityId, from.getTime(), to.getTime());
  }

  public Collection<org.flockdata.data.EntityLog> getLogs(Entity entity) {
    DocumentNode doc = conceptService.findDocumentType(entity.getFortress(), entity.getType(), false);
    if (isMockable(entity, doc)) {
      return mockLogs(entity, doc);
    }

    Set<EntityLog> found = trackLogRepo.findLogs(entity.getId());
    if (found.isEmpty()) {
      return mockLogs(entity, doc);
    }
    Collection<org.flockdata.data.EntityLog> results = new ArrayList<>();
    for (EntityLog result : found) {
      result.setEntity((EntityNode) entity);
      results.add(result);
    }
    return results;
  }

  private Collection<org.flockdata.data.EntityLog> mockLogs(Entity entity, DocumentNode documentType) {
//        DocumentNode documentType = conceptService.findDocumentType(entity.getFortress(), entity.getType(), false);
    boolean mock = isMockable(entity, documentType);
    if (mock) {
      EntityLog mockLog = getMockLog(entity);
      if (mockLog != null) {
        Set<org.flockdata.data.EntityLog> results = new HashSet<>();
        results.add(mockLog);
        return results;
      }
    }
    return new ArrayList<>();
  }


  private EntityLog getMockLog(Entity entity) {
    // DAT-349 returns a mock log if storage history is not being maintained by a KV impl
    DocumentNode documentType = conceptService.findDocumentType(entity.getFortress(), entity.getType(), false);
    if (isMockable(entity, documentType)) {
      LogNode log = new LogNode(entity);
      return new EntityLog(entity, log, entity.getFortressCreatedTz());
    }
    return null;
  }

  public Map<String, Object> findByTransaction(TxRef txRef) {
    //Example showing how to use cypher and extract

    String findByTagRef = " match (tag)-[:AFFECTED]->log<-[logs:LOGGED]-(track) " +
        "              where id(tag)={txRef}" +
        "             return logs, track, log " +
        "           order by logs.sysWhen";
    Map<String, Object> params = new HashMap<>();
    params.put("txRef", txRef.getId());


    Iterable<Map<String, Object>> exResult = template.query(findByTagRef, params);

    Iterator<Map<String, Object>> rows;
    rows = exResult.iterator();

    List<EntityTXResult> simpleResult = new ArrayList<>();
    int i = 1;
    //Iterable<Map<String, Object>> results =
    while (rows.hasNext()) {
      Map<String, Object> row = rows.next();
      EntityLog log = template.convert(row.get("logs"), EntityLog.class);
      Log change = template.convert(row.get("log"), LogNode.class);
      EntityNode entity = template.convert(row.get("track"), EntityNode.class);
      simpleResult.add(new EntityTXResult(entity, log));
      i++;

    }
    Map<String, Object> result = new HashMap<>(i);
    result.put("txRef", txRef.getName());
    result.put("logs", simpleResult);

    return result;
  }

  public org.flockdata.data.EntityLog save(EntityLog log) {
    // DAT-349 - don't persist mocked logs
    if (log.isMocked()) {
      return log;
    }
    logger.debug("Saving track log [{}] - Log ID [{}]", log, log.getLog().getId());
    return template.save(log);
  }

  public String ping() {
    return "Neo4J is OK";
  }

  public EntityLog getLog(EntityNode entity, Long logId) {

    EntityLog mockLog = getMockLog(entity);
    if (mockLog != null) {
      return mockLog;
    }

    Relationship change = template.getRelationship(logId);
    if (change != null) {
      try {
        return (EntityLog) template.getDefaultConverter().convert(change, EntityLog.class);
      } catch (NotFoundException nfe) {
        // Occurs if fd-search has been down and the database is out of sync from multiple restarts
        logger.error("Error converting relationship to a LoggedRelationship");
        return null;
      }
    }
    return null;
  }

  public EntityNode getEntity(Long pk) {
    return entityRepo.findOne(pk);
    //return template.findOne(pk, EntityNode.class);
  }

  public Log fetch(LogNode lastChange) {
    if (lastChange.getId() == null || lastChange.isMocked()) {
      return lastChange;
    }
    return template.fetch(lastChange);
  }

  public EntityLog writeLog(TrackResultBean trackResultBean, Log newLog, DateTime fortressWhen) throws FlockException {
    EntityNode entity = (EntityNode) trackResultBean.getEntity();
    Store store = StoreHelper.resolveStore(trackResultBean, engineConfig.store());
    EntityLog entityLog = new EntityLog(trackResultBean, store, newLog, fortressWhen);

    if (entity.getId() == null)// Graph tracking is suppressed; caller is only creating search docs
    {
      return entityLog;
    }

    if (store.equals(Store.NONE)) {
      return entityLog;
    }

    logger.debug(entity.getKey());

    EntityNode currentState = template.fetch(entity);
    // Entity is being committed in another thread? On occasion key is null on refresh meaning the Log does not get created
    // Easiest way to test is when there is not fortress and you track the first request in to it.
    // DAT-419

    while (currentState.getKey() == null) {
      currentState = template.fetch(entity);
    }

    if (entity.getKey() == null) {
      throw new FlockException("Where has the key gone?");
    }

    entity.setLastUser((FortressUserNode) newLog.getMadeBy());
    entity.setLastChange((LogNode) newLog);
    entity.setFortressLastWhen(fortressWhen.getMillis());

    if (currentState.getLastChange() == null) {
      template.save(entity);
    } else {
      logger.debug("About to saveFortressContentType new log");
      template.save(newLog);
      template.save(entityLog);
      logger.debug("Saved new log");
      setLatest(entity);
    }

    logger.debug("Added Log - Entity [{}], Log [{}], Change [{}]", entity.getId(), newLog.getEntityLog(), newLog.getId());
    // Saving the entity causes the Log properties to be lazy initialised. If the caller wants these, then they need to fetch the object
    return entityLog;
  }

  private void setLatest(EntityNode entity) {
    org.flockdata.data.EntityLog latest = null;
    boolean moreRecent;


    Set<EntityLog> entityLogs = getLogs(entity.getId(), entity.getFortressUpdatedTz().toDate(), new DateTime().toDate());


    for (org.flockdata.data.EntityLog entityLog : entityLogs) {
      if (latest == null || entityLog.getFortressWhen() > latest.getFortressWhen()) {
        latest = entityLog;
      }
    }
    if (latest == null) {
      return;
    }
    moreRecent = (latest.getLog().getEntityLog().getFortressWhen() > entity.getFortressUpdatedTz().getMillis());
    if (moreRecent) {
      logger.debug("Detected a more recent change ", new DateTime(latest.getFortressWhen()), entity.getId(), latest.getFortressWhen());

      entity.setLastChange((LogNode) latest.getLog());
      entity.setLastUser((FortressUserNode) latest.getLog().getMadeBy());
      entity.setFortressLastWhen(latest.getFortressWhen());
    }


    entity = entityRepo.save(entity);
    logger.debug("Saved change for Entity [{}], log [{}]", entity.getId(), latest);

  }

  public void linkEntities(Entity entity, Collection<Entity> entities, String refName) {
    Node target = template.getPersistentState(entity);
    for (Entity sourceEntity : entities) {
      Node dest = template.getPersistentState(sourceEntity);
      if (template.getRelationshipBetween(dest, target, refName) == null) {
        template.createRelationshipBetween(dest, target, refName, null);
      }
    }
  }

  public Map<String, Collection<EntityNode>> getCrossReference(Entity entity, String xRefName) {
    Node n = template.getPersistentState(entity);

    RelationshipType r = DynamicRelationshipType.withName(xRefName);
    Iterable<Relationship> rlxs = n.getRelationships(r);
    Map<String, Collection<EntityNode>> results = new HashMap<>();
    Collection<EntityNode> entities = new ArrayList<>();
    results.put(xRefName, entities);
    for (Relationship rlx : rlxs) {
      entities.add(template.projectTo(rlx.getOtherNode(n), EntityNode.class));
    }
    return results;

  }

  public Map<String, EntityNode> findEntities(Company company, Collection<String> keys) {
    logger.debug("Looking for {} entities for company [{}] ", keys.size(), company);
    Collection<EntityNode> foundEntities = entityRepo.findEntities(company.getId(), keys);
    Map<String, EntityNode> unsorted = new HashMap<>();
    for (EntityNode foundEntity : foundEntities) {
      if (foundEntity.getSegment().getFortress().getCompany().getId().equals(company.getId())) {
        unsorted.put(foundEntity.getKey(), foundEntity);
      }
    }
    return unsorted;
  }

  @Transactional
  public void purgeTagRelationships(Collection<String> entities) {
    // ToDo: Check if this works with huge datasets - it' doesn't fix via batch
    trackLogRepo.purgeTagRelationships(entities);
  }

  @Transactional
  public void purgeFortressLogs(Collection<String> entities) {
    trackLogRepo.purgeLogsWithUsers(entities);
    trackLogRepo.purgeFortressLogs(entities);
  }

  @Transactional
  public void purgePeopleRelationships(Collection<String> entities) {
    entityRepo.purgePeopleRelationships(entities);
  }

  @Transactional
  public void purgeEntities(Collection<String> entities) {
    entityRepo.purgeEntityLinks(entities);
    entityRepo.purgeEntities(entities);
  }

  @Transactional
  public void purgeFortressDocuments(Fortress fortress) {
    documentTypeRepo.getFortressDocumentsInUse(fortress.getId());
    documentTypeRepo.purgeFortressSegmentAssociations(fortress.getId());
    documentTypeRepo.purgeDocumentConceptRelationshipsForFortress(fortress.getId());

    documentTypeRepo.purgeFortressDocuments(fortress.getId());
//        String value = "match (fortress:Fortress)-[r:FORTRESS_DOC]-(d:DocType)-[dr]-(x)" +
//                "where id(fortress) = {fort} return dr";
//
//        Map<String,Object>args = new HashMap<>();
//        args.put("fort", fortress.getId());
//        Result<Map<String, Object>> results = template.query(value, args);
//        for (Map<String, Object> result : results) {
//            logger.info ( "");
//        }


  }

  public EntityLog getLastLog(Long entityId) {
    EntityNode entity = getEntity(entityId);
    return getLastEntityLog(entity);
  }

  public EntityLog getLastEntityLog(EntityNode entity) {

    Log lastChange = entity.getLastChange();
    if (lastChange == null) {
      // If no last change, then this might be a mock log
      return getMockLog(entity);
    }

    return trackLogRepo.getLog(entity.getLastChange().getId());
  }

  public Set<EntityLog> getLogs(Long id, Date date) {
    return trackLogRepo.getLogs(id, date.getTime());
  }

  public Collection<EntityNode> getEntities(Collection<Long> entities) {
    return entityRepo.getEntities(entities);
  }

  public Collection<String> getEntityBatch(Long id, int limit) {
    return entityRepo.findEntitiesWithLimit(id, limit);
  }

  public Collection<String> getEntityBatchForSegment(Long id, Document documentType, Long segmentId, int limit) {
    Collection<String> results = new ArrayList<>();
    Map<String, Object> params = new HashMap<>();
    params.put("0", id);
    params.put("1", segmentId);
    params.put("2", limit);
    String query = " match (fortress:Fortress)-[:DEFINES]-(fs:FortressSegment)-[:TRACKS]->(entity:`" + documentType.getName() + "`) " +
        " where id(fortress)={0} and id(fs)={1}" +
        " return entity.key " +
        " limit {2} ";
    Result<Map<String, Object>> keys = template.query(query, params);
    for (Map<String, Object> key : keys) {
      results.add(key.get("entity.key").toString());

    }
    //return entityRepo.findEntitiesWithLimit(id, segmentId, limit);
    return results;
  }

  public EntityNode findParent(EntityNode childEntity) {
    return entityRepo.findParent(childEntity.getId());
  }

  public Collection<EntityKeyBean> getInboundEntities(EntityNode childEntity, boolean withEntityTags) {
    Collection<EntityKeyBean> results = new ArrayList<>();
    Collection<EntityNode> entities = entityRepo.findInboundEntities(childEntity.getId());

    for (EntityNode entity : entities) {
      Collection<EntityTag> entityTags;
      if (withEntityTags) {
        entityTags = findEntityTags(entity);
        results.add(new EntityKeyBean(entity, entityTags, indexManager.toIndex(entity)).addRelationship(""));
      } else {
        results.add(new EntityKeyBean(entity, indexManager.toIndex(entity)).addRelationship(""));
      }

    }
    return results;
  }

  private Collection<EntityTag> findEntityTags(EntityNode entity) {
    Collection<EntityTag> results = new ArrayList<>();
    results.addAll(entityTagInRepo.getEntityTags(entity.getId()));
    results.addAll(entityTagOutRepo.getEntityTags(entity.getId()));
    return results;
  }

  public Collection<EntityKeyBean> getEntities(Company company, List<EntityKeyBean> entityKeys) {
    assert (company != null);
    Collection<EntityKeyBean> results = new ArrayList<>();
    for (EntityKeyBean entityKey : entityKeys) {
      Entity entity = findByCode(company, entityKey);
      if (entity != null) {
        Collection<EntityTag> entityTags = entityTagService.findEntityTagsWithGeo(entity);
        results.add(
            new EntityKeyBean(entity, entityTags, indexManager.toIndex(entity)).addRelationship(entityKey.getRelationshipName())
                .setParent(entityKey.isParent()));

      }

    }
    return results;
  }

  private Entity findByCode(Company company, EntityKeyBean entityKeyBean) {
    FortressNode fortress = fortressService.findByCode(company, entityKeyBean.getFortressName());

    DocumentNode type = conceptService.findDocumentType(fortress, entityKeyBean.getDocumentType());

    return findByCode(fortress.getId(), type, entityKeyBean.getCode());
  }

  /**
   * Find outbound parents of the source entity across Documents as long as they are registered
   *
   * @param entity  parent entity
   * @param docType registered docType for which potential parent Documents might exist
   * @return all parents
   */
  public Collection<EntityKeyBean> getNestedParentEntities(Entity entity, Document docType) {
    Map<String, DocumentResultBean> parents = conceptService.getParents(docType);
    if (parents.isEmpty()) {
      return new ArrayList<>();
    }

    String query = "match (e:Entity) where id(e) = {entity} with e match (e) ";
    String cypherReturn = null;
    for (String relationship : parents.keySet()) {
      query = query + "-[:" + relationship + "]-(" + relationship + ":" + parents.get(relationship).getName() + ")";
      if (cypherReturn == null) {
        cypherReturn = " return " + relationship;
      } else {
        cypherReturn = cypherReturn + "," + relationship;
      }
    }

    Map<String, Object> params = new HashMap<>();
    params.put("entity", entity.getId());
    Result<Map<String, Object>> results = template.query(query + cypherReturn, params);
    Iterator<Map<String, Object>> rows = results.iterator();
    Collection<EntityKeyBean> connected = new ArrayList<>();

    while (rows.hasNext()) {
      // Should only be the one row;
      Map<String, Object> row = rows.next();
      for (String relationship : parents.keySet()) {
        Node node = (Node) row.get(relationship);
        EntityNode e = getEntity(node.getId());
        connected.add(new EntityKeyBean(e, entityTagService.findEntityTagsWithGeo(e), indexManager.toIndex(e))
            .setRelationshipName(relationship));
      }
    }
    return connected;
  }
}
