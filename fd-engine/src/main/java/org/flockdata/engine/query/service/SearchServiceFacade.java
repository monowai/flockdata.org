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

package org.flockdata.engine.query.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.flockdata.data.Company;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityLog;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.data.Log;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.engine.admin.service.StorageProxy;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.integration.search.DeleteIndex;
import org.flockdata.engine.integration.search.EntitySearchWriter;
import org.flockdata.engine.integration.search.EntitySearchWriter.EntitySearchWriterGateway;
import org.flockdata.engine.integration.search.FdViewQuery.FdViewQueryGateway;
import org.flockdata.engine.track.service.EntityService;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.integration.IndexManager;
import org.flockdata.registration.TagResultBean;
import org.flockdata.search.AdminRequest;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.EsSearchRequestResult;
import org.flockdata.search.QueryParams;
import org.flockdata.search.SearchChanges;
import org.flockdata.search.TagSearchChange;
import org.flockdata.store.StoredContent;
import org.flockdata.track.EntityTagFinder;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.FdTagResultBean;
import org.flockdata.track.bean.SearchChange;
import org.flockdata.track.bean.TrackResultBean;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Search Service interactions
 *
 * @author mholdsworth
 * @since 4/04/2014
 */
@Service
@IntegrationComponentScan
@Transactional
public class SearchServiceFacade {
  private final EntityService entityService;
  private final StorageProxy storageProxy;
  private final DeleteIndex.DeleteIndexGateway deleteIndexGateway;
  private final IndexManager indexManager;
  private final FortressService fortressService;
  private final EntityTagFinder taxonomyTags;
  private final EntityTagFinder defaultTagFinder;
  private final PlatformConfig engineConfig;
  private Logger logger = LoggerFactory.getLogger(SearchServiceFacade.class);
  private EntitySearchWriterGateway searchWriterGateway;
  private EntitySearchWriter entitySearchWriter;
  private FdViewQueryGateway fdViewQueryGateway;


  @Autowired
  public SearchServiceFacade(FortressService fortressService, EntityService entityService, StorageProxy storageProxy,
                             IndexManager indexManager, @Qualifier("engineConfig") PlatformConfig engineConfig,
                             DeleteIndex.DeleteIndexGateway deleteIndexGateway, EntityTagFinder taxonomyTags,
                             EntityTagFinder defaultTagFinder) {

    this.fortressService = fortressService;
    this.entityService = entityService;
    this.storageProxy = storageProxy;
    this.indexManager = indexManager;
    this.engineConfig = engineConfig;
    this.deleteIndexGateway = deleteIndexGateway;
    this.taxonomyTags = taxonomyTags;
    this.defaultTagFinder = defaultTagFinder;
  }

  @Autowired(required = false)
  void setEntitySearchWriter(EntitySearchWriter entitySearchWriter) {
    this.entitySearchWriter = entitySearchWriter;
  }

  @Autowired(required = false)
  private void setEntitySearchWriterGateway(EntitySearchWriterGateway entitySearchWriterGateway) {
    this.searchWriterGateway = entitySearchWriterGateway;
  }

  @Autowired(required = false)
  private void setFdViewQueryGateway(FdViewQueryGateway fdViewQueryGateway) {
    this.fdViewQueryGateway = fdViewQueryGateway;
  }

  public void makeChangeSearchable(SearchChange searchChange) {
    if (searchChange == null) {
      return;
    }
    Collection<SearchChange> searchChanges = new ArrayList<>();
    searchChanges.add(searchChange);
    makeChangesSearchable(searchChanges);
  }

  public Boolean makeChangesSearchable(Collection<SearchChange> searchDocuments) {
    if (searchDocuments.isEmpty())
    //  return new AsyncResult<>(null);
    {
      return false;
    }

    if (searchDocuments.size() == 1) {
      logger.debug("Sending request to index changes [{}]]", searchDocuments.iterator().next().getId());
    } else {
      logger.debug("Sending request to index [{}]] logs", searchDocuments.size());
    }
    if (entitySearchWriter != null) {
      searchWriterGateway.makeSearchChanges(new SearchChanges(searchDocuments));
    } else {
      logger.debug("Search Gateway is disabled");
    }
    logger.debug("[{}] log requests sent to search", searchDocuments.size());
    return true;
  }

  /**
   * This is the primary function to populates an object for indexing into the search service
   *
   * @param trackResultBean track result to process
   * @return SearchChange that will can be indexed
   */
  public EntitySearchChange getEntityChange(TrackResultBean trackResultBean) {
    assert trackResultBean != null;

    Entity entity = trackResultBean.getEntity();
    if (entity.getLastUser() != null && entity.getLastUser().getCode() == null) {
      fortressService.getUser(entity.getLastUser().getId());
    }

    return getEntityChange(trackResultBean, getLog(trackResultBean));
  }

  /**
   * Construct a SearchChange for the Entity Content.
   * <p>
   * Applies the appropriate TagStructure to index as well as set any Parent entity -
   * a [p:parent] - relationship to another entity.
   * <p>
   * If you're looking for how the content gets from the Graph to ElasticSearch you're in the right place.
   *
   * @param trackResultBean Payload to index
   * @param entityLog       Log to work with (usually the "current" log)
   * @return object ready to index
   */
  public EntitySearchChange getEntityChange(TrackResultBean trackResultBean, EntityLog entityLog) {
    ContentInputBean contentInput = trackResultBean.getContentInput();
    Entity entity = trackResultBean.getEntity();

    EntitySearchChange searchDocument = new EntitySearchChange(entity, entityLog, contentInput, indexManager.toIndex(entity));

    if (entityLog != null) {
      // Used to reconcile that the change was actually indexed
      logger.trace("Preparing Search Document [{}]", entityLog);

      searchDocument.setLogId(entityLog.getId());
      if (entityLog.getLog().getMadeBy() != null) {
        searchDocument.setWho(entityLog.getLog().getMadeBy().getCode());
      }
      if (entityLog.getSysWhen() != 0) {
        searchDocument.setSysWhen(entityLog.getSysWhen());
      }
    }
    // ToDo: Can we optimize by using tags already tracked in the result bean?
    EntityTagFinder tagFinder = getTagFinder(fortressService.getTagStructureFinder(entity));
    searchDocument.setStructuredTags(tagFinder.getTagStructure(), tagFinder.getEntityTags(trackResultBean));

    // Description is not carried against the entity - todo: configurable?
    if (trackResultBean.getEntityInputBean() != null) {
      searchDocument.setDescription(trackResultBean.getEntityInputBean().getDescription());
    }
    searchDocument.setName(entity.getName());
    searchDocument.setSearchKey(entity.getSearchKey());

    if (trackResultBean.getEntityInputBean() != null) {
      // Gather all Entities linked directly to this entity
      for (String relationship : trackResultBean.getEntityInputBean().getEntityLinks().keySet()) {
        List<EntityKeyBean> linkTo = filter(entity, trackResultBean.getEntityInputBean().getEntityLinks().get(relationship));
        Collection<EntityKeyBean> links = entityService.getEntities(trackResultBean.getCompany(), linkTo);
        searchDocument.addEntityLinks(links); // Root level entities are added
        for (EntityKeyBean entityKeyBean : linkTo) {
          Entity analyzeMe = entityKeyBean.getResolvedEntity();
          if (analyzeMe == null) {
            analyzeMe = entityService.find(trackResultBean.getCompany(), entityKeyBean);
          }
          if (analyzeMe != null) {
            searchDocument.addEntityLinks(entityService.getNestedParentEntities(analyzeMe, entityKeyBean.getResolvedDocument()));
          }
        }


//                searchDocument.addEntityLinks(linkTo);
      }


    }

    try {
      if (logger.isTraceEnabled()) {
        logger.trace("JSON {}", FdJsonObjectMapper.getObjectMapper().writeValueAsString(searchDocument));
      }
    } catch (JsonProcessingException e) {
      logger.error(e.getMessage());
      return null;
    }

    if (searchDocument.getSysWhen() == 0L) {
      searchDocument.setSysWhen(entity.getDateCreated());
    }

    if (entity.getId() == null) {
      logger.debug("No entityId so we are not expecting a reply");
      searchDocument.setReplyRequired(false);
      searchDocument.setSearchKey(null);
    }

    if (!engineConfig.isSearchRequiredToConfirm())
    // If we already have the search key for this Entity then don't bother us with a reply
    {
      searchDocument.setReplyRequired(entity.getSearch() == null);
    }


    return searchDocument;
  }

  // Don't index a reference document in this Entity unless it is a parent
  private List<EntityKeyBean> filter(Entity entity, List<EntityKeyBean> entityKeyBeen) {
    return entityKeyBeen
        .stream()
        .filter(
            entityKeyBean -> entityKeyBean.isParent() || !entityKeyBean.getDocumentType().equals(entity.getType()))
        .collect(Collectors.toList());
  }

  /**
   * Forces an entity to be re-indexed from the graph through to ElasticSearch
   *
   * @param entity  current view of the entity
   * @param lastLog last known content data
   * @return SearchChange payload that can be sent to fd-search
   */
  public EntitySearchChange rebuild(Entity entity, EntityLog lastLog) {
    // ToDO: this should work via getEntityChange
    // The way that this is implemented may means the description will be lost as that is only
    // persisted in ES
    try {
      Log lastChange = null;
      if (lastLog != null) {
        lastChange = lastLog.getLog();
      }

      //if () {
      // Update against the Entity only by re-indexing the search document
      EntitySearchChange searchDocument;
      //Entity entityBean = new EntityBean(entity);
      if (lastChange != null) {
        if (!entity.isNoLogs()) {
          StoredContent content = storageProxy.read(entity, lastChange);
          if (content == null) {
            logger.error("Unable to locate content for {} ", entity);
            return null;
          }
          searchDocument = new EntitySearchChange(entity, lastLog, content.getContent(), indexManager.toIndex(entity));
        } else {
          searchDocument = new EntitySearchChange(entity, indexManager.toIndex(entity));
        }

        if (lastChange.getMadeBy() != null) {
          searchDocument.setWho(lastChange.getMadeBy().getCode());
        }
      } else {
        searchDocument = new EntitySearchChange(entity, indexManager.toIndex(entity));
        if (entity.getCreatedBy() != null) {
          searchDocument.setWho(entity.getCreatedBy().getCode());
        }
      }
      EntityTagFinder tagFinder = getTagFinder(fortressService.getTagStructureFinder(entity));
      searchDocument.setStructuredTags(tagFinder.getTagStructure(), tagFinder.getEntityTags(new TrackResultBean(entity, new DocumentNode(entity.getType()))));

      if (!engineConfig.isSearchRequiredToConfirm()) {
        searchDocument.setReplyRequired(false);
      }

      searchDocument.setForceReindex(true);

      return searchDocument;
      //}
    } catch (Exception e) {
      logger.error("error", e);
    }
    return null;
  }

  private EntityTagFinder getTagFinder(EntityTag.TAG_STRUCTURE tagStructureFinder) {
    if (tagStructureFinder == EntityTag.TAG_STRUCTURE.TAXONOMY) {
      return taxonomyTags;
    } else {
      return defaultTagFinder;
    }
  }

  public EsSearchRequestResult search(QueryParams queryParams) {
    return fdViewQueryGateway.fdSearch(queryParams);
  }

  public void purge(Fortress fortress) {
    if (deleteIndexGateway != null) {
      if (fortress.isSearchEnabled() && fortress.getRootIndex() != null && !fortress.getRootIndex().equals("")) {
        AdminRequest adminRequest = new AdminRequest(fortress.getRootIndex() + ".*");

        logger.info("Deleting the search indexes {}", adminRequest.getIndexesToDelete().toArray());
        deleteIndexGateway.deleteIndex(adminRequest);
      } else {
        logger.info("Ignoring fortress [{}] - index {} searchEnabled {}", fortress.getName(), fortress.getRootIndex(), fortress.isSearchEnabled());
      }
    } else {
      logger.debug("Delete Index Gateway is not enabled");
    }
  }

  public void purge(String indexName) {
    if (deleteIndexGateway != null) {
      AdminRequest deleteIndex = new AdminRequest();
      deleteIndex.addIndexToDelete(indexName);
      deleteIndexGateway.deleteIndex(deleteIndex);

    } else {
      logger.debug("Delete Index Gateway is not enabled");
    }
  }

  @Async("fd-search")
  @Retryable(include = {NotFoundException.class, InvalidDataAccessResourceUsageException.class, DataIntegrityViolationException.class, ConcurrencyFailureException.class, DeadlockDetectedException.class, ConstraintViolationException.class},
      maxAttempts = 20,
      backoff = @Backoff(delay = 600, multiplier = 5, random = true))
  public void makeChangesSearchable(Fortress fortress, Iterable<TrackResultBean> resultBeans) {
    // ToDo: This needs to be an activation via message-q
    logger.debug("Received request to make changes searchable {}", fortress);
    Collection<SearchChange> changes = new ArrayList<>();
    for (TrackResultBean resultBean : resultBeans) {
      EntitySearchChange change = getChangeToPublish(fortress, resultBean);
      if (change != null) {
        changes.add(change);
      } else {
        logger.debug("Ignoring request to index {}", resultBean);
      }
    }
    if (changes.isEmpty()) {
      logger.debug("No changes to index");
    } else {
      makeChangesSearchable(changes);
    }
    logger.debug("Processed request to make changes searchable {}", fortress);
  }

  /**
   * Returns a change to published
   *
   * @param fortress        hydrated fortress object
   * @param trackResultBean data from which to create the search change
   * @return null if search is not required or is being actively suppressed
   */
  private EntitySearchChange getChangeToPublish(Fortress fortress, TrackResultBean trackResultBean) {
    if (trackResultBean == null) {
      return null;
    }

    // DocumentType can override the fortress
    if (isSearchSuppressed(trackResultBean.getDocumentType())) {
      return null;
    }

    if (trackResultBean.getEntity() == null || !fortress.isSearchEnabled()) {
      return null;
    }

    if (trackResultBean.getEntityInputBean() != null && trackResultBean.getEntityInputBean().isEntityOnly()) {
      return getEntityChange(trackResultBean);
    }

    if (trackResultBean.getCurrentLog() != null) {
      return getEntityChange(trackResultBean);
    }

    return null;

  }

  private boolean isSearchSuppressed(Document documentType) {
    if (documentType == null) {
      return true; // Can't index a doc with no doc type
    }
    if (documentType.isSearchEnabled() != null) // no-null doc type may want to suppress search
    {
      return documentType.isSearchEnabled();
    }
    return false;
  }

  private EntityLog getLog(TrackResultBean trackResultBean) {
    if (!trackResultBean.processLog()) {
      logger.debug("Tracking Entity Content to fd-search is suppressed. Content present {}, LogStatus {}", trackResultBean.getContentInput() != null, trackResultBean.getLogStatus());
      return null;
    }
    if (trackResultBean.getLogStatus() != ContentInputBean.LogStatus.OK || trackResultBean.getCurrentLog() == null) {
      logger.debug("No Entity Content to index against, LogStatus {}", trackResultBean.getLogStatus());
      return null;
    }
    return trackResultBean.getCurrentLog();
  }

  public void reIndex(Collection<Long> entities) {
    // To support DAT-279 - not going to work well with massive result sets
    Collection<EntityNode> entitiesSet = entityService.getEntities(entities);
    Collection<SearchChange> searchChanges = new ArrayList<>();

    for (EntityNode entity : entitiesSet) {
      SearchChange change = rebuild(entity, entityService.getLastEntityLog(entity.getId()));
      if (change != null && entity.getSegment().getFortress().isSearchEnabled() && !entity.isSearchSuppressed()) {
        searchChanges.add(change);
      }
    }
    makeChangesSearchable(searchChanges);
  }

  public void setTags(Entity entity, EntitySearchChange searchDocument) {
    EntityTagFinder tagFinder = getTagFinder(fortressService.getTagStructureFinder(entity));
    searchDocument.setStructuredTags(tagFinder.getTagStructure(), tagFinder.getEntityTags(new TrackResultBean(entity, new DocumentNode(entity.getType()))));

  }

  public Boolean makeTagsSearchable(Company company, Collection<FdTagResultBean> tagResults) {
    CompanyNode fdCompany = (CompanyNode) company;
    Collection<SearchChange> tagSearchChanges = tagResults.stream().filter(TagResultBean::isNewTag)
        .map(tagResult ->
            getTagChangeToPublish(fdCompany, tagResult))
        .collect(Collectors.toCollection(ArrayList::new));

    return makeChangesSearchable(tagSearchChanges);
  }

  private TagSearchChange getTagChangeToPublish(CompanyNode company, FdTagResultBean tagResult) {
    String indexName = indexManager.getTagIndexRoot(company, tagResult.getTag());
    return new TagSearchChange(indexName, tagResult.getTag());
  }
}