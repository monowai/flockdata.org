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

package org.flockdata.engine.track.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.flockdata.authentication.SecurityHelper;
import org.flockdata.data.Company;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.data.Log;
import org.flockdata.data.Segment;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.engine.admin.service.StorageProxy;
import org.flockdata.engine.data.dao.EntityDaoNeo;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.FortressUserNode;
import org.flockdata.engine.data.graph.LogNode;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.integration.IndexManager;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchResult;
import org.flockdata.store.LogRequest;
import org.flockdata.store.StoredContent;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.EntityLogResult;
import org.flockdata.track.bean.EntitySummaryBean;
import org.flockdata.track.bean.EntityToEntityLinkInput;
import org.flockdata.track.bean.FdTagResultBean;
import org.flockdata.track.bean.LogDetailBean;
import org.flockdata.track.bean.TrackResultBean;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional services to support record and working with entities and logs
 *
 * @author mholdsworth
 * @since 8/04/2013
 */
@Service
@Transactional
public class EntityServiceNeo4J implements EntityService {

  private static final String EMPTY = "";

  private final FortressService fortressService;

  private final SecurityHelper securityHelper;

  private final EntityTagService entityTagService;

  private final ConceptService conceptService;

  private final IndexManager indexManager;

  private final StorageProxy contentReader;

  private final EntityDaoNeo entityDao;

  private final StorageProxy storageProxy;

  private final PlatformConfig platformConfig;

  private final LogRetryService logRetryService;

  private Logger logger = LoggerFactory.getLogger(EntityServiceNeo4J.class);

  @Autowired
  public EntityServiceNeo4J(SecurityHelper securityHelper, StorageProxy storageProxy, @Qualifier("engineConfig") PlatformConfig platformConfig, EntityDaoNeo entityDao, StorageProxy contentReader, IndexManager indexManager, ConceptService conceptService, EntityTagService entityTagService, FortressService fortressService, LogRetryService logRetryService) {
    this.securityHelper = securityHelper;
    this.storageProxy = storageProxy;
    this.platformConfig = platformConfig;
    this.entityDao = entityDao;
    this.contentReader = contentReader;
    this.indexManager = indexManager;
    this.conceptService = conceptService;
    this.entityTagService = entityTagService;
    this.fortressService = fortressService;
    this.logRetryService = logRetryService;
  }

  @Override
  public Collection<EntityKeyBean> getEntities(Company company, List<EntityKeyBean> entities) {
    return entityDao.getEntities(company, entities);
  }

  @Override
  public Collection<EntityKeyBean> getNestedParentEntities(Entity entity, Document docType) {
    return entityDao.getNestedParentEntities(entity, docType);
  }

  @Override
  public Entity find(Company company, EntityKeyBean entityKeyBean) {
    return findByCode(company, entityKeyBean.getFortressName(), entityKeyBean.getDocumentType(), entityKeyBean.getCode());
  }

  @Override
  public Map<String, Object> getEntityDataLast(Company company, Entity entity) throws FlockException {
    if (entity != null) {

      EntityLog log = getLastEntityLog(entity.getId());
      if (log != null) {
        StoredContent content = storageProxy.read(entity, log.getLog());
        if (content == null) {
          throw new FlockException("Unable to locate content for [" + entity.getKey() + "]. Log found [" + log + "]");
        }
        return content.getData();
      } else {
        // Look for Entity data, not log history
        StoredContent content = storageProxy.read(new LogRequest(entity));
        if (content == null) {
          throw new FlockException("Unable to locate content for [" + entity.getKey() + "]. ");
        }
        return content.getData();

      }
    }
    return null;

  }

  @Override
  public Map<String, Object> getEntityDataLast(Company company, String key) throws FlockException {
    EntityNode entity = getEntity(company, key);
    return getEntityDataLast(company, entity);
  }

  @Override
  public StoredContent getContent(EntityNode entity, LogNode log) {
    return contentReader.read(entity, log);
  }

  /**
   * Creates a unique Entity for the fortress. FortressUserNode is automatically
   * created if it does not exist.
   *
   * @return unique primary key to be used for subsequent log calls
   */
  private TrackResultBean createEntity(DocumentNode documentType, Segment segment, EntityInputBean entityInput, Future<Collection<FdTagResultBean>> tags) throws FlockException {

    EntityNode entity = null;
    if (entityInput.getKey() != null) {
      entity = getEntity(segment.getCompany(), entityInput.getKey());
    }

    if (entity == null && (entityInput.getCode() != null && !entityInput.getCode().equals(EMPTY))) {
      entity = (EntityNode) findByCode(segment.getFortress(), documentType, entityInput.getCode());
    }

    if (entity != null) {
      logger.trace("Existing entity found by Caller Ref [{}] found [{}]", entityInput.getCode(), entity.getKey());
      //entityInputBean.setKey(entity.getKey());

      logger.trace("Existing entity [{}]", entity);
      TrackResultBean trackResult = new TrackResultBean(segment.getFortress(), entity, documentType, entityInput);
      trackResult.entityExisted();
      trackResult.setContentInput(entityInput.getContent());
      trackResult.setDocumentType(documentType);
      if (entityInput.getContent() != null && entityInput.getContent().getWhen() != null) {
        // Communicating the POTENTIAL last update so it can be recorded in the tag relationships
        entity.setFortressLastWhen(entityInput.getContent().getWhen().getTime());
      }
      boolean saveEntity = false;

      // Entity properties can be updated
      if (entityInput.getProperties() != null) {
        if (entity.setProperties(entityInput.getProperties())) {
          saveEntity = true;

        }
      }
      if (entityInput.getSegment() != null) {
        if (!entity.getSegment().getId().equals(segment.getId())) {
          entity.setSegment(segment);
          saveEntity = true;
          // ToDo - delete the search doc in the previous segment !!
        }
      }
      // We can update the entity name?
      if (entityInput.getName() != null && !entity.getName().equals(entityInput.getName())) {
        saveEntity = true;
        entity.setName(entityInput.getName());
      }


      if (saveEntity) {
        entityDao.save(entity);
      }
      // Could be rewriting tags
      // DAT-153 - move this to the end of the process?
      EntityLog entityLog = entityDao.getLastEntityLog(entity);
      getTags(tags);
      Company company = segment.getCompany();
      trackResult.setTags(
          entityTagService.associateTags(company, entity, entityLog, entityInput)
      );
      if (!entityInput.getEntityLinks().isEmpty()) {
        EntityKeyBean thisEntity = new EntityKeyBean(entity, indexManager.toIndex(entity));
        for (String relationship : entityInput.getEntityLinks().keySet()) {
          linkEntities(company, thisEntity, entityInput.getEntityLinks().get(relationship), relationship);
        }
      }

      return trackResult;
    }
    try {
      entity = makeEntity(segment, documentType, entityInput);
    } catch (FlockException e) {
      logger.error(e.getMessage());
      return new TrackResultBean("Error processing entityInput [{}]" + entityInput + ". Error " + e.getMessage());
    }

    TrackResultBean trackResult = new TrackResultBean(segment.getFortress(), entity, documentType, entityInput);
    trackResult.setDocumentType(documentType);

    // Flag the entity as having been newly created. The flag is transient and
    // this saves on having to pass the property as a method variable when
    // associating the tags
    entity.setNew();
    trackResult.setNewEntity();

    trackResult.setTags(
        entityTagService.associateTags(segment.getCompany(), entity, null, entityInput)
    );

    trackResult.setContentInput(entityInput.getContent());
    if (entity.isNewEntity() && entityInput.getContent() != null) {
      // DAT-342
      // We prep the content up-front in order to get it distributed to other services
      // ASAP
      // Minimal defaults that are otherwise set in the LogService
      FortressUserNode contentUser = null;
      if (entityInput.getContent().getFortressUser() != null) {
        contentUser = fortressService.getFortressUser(segment.getFortress(), entityInput.getContent().getFortressUser());
      }

      if (entityInput.getContent().getEvent() == null) {
        entityInput.getContent().setEvent(LogNode.CREATE);
      }
      LogNode log = logRetryService.prepareLog(segment.getCompany(), (contentUser != null ? contentUser : entity.getCreatedBy()), trackResult, null, null);

      DateTime contentWhen = (trackResult.getContentInput().getWhen() == null ? new DateTime(DateTimeZone.forID(segment.getFortress().getTimeZone())) : new DateTime(trackResult.getContentInput().getWhen()));
      EntityLog entityLog = new EntityLog(entity, log, contentWhen);

      //if (trackResult.getContentInput().getWhen()!= null )

      logger.debug("Setting preparedLog for entity {}", entity);
      //LogResultBean logResult = new LogResultBean(trackResult.getContentInput());
      //logResult.setLogToIndex(entityLog);
      trackResult.setCurrentLog(entityLog);
    }

    return trackResult;

  }

  public Collection<FdTagResultBean> getTags(Future<Collection<FdTagResultBean>> tags) throws FlockException {
    if (tags != null) {
      try {
        return tags.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new FlockException(e.getMessage());
      }
    }
    return null;
  }

  private EntityNode makeEntity(Segment segment, DocumentNode documentType, EntityInputBean entityInput) throws FlockException {
    String fortressUser = entityInput.getFortressUser();
    if (fortressUser == null && entityInput.getContent() != null) {
      fortressUser = entityInput.getContent().getFortressUser();
    }

    FortressUserNode entityUser = null;
    if (fortressUser != null) {
      entityUser = fortressService.getFortressUser(segment.getFortress(), fortressUser);
    }


    EntityNode entity = entityDao.create(entityInput, segment, entityUser, documentType);
    if (entity.getId() == null) {
      entityInput.setKey("NT " + segment.getFortress().getId()); // We ain't tracking this
    } else if (!entityInput.getEntityLinks().isEmpty()) {
      // DAT-525
      EntityKeyBean thisEntity = new EntityKeyBean(entity, indexManager.toIndex(entity));
      for (String relationship : entityInput.getEntityLinks().keySet()) {
        linkEntities(segment.getCompany(), thisEntity, entityInput.getEntityLinks().get(relationship), relationship);
      }

    }
    //entityInput.setKey(entity.getKey());
    logger.trace("Entity created: id=[{}] key=[{}] for fortress [{}] callerKeyRef = [{}]", entity.getId(), entity.getKey(), segment.getFortress().getCode(), entity.getExtKey());
    return entity;
  }

  @Override
  public EntityNode getEntity(Company company, String key) {
    if (company == null) {
      throw new NotFoundException("Illegal Company");
    }

    EntityNode entity = getEntity(company, key, true);
    if (entity == null) {
      throw new NotFoundException("Unable to find the requested Entity by the key " + key);
    }
    return entity;
  }

  @Override
  public EntityNode getEntity(Company company, String key, boolean inflate) {

    if (company == null) {
      return getEntity(securityHelper.getCompany(), key);
    }
    EntityNode entity = entityDao.findEntity(key, inflate);
    if (entity == null || entity.getSegment() == null) {
      return null;
    }

    if (!(entity.getSegment().getFortress().getCompany().getId().equals(company.getId()))) {
      throw new SecurityException("CompanyNode mismatch. [" + key + "] working for [" + company.getName() + "] cannot write Entities for [" + entity.getSegment().getFortress().getCompany().getName() + "]");
    }
    return entity;
  }

  @Override
  public Entity getEntity(EntityNode entity) {
    return entityDao.fetch(entity);
  }

  @Override
  public Collection<Entity> getEntities(Fortress fortress, Long lastEntityId) {
    return entityDao.findEntities(fortress.getId(), lastEntityId);
  }

  @Override
  public Collection<Entity> getEntities(Fortress fortress, String docTypeName, Long skipTo) {
    DocumentNode docType = conceptService.resolveByDocCode(fortress, docTypeName);
    return entityDao.findEntities(fortress.getId(), docType.getName(), skipTo);
  }

  private EntityNode getEntity(Long id) {
    return entityDao.getEntity(id);
  }

  @Override
  public void updateEntity(EntityNode entity) {
    entityDao.save(entity);
  }

  @Override
  public EntityLog getLastEntityLog(Long entityId) {
    return entityDao.getLastLog(entityId);
  }

  @Override
  public Collection<org.flockdata.data.EntityLog> getEntityLogs(Entity entity) {
    return entityDao.getLogs(entity);
  }

  @Override
  public Collection<EntityLogResult> getEntityLogs(Company company, String key) {
    return getEntityLogs(company, key, false);
  }

  @Override
  public Collection<EntityLogResult> getEntityLogs(Company company, String key, boolean withData) {
    EntityNode entity = getEntity(company, key);
    Collection<org.flockdata.data.EntityLog> entityLogs;
    Collection<EntityLogResult> results = new ArrayList<>();
    if (entity.getSegment().getFortress().isStoreEnabled()) {
      entityLogs = entityDao.getLogs(entity);
      for (org.flockdata.data.EntityLog log : entityLogs) {
        if (withData) {
          StoredContent storedContent = contentReader.read(log.getEntity(), log.getLog());
          results.add(new EntityLogResult(log, storedContent));
        } else {
          results.add(new EntityLogResult(log));
        }
      }
    } else {
      results.add(new EntityLogResult(entityDao.getLastEntityLog(entity)));
    }

    return results;
  }

  @Override
  public Set<EntityLog> getEntityLogs(Company company, String key, Date from, Date to) throws FlockException {
    EntityNode entity = getEntity(company, key);
    return entityDao.getLogs(entity.getId(), from, to);
  }


  /**
   * This can be used toa assist in compensating transactions to roll back the last change
   * if the caller decides a rollback is required after the log has been written.
   * If there are no Log records left, then the entity will also be removed and the
   * AB key will be forever invalid.
   *
   * @param company validated company the caller is authorised to work with
   * @param entity  UID of the entity
   * @return EntitySearchChange search change to index, or null if there are no logs
   */
  @Override
  public EntitySearchChange cancelLastLog(Company company, EntityNode entity) throws IOException, FlockException {
    org.flockdata.data.EntityLog existingLog = getLastEntityLog(entity.getId());
    if (existingLog == null) {
      return null;
    }

    LogNode currentLog = (LogNode) existingLog.getLog();
    LogNode fromLog = (LogNode) currentLog.getPreviousLog();
    String searchKey = entity.getSearchKey();
    EntityLog newEntityLog = null;
    if (fromLog != null) {
      entityDao.fetch(entity);
      entityTagService.findEntityTagResults(entity);
      entityDao.fetch(fromLog);
      entityDao.delete(currentLog);
      newEntityLog = entityDao.getLog(entity, fromLog.getEntityLog().getId());
      entity.setLastChange(fromLog);
      entity.setLastUser(fortressService.getFortressUser(entity.getSegment().getFortress(), fromLog.getMadeBy().getCode()));
      entity.setFortressLastWhen(newEntityLog.getFortressWhen());
      entity = entityDao.save(entity);
      entityTagService.moveTags(company, fromLog, entity);

    } else {
      // No changes left, there is now just an entity
      // ToDo: What to to with the entity? Delete it? Store the "canceled By" User? Assign the log to a Cancelled RLX?
      // Delete from ElasticSearch??
      entity.setLastUser(fortressService.getFortressUser(entity.getSegment().getFortress(), entity.getCreatedBy().getCode()));
      entity.setFortressLastWhen(0L);
      entity.setSearchKey(null);
      entity = entityDao.save(entity);
      entityDao.delete(currentLog);
    }
    EntitySearchChange searchDocument = null;
    if (fromLog == null) {
      if (entity.getSegment().getFortress().isSearchEnabled()) {
        // Nothing to index, no changes left so we're done
        searchDocument = new EntitySearchChange(entity, indexManager.toIndex(entity));
        searchDocument.setDelete(true);
        searchDocument.setSearchKey(searchKey);
      }
      return searchDocument;
    }

    // Sync the update to fd-search.
    if (entity.getSegment().getFortress().isSearchEnabled() && !entity.isSearchSuppressed()) {
      // Update against the Entity only by re-indexing the search document
      StoredContent priorContent = contentReader.read(entity, fromLog);

      searchDocument = new EntitySearchChange(entity, newEntityLog, priorContent.getContent(), indexManager.toIndex(entity));
      //EntityTagFinder tagFinder = getTagFinder(fortressService.getTagStructureFinder(entity));


      searchDocument.setReplyRequired(false);
      searchDocument.setForceReindex(true);
    }
    return searchDocument;
  }

  /**
   * counts the number of logs that exist for the given entity
   *
   * @param company validated company the caller is authorised to work with
   * @param key     GUID
   * @return count
   */
  @Override
  public int getLogCount(Company company, String key) throws FlockException {
    EntityNode entity = getEntity(company, key);
    logger.debug("looking for logs for Entity id [{}] - key [{}]", entity.getId(), key);
    int logs = entityDao.getLogs(entity).size();
    logger.debug("Log count {}", logs);
    return logs;
  }

  @Override
  public Entity findByCode(Company company, String fortress, String documentCode, String code) throws NotFoundException {
    FortressNode iFortress = fortressService.findByName(company, fortress);
    if (iFortress == null) {
      return null;
    }

    return findByCode(iFortress, documentCode, code);
  }

  /**
   * \
   * inflates the search result with dependencies populated
   *
   * @param fortress     System
   * @param documentType Class of doc
   * @param code         fortressName PK
   * @return hydrated entity
   */
  @Override
  public Entity findByCodeFull(FortressNode fortress, String documentType, String code) {
    return findByCode(fortress, documentType, code);
  }

  /**
   * Locates all the Entities irrespective of the document type. Use this when you know that that key is
   * unique for the entire fortressName
   *
   * @param company      Company you are authorised to work with
   * @param fortressName Fortress to restrict the search to
   * @param code         key to locate
   * @return entities
   */
  @Override
  public Iterable<Entity> findByCode(Company company, String fortressName, String code) throws NotFoundException {
    FortressNode fortress = fortressService.findByName(company, fortressName);
    return findByCode(fortress, code);
  }

  private Collection<Entity> findByCode(FortressNode fortress, String code) {
    return entityDao.findByCode(fortress.getId(), code.trim());
  }

  public Entity findByCode(Fortress fortress, String documentName, String code) {

    DocumentNode doc = conceptService.resolveByDocCode(fortress, documentName, false);
    if (doc == null) {
      logger.debug("Unable to find document for code {}, {}, {}", fortress, documentName, code);
      return null;
    }
    return findByCode(fortress, doc, code);

  }

  /**
   * @param fortress     owning system
   * @param documentType class of document
   * @param code         fortressName primary key
   * @return LogResultBean or NULL.
   */
  public Entity findByCode(Fortress fortress, Document documentType, String code) {
    return entityDao.findByCode(fortress.getId(), documentType, code.trim());
  }

  @Override
  public EntitySummaryBean getEntitySummary(Company company, String key) throws FlockException {
    Entity entity = getEntity(company, key, true);
    if (entity == null) {
      throw new FlockException("Invalid entity key [" + key + "]");
    }
    Collection<org.flockdata.data.EntityLog> changes = getEntityLogs(entity);
    Collection<EntityTag> tags = entityTagService.findEntityTagsWithGeo(entity);
    EntitySummaryBean esb = new EntitySummaryBean(entity, changes, tags);
    esb.setIndex(indexManager.toIndex(entity));
    return esb;
  }

  @Override
  public LogDetailBean getFullDetail(Company company, String key, Long logId) {
    EntityNode entity = getEntity(company, key, true);
    if (entity == null) {
      return null;
    }

    EntityLog entityLog = entityDao.getLog(entity, logId);
    entityDao.fetch((LogNode) entityLog.getLog());
    StoredContent what = contentReader.read(new LogRequest(entity, entityLog.getLog()));

    return new LogDetailBean(entityLog, what);
  }

  @Override
  public EntityLog getLogForEntity(EntityNode entity, Long logId) {
    if (entity != null) {

      EntityLog entityLog = entityDao.getLog(entity, logId);
      if (!entityLog.getEntity().getId().equals(entity.getId())) {
        return null;
      }

      entityDao.fetch((LogNode) entityLog.getLog());
      return entityLog;
    }
    return null;
  }

  @Override
  public Collection<TrackResultBean> trackEntities(DocumentNode documentType, Segment segment, Collection<EntityInputBean> entityInputs, Future<Collection<FdTagResultBean>> tags) throws InterruptedException, ExecutionException, FlockException {
    Collection<TrackResultBean> arb = new ArrayList<>();
    for (EntityInputBean inputBean : entityInputs) {

      TrackResultBean result = createEntity(documentType, segment, inputBean, tags);
      if (result.getEntity() != null) {
        logger.trace("Batch Processed {}, code=[{}], documentName=[{}]", result.getEntity().getId(), inputBean.getCode(), inputBean.getDocumentType().getName());
      }
      arb.add(result);
    }

    return arb;

  }

  /**
   * Cross references to Entities to create a link
   *
   * @param company          validated company the caller is authorised to work with
   * @param key              source from which a xref will be created
   * @param xRef             target for the xref
   * @param relationshipName name of the relationship
   */
  @Override
  public Collection<String> crossReference(Company company, String key, Collection<String> xRef, String relationshipName) throws FlockException {
    EntityNode entity = getEntity(company, key);
    if (entity == null) {
      throw new FlockException("Unable to find the Entity [" + key + "]. Perhaps it has not been processed yet?");
    }
    Collection<Entity> targets = new ArrayList<>();
    Collection<String> ignored = new ArrayList<>();
    for (String next : xRef) {

      try {
        targets.add(getEntity(company, next));
      } catch (NotFoundException nfe) {
        ignored.add(next);

      }
    }
    entityDao.linkEntities(entity, targets, relationshipName);
    return ignored;
  }

  @Override
  public Map<String, Collection<EntityNode>> getCrossReference(Company company, String key, String relationship) throws FlockException {
    EntityNode entity = getEntity(company, key);
    if (entity == null) {
      throw new FlockException("Unable to find the Entity [" + key + "]. Perhaps it has not been processed yet?");
    }

    return entityDao.getCrossReference(entity, relationship);
  }

  @Override
  public Map<String, Collection<EntityNode>> getCrossReference(Company company, String fortressName, String code, String xRefName) throws FlockException {
    FortressNode fortress = fortressService.findByName(company, fortressName);

    Entity source = entityDao.findByCodeUnique(fortress.getId(), code);
    if (source == null) {
      throw new FlockException("Unable to find the Entity [" + code + "]");
    }

    return entityDao.getCrossReference(source, xRefName);
  }

  @Override
  public Collection<EntityKeyBean> linkEntities(Company company, EntityKeyBean sourceKey, Collection<EntityKeyBean> entityKeys, String linkName) throws FlockException {
    FortressNode fortress = fortressService.findByCode(company, sourceKey.getFortressName());
    if (fortress == null) {
      throw new FlockException("Unable to locate the fortress " + sourceKey.getFortressName());
    }
    Entity sourceEntity;
    if (sourceKey.getDocumentType() == null || sourceKey.getDocumentType().equals("*")) {
      sourceEntity = entityDao.findByCodeUnique(fortress.getId(), sourceKey.getCode());
    } else {
      DocumentNode document = conceptService.resolveByDocCode(fortress, sourceKey.getDocumentType(), false);
      sourceKey.setResolvedDocument(document);
      if (sourceKey.getKey() != null) {
        sourceEntity = entityDao.findEntity(sourceKey.getKey(), true);
      } else {
        sourceEntity = entityDao.findByCode(fortress.getId(), document, sourceKey.getCode());
      }
      sourceKey.setResolvedEntity(sourceEntity);
    }
    if (sourceEntity == null)
    // ToDo: Should we create it??
    {
      throw new FlockException("Unable to locate the source Entity [" + sourceKey + "]");
    }

    Collection<Entity> targets = new ArrayList<>();
    List<EntityKeyBean> ignored = new ArrayList<>();

    for (EntityKeyBean targetKey : entityKeys) {
      int count = 1;

      Collection<Entity> entities = new ArrayList<>();
      if (targetKey.getDocumentType().equals("*") || targetKey.getDocumentType().equalsIgnoreCase("entity")) {
        entities = findByCode(fortress, targetKey.getCode());
      } else {
        Entity entity = findByCode(fortressService.findByCode(company, targetKey.getFortressName()), targetKey.getDocumentType(), targetKey.getCode());
        if (entity == null) {
          if (targetKey.getMissingAction() == EntityKeyBean.ACTION.CREATE) {
            // DAT-443 - Create a place holding entity if the requested one does not exist
            DocumentNode documentType = conceptService.resolveByDocCode(fortress, targetKey.getDocumentType(), false);
            if (documentType != null) {
              EntityInputBean eib = new EntityInputBean(fortress, new DocumentTypeInputBean(targetKey.getDocumentType())).setCode(targetKey.getCode());
              TrackResultBean trackResult = createEntity(documentType, fortress.getDefaultSegment(), eib, null);
              entity = trackResult.getEntity();
            }
          } else if (targetKey.getMissingAction() == EntityKeyBean.ACTION.IGNORE) {
            ignored.add(targetKey);
//                        entityKeys.remove(targetKey);
          } else {
            throw new FlockException("Unable to resolve the target entity " + targetKey.toString());
          }
        }
        if (entity != null) {
          targetKey.setResolvedEntity(entity);
          if (targetKey.getResolvedDocument() == null) {
            targetKey.setResolvedDocument(conceptService.findDocumentType(entity.getFortress(), targetKey.getDocumentType()));
          }
          entities.add(entity);
        }
      }
      if (!entities.isEmpty()) {
        for (Entity entity : entities) {
          if (count > 1 || count == 0) {
            ignored.add(targetKey);
          } else {
            targets.add(entity);
          }
          count++;
        }
      }

    }
    // ToDo: Update search doc?
    if (!targets.isEmpty()) {
      entityDao.linkEntities(sourceEntity, targets, linkName);
    }

    ignored.forEach(entityKeys::remove);

    return ignored;
  }

  @Override
  public Map<String, EntityNode> getEntities(Company company, Collection<String> keys) {
    return entityDao.findEntities(company, keys);
  }

  @Override
  public void purge(Fortress fortress, Collection<String> keys) {
    entityDao.purgeTagRelationships(keys);
    entityDao.purgeFortressLogs(keys);
    entityDao.purgePeopleRelationships(keys);
    entityDao.purgeEntities(keys);
    //logger.info("Completed entity purge routine {}", fortress);

  }

  @Override
  public void purgeFortressDocs(Fortress fortress) {
    entityDao.purgeFortressDocuments(fortress);

  }

  @Override
  public void recordSearchResult(SearchResult searchResult, Long entityId) throws FlockException {
    // Only exists and is public because we need the transaction
    EntityNode entity;
    try {
      entity = getEntity(entityId); // Happens during development when Graph is cleared down and incoming search results are on the q
    } catch (DataRetrievalFailureException | IllegalStateException e) {
      logger.error("Unable to locate entity for entity {} in order to handle the search key. Ignoring.", entityId);
      throw new FlockException("Unable to locate entity for entity " + entityId + " in order to handle the search result.");
    }

    if (entity == null) {
      logger.error("key could not be found for [{}]", searchResult);
      throw new AmqpRejectAndDontRequeueException("key could not be found for [{" + searchResult.getKey() + "}]");
    }

    if (platformConfig.isSearchRequiredToConfirm()) { // Search ACK
      entity.setSearchKey(searchResult.getSearchKey());
      entity.bumpSearch();
      entityDao.save(entity, true); // We don't treat this as a "changed" so we do it quietly
      logger.debug("Updated Entity {}. searchKey {} search searchResult =[{}]", entity.getId(), entity.getSearchKey(), searchResult);
    } else {
      logger.debug("No need to update searchKey");
    }

    if (searchResult.getLogId() == null || searchResult.getLogId() == 0L) {
      // Indexing entity meta data only
      return;
    }
    EntityLog entityLog;
    // The change has been indexed
    try {
      entityLog = entityDao.getLog(entity, searchResult.getLogId());
      if (entityLog == null) {
        logger.error("Illegal node requested from handleSearchResult [{}]", searchResult.getLogId());
        return;
      }
    } catch (DataRetrievalFailureException e) {
      logger.error("Unable to locate track log {} for entityId {} in order to handle the search key. Ignoring.", searchResult.getLogId(), entity.getId());
      return;
    }

    // Another thread may have processed this so saveFortressContentType an update
    if (!entityLog.isIndexed()) {
      // We need to know that the change we requested to index has been indexed.
      logger.debug("Updating index status for {}", entityLog);
      entityLog.setIsIndexed();
      entityDao.save(entityLog);
      logger.debug("Updated index status for {}", entityLog);

    } else {
      logger.trace("Skipping {} as it is already indexed", entityLog);
    }
  }

  @Override
  public Collection<EntityTag> getLastLogTags(Company company, String key) throws FlockException {
    org.flockdata.data.EntityLog lastLog = getLastEntityLog(company, key);
    if (lastLog == null) {
      return new ArrayList<>();
    }

    return getLogTags(company, lastLog.getLog());
  }

  @Override
  public EntityLog getLastEntityLog(Company company, String key) throws FlockException {
    EntityNode entity = getEntity(company, key);
    if (entity == null) {
      throw new NotFoundException("Unable to locate the requested Entity for key " + key);
    }
    return entityDao.getLastEntityLog(entity);
  }

  private Collection<EntityTag> getLogTags(Company company, Log log) {
    return entityTagService.findLogTags(company, log);

  }

  @Override
  public org.flockdata.data.EntityLog getEntityLog(CompanyNode company, String key, Long logId) throws FlockException {
    EntityNode entity = getEntity(company, key);
    org.flockdata.data.EntityLog log = entityDao.getLog(entity, logId);

    if (log == null) {
      throw new FlockException(String.format("Invalid logId %d for %s ", logId, key));
    }

    if (!log.getEntity().getId().equals(entity.getId())) {
      throw new FlockException(String.format("Invalid logId %d for %s ", logId, key));
    }
    return log;
  }

  @Override
  public Collection<EntityTag> getLogTags(Company company, org.flockdata.data.EntityLog entityLog) {
    return getLogTags(company, entityLog.getLog());  //To change body of created methods use File | Settings | File Templates.
  }

  @Override
  public Collection<EntityToEntityLinkInput> linkEntities(Company
                                                              company, Collection<EntityToEntityLinkInput> entityLinks) {
    for (EntityToEntityLinkInput entityLink : entityLinks) {
      Map<String, List<EntityKeyBean>> references = entityLink.getReferences();
      for (String xRefName : references.keySet()) {
        try {
          Collection<EntityKeyBean> notFound = linkEntities(company,
              new EntityKeyBean(entityLink),
              references.get(xRefName), xRefName);
          entityLink.setIgnored(xRefName, notFound);
//                    references.put(relationship, notFound);
        } catch (FlockException de) {
          logger.error("Exception while cross-referencing Entities. This message is being returned to the caller - [{}]", de.getMessage());
          entityLink.setServiceMessage(de.getMessage());
        }
      }
    }
    return entityLinks;
  }

  @Override
  public EntityNode save(EntityNode entity) {
    return entityDao.save(entity);
  }

  @Override
  public Collection<EntityNode> getEntities(Collection<Long> entities) {
    return entityDao.getEntities(entities);
  }

  @Override
  public Collection<String> getEntityBatch(Fortress fortress, int limit) {
    return entityDao.getEntityBatch(fortress.getId(), limit);

  }

  @Override
  public Collection<String> getEntityBatch(Fortress fortress, Document documentType, Segment fortressSegment, int count) {
    return entityDao.getEntityBatchForSegment(fortress.getId(), documentType, fortressSegment.getId(), count);
  }
}
