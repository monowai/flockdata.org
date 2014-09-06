package com.auditbucket.engine.service;

import com.auditbucket.dao.TrackDao;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.registration.model.Company;
import com.auditbucket.search.model.*;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.LogResultBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.*;
import com.auditbucket.track.service.TrackService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

/**
 * Search Service interactions
 * User: mike
 * Date: 4/04/14
 * Time: 9:18 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class SearchServiceFacade {
    private Logger logger = LoggerFactory.getLogger(SearchServiceFacade.class);

    @Autowired
    TrackDao trackDao;

    @Autowired
    TrackService trackService;

    @Qualifier("abSearchGateway")
    @Autowired
    AbSearchGateway searchGateway;

    @Autowired
    TagTrackService tagTrackService;

    @Autowired
    KvService kvService;

    @Autowired
    FortressService fortressService;


    /**
     * Callback handler that is invoked from ab-search. This routine ties the generated search document ID
     * to the MetaHeader
     * <p/>
     * ToDo: On completion of this, an outbound message should be posted so that the caller can be made aware(?)
     *
     * @param searchResults contains keys to tie the search to the meta header
     */
    @ServiceActivator(inputChannel = "searchResult")
    public void handleSearchResult(SearchResults searchResults) {
        Collection<SearchResult> theResults = searchResults.getSearchResults();
        int count = 0;
        int size = theResults.size();
        logger.trace("handleSearchResult processing {} incoming search results", size);
        for (SearchResult searchResult : theResults) {
            count ++;
            logger.trace("Updating {}/{} from search metaKey =[{}]", count, size, searchResult);
            Long metaId = searchResult.getMetaId();
            if (metaId == null)
                return;

            trackService.saveMetaData(searchResult, metaId);
        }
        logger.debug("Finished processing search results");
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

    public void makeChangeSearchable(SearchChange searchChange) {
        if (searchChange == null)
            return;
        Collection<SearchChange> searchChanges = new ArrayList<>();
        searchChanges.add(searchChange);
        makeChangesSearchable(searchChanges);
    }

    public void makeChangesSearchable(Collection<SearchChange> searchDocument) {
        if (searchDocument == null || searchDocument.size() == 0)
            return ;
        logger.debug("Sending request to index [{}]] logs", searchDocument.size());

        searchGateway.makeSearchChanges(new MetaSearchChanges(searchDocument));
        logger.debug("Requests sent [{}]] logs", searchDocument.size());
    }

    public SearchChange getSearchChange(Company company, TrackResultBean resultBean, String event, Date when) {
        MetaHeader header = resultBean.getMetaHeader();

        if (header.getLastUser() != null)
            fortressService.fetch(header.getLastUser());
        SearchChange searchDocument = new MetaSearchChange(header, null, event, new DateTime(when));
        if (resultBean.getTags() != null) {
            searchDocument.setTags(resultBean.getTags());
            //searchDocument.setSearchKey(header.getCallerRef());

            if (header.getId() == null) {
                logger.debug("No HeaderID so we are not expecting a reply");
                searchDocument.setWhen(null);
                searchDocument.setReplyRequired(false);
            }
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
        searchDocument = new MetaSearchChange(metaHeader, (HashMap<String, Object>) logInput.getWhat(), event.getCode(), fortressWhen);
        searchDocument.setWho(trackLog.getLog().getWho().getCode());
        searchDocument.setTags(tagTrackService.findTrackTags(metaHeader.getFortress().getCompany(), metaHeader));
        searchDocument.setDescription(metaHeader.getDescription());
        searchDocument.setName(metaHeader.getName());
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
                lastChange = lastLog.getLog();

            if (metaHeader.getFortress().isSearchActive() && !metaHeader.isSearchSuppressed()) {
                // Update against the MetaHeader only by re-indexing the search document
                HashMap<String, Object> lastWhat;
                MetaSearchChange searchDocument;
                if (lastChange != null) {
                    lastWhat = (HashMap<String, Object>) kvService.getWhat(metaHeader, lastChange).getWhat();
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

    public EsSearchResult search(QueryParams queryParams) {
        return searchGateway.search(queryParams);
    }

    public void purge(String indexName) {
        // ToDO: Implement this
        logger.info("Purge the search Fortress {}", indexName);
    }

    public void makeChangesSearchable(Iterable<TrackResultBean> resultBeans) {
        Collection<SearchChange> changes = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            SearchChange change = getSearchChange(resultBean);
            if (change!=null )
                changes.add(change);
        }
        makeChangesSearchable(changes);

    }

    private SearchChange getSearchChange(TrackResultBean trackResultBean) {
        if ( trackResultBean == null )
            return null;
        if (trackResultBean.getMetaInputBean()!=null && trackResultBean.getMetaInputBean().isMetaOnly()){
            return getMetaSearchChange(trackResultBean);
        }

        if ( trackResultBean.getMetaHeader()== null || !trackResultBean.getMetaHeader().getFortress().isSearchActive())
            return null;

        LogResultBean logResultBean = trackResultBean.getLogResult();
        LogInputBean input = trackResultBean.getLog();

        if ( !trackResultBean.processLog())
            return null;

        if (logResultBean != null && logResultBean.getLogToIndex() != null && logResultBean.getStatus() == LogInputBean.LogStatus.OK) {
            try {
                DateTime fWhen = new DateTime(logResultBean.getLogToIndex().getFortressWhen());
                return prepareSearchDocument(logResultBean.getLogToIndex().getMetaHeader(), input, input.getChangeEvent(), fWhen, logResultBean.getLogToIndex());
            } catch (JsonProcessingException e) {
                logResultBean.setMessage("Error processing JSON document");
                logResultBean.setStatus(LogInputBean.LogStatus.ILLEGAL_ARGUMENT);
            }
        }
        return null;
    }

    private SearchChange getMetaSearchChange(TrackResultBean trackResultBean) {
        return getSearchChange(trackResultBean.getMetaHeader().getFortress().getCompany(), trackResultBean);
    }

}