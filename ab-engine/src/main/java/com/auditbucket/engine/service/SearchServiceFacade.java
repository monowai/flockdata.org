package com.auditbucket.engine.service;

import com.auditbucket.dao.TrackDao;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.MetaSearchChange;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.search.model.SearchResult;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.ChangeLog;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.SearchChange;
import com.auditbucket.track.model.TrackLog;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // The change has been indexed
        TrackLog when = trackDao.getLog(searchResult.getLogId());
        if (when == null) {
            logger.error("Illegal node requested from handleSearchResult [{}]", searchResult.getLogId());
            return;
        }

        // Another thread may have processed this so save an update
        if (when != null && !when.isIndexed()) {
            // We need to know that the change we requested to index has been indexed.
            logger.debug("Updating index status for {}", when);
            when.setIsIndexed();
            trackDao.save(when);

        } else {
            logger.debug("Skipping {}", when);
        }
    }

    @Async
    public Future<Void> makeHeaderSearchable(Company company, TrackResultBean resultBean, String event, Date when) {
        MetaHeader header = resultBean.getMetaHeader();
        if (header.isSearchSuppressed() || !header.getFortress().isSearchActive())
            return null;

        SearchChange searchDocument = getSearchChange(company, resultBean, event, when);
        if (searchDocument == null) return null;
        makeChangeSearchable(searchDocument);
        return null;
    }

    @Async
    public Future<Void> makeChangeSearchable(SearchChange searchDocument) {
        if (searchDocument == null)
            return null;
        logger.debug("Sending request to index trackLog [{}]]", searchDocument);
        searchGateway.makeChangeSearchable(searchDocument);
        return null;
    }

    public SearchChange getSearchChange(Company company, TrackResultBean resultBean, String event, Date when) {
        MetaHeader header = resultBean.getMetaHeader();

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

    public void rebuild(Company company, MetaHeader metaHeader, TrackLog lastLog) {
        try {

            ChangeLog lastChange = null;
            if (lastLog != null)
                lastChange = lastLog.getChange();
            else {
                // ToDo: This will not work for meta-data index headers. Work loop also needs looking at
                logger.info("No last change for {}, ignoring the re-index request for this header", metaHeader.getCallerRef());
            }

            if (metaHeader.getFortress().isSearchActive() && !metaHeader.isSearchSuppressed()) {
                // Update against the MetaHeader only by re-indexing the search document
                Map<String, Object> lastWhat;
                if (lastChange != null)
                    lastWhat = whatService.getWhat(metaHeader, lastChange).getWhatMap();
                else
                    return; // ToDo: fix reindex header only scenario, i.e. no "change/what"

                MetaSearchChange searchDocument = new MetaSearchChange(metaHeader, lastWhat, lastChange.getEvent().getCode(), new DateTime(lastLog.getFortressWhen()));
                searchDocument.setTags(tagTrackService.findTrackTags(company, metaHeader));
                searchDocument.setReplyRequired(false);
                searchDocument.setWho(lastChange.getWho().getCode());
                makeChangeSearchable(searchDocument);
            }
        } catch (Exception e) {
            logger.error("error", e);
        }

    }

    public EsSearchResult search(QueryParams queryParams){
        return searchGateway.search(queryParams);
    }
}