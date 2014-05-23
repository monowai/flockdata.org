package com.auditbucket.engine.service;

import com.auditbucket.dao.TrackDao;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.search.model.*;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;

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
    TrackDao trackDao;

    @Autowired
    AbSearchGateway searchGateway;

    @Autowired
    TagTrackService tagTrackService;

    @Autowired
    WhatService whatService;

    @Autowired
    FortressService fortressService;


    /**
     * Callback handler that is invoked from ab-search. This routine ties the generated search document ID
     * to the MetaHeader
     * <p/>
     * ToDo: On completion of this, an outbound message should be posted so that the caller can be made aware(?)
     *
     * @param searchResult contains keys to tie the search to the meta header
     */

    @ServiceActivator(inputChannel = "searchResult")
    public void handleSearchResult(SearchResult searchResult) {

        logger.trace("Updating from search metaKey =[{}]", searchResult);
        Long metaId = searchResult.getMetaId();
        if (metaId == null)
            return;
        MetaHeader header;
        try {
            header = trackDao.getHeader(metaId); // Happens during development when Graph is cleared down and incoming search results are on the q
        } catch (DataRetrievalFailureException e) {
            logger.error("Unable to locate header for metaId {} in order to handle the search callerRef. Ignoring.", metaId);
            return;
        }

        if (header == null) {
            logger.error("metaKey could not be found for [{}]", searchResult);
            return;
        }

        if (header.getSearchKey() == null) {
            header.setSearchKey(searchResult.getSearchKey());
            trackDao.save(header);
            logger.trace("Updating Header{} search searchResult =[{}]", header.getMetaKey(), searchResult);
        }

        if (searchResult.getLogId() == null) {
            // Indexing header meta data only
            return;
        }
        TrackLog when;
        // The change has been indexed
        try {
            when = trackDao.getLog(searchResult.getLogId());
            if (when == null) {
                logger.error("Illegal node requested from handleSearchResult [{}]", searchResult.getLogId());
                return;
            }
        } catch (DataRetrievalFailureException e) {
            logger.error("Unable to locate track log {} for metaId {} in order to handle the search callerRef. Ignoring.", searchResult.getLogId(), metaId);
            return;
        }

        // Another thread may have processed this so save an update
        if (!when.isIndexed()) {
            // We need to know that the change we requested to index has been indexed.
            logger.debug("Updating index status for {}", when);
            when.setIsIndexed();
            trackDao.save(when);

        } else {
            logger.debug("Skipping {}", when);
        }
    }

    public SearchChange getSearchChange(Company company, TrackResultBean resultBean) {
        SearchChange searchChange = null;
        MetaHeader header = resultBean.getMetaHeader();
        if (!(header.isSearchSuppressed() || !header.getFortress().isSearchActive())) {
            //result, result.getMetaInputBean().getEvent(), result.getMetaInputBean().getWhen()
            searchChange = getSearchChange(company, resultBean, resultBean.getMetaInputBean().getEvent(), resultBean.getMetaInputBean().getWhen());
        }
        return searchChange;


    }

//    public Collection<SearchChange> getSearchDocuments(Company company, Collection<TrackResultBean> resultBeans) {
//        Collection<SearchChange>searchChanges = new ArrayList<>();
//        for (TrackResultBean resultBean : resultBeans) {
//            searchChanges.add(getSearchDocument(company, resultBean));
//        }
//        return searchChanges;
//    }

    public void makeChangeSearchable(SearchChange searchChange) {
        if (searchChange == null)
            return;
        Collection<SearchChange> searchChanges = new ArrayList<>();
        searchChanges.add(searchChange);
        makeChangeSearchable(searchChanges);
    }

    @Async
    public Future<Void> makeChangeSearchable(Collection<SearchChange> searchDocument) {
        if (searchDocument == null)
            return null;
        logger.debug("Sending request to index [{}]] logs", searchDocument.size());

        searchGateway.makeSearchChanges(new MetaSearchChanges(searchDocument));
        return null;
    }

    public SearchChange getSearchChange(Company company, TrackResultBean resultBean, String event, Date when) {
        MetaHeader header = resultBean.getMetaHeader();

        if (header.getLastUser() != null)
            fortressService.fetch(header.getLastUser());
        SearchChange searchDocument = new MetaSearchChange(header, null, event, new DateTime(when));
        if (resultBean.getTags() != null) {
            searchDocument.setTags(resultBean.getTags());
            searchDocument.setReplyRequired(false);
            searchDocument.setSearchKey(header.getCallerRef());

            if (header.getId() == null)
                searchDocument.setWhen(null);
            searchDocument.setSysWhen(header.getWhenCreated());

        } else {
            searchDocument.setTags(tagTrackService.findTrackTags(company, header));
        }
        return searchDocument;
    }

    private static final ObjectMapper om = new ObjectMapper();

    public SearchChange prepareSearchDocument(MetaHeader metaHeader, LogInputBean logInput, ChangeEvent event, DateTime fortressWhen, TrackLog trackLog) throws JsonProcessingException {

        if (metaHeader.isSearchSuppressed())
            return null;
        SearchChange searchDocument;
        searchDocument = new MetaSearchChange(metaHeader, logInput.getMapWhat(), event.getCode(), fortressWhen);
        searchDocument.setWho(trackLog.getChange().getWho().getCode());
        searchDocument.setTags(tagTrackService.findTrackTags(metaHeader.getFortress().getCompany(), metaHeader));
        searchDocument.setDescription(metaHeader.getName());
        try {
            if (logger.isTraceEnabled())
                logger.trace("JSON {}", om.writeValueAsString(searchDocument));
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            throw (e);
        }
        if (trackLog.getSysWhen() != 0)
            searchDocument.setSysWhen(trackLog.getSysWhen());
        else
            searchDocument.setSysWhen(metaHeader.getWhenCreated());

        // Used to reconcile that the change was actually indexed
        logger.trace("Preparing Search Document [{}]", trackLog);
        searchDocument.setLogId(trackLog.getId());
        return searchDocument;
    }

    public MetaSearchChange rebuild(Company company, MetaHeader metaHeader, TrackLog lastLog) {

        try {
            Log lastChange = null;
            if (lastLog != null)
                lastChange = lastLog.getChange();

            if (metaHeader.getFortress().isSearchActive() && !metaHeader.isSearchSuppressed()) {
                // Update against the MetaHeader only by re-indexing the search document
                Map<String, Object> lastWhat;
                MetaSearchChange searchDocument;
                if (lastChange != null) {
                    lastWhat = whatService.getWhat(metaHeader, lastChange).getWhat();
                    searchDocument = new MetaSearchChange(metaHeader, lastWhat, lastChange.getEvent().getCode(), new DateTime(lastLog.getFortressWhen()));
                    searchDocument.setWho(lastChange.getWho().getCode());
                } else {
                    searchDocument = new MetaSearchChange(metaHeader, null, metaHeader.getEvent(), metaHeader.getFortressDateCreated());
                    if (metaHeader.getCreatedBy() != null)
                        searchDocument.setWho(metaHeader.getCreatedBy().getCode());
                }

                searchDocument.setTags(tagTrackService.findTrackTags(company, metaHeader));
                searchDocument.setReplyRequired(false);

                return searchDocument;
            }
        } catch (Exception e) {
            logger.error("error", e);
        }
        return null;
    }

    public EsSearchResult<Collection<String>> search(QueryParams queryParams) {
        return searchGateway.search(queryParams);
    }

    public void purge(String indexName) {
        // ToDO: Implement this
        logger.info("Purge the search Fortress {}", indexName);
    }
}