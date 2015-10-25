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

package org.flockdata.engine.track.service;

import org.flockdata.engine.concept.service.TxService;
import org.flockdata.engine.dao.EntityDaoNeo;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.helper.SecurityHelper;
import org.flockdata.kv.KvContent;
import org.flockdata.kv.service.KvService;
import org.flockdata.model.*;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.registration.service.SystemUserService;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.SearchResult;
import org.flockdata.track.bean.*;
import org.flockdata.track.service.EntityService;
import org.flockdata.track.service.EntityTagService;
import org.flockdata.track.service.FortressService;
import org.flockdata.track.service.TagService;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Transactional services to support record and working with entities and logs
 * <p>
 * User: Mike Holdsworth
 * Date: 8/04/13
 */
@Service
@Transactional
public class EntityServiceNeo4J implements EntityService {

    private static final String EMPTY = "";

    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    SystemUserService sysUserService;

    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    EntityTagService entityTagService;

    @Autowired
    ConceptService conceptService;

    @Autowired
    TxService txService;

    @Autowired
    KvService kvService;

    @Autowired
    EntityDaoNeo entityDao;

    @Autowired
    TagService tagService;

    private Logger logger = LoggerFactory.getLogger(EntityServiceNeo4J.class);

//    @Override
//    @Async
//    public Future<EntityKeyBean> findParent(Entity childEntity) {
//        Entity parent = entityDao.findParent (childEntity);
//        if ( parent != null )
//            return new AsyncResult<>(new EntityKeyBean(parent));
//        return new AsyncResult<>(null);
//    }
@Override
//    @Async
    public EntityKeyBean findParent(Entity childEntity) {
        Entity parent = entityDao.findParent (childEntity);
        if ( parent != null )
            return new EntityKeyBean(parent);
        return null;
    }


@Override
    public KvContent getWhat(Entity entity, Log change) {
        return kvService.getContent(entity, change);
    }

    /**
     * Creates a unique Entity for the fortress. FortressUserNode is automatically
     * created if it does not exist.
     *
     * @return unique primary key to be used for subsequent log calls
     */
    public TrackResultBean createEntity(Fortress fortress, DocumentType documentType, EntityInputBean entityInputBean, Collection<Tag> tags) throws FlockException {

        Entity entity = null;
        if (entityInputBean.getMetaKey() != null) {
            entity = getEntity(fortress.getCompany(), entityInputBean.getMetaKey());
        }

        if (entity == null && (entityInputBean.getCode() != null && !entityInputBean.getCode().equals(EMPTY)))
            entity = findByCode(fortress, documentType, entityInputBean.getCode());

        if (entity != null) {
            logger.trace("Existing entity found by Caller Ref [{}] found [{}]", entityInputBean.getCode(), entity.getMetaKey());
            //entityInputBean.setMetaKey(entity.getMetaKey());

            logger.trace("Existing entity [{}]", entity);
            TrackResultBean trackResult = new TrackResultBean(fortress, entity, entityInputBean);
            trackResult.entityExisted();
            trackResult.setContentInput(entityInputBean.getContent());
            trackResult.setDocumentType(documentType);
            if (entityInputBean.getContent() != null && entityInputBean.getContent().getWhen() != null) {
                // Communicating the POTENTIAL last update so it can be recorded in the tag relationships
                entity.setFortressLastWhen(entityInputBean.getContent().getWhen().getTime());
            }
            boolean saveEntity = false;

            // Entity properties can be updated
            if (entityInputBean.getProperties() != null) {
                if (entity.setProperties(entityInputBean.getProperties())) {
                    saveEntity = true;

                }
            }
            // We can update the entity name?
            if (entityInputBean.getName() != null && !entity.getName().equals(entityInputBean.getName())) {
                saveEntity = true;
                entity.setName(entityInputBean.getName());
            }


            if (saveEntity)
                entityDao.save(entity);
            // Could be rewriting tags
            // DAT-153 - move this to the end of the process?
            EntityLog entityLog = entityDao.getLastEntityLog(entity);
            trackResult.setTags(
                    entityTagService.associateTags(fortress.getCompany(), entity, entityLog, entityInputBean)
            );
            return trackResult;
        }

        try {
            entity = makeEntity(fortress, documentType, entityInputBean);
        } catch (FlockException e) {
            logger.error(e.getMessage());
            return new TrackResultBean("Error processing entityInput [{}]" + entityInputBean + ". Error " + e.getMessage());
        }

        TrackResultBean trackResult = new TrackResultBean(fortress, entity, entityInputBean);
        trackResult.setDocumentType(documentType);

        // Flag the entity as having been newly created. The flag is transient and
        // this saves on having to pass the property as a method variable when
        // associating the tags
        entity.setNew();
        trackResult.setNewEntity();

        if (tags != null)
            tags.clear();
        trackResult.setTags(
                entityTagService.associateTags(fortress.getCompany(), entity, null, entityInputBean)
        );

        trackResult.setContentInput(entityInputBean.getContent());
        if (entity.isNewEntity() && entityInputBean.getContent() != null) {
            // DAT-342
            // We prep the content up-front in order to get it distributed to other services
            // ASAP
            // Minimal defaults that are otherwise set in the LogService
            FortressUser contentUser = null;
            if (entityInputBean.getContent().getFortressUser() != null)
                contentUser = fortressService.getFortressUser(fortress, entityInputBean.getContent().getFortressUser());

            if (entityInputBean.getContent().getEvent() == null) {
                entityInputBean.getContent().setEvent(Log.CREATE);
            }
            Log log = entityDao.prepareLog(fortress.getCompany(), (contentUser != null ? contentUser : entity.getCreatedBy()), trackResult, null, null);

            DateTime contentWhen = (trackResult.getContentInput().getWhen() == null ? new DateTime(DateTimeZone.forID(fortress.getTimeZone())) : new DateTime(trackResult.getContentInput().getWhen()));
            EntityLog entityLog = new EntityLog(entity, log, contentWhen);

            //if (trackResult.getContentInput().getWhen()!= null )

            logger.debug("Setting preparedLog for entity {}", entity);
            //LogResultBean logResult = new LogResultBean(trackResult.getContentInput());
            //logResult.setLogToIndex(entityLog);
            trackResult.setCurrentLog(entityLog);
        }

        return trackResult;

    }

    public Entity makeEntity(Fortress fortress, DocumentType documentType, EntityInputBean entityInput) throws FlockException {
        String fortressUser = entityInput.getFortressUser();
        if (fortressUser == null && entityInput.getContent() != null)
            fortressUser = entityInput.getContent().getFortressUser();

        FortressUser entityUser = null;
        if (fortressUser != null)
            entityUser = fortressService.getFortressUser(fortress, fortressUser);


        Entity entity = entityDao.create(entityInput, fortress, entityUser, documentType);
        if (entity.getId() == null)
            entityInput.setMetaKey("NT " + fortress.getId()); // We ain't tracking this

        //entityInput.setMetaKey(entity.getMetaKey());
        logger.trace("Entity created: id=[{}] key=[{}] for fortress [{}] callerKeyRef = [{}]", entity.getId(), entity.getMetaKey(), fortress.getCode(), entity.getKey());
        return entity;
    }

    /**
     * When you have no API key, find if authorised
     *
     * @param metaKey known GUID
     * @return entity the caller is authorised to view
     */
    @Override
    public Entity getEntity(@NotEmpty String metaKey) {
        String userName = securityHelper.getLoggedInUser();
        SystemUser su = sysUserService.findByLogin(userName);
        if (su == null)
            throw new SecurityException(userName + "Not authorised to retrieve metaKeys");

        return getEntity(su.getCompany(), metaKey, false);
    }

    @Override
    public Entity getEntity(Company company, String metaKey) throws NotFoundException {
        if (company == null)
            throw new NotFoundException("Illegal Company");

        Entity entity = getEntity(company, metaKey, true);
        if (entity == null)
            throw new NotFoundException("Unable to find the requested Entity by the metaKey " + metaKey);
        return entity;
    }

    @Override
    public Entity getEntity(Company company, @NotEmpty String metaKey, boolean inflate) {

        if (company == null)
            return getEntity(metaKey);
        Entity ah = entityDao.findEntity(metaKey, inflate);
        if (ah == null || ah.getFortress() == null)
            return null;

        if (!(ah.getFortress().getCompany().getId().equals(company.getId())))
            throw new SecurityException("CompanyNode mismatch. [" + metaKey + "] working for [" + company.getName() + "] cannot write meta records for [" + ah.getFortress().getCompany().getName() + "]");
        return ah;
    }

    @Override
    public Entity getEntity(Entity entity) {
        return entityDao.fetch(entity);
    }

    @Override
    public Collection<Entity> getEntities(Fortress fortress, Long lastEntityId) {
        return entityDao.findEntities(fortress.getId(), lastEntityId);
    }

    @Override
    public Collection<Entity> getEntities(Fortress fortress, String docTypeName, Long skipTo) {
        DocumentType docType = conceptService.resolveByDocCode(fortress, docTypeName);
        return entityDao.findEntities(fortress.getId(), docType.getName(), skipTo);
    }

    Entity getEntity(Long id) {
        return entityDao.getEntity(id);
    }

    @Override
    public void updateEntity(Entity entity) {
        entityDao.save(entity);
    }

    @Override
    public EntityLog getLastEntityLog(Long entityId) {
        return entityDao.getLastLog(entityId);
    }

    @Override
    public Set<EntityLog> getEntityLogs(Entity entity) {
        return entityDao.getLogs(entity);
    }

    @Override
    public Set<EntityLog> getEntityLogs(Company company, String metaKey) throws FlockException {
        Entity entity = getEntity(company, metaKey);
        if (entity.getFortress().isStoreEnabled())
            return entityDao.getLogs(entity);
        Set<EntityLog> logs = new HashSet<>();
        logs.add(entityDao.getLastEntityLog(entity));
        return logs;
    }

    @Override
    public Set<EntityLog> getEntityLogs(Company company, String metaKey, Date from, Date to) throws FlockException {
        Entity entity = getEntity(company, metaKey);
        return getLogs(entity, from, to);
    }

    Set<EntityLog> getLogs(Entity entity, Date from, Date to) {
        return entityDao.getLogs(entity.getId(), from, to);
    }

    /**
     * This can be used toa assist in compensating transactions to roll back the last change
     * if the caller decides a rollback is required after the log has been written.
     * If there are no Log records left, then the entity will also be removed and the
     * AB metaKey will be forever invalid.
     *
     * @param company validated company the caller is authorised to work with
     * @param entity  UID of the entity
     * @return EntitySearchChange search change to index, or null if there are no logs
     */
    @Override
    public EntitySearchChange cancelLastLog(Company company, Entity entity) throws IOException, FlockException {
        EntityLog existingLog = getLastEntityLog(entity.getId());
        if (existingLog == null)
            return null;

        Log currentLog = existingLog.getLog();
        Log fromLog = currentLog.getPreviousLog();
        String searchKey = entity.getSearchKey();
        EntityLog newEntityLog = null;
        if (fromLog != null) {
            entityDao.fetch(entity);
            entityTagService.findEntityTags(company, entity);
            entityDao.fetch(fromLog);
            entityDao.delete(currentLog);
            newEntityLog = entityDao.getLog(entity, fromLog.getEntityLog().getId());
            entity.setLastChange(fromLog);
            entity.setLastUser(fortressService.getFortressUser(entity.getFortress(), fromLog.getMadeBy().getCode()));
            entity.setFortressLastWhen(newEntityLog.getFortressWhen());
            entity = entityDao.save(entity);
            entityTagService.moveTags(company, fromLog, entity);

        } else {
            // No changes left, there is now just an entity
            // ToDo: What to to with the entity? Delete it? Store the "canceled By" User? Assign the log to a Cancelled RLX?
            // Delete from ElasticSearch??
            entity.setLastUser(fortressService.getFortressUser(entity.getFortress(), entity.getCreatedBy().getCode()));
            entity.setFortressLastWhen(0l);
            entity.setSearchKey(null);
            entity = entityDao.save(entity);
            entityDao.delete(currentLog);
        }
        kvService.delete(entity, currentLog); // ToDo: Move to mediation facade
        EntitySearchChange searchDocument = null;
        if (fromLog == null) {
            // Nothing to index, no changes left so we're done
            searchDocument = new EntitySearchChange(entity);
            searchDocument.setDelete(true);
            searchDocument.setSearchKey(searchKey);
            return searchDocument;
        }

        // Sync the update to fd-search.
        if (entity.getFortress().isSearchEnabled() && !entity.isSearchSuppressed()) {
            // Update against the Entity only by re-indexing the search document
            KvContent priorContent = kvService.getContent(entity, fromLog);

            searchDocument = new EntitySearchChange(entity, newEntityLog, priorContent.getContent());
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
     * @param metaKey GUID
     * @return count
     */
    @Override
    public int getLogCount(Company company, String metaKey) throws FlockException {
        Entity entity = getEntity(company, metaKey);
        logger.debug("looking for logs for Entity id [{}] - metaKey [{}]", entity.getId(), metaKey);
        int logs = entityDao.getLogs(entity).size();
        logger.debug("Log count {}", logs);
        return logs;
    }

    @Override
    public Entity findByCode(Company company, String fortress, String documentCode, String callerRef) throws NotFoundException {
        Fortress iFortress = fortressService.findByName(company, fortress);
        if (iFortress == null)
            return null;

        return findByCode(iFortress, documentCode, callerRef);
    }

    @Override
    public Entity findByCallerRefFull(Long fortressId, String documentType, String callerRef) {
        Fortress fortress = fortressService.getFortress(fortressId);
        return findByCallerRefFull(fortress, documentType, callerRef);

    }

    /**
     * \
     * inflates the search result with dependencies populated
     *
     * @param fortress     System
     * @param documentType Class of doc
     * @param callerRef    fortressName PK
     * @return hydrated entity
     */
    @Override
    public Entity findByCallerRefFull(Fortress fortress, String documentType, String callerRef) {
        return findByCode(fortress, documentType, callerRef);
    }

    /**
     * Locates all the Entities irrespective of the document type. Use this when you know that that metaKey is
     * unique for the entire fortressName
     *
     * @param company      Company you are authorised to work with
     * @param fortressName Fortress to restrict the search to
     * @param callerRef    key to locate
     * @return entities
     */
    @Override
    public Iterable<Entity> findByCode(Company company, String fortressName, String callerRef) throws NotFoundException {
        Fortress fortress = fortressService.findByName(company, fortressName);
        return findByCode(fortress, callerRef);
    }

    private Collection<Entity> findByCode(Fortress fortress, String callerRef) {
        return entityDao.findByCode(fortress.getId(), callerRef.trim());
    }

    public Entity findByCode(Fortress fortress, String documentName, String callerRef) {

        DocumentType doc = conceptService.resolveByDocCode(fortress, documentName, false);
        if (doc == null) {
            logger.debug("Unable to find document for callerRef {}, {}, {}", fortress, documentName, callerRef);
            return null;
        }
        return findByCode(fortress, doc, callerRef);

    }

    /**
     * @param fortress     owning system
     * @param documentType class of document
     * @param callerRef    fortressName primary key
     * @return LogResultBean or NULL.
     */
    public Entity findByCode(Fortress fortress, DocumentType documentType, String callerRef) {
        return entityDao.findByCode(fortress.getId(), documentType.getId(), callerRef.trim());
    }

    @Override
    public EntitySummaryBean getEntitySummary(Company company, String metaKey) throws FlockException {
        Entity entity = getEntity(company, metaKey, true);
        if (entity == null)
            throw new FlockException("Invalid Meta Key [" + metaKey + "]");
        Set<EntityLog> changes = getEntityLogs(entity);
        Collection<EntityTag> tags = entityTagService.getEntityTags(entity);
        return new EntitySummaryBean(entity, changes, tags);
    }

    @Override
    public LogDetailBean getFullDetail(Company company, String metaKey, Long logId) {
        Entity entity = getEntity(company, metaKey, true);
        if (entity == null)
            return null;

        EntityLog log = entityDao.getLog(entity, logId);
        entityDao.fetch(log.getLog());
        KvContent what = kvService.getContent(entity, log.getLog());

        return new LogDetailBean(log, what);
    }

    @Override
    public EntityLog getLogForEntity(Entity entity, Long logId) {
        if (entity != null) {

            EntityLog log = entityDao.getLog(entity, logId);
            if (!log.getEntity().getId().equals(entity.getId()))
                return null;

            entityDao.fetch(log.getLog());
            return log;
        }
        return null;
    }

    @Override
    public Collection<TrackResultBean> trackEntities(Fortress fortress, Collection<EntityInputBean> entityInputs, Collection<Tag> tags) throws InterruptedException, ExecutionException, FlockException, IOException {
        Collection<TrackResultBean> arb = new ArrayList<>();
        DocumentType documentType = null;
        for (EntityInputBean inputBean : entityInputs) {
            if (documentType == null || documentType.getCode() == null || documentType.getId() == null)
                documentType = conceptService.resolveByDocCode(fortress, inputBean.getDocumentName());
            else if (!documentType.getCode().equalsIgnoreCase(inputBean.getDocumentName())) {
                documentType = conceptService.resolveByDocCode(fortress, inputBean.getDocumentName());
            }
            assert (documentType != null);
            assert (documentType.getCode() != null);
            TrackResultBean result = createEntity(fortress, documentType, inputBean, tags);
            logger.trace("Batch Processed {}, callerRef=[{}], documentName=[{}]", result.getEntity().getId(), inputBean.getCode(), inputBean.getDocumentName());
            arb.add(result);
        }

        return arb;

    }

    /**
     * Cross references to Entities to create a link
     *
     * @param company          validated company the caller is authorised to work with
     * @param metaKey          source from which a xref will be created
     * @param xRef             target for the xref
     * @param relationshipName name of the relationship
     */
    @Override
    public Collection<String> crossReference(Company company, String metaKey, Collection<String> xRef, String relationshipName) throws FlockException {
        Entity entity = getEntity(company, metaKey);
        if (entity == null) {
            throw new FlockException("Unable to find the Entity [" + metaKey + "]. Perhaps it has not been processed yet?");
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
    public Map<String, Collection<Entity>> getCrossReference(Company company, String metaKey, String xRefName) throws FlockException {
        Entity entity = getEntity(company, metaKey);
        if (entity == null) {
            throw new FlockException("Unable to find the Entity [" + metaKey + "]. Perhaps it has not been processed yet?");
        }

        return entityDao.getCrossReference(entity, xRefName);
    }

    @Override
    public Map<String, Collection<Entity>> getCrossReference(Company company, String fortressName, String callerRef, String xRefName) throws FlockException {
        Fortress fortress = fortressService.findByName(company, fortressName);

        Entity source = entityDao.findByCodeUnique(fortress.getId(), callerRef);
        if (source == null) {
            throw new FlockException("Unable to find the Entity [" + callerRef + "]");
        }

        return entityDao.getCrossReference(source, xRefName);
    }

    @Override
    public List<EntityKeyBean> linkEntities(Company company, EntityKeyBean sourceKey, Collection<EntityKeyBean> entityKeys, String linkName) throws FlockException {
        Fortress f = fortressService.findByCode(company, sourceKey.getFortressName());
        if (f == null)
            throw new FlockException("Unable to locate the fortress " + sourceKey.getFortressName());
        Entity fromEntity;
        if (sourceKey.getDocumentType() == null || sourceKey.getDocumentType().equals("*"))
            fromEntity = entityDao.findByCodeUnique(f.getId(), sourceKey.getCode());
        else {
            DocumentType document = conceptService.resolveByDocCode(f, sourceKey.getDocumentType(), false);
            fromEntity = entityDao.findByCode(f.getId(), document.getId(), sourceKey.getCode());
        }
        if (fromEntity == null)
            // ToDo: Should we create it??
            throw new FlockException("Unable to locate the source Entity [" + sourceKey + "]");

        //16051954
        Collection<Entity> targets = new ArrayList<>();
        List<EntityKeyBean> ignored = new ArrayList<>();

        for (EntityKeyBean entityKey : entityKeys) {
            int count = 1;

            Collection<Entity> entities = new ArrayList<>();
            if (entityKey.getDocumentType().equals("*"))
                entities = findByCode(f, entityKey.getCode());
            else {
                Entity entity = findByCode(fortressService.findByCode(company, entityKey.getFortressName()), entityKey.getDocumentType(), entityKey.getCode());
                if (entity == null) {
                    // DAT-443
                    // Create a place holding entity if the requested one does not exist
                    DocumentType documentType = conceptService.resolveByDocCode(f, entityKey.getDocumentType(), false);
                    if (documentType != null) {
                        EntityInputBean eib = new EntityInputBean(f.getCode(), entityKey.getDocumentType()).setCode(entityKey.getCode());
                        TrackResultBean trackResult = createEntity(f, documentType, eib, null);
                        entity = trackResult.getEntity();
                    } else {
                        ignored.add(entityKey);
                    }
                }
                if (entity != null) {
                    entities.add(entity);
                    //entities = array;
                }
            }
            if (entities != null) {
                for (Entity entity : entities) {
                    if (count > 1 || count == 0)
                        ignored.add(entityKey);
                    else
                        targets.add(entity);
                    count++;
                }
            }

        }
        if (!targets.isEmpty())
            entityDao.linkEntities(fromEntity, targets, linkName);
        return ignored;
    }

    @Override
    public Map<String, Entity> getEntities(Company company, Collection<String> metaKeys) {
        return entityDao.findEntities(company, metaKeys);
    }

    @Override
    public void purge(Fortress fortress, Collection<String> metaKeys) {
        entityDao.purgeTagRelationships(metaKeys);
        entityDao.purgeFortressLogs(metaKeys);
        entityDao.purgePeopleRelationships(metaKeys);
        entityDao.purgeEntities(metaKeys);
        //logger.info("Completed entity purge routine {}", fortress);

    }

    @Override
    public void purgeFortressDocs(Fortress fortress) {
        entityDao.purgeFortressDocuments(fortress);

    }

    @Override
    public void recordSearchResult(SearchResult searchResult, Long metaId) throws FlockException {
        // Only exists and is public because we need the transaction
        Entity entity;
        try {
            entity = getEntity(metaId); // Happens during development when Graph is cleared down and incoming search results are on the q
        } catch (DataRetrievalFailureException | IllegalStateException e) {
            logger.error("Unable to locate entity for entity {} in order to handle the search metaKey. Ignoring.", metaId);
            throw new FlockException("Unable to locate entity for entity " + metaId + " in order to handle the search result.");
        }

        if (entity == null) {
            logger.error("metaKey could not be found for [{}]", searchResult);
            throw new AmqpRejectAndDontRequeueException("metaKey could not be found for [{" + searchResult.getMetaKey() + "}]");
        }

        if (entity.getSearch() == null) { // Search ACK
            entity.setSearchKey(searchResult.getSearchKey());
            entity.bumpSearch();
            entityDao.save(entity, true); // We don't treat this as a "changed" so we do it quietly
            logger.debug("Updated Entity {}. searchKey {} search searchResult =[{}]", entity.getId(), entity.getSearchKey(), searchResult);
        } else {
            logger.debug("No need to update searchKey");
        }

        if (searchResult.getLogId() == null || searchResult.getLogId() == 0l) {
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
            logger.error("Unable to locate track log {} for metaId {} in order to handle the search metaKey. Ignoring.", searchResult.getLogId(), entity.getId());
            return;
        }

        // Another thread may have processed this so save an update
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
    public Collection<EntityTag> getLastLogTags(Company company, String metaKey) throws FlockException {
        EntityLog lastLog = getLastEntityLog(company, metaKey);
        if (lastLog == null)
            return new ArrayList<>();

        return getLogTags(company, lastLog.getLog());
    }

    @Override
    public EntityLog getLastEntityLog(Company company, String metaKey) throws FlockException {
        Entity entity = getEntity(company, metaKey);
        if (entity == null)
            throw new NotFoundException("Unable to locate the requested Entity for metaKey " + metaKey);
        return entityDao.getLastEntityLog(entity);
    }

    private Collection<EntityTag> getLogTags(Company company, Log log) {
        return entityTagService.findLogTags(company, log);

    }

    @Override
    public EntityLog getEntityLog(Company company, String metaKey, Long logId) throws FlockException {
        Entity entity = getEntity(company, metaKey);
        EntityLog log = entityDao.getLog(entity, logId);

        if (log == null)
            throw new FlockException(String.format("Invalid logId %d for %s ", logId, metaKey));

        if (!log.getEntity().getId().equals(entity.getId()))
            throw new FlockException(String.format("Invalid logId %d for %s ", logId, metaKey));
        return log;
    }

    @Override
    public Collection<EntityTag> getLogTags(Company company, EntityLog entityLog) {
        return getLogTags(company, entityLog.getLog());  //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public Collection<EntityLinkInputBean> linkEntities(Company company, Collection<EntityLinkInputBean> entityLinks) {
        for (EntityLinkInputBean entityLink : entityLinks) {
            Map<String, List<EntityKeyBean>> references = entityLink.getReferences();
            for (String xRefName : references.keySet()) {
                try {
                    List<EntityKeyBean> notFound = linkEntities(company,
                            new EntityKeyBean(entityLink),
                            references.get(xRefName), xRefName);
                    entityLink.setIgnored(xRefName, notFound);
//                    references.put(xRefName, notFound);
                } catch (FlockException de) {
                    logger.error("Exception while cross-referencing Entities. This message is being returned to the caller - [{}]", de.getMessage());
                    entityLink.setServiceMessage(de.getMessage());
                }
            }
        }
        return entityLinks;
    }

    @Override
    public Entity save(Entity entity) {
        return entityDao.save(entity);
    }

    @Override
    public Collection<Entity> getEntities(Collection<Long> entities) {
        return entityDao.getEntities(entities);
    }

    @Override
    public Collection<String> getEntityBatch(Fortress fortress, int limit) {
        return entityDao.getEntityBatch(fortress.getId(), limit);

    }
}
