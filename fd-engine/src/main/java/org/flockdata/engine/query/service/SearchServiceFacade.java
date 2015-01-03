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
import org.flockdata.track.service.EntityTagService;
import org.flockdata.track.service.FortressService;
import org.flockdata.track.service.TrackService;
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
import java.util.Date;

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
    TrackService trackService;

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
    @ServiceActivator(inputChannel = "searchDocSyncResult", requiresReply = "false", adviceChain = {"retrier"})
    public Boolean searchDocSyncResult(byte[] searchResults) throws IOException {
        return searchDocSyncResult(objectMapper.readValue(searchResults, SearchResults.class));
    }

    /**
         * Callback handler that is invoked from fd-search. This routine ties the generated search document ID
         * to the Entity
         * <p/>
         * ToDo: On completion of this, an outbound message should be posted so that the caller can be made aware(?)
         *
     * @param searchResults contains keys to tie the search to the entity
     */
    @ServiceActivator(inputChannel = "searchSyncResult", requiresReply = "false")
    public Boolean searchDocSyncResult(SearchResults searchResults) {
        Collection<SearchResult> theResults = searchResults.getSearchResults();
        int count = 0;
        int size = theResults.size();
        logger.trace("handleSearchResult processing {} incoming search results", size);
        for (SearchResult searchResult : theResults) {
            count ++;
            logger.trace("Updating {}/{} from search metaKey =[{}]", count, size, searchResult);
            Long entityId = searchResult.getMetaId();
            if (entityId == null)
                return false;

            try {
                trackService.recordSearchResult(searchResult, entityId);
            } catch (FlockException e) {
                logger.error("Unexpected error recording searchResult for entityId "+entityId, e);
            }
        }
        logger.trace("Finished processing search results");
        return true;
    }


    public SearchChange getSearchChange(Company company, TrackResultBean resultBean) {
        SearchChange searchChange = null;
        Entity entity = resultBean.getEntity();
        if (!(entity.isSearchSuppressed() || !entity.getFortress().isSearchActive())) {
            searchChange = getSearchChange(company, resultBean, resultBean.getEntityInputBean().getEvent(), resultBean.getEntityInputBean().getWhen());
        }
        return searchChange;


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
        logger.debug("Sending request to index [{}]] logs", searchDocument.size());
        searchGateway.makeSearchChanges(new EntitySearchChanges(searchDocument));
        logger.debug("[{}] log requests sent to search", searchDocument.size());
        return true;
    }

    public SearchChange getSearchChange(Company company, TrackResultBean resultBean, String event, Date when) {
        Entity entity = resultBean.getEntity();

        if (entity.getLastUser() != null)
            fortressService.fetch(entity.getLastUser());
        Log log = (resultBean.getLogResult()== null ? null:resultBean.getLogResult().getWhatLog());
        SearchChange searchDocument = new EntitySearchChange(new EntityBean(entity), resultBean.getContentInput(), log);
        if (resultBean.getTags() != null) {
            searchDocument.setTags(resultBean.getTags());
            //searchDocument.setSearchKey(entity.getCallerRef());

            if (entity.getId() == null) {
                logger.debug("No entityId so we are not expecting a reply");
                searchDocument.setWhen(null);
                searchDocument.setReplyRequired(false);
            }
            searchDocument.setSysWhen(entity.getWhenCreated());

        } else {
            searchDocument.setTags(entityTagService.getEntityTags(company, entity));
        }
        return searchDocument;
    }

    public SearchChange prepareSearchDocument(Company company, EntityBean entity, ContentInputBean contentInput, EntityLog entityLog) throws JsonProcessingException {
        assert entity!=null ;
        if (entity.isSearchSuppressed())
            return null;
        SearchChange searchDocument;
        searchDocument = new EntitySearchChange(entity, contentInput, entityLog.getLog() );
        searchDocument.setWho(entityLog.getLog().getWho().getCode());
        searchDocument.setTags(entityTagService.getEntityTags(company, entity.getId()));
        searchDocument.setDescription(entity.getDescription());
        searchDocument.setName(entity.getName());
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
            searchDocument.setSysWhen(entity.getWhenCreated());

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

            if (entity.getFortress().isSearchActive() && !entity.isSearchSuppressed()) {
                // Update against the Entity only by re-indexing the search document
                EntitySearchChange searchDocument;
                EntityBean entityBean = new EntityBean(entity);
                if (lastChange != null) {
                    EntityContent content = kvGateway.getContent(entity, lastChange);
                    searchDocument = new EntitySearchChange(entityBean, content, lastChange);
                    searchDocument.setWho(lastChange.getWho().getCode());
                } else {
                    searchDocument = new EntitySearchChange(entityBean);
                    if (entity.getCreatedBy() != null)
                        searchDocument.setWho(entity.getCreatedBy().getCode());
                }

                searchDocument.setTags(entityTagService.getEntityTags(company, entity));
                searchDocument.setReplyRequired(false);

                return searchDocument;
            }
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
        Collection<TrackResultBean>results = new ArrayList<>();
        results.add(trackResult);
        makeChangesSearchable(fortress, results);

    }

    @Async ("fd-search")
    public void makeChangesSearchable(Fortress fortress, Iterable<TrackResultBean> resultBeans) {
        logger.debug("Received request to make changes searchable {}", fortress);
        Collection<SearchChange> changes = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            SearchChange change = getSearchChange(fortress, resultBean);
            if (change!=null )
                changes.add(change);
        }
        makeChangesSearchable(changes);
//        return new AsyncResult<>(null);
    }

    private SearchChange getSearchChange(Fortress fortress, TrackResultBean trackResultBean) {
        if ( trackResultBean == null )
            return null;
        if (trackResultBean.getEntityInputBean()!=null && trackResultBean.getEntityInputBean().isMetaOnly()){
            return getSearchChange(fortress.getCompany(), trackResultBean);
        }

        if ( trackResultBean.getEntity()== null || !fortress.isSearchActive())
            return null;

        LogResultBean logResultBean = trackResultBean.getLogResult();
        ContentInputBean input = trackResultBean.getContentInput();

        if ( !trackResultBean.processLog())
            return null;

        if (logResultBean != null && logResultBean.getLogToIndex() != null && logResultBean.getStatus() == ContentInputBean.LogStatus.OK) {
            try {
                return prepareSearchDocument(fortress.getCompany(), trackResultBean.getEntityBean(), input, logResultBean.getWhatLog().getEntityLog());
            } catch (JsonProcessingException e) {
                logResultBean.setMessage("Error processing JSON document");
                logResultBean.setStatus(ContentInputBean.LogStatus.ILLEGAL_ARGUMENT);
            }
        }
        return null;
    }

    public void refresh(Company company, Collection<Long> entities) {
        // To support DAT-279 - not going to work well with massive result sets
        Collection<Entity> entitiesSet = trackService.getEntities(entities);
        Collection<SearchChange>searchChanges = new ArrayList<>();

        for (Entity entity : entitiesSet) {
            SearchChange change = rebuild(company, entity, trackService.getLastEntityLog(entity.getId()));
            if ( change !=null )
                searchChanges.add(change);
        }
        makeChangesSearchable(searchChanges);
    }
}