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

package org.flockdata.engine.query.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.engine.query.endpoint.FdSearchGateway;
import org.flockdata.engine.track.EntityDaoNeo;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.helper.FlockException;
import org.flockdata.kv.service.KvService;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.search.model.*;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.bean.LogResultBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.*;
import org.flockdata.track.service.EntityService;
import org.flockdata.track.service.EntityTagService;
import org.flockdata.track.service.FortressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.ServiceActivator;
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

    static final ObjectMapper objectMapper = FlockDataJsonFactory.getObjectMapper();

    //
    @ServiceActivator(inputChannel = "searchDocSyncResult", requiresReply = "false", adviceChain = {"fds.retry"})
    public Boolean searchDocSyncResult(byte[] searchResults) throws IOException {
        return searchDocSyncResult(objectMapper.readValue(searchResults, SearchResults.class));
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
    public Boolean searchDocSyncResult(SearchResults searchResults) {
        Collection<SearchResult> theResults = searchResults.getSearchResults();
        int count = 0;
        int size = theResults.size();
        logger.debug("searchDocSyncResult processing {} incoming search results", size);
        for (SearchResult searchResult : theResults) {
            count++;
            logger.debug("Updating {}/{} from search metaKey =[{}]", count, size, searchResult);
            Long entityId = searchResult.getEntityId();
            if (entityId == null)
                return false;

            try {
                entityService.recordSearchResult(searchResult, entityId);
            } catch (FlockException e) {
                logger.error("Unexpected error recording searchResult for entityId " + entityId, e);
            }
        }
        logger.trace("Finished processing search results");
        return true;
    }

    /**
     * Returns a SearchChange document that can be indexed. Content is based on the resultbean
     *
     * @param company
     * @param resultBean result of a trackRequest
     * @return null if there is not content
     */

    public SearchChange getSearchChange(Company company, TrackResultBean resultBean) {
        SearchChange searchDocument = null;
        Entity entity = resultBean.getEntity();
        if (entity.getLastUser() != null)
            fortressService.fetch(entity.getLastUser());
        EntityLog entityLog = (resultBean.getLogResult() == null ? null : resultBean.getLogResult().getLogToIndex());

        searchDocument = new EntitySearchChange(new EntityBean(entity), entityLog, resultBean.getContentInput());
        searchDocument.setTags(entityTagService.getEntityTagsWithGeo(company, entity));
        //if (resultBean.getTags() != null) {
//                searchDocument.setTags(resultBean.getTags());
        searchDocument.setSearchKey(entity.getSearchKey());

        if (entity.getId() == null) {
            logger.debug("No entityId so we are not expecting a reply");
            searchDocument.setWhen(null);
            searchDocument.setReplyRequired(false);
        }
        searchDocument.setSysWhen(entity.getWhenCreated());
        return searchDocument;
    }

    public void makeChangeSearchable(SearchChange searchChange) {
        if (searchChange == null)
            return;
        Collection<SearchChange> searchChanges = new ArrayList<>();
        searchChanges.add(searchChange);
        makeChangesSearchable(searchChanges);
    }

    public Boolean makeChangesSearchable(Collection<SearchChange> searchDocument) {
        if (searchDocument.isEmpty())
            //  return new AsyncResult<>(null);
            return false;

        if (searchDocument.size() == 1)
            logger.debug("Sending request to index entity [{}]]", searchDocument.iterator().next().getEntityId());
        else
            logger.debug("Sending request to index [{}]] logs", searchDocument.size());
        searchGateway.makeSearchChanges(new EntitySearchChanges(searchDocument));
        logger.debug("[{}] log requests sent to search", searchDocument.size());
        return true;
    }

    public SearchChange prepareSearchDocument(Company company, TrackResultBean trackResultBean) throws JsonProcessingException {
        assert trackResultBean != null;

        if (trackResultBean.getEntity().isSearchSuppressed())
            return null;

        EntityLog entityLog = getLog(trackResultBean);

        if (entityLog == null) {
            logger.debug("No log to index");
            logger.debug("No entityLog could be found in the log to index");
            return null; // No log to process
        }

        SearchChange searchDocument= new EntitySearchChange(trackResultBean.getEntityBean(), entityLog, trackResultBean.getContentInput());
        if (entityLog.getLog().getWho() != null)
            searchDocument.setWho(entityLog.getLog().getWho().getCode());
        // ToDo: Can we optimize by using tags already tracked in the result bean?
        searchDocument.setTags(entityTagService.getEntityTagsWithGeo(company, trackResultBean.getEntity()));
        searchDocument.setDescription(trackResultBean.getEntityBean().getDescription());
        searchDocument.setName(trackResultBean.getEntityBean().getName());
        try {
            if (logger.isTraceEnabled())
                logger.trace("JSON {}", FlockDataJsonFactory.getObjectMapper().writeValueAsString(searchDocument));
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            throw (e);
        }
        if (entityLog.getSysWhen() != 0)
            searchDocument.setSysWhen(entityLog.getSysWhen());
        else
            searchDocument.setSysWhen(trackResultBean.getEntityBean().getWhenCreated());

        // Used to reconcile that the change was actually indexed
        logger.trace("Preparing Search Document [{}]", entityLog);
        searchDocument.setLogId(entityLog.getId());
        return searchDocument;
    }

    public EntitySearchChange rebuild(Company company, Entity entity, EntityLog lastLog) {

        try {
            Log lastChange = null;
            if (lastLog != null)
                lastChange = lastLog.getLog();

            //if () {
            // Update against the Entity only by re-indexing the search document
            EntitySearchChange searchDocument;
            EntityBean entityBean = new EntityBean(entity);
            if (lastChange != null) {
                KvContent content = kvGateway.getContent(entity, lastChange);
                if ( content == null ) {
                    logger.error("Unable to located content for {} ", entity);
                    return null;
                }
                searchDocument = new EntitySearchChange(entityBean, lastLog, content.getContent());
                if (lastChange.getWho() != null)
                    searchDocument.setWho(lastChange.getWho().getCode());
            } else {
                searchDocument = new EntitySearchChange(entityBean);
                if (entity.getCreatedBy() != null)
                    searchDocument.setWho(entity.getCreatedBy().getCode());
            }

            searchDocument.setTags(entityTagService.getEntityTagsWithGeo(company, entity));
            searchDocument.setReplyRequired(false);

            return searchDocument;
            //}
        } catch (Exception e) {
            logger.error("error", e);
        }
        return null;
    }

    public EsSearchResult search(QueryParams queryParams) {
        return searchGateway.search(queryParams);
    }

    public TagCloud getTagCloud(TagCloudParams tagCloudParams) {
        return searchGateway.getTagCloud(tagCloudParams);
    }

    public void purge(String indexName) {
        // ToDO: Implement this
        logger.info("Purge the search Fortress {}", indexName);
    }

    public void makeChangeSearchable(Fortress fortress, TrackResultBean trackResult) {
        Collection<TrackResultBean> results = new ArrayList<>();
        results.add(trackResult);
        makeChangesSearchable(fortress, results);

    }

    @Async("fd-search")
    public void makeChangesSearchable(Fortress fortress, Iterable<TrackResultBean> resultBeans) {
        logger.debug("Received request to make changes searchable {}", fortress);
        Collection<SearchChange> changes = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            SearchChange change = getChangeToPublish(fortress, resultBean);
            if (change != null)
                changes.add(change);
        }
        makeChangesSearchable(changes);
        logger.debug("Processed request to make changes searchable {}", fortress);
    }

    SearchChange getChangeToPublish(Fortress fortress, TrackResultBean trackResultBean) {
        if (trackResultBean == null)
            return null;

        if (trackResultBean.getEntity() == null || !fortress.isSearchActive())
            return null;

        if (trackResultBean.getEntityInputBean() != null && trackResultBean.getEntityInputBean().isMetaOnly()) {
            return getSearchChange(fortress.getCompany(), trackResultBean);
        }

        try {
            return prepareSearchDocument(fortress.getCompany(), trackResultBean);
        } catch (JsonProcessingException e) {
            trackResultBean.setServiceMessage("Error processing JSON document");
            if (trackResultBean.getLogResult() != null) {
                trackResultBean.getLogResult().setStatus(ContentInputBean.LogStatus.ILLEGAL_ARGUMENT);
            }
        }
        //}
        return null;
    }

    private EntityLog getLog(TrackResultBean trackResultBean) {
        LogResultBean logResultBean = trackResultBean.getLogResult();

        if (!trackResultBean.processLog()) {
            logger.debug("Tracking this entity through to search has been suppressed by the caller");
            return null;
        }
        EntityLog entityLog = null;
        if (logResultBean != null && logResultBean.getLogToIndex() != null && logResultBean.getStatus() == ContentInputBean.LogStatus.OK) {
            entityLog = logResultBean.getLogToIndex();
            logger.debug("Returning logToIndex from resultBean");
        }
        return entityLog;
    }

    public void refresh(Company company, Collection<Long> entities) {
        // To support DAT-279 - not going to work well with massive result sets
        Collection<Entity> entitiesSet = entityService.getEntities(entities);
        Collection<SearchChange> searchChanges = new ArrayList<>();

        for (Entity entity : entitiesSet) {
            SearchChange change = rebuild(company, entity, entityService.getLastEntityLog(entity.getId()));
            if (change != null && entity.getFortress().isSearchActive() && !entity.isSearchSuppressed())
                searchChanges.add(change);
        }
        makeChangesSearchable(searchChanges);
    }
}