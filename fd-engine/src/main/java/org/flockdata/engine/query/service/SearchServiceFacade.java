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

package org.flockdata.engine.query.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.engine.dao.EntityDaoNeo;
import org.flockdata.engine.query.endpoint.FdSearchGateway;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.helper.FlockException;
import org.flockdata.kv.KvContent;
import org.flockdata.kv.service.KvService;
import org.flockdata.model.*;
import org.flockdata.search.model.*;
import org.flockdata.track.EntityTagFinder;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.SearchChangeBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.service.EntityService;
import org.flockdata.track.service.EntityTagService;
import org.flockdata.track.service.FortressService;
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
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Search Service interactions
 * User: mike
 * Date: 4/04/14
 * Time: 9:18 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
@Transactional
public class SearchServiceFacade {
    private Logger logger = LoggerFactory.getLogger(SearchServiceFacade.class);

    @Autowired
    EntityDaoNeo trackDao;

    @Autowired
    EntityService entityService;

    @Qualifier("fdSearchGateway")
    @Autowired
    FdSearchGateway searchGateway;

    @Autowired
    EntityTagService entityTagService;

    @Autowired
    KvService kvGateway;

    @Autowired
    FortressService fortressService;

    @Autowired
    EntityTagFinder childToRootFinder;

    static final ObjectMapper objectMapper = FlockDataJsonFactory.getObjectMapper();

    //
    @ServiceActivator(inputChannel = "searchDocSyncResult", requiresReply = "false", adviceChain = {"fds.retry"})
    public void searchDocSyncResult(byte[] searchResults) throws IOException {
        searchDocSyncResult(objectMapper.readValue(searchResults, SearchResults.class));
    }

    /**
     * Callback handler that is invoked from fd-search. This routine ties the generated search document ID
     * to the Entity
     * <p>
     * ToDo: On completion of this, an outbound message should be posted so that the caller can be made aware(?)
     *
     * @param searchResults contains keys to tie the search to the entity
     */
    @ServiceActivator(inputChannel = "searchSyncResult", requiresReply = "false")
    public void searchDocSyncResult(SearchResults searchResults) {
        Collection<SearchResult> theResults = searchResults.getSearchResults();
        int count = 0;
        int size = theResults.size();
        logger.debug("searchDocSyncResult processing {} incoming search results", size);
        for (SearchResult searchResult : theResults) {
            count++;
            logger.debug("Updating {}/{} from search metaKey =[{}]", count, size, searchResult);
            Long entityId = searchResult.getEntityId();
            if (entityId == null)
                return ;

            try {
                entityService.recordSearchResult(searchResult, entityId);
            } catch (FlockException e) {
                logger.error("Unexpected error recording searchResult for entityId " + entityId, e);
            }
        }
        logger.trace("Finished processing search results");
    }

    public void makeChangeSearchable(SearchChangeBean searchChange) {
        if (searchChange == null)
            return;
        Collection<SearchChangeBean> searchChanges = new ArrayList<>();
        searchChanges.add(searchChange);
        makeChangesSearchable(searchChanges);
    }

    public Boolean makeChangesSearchable(Collection<SearchChangeBean> searchDocuments) {
        if (searchDocuments.isEmpty())
            //  return new AsyncResult<>(null);
            return false;

        if (searchDocuments.size() == 1)
            logger.debug("Sending request to index entity [{}]]", searchDocuments.iterator().next().getEntityId());
        else
            logger.debug("Sending request to index [{}]] logs", searchDocuments.size());
        searchGateway.makeSearchChanges(new EntitySearchChanges(searchDocuments));
        logger.debug("[{}] log requests sent to search", searchDocuments.size());
        return true;
    }

    public Iterable<EntityTag> getEntityTags(Entity entity){
        //DocumentType documentType = fortressService.findDocumentType(entity);
        EntityTagFinder tagFinder = null;
        try {
            tagFinder = fortressService.getSearchTagFinder(entity);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            logger.error("Unable to create specified EntityTagFinder");
        }
        if ( tagFinder == null) {
            // Default behaviour - all tags directly connected to the entity and any GEO payloads
            return entityTagService.getEntityTagsWithGeo(entity);
        } else {
            return childToRootFinder.getEntityTags(entity);


        }

    }
    /**
     * This is the primary function to populates an object for indexing into the search service
     *
     *
     * @param trackResultBean track result to process
     * @return SearchChange that will can be indexed
     */
    public SearchChangeBean getSearchChange(TrackResultBean trackResultBean) {
        assert trackResultBean != null;

        Entity entity = trackResultBean.getEntity();
        if (entity.getLastUser() != null && entity.getLastUser().getCode()==null)
            fortressService.getUser(entity.getLastUser().getId());

        EntityLog entityLog = getLog(trackResultBean);
        SearchChangeBean searchDocument = new EntitySearchChange(trackResultBean.getEntity(), entityLog, trackResultBean.getContentInput());

        if (entityLog!=null) {
            // Used to reconcile that the change was actually indexed
            logger.trace("Preparing Search Document [{}]", entityLog);

            searchDocument.setLogId(entityLog.getId());
            if (entityLog.getLog().getMadeBy() != null)
                searchDocument.setWho(entityLog.getLog().getMadeBy().getCode());
            if (entityLog.getSysWhen() != 0)
                searchDocument.setSysWhen(entityLog.getSysWhen());
        }
        // ToDo: Can we optimize by using tags already tracked in the result bean?
        searchDocument.setTags(getEntityTags(trackResultBean.getEntity()));
        searchDocument.setDescription(trackResultBean.getEntity().getDescription());
        searchDocument.setName(trackResultBean.getEntity().getName());
        searchDocument.setSearchKey(entity.getSearchKey());
        try {
            //if (logger.isTraceEnabled())
            logger.trace("JSON {}", FlockDataJsonFactory.getObjectMapper().writeValueAsString(searchDocument));
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            return  null;
        }

        if( searchDocument.getSysWhen() == 0l)
            searchDocument.setSysWhen(trackResultBean.getEntity().getDateCreated());

        if (entity.getId() == null) {
            logger.debug("No entityId so we are not expecting a reply");
            //searchDocument.setWhen(null);
            searchDocument.setReplyRequired(false);
            searchDocument.setSearchKey(null);
        }

        // If we already have this search key, then don't bother us with a reply
        if ( entity.getSearch() !=null )
            searchDocument.setReplyRequired(false);

        return searchDocument;
    }

    public EntitySearchChange rebuild(Entity entity, EntityLog lastLog) {

        try {
            Log lastChange = null;
            if (lastLog != null)
                lastChange = lastLog.getLog();

            //if () {
            // Update against the Entity only by re-indexing the search document
            EntitySearchChange searchDocument;
            //Entity entityBean = new EntityBean(entity);
            if (lastChange != null) {
                if ( !entity.isNoLogs()) {
                    KvContent content = kvGateway.getContent(entity, lastChange);
                    if (content == null) {
                        logger.error("Unable to locate content for {} ", entity);
                        return null;
                    }
                    searchDocument = new EntitySearchChange(entity, lastLog, content.getContent());
                } else
                    searchDocument = new EntitySearchChange(entity);

                if (lastChange.getMadeBy() != null)
                    searchDocument.setWho(lastChange.getMadeBy().getCode());
            } else {
                searchDocument = new EntitySearchChange(entity);
                if (entity.getCreatedBy() != null)
                    searchDocument.setWho(entity.getCreatedBy().getCode());
            }

            searchDocument.setTags(getEntityTags(entity));
            searchDocument.setReplyRequired(false);

            return searchDocument;
            //}
        } catch (Exception e) {
            logger.error("error", e);
        }
        return null;
    }

    public EsSearchResult search(QueryParams queryParams) {
        return searchGateway.fdSearch(queryParams);
    }

    public TagCloud getTagCloud(TagCloudParams tagCloudParams) {
        return searchGateway.getTagCloud(tagCloudParams);
    }

    public void purge(String indexName) {
        // ToDO: Implement this
        logger.info("You have to manually purge the ElasticSearch index {}", indexName);
    }

//    public void makeChangeSearchable(Fortress fortress, TrackResultBean trackResult) {
//        Collection<TrackResultBean> results = new ArrayList<>();
//        results.add(trackResult);
//        makeChangesSearchable(fortress, results);
//
//    }

    @Async("fd-search")
    @Retryable(include = {NotFoundException.class, InvalidDataAccessResourceUsageException.class, DataIntegrityViolationException.class, ConcurrencyFailureException.class, DeadlockDetectedException.class, ConstraintViolationException.class},
            maxAttempts = 20,
            backoff = @Backoff(delay = 600, multiplier = 5, random = true))
    public void makeChangesSearchable(Fortress fortress, Iterable<TrackResultBean> resultBeans) {
        // ToDo: This needs to be an activation via message-q
        logger.debug("Received request to make changes searchable {}", fortress);
        Collection<SearchChangeBean> changes = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            SearchChangeBean change = getChangeToPublish(fortress, resultBean);
            if (change != null)
                changes.add(change);
            else
                logger.debug("Ignoring request to index {}", resultBean);
        }
        if ( changes.isEmpty()){
            logger.debug("No changes to index");
        } else {
            makeChangesSearchable(changes);
        }
        logger.debug("Processed request to make changes searchable {}", fortress);
    }

    /**
     * Returns a change to published
     * @param fortress        hydrated fortress object
     * @param trackResultBean data from which to create the search change
     * @return null if search is not required or is being actively suppressed
     */
    SearchChangeBean getChangeToPublish(Fortress fortress, TrackResultBean trackResultBean) {
        if (trackResultBean == null)
            return null;

        if (trackResultBean.getEntity() == null || !fortress.isSearchEnabled())
            return null;

        if (trackResultBean.getEntityInputBean() != null && trackResultBean.getEntityInputBean().isEntityOnly()) {
            return getSearchChange(trackResultBean);
        }

        if (trackResultBean.getCurrentLog() != null)
            return getSearchChange(trackResultBean);

        return null;

    }

    private EntityLog getLog(TrackResultBean trackResultBean) {
        //EntityLog entityLog = trackResultBean.getCurrentLog();

        if (!trackResultBean.processLog()) {
            logger.debug("Tracking this entity through to search has been suppressed by the caller");
            return null;
        }
        if (trackResultBean.getLogStatus() != ContentInputBean.LogStatus.OK || trackResultBean.getCurrentLog() == null) {

            logger.debug("No entity log to index against");
            return null;
        }
        return trackResultBean.getCurrentLog();
    }

    public void refresh(Company company, Collection<Long> entities) {
        // To support DAT-279 - not going to work well with massive result sets
        Collection<Entity> entitiesSet = entityService.getEntities(entities);
        Collection<SearchChangeBean> searchChanges = new ArrayList<>();

        for (Entity entity : entitiesSet) {
            SearchChangeBean change = rebuild(entity, entityService.getLastEntityLog(entity.getId()));
            if (change != null && entity.getFortress().isSearchEnabled() && !entity.isSearchSuppressed())
                searchChanges.add(change);
        }
        makeChangesSearchable(searchChanges);
    }
}