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

package com.auditbucket.engine.service;

import com.auditbucket.engine.repo.neo4j.EntityDaoNeo;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.SystemUserService;
import com.auditbucket.search.model.EntitySearchChange;
import com.auditbucket.search.model.SearchResult;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.EntitySummaryBean;
import com.auditbucket.track.bean.LogDetailBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.*;
import com.auditbucket.track.service.EntityTagService;
import com.auditbucket.track.service.SchemaService;
import com.auditbucket.track.service.TrackService;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Transactional services to support record and working with headers and logs
 * <p/>
 * User: Mike Holdsworth
 * Date: 8/04/13
 */
@Service
@Transactional
public class TrackServiceNeo4j implements TrackService {
    private static final String EMPTY = "";
    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    TrackEventService trackEventService;

    @Autowired
    SystemUserService sysUserService;

    @Autowired
    private SecurityHelper securityHelper;


    @Autowired
    EntityTagService entityTagService;

    @Autowired
    SchemaService schemaService;

    @Autowired
    TxService txService;

    @Autowired
    KvService kvService;

    @Autowired
    EntityDaoNeo trackDao;

    @Autowired
    com.auditbucket.track.service.TagService tagService;

    private Logger logger = LoggerFactory.getLogger(TrackServiceNeo4j.class);

    @Override
    public LogWhat getWhat(Entity entity, Log change) {
        return kvService.getWhat(entity, change);
    }

    /**
     * Creates a unique Entity for the fortress. FortressUserNode is automatically
     * created if it does not exist.
     *
     * @return unique primary key to be used for subsequent log calls
     */
    public TrackResultBean createEntity(Fortress fortress, EntityInputBean inputBean) {
        DocumentType documentType = schemaService.resolveDocType(fortress, inputBean.getDocumentType());

        Entity ah = null;
        if (inputBean.getMetaKey() != null) {
            ah = getEntity(fortress.getCompany(), inputBean.getMetaKey());
        }
        if (ah == null && (inputBean.getCallerRef() != null && !inputBean.getCallerRef().equals(EMPTY)))
            ah = findByCallerRef(fortress, documentType, inputBean.getCallerRef());
        if (ah != null) {
            logger.debug("Existing entity record found by Caller Ref [{}] found [{}]", inputBean.getCallerRef(), ah.getMetaKey());
            inputBean.setMetaKey(ah.getMetaKey());

            TrackResultBean arb = new TrackResultBean(ah);
            arb.setEntityInputBean(inputBean);
            arb.setWasDuplicate();
            arb.setLogInput(inputBean.getLog());
            // Could be rewriting tags
            // DAT-153 - move this to the end of the process?
            TrackLog trackLog = getLastLog(ah.getId());
            arb.setTags(entityTagService.associateTags(fortress.getCompany(), ah, trackLog, inputBean.getTags()));
            return arb;
        }

        try {
            ah = makeEntity(inputBean, fortress, documentType);
        } catch (DatagioException e) {
            logger.error(e.getMessage());
            return new TrackResultBean("Error processing inputBean [{}]" + inputBean + ". Error " + e.getMessage());
        }
        TrackResultBean resultBean = new TrackResultBean(ah);
        resultBean.setEntityInputBean(inputBean);
        resultBean.setTags(entityTagService.associateTags(fortress.getCompany(), resultBean.getEntity(), null, inputBean.getTags()));

        resultBean.setLogInput(inputBean.getLog());
        return resultBean;

    }

    public Entity makeEntity(EntityInputBean inputBean, Fortress fortress, DocumentType documentType) throws DatagioException {
        Entity entity = trackDao.create(inputBean, fortress, documentType);
        if (entity.getId() == null)
            inputBean.setMetaKey("NT " + fortress.getFortressKey()); // We ain't tracking this

        inputBean.setMetaKey(entity.getMetaKey());
        logger.trace("Entity created: id=[{}] key=[{}] for fortress [{}] callerKeyRef = [{}]", entity.getId(), entity.getMetaKey(), fortress.getCode(), entity.getCallerKeyRef());
        return entity;
    }

    /**
     * When you have no API key, find if authorised
     *
     * @param metaKey known GUID
     * @return header the caller is authorised to view
     */
    @Override
    public Entity getEntity(@NotEmpty String metaKey) {
        String userName = securityHelper.getLoggedInUser();
        SystemUser su = sysUserService.findByLogin(userName);
        if (su == null)
            throw new SecurityException(userName + "Not authorised to retrieve headers");

        return getEntity(su.getCompany(), metaKey, false);
    }

    @Override
    public Entity getEntity(Company company, String metaKey) {
        if (company == null && metaKey != null)
            return getEntity(metaKey); // we can still find by authenticated user

        if (company == null)
            return null;

        return getEntity(company, metaKey, false);
    }

    @Override
    public Entity getEntity(Company company, @NotEmpty String metaKey, boolean inflate) {

        if (company == null)
            return getEntity(metaKey);
        Entity ah = trackDao.findEntity(metaKey, inflate);
        if (ah == null || ah.getFortress() == null)
            return null;

        if (!(ah.getFortress().getCompany().getId().equals(company.getId())))
            throw new SecurityException("CompanyNode mismatch. [" + metaKey + "] working for [" + company.getName() + "] cannot write meta records for [" + ah.getFortress().getCompany().getName() + "]");
        return ah;
    }

    @Override
    public Entity getEntity(Entity entity) {
        return trackDao.fetch(entity);
    }

    @Override
    public Collection<Entity> getEntities(Fortress fortress, Long skipTo) {
        return trackDao.findHeaders(fortress.getId(), skipTo);
    }

    @Override
    public Collection<Entity> getEntities(Fortress fortress, String docTypeName, Long skipTo) {
        DocumentType docType = schemaService.resolveDocType(fortress, docTypeName);
        return trackDao.findHeaders(fortress.getId(), docType.getName(), skipTo);
    }


    Entity getEntity(Long id) {
        return trackDao.getHeader(id);
    }


    @Override
    public void updateHeader(Entity entity) {
        trackDao.save(entity);
    }

    @Override
    public TrackLog getLastLog(String metaKey) throws DatagioException {
        Entity header = getValidEntity(metaKey);
        return getLastLog(header.getId());

    }

    @Override
    public TrackLog getLastLog(Long headerId) {
        return trackDao.getLastLog(headerId);
    }

    @Override
    public Set<TrackLog> getLogs(Long headerId) {
        return trackDao.getLogs(headerId);
    }

    @Override
    public Set<TrackLog> getLogs(Company company, String headerKey) throws DatagioException {
        Entity entity = getEntity(company, headerKey);
        return trackDao.getLogs(entity.getId());
    }

    @Override
    public Set<TrackLog> getLogs(String metaKey, Date from, Date to) throws DatagioException {
        Entity entity = getValidEntity(metaKey);
        return getLogs(entity, from, to);
    }

    Set<TrackLog> getLogs(Entity entity, Date from, Date to) {
        return trackDao.getLogs(entity.getId(), from, to);
    }

    /**
     * This can be used toa assist in compensating transactions to roll back the last change
     * if the caller decides a rollback is required after the log has been written.
     * If there are no Log records left, then the entity will also be removed and the
     * AB headerKey will be forever invalid.
     *
     * @param company   validated company the caller is authorised to work with
     * @param entity UID of the entity
     * @return Future<MetaSearchChange> search change to index, or null if there are no logs
     */
    @Override
    public EntitySearchChange cancelLastLog(Company company, Entity entity) throws IOException, DatagioException {
        TrackLog existingLog = getLastLog(entity.getId());
        if (existingLog == null)
            return null;

        Log currentLog = existingLog.getLog();
        Log fromLog = currentLog.getPreviousLog();
        String searchKey = entity.getSearchKey();

        if (fromLog != null) {
            trackDao.fetch(fromLog);
            TrackLog newTrack = trackDao.getLog(fromLog.getTrackLog().getId());
            entity.setLastChange(fromLog);
            entity.setLastUser(fortressService.getFortressUser(entity.getFortress(), fromLog.getWho().getCode()));
            entity.setFortressLastWhen(newTrack.getFortressWhen());
            entity = trackDao.save(entity);
            entityTagService.moveTags(company, fromLog, entity);
            trackDao.delete(currentLog);

        } else {
            // No changes left, there is now just a header
            // ToDo: What to to with the header? Delete it? Store the "canceled By" User? Assign the log to a Cancelled RLX?
            // Delete from ElasticSearch??
            entity.setLastUser(fortressService.getFortressUser(entity.getFortress(), entity.getCreatedBy().getCode()));
            entity.setFortressLastWhen(0l);
            entity.setSearchKey(null);
            entity = trackDao.save(entity);
            trackDao.delete(currentLog);
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

        // Sync the update to ab-search.
        if (entity.getFortress().isSearchActive() && !entity.isSearchSuppressed()) {
            // Update against the Entity only by re-indexing the search document
            HashMap<String, Object> priorWhat = (HashMap<String, Object>) kvService.getWhat(entity, fromLog).getWhat();

            searchDocument = new EntitySearchChange(entity, priorWhat, fromLog.getEvent().getCode(), new DateTime(fromLog.getTrackLog().getFortressWhen()));
            searchDocument.setTags(entityTagService.findTrackTags(company, entity));
            searchDocument.setReplyRequired(false);
            searchDocument.setForceReindex(true);
        }
        return searchDocument;
    }

    /**
     * counts the number of logs that exist for the given entity
     *
     * @param company   validated company the caller is authorised to work with
     * @param metaKey GUID
     * @return count
     */
    @Override
    public int getLogCount(Company company, String metaKey) throws DatagioException {
        Entity entity = getEntity(company, metaKey);
        logger.debug("looking for logs for Entity id [{}] - metaKey [{}]", entity.getId(), metaKey);
        int logs = trackDao.getLogs(entity.getId()).size();
        logger.debug("Log count {}", logs);
        return logs;
    }

    private Entity getValidEntity(String headerKey) throws DatagioException {
        return getValidEntity(headerKey, false);
    }

    private Entity getValidEntity(String headerKey, boolean inflate) throws DatagioException {
        Entity header = trackDao.findEntity(headerKey, inflate);
        if (header == null) {
            throw new DatagioException("No entity for [" + headerKey + "]");
        }
        String userName = securityHelper.getLoggedInUser();
        SystemUser sysUser = sysUserService.findByLogin(userName);

        if (!header.getFortress().getCompany().getId().equals(sysUser.getCompany().getId())) {
            throw new SecurityException("Not authorised to work with this meta data");
        }
        return header;

    }

    @Override
    public Entity findByCallerRef(String fortress, String documentType, String callerRef) {
        Fortress iFortress = fortressService.findByName(fortress);
        if (iFortress == null)
            return null;

        return findByCallerRef(iFortress, documentType, callerRef);
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
        return findByCallerRef(fortress, documentType, callerRef);
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
    public Iterable<Entity> findByCallerRef(Company company, String fortressName, String callerRef) {
        Fortress fortress = fortressService.findByName(company, fortressName);
        return findByCallerRef(fortress, callerRef);
    }

    @Override
    public Collection<Entity> findByCallerRef(Fortress fortress, String callerRef) {
        return trackDao.findByCallerRef(fortress.getId(), callerRef.trim());
    }


    @Override
    public Entity findByCallerRef(Fortress fortress, String documentType, String callerRef) {

        DocumentType doc = schemaService.resolveDocType(fortress, documentType, false);
        if (doc == null) {
            logger.debug("Unable to find document for callerRef {}, {}, {}", fortress, documentType, callerRef);
            return null;
        }
        return findByCallerRef(fortress, doc, callerRef);

    }

    /**
     * @param fortress     owning system
     * @param documentType class of document
     * @param callerRef    fortressName primary key
     * @return LogResultBean or NULL.
     */
    @Override
    public Entity findByCallerRef(Fortress fortress, DocumentType documentType, String callerRef) {
        return trackDao.findByCallerRef(fortress.getId(), documentType.getId(), callerRef.trim());
    }


    @Override
    public EntitySummaryBean getEntitySummary(Company company, String metaKey) throws DatagioException {
        Entity header = getEntity(company, metaKey, true);
        if (header == null)
            throw new DatagioException("Invalid Meta Key [" + metaKey + "]");
        Set<TrackLog> changes = getLogs(header.getId());
        Set<TrackTag> tags = entityTagService.findTrackTags(company, header);
        return new EntitySummaryBean(header, changes, tags);
    }


    @Override
    public LogDetailBean getFullDetail(String metaKey, Long logId) {
        Company company = securityHelper.getCompany();
        return getFullDetail(company, metaKey, logId);
    }

    @Override
    public LogDetailBean getFullDetail(Company company, String metaKey, Long logId) {
        Entity entity = getEntity(company, metaKey, true);
        if (entity == null)
            return null;

        TrackLog log = trackDao.getLog(logId);
        trackDao.fetch(log.getLog());
        LogWhat what = kvService.getWhat(entity, log.getLog());
        log.getLog().setWhat(what);
        return new LogDetailBean(log, what);
    }

    @Override
    public TrackLog getLogForEntity(Entity header, Long logId) {
        if (header != null) {

            TrackLog log = trackDao.getLog(logId);
            if (!log.getEntity().getId().equals(header.getId()))
                return null;

            trackDao.fetch(log.getLog());
            return log;
        }
        return null;
    }

    @Override
    public Iterable<TrackResultBean> trackEntities(Fortress fortress, Iterable<EntityInputBean> inputBeans) throws InterruptedException, ExecutionException, DatagioException, IOException {
        Collection<TrackResultBean> arb = new CopyOnWriteArrayList<>();
        for (EntityInputBean inputBean : inputBeans) {
            logger.trace("Batch Processing metaKey=[{}], documentType=[{}]", inputBean.getCallerRef(), inputBean.getDocumentType());
            arb.add(createEntity(fortress, inputBean));
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
    public Collection<String> crossReference(Company company, String metaKey, Collection<String> xRef, String relationshipName) throws DatagioException {
        Entity header = getEntity(company, metaKey);
        if (header == null) {
            throw new DatagioException("Unable to find the Meta Header [" + metaKey + "]. Perhaps it has not been processed yet?");
        }
        Collection<Entity> targets = new ArrayList<>();
        Collection<String> ignored = new ArrayList<>();
        for (String next : xRef) {
            Entity m = getEntity(company, next);
            if (m != null) {
                targets.add(m);
            } else {
                ignored.add(next);
                //logger.info ("Unable to find MetaKey ["+entity+"]. Skipping");
            }
        }
        trackDao.crossReference(header, targets, relationshipName);
        return ignored;
    }

    @Override
    public Map<String, Collection<Entity>> getCrossReference(Company company, String metaKey, String xRefName) throws DatagioException {
        Entity header = getEntity(company, metaKey);
        if (header == null) {
            throw new DatagioException("Unable to find the Meta Header [" + metaKey + "]. Perhaps it has not been processed yet?");
        }

        return trackDao.getCrossReference(company, header, xRefName);
    }

    @Override
    public Map<String, Collection<Entity>> getCrossReference(Company company, String fortressName, String callerRef, String xRefName) throws DatagioException {
        Fortress fortress = fortressService.findByName(company, fortressName);

        Entity source = trackDao.findByCallerRefUnique(fortress.getId(), callerRef);
        if (source == null) {
            throw new DatagioException("Unable to find the Meta Header [" + callerRef + "]");
        }

        return trackDao.getCrossReference(company, source, xRefName);
    }

    @Override
    public List<EntityKey> crossReferenceByCallerRef(Company company, EntityKey sourceKey, Collection<EntityKey> entityKeys, String xRefName) throws DatagioException {
        Fortress f = fortressService.findByName(company, sourceKey.getFortressName());
        if ( f == null )
            throw new DatagioException("Unable to locate the fortress "+sourceKey.getFortressName());
        Entity fromEntity;
        if (sourceKey.getDocumentType() == null || sourceKey.getDocumentType().equals("*"))
            fromEntity = trackDao.findByCallerRefUnique(f.getId(), sourceKey.getCallerRef());
        else {
            DocumentType document = schemaService.resolveDocType(f, sourceKey.getDocumentType(), false);
            fromEntity = trackDao.findByCallerRef(f.getId(), document.getId(), sourceKey.getCallerRef());
        }
        if (fromEntity == null)
            throw new DatagioException("Unable to locate the Entity for MetaKey [" + sourceKey + "]");

        //16051954
        Collection<Entity> targets = new ArrayList<>();
        List<EntityKey> ignored = new ArrayList<>();

        for (EntityKey entityKey : entityKeys) {
            int count = 1;

            Collection<Entity> entities;
            if (entityKey.getDocumentType().equals("*"))
                entities = findByCallerRef(f, entityKey.getCallerRef());
            else {
                Entity mh = findByCallerRef(fortressService.findByName(company, entityKey.getFortressName()), entityKey.getDocumentType(), entityKey.getCallerRef());
                if (mh == null) {
                    ignored.add(entityKey);
                    entities = null;

                } else {
                    Collection<Entity> array = new ArrayList<>();
                    array.add(mh);
                    entities = array;
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
            trackDao.crossReference(fromEntity, targets, xRefName);
        return ignored;
    }

    @Override
    public Map<String, Entity> getEntities(Company company, Collection<String> metaKeys) {
        return trackDao.findEntities(company, metaKeys);
    }

    @Override
    public void purge(Fortress fortress) {

        trackDao.purgeTagRelationships(fortress);

        trackDao.purgeFortressLogs(fortress);
        trackDao.purgePeopleRelationships(fortress);
        schemaService.purge(fortress);
        //trackDao.purgeFortressDocuments(fortress);
        trackDao.purgeEntities(fortress);

    }

    @Override
    public void saveMetaData(SearchResult searchResult, Long metaId) {
        // Only exists and is public because we need the transaction
        Entity entity;
        try {
            entity = getEntity(metaId); // Happens during development when Graph is cleared down and incoming search results are on the q
        } catch (DataRetrievalFailureException e) {
            logger.error("Unable to locate entity for metaId {} in order to handle the search metaKey. Ignoring.", metaId);
            return;
        }

        if (entity == null) {
            logger.error("metaKey could not be found for [{}]", searchResult);
            return;
        }

        if (entity.getSearchKey() == null) {
            entity.setSearchKey(searchResult.getSearchKey());
            trackDao.save(entity);
            logger.trace("Updating Header{} search searchResult =[{}]", entity.getMetaKey(), searchResult);
        }

        if (searchResult.getLogId() == null) {
            // Indexing entity meta data only
            return;
        }
        TrackLog trackLog;
        // The change has been indexed
        try {
            trackLog = trackDao.getLog(searchResult.getLogId());
            if (trackLog == null) {
                logger.error("Illegal node requested from handleSearchResult [{}]", searchResult.getLogId());
                return;
            }
        } catch (DataRetrievalFailureException e) {
            logger.error("Unable to locate track log {} for metaId {} in order to handle the search metaKey. Ignoring.", searchResult.getLogId(), entity.getId());
            return;
        }

        // Another thread may have processed this so save an update
        if (!trackLog.isIndexed()) {
            // We need to know that the change we requested to index has been indexed.
            logger.trace("Updating index status for {}", trackLog);
            trackLog.setIsIndexed();
            trackDao.save(trackLog);

        } else {
            logger.trace("Skipping {} as it is already indexed", trackLog);
        }
    }

    @Override
    public Set<TrackTag> getLastLogTags(Company company, String metaKey) throws DatagioException {
        TrackLog lastLog = getLastLog(company, metaKey);
        if (lastLog == null)
            return new HashSet<>();

        return getLogTags(company, lastLog.getLog());
    }
    @Override
    public TrackLog getLastLog(Company company, String metaKey) throws DatagioException {
        Entity header = getEntity(company, metaKey);
        return trackDao.getLastLog(header.getId());
    }


    private Set<TrackTag> getLogTags(Company company, Log log) {
        return entityTagService.findLogTags(company, log);

    }

    @Override
    public TrackLog getLog(Company company, String metaKey, long logId) throws DatagioException {
        Entity header = getEntity(company, metaKey);
        TrackLog log = trackDao.getLog(logId);

        if (log == null)
            throw new DatagioException(String.format("Invalid logId %d for %s ", logId, metaKey));

        if (!log.getEntity().getId().equals(header.getId()))
            throw new DatagioException(String.format("Invalid logId %d for %s ", logId, metaKey));
        return log;
    }

    @Override
    public Set<TrackTag> getLogTags(Company company, TrackLog tl) {
        return getLogTags(company, tl.getLog());  //To change body of created methods use File | Settings | File Templates.
    }
}
