/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.admin.service.StorageProxy;
import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.engine.dao.EntityDaoNeo;
import org.flockdata.engine.meta.service.TxService;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.*;
import org.flockdata.registration.TagResultBean;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.registration.service.SystemUserService;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.search.model.SearchResult;
import org.flockdata.shared.IndexManager;
import org.flockdata.store.StoredContent;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Transactional services to support record and working with entities and logs
 * <p/>
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
    IndexManager indexHelper;

    @Autowired
    TxService txService;

    @Autowired
    StorageProxy contentReader;

    @Autowired
    EntityDaoNeo entityDao;

    @Autowired
    TagService tagService;

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig platformConfig;

    private Logger logger = LoggerFactory.getLogger(EntityServiceNeo4J.class);

    @Override
    public EntityKeyBean findParent(Entity childEntity) {
        Entity parent = entityDao.findParent(childEntity);
        if (parent != null)
            return new EntityKeyBean(parent, indexHelper.parseIndex(parent));
        return null;
    }

    @Override
    public Collection<EntityKeyBean> getInboundEntities(Entity entity, boolean withEntityTags) {
        return entityDao.getInboundEntities(entity, withEntityTags);
    }

    @Override
    public StoredContent getContent(Entity entity, Log log) {
        return contentReader.read(entity, log);
    }

    /**
     * Creates a unique Entity for the fortress. FortressUserNode is automatically
     * created if it does not exist.
     *
     * @return unique primary key to be used for subsequent log calls
     */
    private TrackResultBean createEntity(DocumentType documentType, FortressSegment segment, EntityInputBean entityInput, Future<Collection<TagResultBean>> tags) throws FlockException {

        Entity entity = null;
        if (entityInput.getKey() != null) {
            entity = getEntity(segment.getCompany(), entityInput.getKey());
        }

        if (entity == null && (entityInput.getCode() != null && !entityInput.getCode().equals(EMPTY)))
            entity = findByCode(segment.getFortress(), documentType, entityInput.getCode());

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


            if (saveEntity)
                entityDao.save(entity);
            // Could be rewriting tags
            // DAT-153 - move this to the end of the process?
            EntityLog entityLog = entityDao.getLastEntityLog(entity);
            getTags(tags);

            trackResult.setTags(
                    entityTagService.associateTags(segment.getCompany(), entity, entityLog, entityInput)
            );
            if (!entityInput.getEntityLinks().isEmpty()) {
                EntityKeyBean thisEntity = new EntityKeyBean(entity, indexHelper.parseIndex(entity));
                for (String relationship : entityInput.getEntityLinks().keySet()) {
                    linkEntities(segment.getCompany(), thisEntity, entityInput.getEntityLinks().get(relationship), relationship);
                }
            }

            return trackResult;
        }
        Collection<TagResultBean>createdTags = null;
        try {
            createdTags = getTags(tags);
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
            FortressUser contentUser = null;
            if (entityInput.getContent().getFortressUser() != null)
                contentUser = fortressService.getFortressUser(segment.getFortress(), entityInput.getContent().getFortressUser());

            if (entityInput.getContent().getEvent() == null) {
                entityInput.getContent().setEvent(Log.CREATE);
            }
            Log log = entityDao.prepareLog(segment.getCompany(), (contentUser != null ? contentUser : entity.getCreatedBy()), trackResult, null, null);

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

    public Collection<TagResultBean> getTags(Future<Collection<TagResultBean>> tags) throws FlockException {
        if (tags != null)
            try {
                return tags.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new FlockException(e.getMessage());
            }
        return null;
    }

    private Entity makeEntity(FortressSegment segment, DocumentType documentType, EntityInputBean entityInput) throws FlockException {
        String fortressUser = entityInput.getFortressUser();
        if (fortressUser == null && entityInput.getContent() != null)
            fortressUser = entityInput.getContent().getFortressUser();

        FortressUser entityUser = null;
        if (fortressUser != null)
            entityUser = fortressService.getFortressUser(segment.getFortress(), fortressUser);


        Entity entity = entityDao.create(entityInput, segment, entityUser, documentType);
        if (entity.getId() == null)
            entityInput.setKey("NT " + segment.getFortress().getId()); // We ain't tracking this
        else if (!entityInput.getEntityLinks().isEmpty()) {
            // DAT-525
            EntityKeyBean thisEntity = new EntityKeyBean(entity, indexHelper.parseIndex(entity));
            for (String relationship : entityInput.getEntityLinks().keySet()) {
                linkEntities(segment.getCompany(), thisEntity, entityInput.getEntityLinks().get(relationship), relationship);
            }

        }
        //entityInput.setKey(entity.getKey());
        logger.trace("Entity created: id=[{}] key=[{}] for fortress [{}] callerKeyRef = [{}]", entity.getId(), entity.getKey(), segment.getFortress().getCode(), entity.getExtKey());
        return entity;
    }

    /**
     * When you have no API key, find if authorised
     *
     * @param key known GUID
     * @return entity the caller is authorised to view
     */
    @Override
    public Entity getEntity(@NotEmpty String key) {
        String userName = securityHelper.getLoggedInUser();
        SystemUser su = sysUserService.findByLogin(userName);
        if (su == null)
            throw new SecurityException(userName + "Not authorised to retrieve keys");

        return getEntity(su.getCompany(), key, false);
    }

    @Override
    public Entity getEntity(Company company, String key) throws NotFoundException {
        if (company == null)
            throw new NotFoundException("Illegal Company");

        Entity entity = getEntity(company, key, true);
        if (entity == null)
            throw new NotFoundException("Unable to find the requested Entity by the key " + key);
        return entity;
    }

    @Override
    public Entity getEntity(Company company, @NotEmpty String key, boolean inflate) {

        if (company == null)
            return getEntity(key);
        Entity entity = entityDao.findEntity(key, inflate);
        if (entity == null || entity.getSegment() == null)
            return null;

        if (!(entity.getSegment().getFortress().getCompany().getId().equals(company.getId())))
            throw new SecurityException("CompanyNode mismatch. [" + key + "] working for [" + company.getName() + "] cannot write meta records for [" + entity.getSegment().getFortress().getCompany().getName() + "]");
        return entity;
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
    public Set<EntityLog> getEntityLogs(Company company, String key) throws FlockException {
        Entity entity = getEntity(company, key);
        if (entity.getSegment().getFortress().isStoreEnabled())
            return entityDao.getLogs(entity);
        Set<EntityLog> logs = new HashSet<>();
        logs.add(entityDao.getLastEntityLog(entity));
        return logs;
    }

    @Override
    public Set<EntityLog> getEntityLogs(Company company, String key, Date from, Date to) throws FlockException {
        Entity entity = getEntity(company, key);
        return getLogs(entity, from, to);
    }

    Set<EntityLog> getLogs(Entity entity, Date from, Date to) {
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
            entity.setLastUser(fortressService.getFortressUser(entity.getSegment().getFortress(), fromLog.getMadeBy().getCode()));
            entity.setFortressLastWhen(newEntityLog.getFortressWhen());
            entity = entityDao.save(entity);
            entityTagService.moveTags(company, fromLog, entity);

        } else {
            // No changes left, there is now just an entity
            // ToDo: What to to with the entity? Delete it? Store the "canceled By" User? Assign the log to a Cancelled RLX?
            // Delete from ElasticSearch??
            entity.setLastUser(fortressService.getFortressUser(entity.getSegment().getFortress(), entity.getCreatedBy().getCode()));
            entity.setFortressLastWhen(0l);
            entity.setSearchKey(null);
            entity = entityDao.save(entity);
            entityDao.delete(currentLog);
        }
        //kvService.delete(entity, currentLog); // ToDo: Move to mediation facade
        EntitySearchChange searchDocument = null;
        if (fromLog == null) {
            if (entity.getSegment().getFortress().isSearchEnabled()) {
                // Nothing to index, no changes left so we're done
                searchDocument = new EntitySearchChange(entity, indexHelper.parseIndex(entity));
                searchDocument.setDelete(true);
                searchDocument.setSearchKey(searchKey);
            }
            return searchDocument;
        }

        // Sync the update to fd-search.
        if (entity.getSegment().getFortress().isSearchEnabled() && !entity.isSearchSuppressed()) {
            // Update against the Entity only by re-indexing the search document
            StoredContent priorContent = contentReader.read(entity, fromLog);

            searchDocument = new EntitySearchChange(entity, newEntityLog, priorContent.getContent(), indexHelper.parseIndex(entity));
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
        Entity entity = getEntity(company, key);
        logger.debug("looking for logs for Entity id [{}] - key [{}]", entity.getId(), key);
        int logs = entityDao.getLogs(entity).size();
        logger.debug("Log count {}", logs);
        return logs;
    }

    @Override
    public Entity findByCode(Company company, String fortress, String documentCode, String code) throws NotFoundException {
        Fortress iFortress = fortressService.findByName(company, fortress);
        if (iFortress == null)
            return null;

        return findByCode(iFortress, documentCode, code);
    }

    @Override
    public Entity findByCodeFull(Long fortressId, String documentType, String code) {
        Fortress fortress = fortressService.getFortress(fortressId);
        return findByCodeFull(fortress, documentType, code);

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
    public Entity findByCodeFull(Fortress fortress, String documentType, String code) {
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
        Fortress fortress = fortressService.findByName(company, fortressName);
        return findByCode(fortress, code);
    }

    private Collection<Entity> findByCode(Fortress fortress, String code) {
        return entityDao.findByCode(fortress.getId(), code.trim());
    }

    public Entity findByCode(Fortress fortress, String documentName, String code) {

        DocumentType doc = conceptService.resolveByDocCode(fortress, documentName, false);
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
    public Entity findByCode(Fortress fortress, DocumentType documentType, String code) {
        return entityDao.findByCode(fortress.getId(), documentType, code.trim());
    }

    @Autowired
    IndexManager indexManager;

    @Override
    public EntitySummaryBean getEntitySummary(Company company, String key) throws FlockException {
        Entity entity = getEntity(company, key, true);
        if (entity == null)
            throw new FlockException("Invalid Meta Key [" + key + "]");
        Set<EntityLog> changes = getEntityLogs(entity);
        Collection<EntityTag> tags = entityTagService.getEntityTags(entity);
        EntitySummaryBean esb = new EntitySummaryBean(entity, changes, tags);
        esb.setIndex(indexManager.parseIndex(entity));
        return esb;
    }

    @Override
    public LogDetailBean getFullDetail(Company company, String key, Long logId) {
        Entity entity = getEntity(company, key, true);
        if (entity == null)
            return null;

        EntityLog log = entityDao.getLog(entity, logId);
        entityDao.fetch(log.getLog());
        StoredContent what = contentReader.read(entity, log.getLog());

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
    public Collection<TrackResultBean> trackEntities(DocumentType documentType, FortressSegment segment, Collection<EntityInputBean> entityInputs, Future<Collection<TagResultBean>> tags) throws InterruptedException, ExecutionException, FlockException, IOException {
        Collection<TrackResultBean> arb = new ArrayList<>();
        for (EntityInputBean inputBean : entityInputs) {
            if (documentType == null || documentType.getCode() == null || documentType.getId() == null)
                documentType = conceptService.resolveByDocCode(segment.getFortress(), inputBean.getDocumentType().getName());

            assert (documentType != null);
            assert (documentType.getCode() != null);
            TrackResultBean result = createEntity(documentType, segment, inputBean, tags);
            if (result.getEntity() != null)
                logger.trace("Batch Processed {}, code=[{}], documentName=[{}]", result.getEntity().getId(), inputBean.getCode(), inputBean.getDocumentType().getName());
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
        Entity entity = getEntity(company, key);
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
    public Map<String, Collection<Entity>> getCrossReference(Company company, String key, String relationship) throws FlockException {
        Entity entity = getEntity(company, key);
        if (entity == null) {
            throw new FlockException("Unable to find the Entity [" + key + "]. Perhaps it has not been processed yet?");
        }

        return entityDao.getCrossReference(entity, relationship);
    }

    @Override
    public Map<String, Collection<Entity>> getCrossReference(Company company, String fortressName, String code, String xRefName) throws FlockException {
        Fortress fortress = fortressService.findByName(company, fortressName);

        Entity source = entityDao.findByCodeUnique(fortress.getId(), code);
        if (source == null) {
            throw new FlockException("Unable to find the Entity [" + code + "]");
        }

        return entityDao.getCrossReference(source, xRefName);
    }

    @Override
    public Collection<EntityKeyBean> linkEntities(Company company, EntityKeyBean sourceKey, Collection<EntityKeyBean> entityKeys, String linkName) throws FlockException {
        Fortress fortress = fortressService.findByCode(company, sourceKey.getFortressName());
        if (fortress == null)
            throw new FlockException("Unable to locate the fortress " + sourceKey.getFortressName());
        Entity sourceEntity;
        if (sourceKey.getDocumentType() == null || sourceKey.getDocumentType().equals("*"))
            sourceEntity = entityDao.findByCodeUnique(fortress.getId(), sourceKey.getCode());
        else {
            DocumentType document = conceptService.resolveByDocCode(fortress, sourceKey.getDocumentType(), false);
            sourceEntity = entityDao.findByCode(fortress.getId(), document, sourceKey.getCode());
        }
        if (sourceEntity == null)
            // ToDo: Should we create it??
            throw new FlockException("Unable to locate the source Entity [" + sourceKey + "]");

        //16051954
        Collection<Entity> targets = new ArrayList<>();
        List<EntityKeyBean> ignored = new ArrayList<>();

        for (EntityKeyBean targetKey : entityKeys) {
            int count = 1;

            Collection<Entity> entities = new ArrayList<>();
            if (targetKey.getDocumentType().equals("*"))
                entities = findByCode(fortress, targetKey.getCode());
            else {
                Entity entity = findByCode(fortressService.findByCode(company, targetKey.getFortressName()), targetKey.getDocumentType(), targetKey.getCode());
                if (entity == null) {
                    if (targetKey.getMissingAction() == EntityKeyBean.ACTION.CREATE) {
                        // DAT-443 - Create a place holding entity if the requested one does not exist
                        DocumentType documentType = conceptService.resolveByDocCode(fortress, targetKey.getDocumentType(), false);
                        if (documentType != null) {
                            EntityInputBean eib = new EntityInputBean(fortress.getCode(), targetKey.getDocumentType()).setCode(targetKey.getCode());
                            TrackResultBean trackResult = createEntity(documentType, fortress.getDefaultSegment(), eib, null);
                            entity = trackResult.getEntity();
                        }
                    } else if (targetKey.getMissingAction() == EntityKeyBean.ACTION.IGNORE) {
                        ignored.add(targetKey);
                    } else {
                        throw new FlockException("Unable to resolve the target entity " + targetKey.toString());
                    }
                }
                if (entity != null) {
                    entities.add(entity);
                }
            }
            if (entities != null) {
                for (Entity entity : entities) {
                    if (count > 1 || count == 0)
                        ignored.add(targetKey);
                    else
                        targets.add(entity);
                    count++;
                }
            }

        }
        // ToDo: Update search doc?
        if (!targets.isEmpty())
            entityDao.linkEntities(sourceEntity, targets, linkName);
        return ignored;
    }

    @Override
    public Map<String, Entity> getEntities(Company company, Collection<String> keys) {
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
    public void recordSearchResult(SearchResult searchResult, Long metaId) throws FlockException {
        // Only exists and is public because we need the transaction
        Entity entity;
        try {
            entity = getEntity(metaId); // Happens during development when Graph is cleared down and incoming search results are on the q
        } catch (DataRetrievalFailureException | IllegalStateException e) {
            logger.error("Unable to locate entity for entity {} in order to handle the search key. Ignoring.", metaId);
            throw new FlockException("Unable to locate entity for entity " + metaId + " in order to handle the search result.");
        }

        if (entity == null) {
            logger.error("key could not be found for [{}]", searchResult);
            throw new AmqpRejectAndDontRequeueException("key could not be found for [{" + searchResult.getKey() + "}]");
        }

        if (entity.getSearch() == null || platformConfig.isSearchRequiredToConfirm()) { // Search ACK
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
            logger.error("Unable to locate track log {} for metaId {} in order to handle the search key. Ignoring.", searchResult.getLogId(), entity.getId());
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
    public Collection<EntityTag> getLastLogTags(Company company, String key) throws FlockException {
        EntityLog lastLog = getLastEntityLog(company, key);
        if (lastLog == null)
            return new ArrayList<>();

        return getLogTags(company, lastLog.getLog());
    }

    @Override
    public EntityLog getLastEntityLog(Company company, String key) throws FlockException {
        Entity entity = getEntity(company, key);
        if (entity == null)
            throw new NotFoundException("Unable to locate the requested Entity for key " + key);
        return entityDao.getLastEntityLog(entity);
    }

    private Collection<EntityTag> getLogTags(Company company, Log log) {
        return entityTagService.findLogTags(company, log);

    }

    @Override
    public EntityLog getEntityLog(Company company, String key, Long logId) throws FlockException {
        Entity entity = getEntity(company, key);
        EntityLog log = entityDao.getLog(entity, logId);

        if (log == null)
            throw new FlockException(String.format("Invalid logId %d for %s ", logId, key));

        if (!log.getEntity().getId().equals(entity.getId()))
            throw new FlockException(String.format("Invalid logId %d for %s ", logId, key));
        return log;
    }

    @Override
    public Collection<EntityTag> getLogTags(Company company, EntityLog entityLog) {
        return getLogTags(company, entityLog.getLog());  //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public Collection<EntityLinkInputBean> linkEntities(Company
                                                                company, Collection<EntityLinkInputBean> entityLinks) {
        for (EntityLinkInputBean entityLink : entityLinks) {
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
