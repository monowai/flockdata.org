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

import com.auditbucket.helper.Command;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.DeadlockRetry;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.registration.service.TagService;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.MetaSearchChange;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.track.bean.*;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.SearchChange;
import com.auditbucket.track.model.TrackLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

/**
 * Non transactional coordinator for mediation services
 * <p/>
 * User: Mike Holdsworth
 * Since: 28/08/13
 */
@Service
public class MediationFacade {
    @Autowired
    TrackService trackService;

    @Autowired
    TagTrackService tagTrackService;

    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    SearchServiceFacade searchService;

    @Autowired
    SchemaService schemaService;

    @Autowired
    TagService tagService;

    private Logger logger = LoggerFactory.getLogger(MediationFacade.class);

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    EngineConfig engineConfig;

    static DecimalFormat f = new DecimalFormat();

    /**
     * Process the MetaHeader input for a company asynchronously
     *
     * @param company    for
     * @param fortress   system
     * @param inputBeans data
     * @return process count - don't rely on it, why would you want it?
     * @throws com.auditbucket.helper.DatagioException
     *
     */

    @Async
    public Future<Integer> createHeadersAsync(final Company company, final Fortress fortress, List<MetaInputBean> inputBeans) throws DatagioException, IOException {
        // ToDo: Return strings which contain only the caller ref data that failed.
        return new AsyncResult<>(createHeaders(company, fortress, inputBeans, 5));
    }

    public Integer createHeaders(final Company company, final Fortress fortress, final List<MetaInputBean> inputBeans, int listSize) throws DatagioException, IOException {
        fortress.setCompany(company);
        Long id = DateTime.now().getMillis();
        StopWatch watch = new StopWatch();
        watch.start();
        logger.info("Starting Batch [{}] - size [{}]", id, inputBeans.size());
        boolean newMode = true;
        if (newMode) {

            // Tune to balance against concurrency and batch transaction insert efficiency.
            List<List<MetaInputBean>> splitList = Lists.partition(inputBeans, listSize);

            for (List<MetaInputBean> metaInputBeans : splitList) {

                class DLCommand implements Command {
                    Iterable<MetaInputBean> headers = null;
                    Iterable<TrackResultBean> resultBeans;

                    DLCommand(List<MetaInputBean> processList) {
                        this.headers = new CopyOnWriteArrayList<>(processList);
                    }

                    @Override
                    public Command execute() throws DatagioException, IOException {
                        resultBeans = trackService.createHeaders(headers, company, fortress);
                        resultBeans = processLogs(company, resultBeans);

                        distributeChanges(company, resultBeans);
                        return this;
                    }
                }
                DeadlockRetry.execute(new DLCommand(metaInputBeans), "creating headers", 20);

            }

        } else {
            logger.info("Processing in slow Transaction mode");
            Collection<SearchChange> searchChanges = new ArrayList<>();
            for (MetaInputBean inputBean : inputBeans) {
                TrackResultBean result = createHeader(company, fortress, inputBean);
                if (result != null)
                    searchChanges.add(searchService.getSearchChange(company, result));
            }
            searchService.makeChangesSearchable(searchChanges);
        }
        watch.stop();
        logger.info("Completed Batch [{}] - secs= {}, RPS={}", id, f.format(watch.getTotalTimeSeconds()), f.format(inputBeans.size() / watch.getTotalTimeSeconds()));
        return inputBeans.size();
    }

    public TrackResultBean createHeader(MetaInputBean inputBean, String apiKey) throws DatagioException, IOException {
        if (inputBean == null)
            throw new DatagioException("No input to process");
        LogInputBean logBean = inputBean.getLog();
        if (logBean != null) // Error as soon as we can
            logBean.setWhat(logBean.getWhat());

        Company company = registrationService.resolveCompany(apiKey);
        Fortress fortress = fortressService.registerFortress(company, new FortressInputBean(inputBean.getFortress(), false));
        fortress.setCompany(company);
        return createHeader(company, fortress, inputBean);
    }

    public TrackResultBean createHeader(final Company company, final Fortress fortress, final MetaInputBean inputBean) throws DatagioException, IOException {
        if (inputBean == null)
            throw new DatagioException("No input to process!");

        class HeaderDeadlockRetry implements Command {
            TrackResultBean result = null;

            @Override
            public Command execute() throws DatagioException, IOException {
                result = trackService.createHeader(company, fortress, inputBean);
                result = processLogFromResult(company, result);
                distributeChange(company, result);
                return this;
            }
        }

        HeaderDeadlockRetry c = new HeaderDeadlockRetry();
        com.auditbucket.helper.DeadlockRetry.execute(c, "create header", 10);
        return c.result;
    }

    public Collection<TrackResultBean> processLogs(Company company, Iterable<TrackResultBean> resultBeans) throws DatagioException, IOException {
        Collection<TrackResultBean> logResults = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            logResults.add(processLogFromResult(company, resultBean));
        }
        return logResults;
    }

    public LogResultBean processLog( LogInputBean input) throws DatagioException, IOException {
        return processLog(registrationService.resolveCompany(null), input);
    }

//    public LogResultBean processLog( String apiKey, LogInputBean input) throws DatagioException, IOException {
//        return processLog(registrationService.resolveCompany(apiKey), input);
//    }

    public LogResultBean processLog(Company company, LogInputBean input) throws DatagioException, IOException {
        MetaHeader header = trackService.getHeader(company, input.getMetaKey());
        LogResultBean logResultBean = writeLog(header, input);
        distributeChange(company , new TrackResultBean(logResultBean, input));
        return logResultBean;
    }

    private LogResultBean processCompanyLog(Company company, TrackResultBean resultBean) throws DatagioException, IOException {
        MetaHeader header = resultBean.getMetaHeader();
        if (header == null)
            header = trackService.getHeader(company, resultBean.getMetaKey());
        return writeLog(header, resultBean.getLog());
    }

    private TrackResultBean processLogFromResult(Company company, TrackResultBean resultBean) throws DatagioException, IOException {
        LogInputBean logBean = resultBean.getLog();
        MetaHeader header = resultBean.getMetaHeader();
        // Here on could be spun in to a separate thread. The log has to happen eventually
        //   and shouldn't fail.
        if (resultBean.getLog() != null) {
            // Secret back door so that the log result can quickly get the auditid
            logBean.setMetaId(resultBean.getAuditId());
            logBean.setMetaKey(resultBean.getMetaKey());
            logBean.setCallerRef(resultBean.getCallerRef());

            LogResultBean logResult;
            if (header != null)
                logResult = writeLog(header, logBean);
            else
                logResult = processCompanyLog(company, resultBean);

            logResult.setMetaKey(null);// Don't duplicate the text as it's in the header
            logResult.setFortressUser(null);
            resultBean.setLogResult(logResult);

        }
        return resultBean;
    }

    /**
     * Will locate the track header from the supplied input
     *
     * @param company valid company the caller can operate on
     * @param input   payload containing at least the metaKey
     * @return result of the log
     */
    public LogResultBean processLogForCompany(Company company, LogInputBean input) throws DatagioException, IOException {
        MetaHeader header = trackService.getHeader(company, input.getMetaKey());
        if (header == null)
            throw new DatagioException("Unable to find the request auditHeader " + input.getMetaKey());
        LogResultBean logResultBean = writeLog(header, input);
        distributeChange(company, new TrackResultBean(logResultBean, input));
        return logResultBean;
    }

    @Async
    public void distributeChanges(Company company, Iterable<TrackResultBean> resultBeans) throws IOException {
        logger.debug("Distributing changes to KV and Search");
        if (engineConfig.isConceptsEnabled())
            schemaService.registerConcepts(company, resultBeans);
        whatService.doKvWrite(resultBeans);
        Collection<SearchChange> changes = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            SearchChange change = getSearchChange(resultBean);
            if (change!=null )
                changes.add(change);
        }
        searchService.makeChangesSearchable(changes);
    }

    @Async
    public TrackResultBean distributeChange(Company company, TrackResultBean trackResultBean) throws IOException {
        ArrayList<TrackResultBean>results = new ArrayList<>();
        results.add(trackResultBean);
        distributeChanges(company, results);
        return trackResultBean;
    }

    private SearchChange getMetaSearchChange(TrackResultBean trackResultBean) {
        return searchService.getSearchChange(trackResultBean.getMetaHeader().getFortress().getCompany(), trackResultBean);
    }

    private SearchChange getSearchChange(TrackResultBean trackResultBean) {
        if (trackResultBean.getMetaInputBean()!=null && trackResultBean.getMetaInputBean().isMetaOnly()){
            return getMetaSearchChange(trackResultBean);
        }
        if ( !trackResultBean.getMetaHeader().getFortress().isSearchActive())
            return null;

        LogResultBean logResultBean = trackResultBean.getLogResult();
        LogInputBean input = trackResultBean.getLog();

        if ( !trackResultBean.processLog())
            return null;

        if (logResultBean != null && logResultBean.getLogToIndex() != null && logResultBean.getStatus() == LogInputBean.LogStatus.OK) {
            try {
                DateTime fWhen = new DateTime(logResultBean.getLogToIndex().getFortressWhen());
                return searchService.prepareSearchDocument(logResultBean.getLogToIndex().getMetaHeader(), input, input.getChangeEvent(), fWhen, logResultBean.getLogToIndex());
            } catch (JsonProcessingException e) {
                logResultBean.setMessage("Error processing JSON document");
                logResultBean.setStatus(LogInputBean.LogStatus.ILLEGAL_ARGUMENT);
            }
        }
        return null;
    }

    /**
     * Deadlock safe processor to creates a log
     *
     * @param header       Header that the caller is authorised to work with
     * @param logInputBean log details to apply to the authorised header
     * @return result details
     * @throws com.auditbucket.helper.DatagioException
     *
     */
    public LogResultBean writeLog(final MetaHeader header, final LogInputBean logInputBean) throws DatagioException, IOException {
        logInputBean.setWhat(logInputBean.getWhat());
        class DeadLockCommand implements Command {
            LogResultBean result = null;

            @Override
            public Command execute() throws DatagioException, IOException {
                result = trackService.createLog(header, logInputBean);
                return this;
            }
        }
        DeadLockCommand c = new DeadLockCommand();
        DeadlockRetry.execute(c, "processing log for header", 20);

        return c.result;

    }

    /**
     * Rebuilds all search documents for the supplied fortress
     *
     * @param fortressName name of the fortress to rebuild
     * @throws com.auditbucket.helper.DatagioException
     *
     */
    @Async
    public Future<Long> reindex(Company company, String fortressName) throws DatagioException {
        Fortress fortress = fortressService.findByCode(company, fortressName);
        if (fortress == null)
            throw new DatagioException("Fortress [" + fortress + "] could not be found");
        Long skipCount = 0l;
        long result = reindex(fortress, skipCount);
        logger.info("Reindex Search request completed. Processed [" + result + "] headers for [" + fortressName + "]");
        return new AsyncResult<>(result);
    }

    private long reindex(Fortress fortress, Long skipCount) {

        Collection<MetaHeader> headers = trackService.getHeaders(fortress, skipCount);
        if (headers.isEmpty())
            return skipCount;
        skipCount = reindexHeaders(fortress.getCompany(), headers, skipCount);
        return reindex(fortress, skipCount);

    }

    /**
     * Rebuilds all search documents for the supplied fortress of the supplied document type
     *
     * @param fortressName name of the fortress to rebuild
     * @throws com.auditbucket.helper.DatagioException
     *
     */
    @Async
    public void reindexByDocType(Company company, String fortressName, String docType) throws DatagioException {
        Fortress fortress = fortressService.findByName(company, fortressName);
        if (fortress == null)
            throw new DatagioException("Fortress [" + fortress + "] could not be found");
        Long skipCount = 0l;
        long result = reindexByDocType(skipCount, fortress, docType);
        logger.info("Reindex Search request completed. Processed [" + result + "] headers for [" + fortressName + "] and document type [" + docType + "]");
    }

    private long reindexByDocType(Long skipCount, Fortress fortress, String docType) {

        Collection<MetaHeader> headers = trackService.getHeaders(fortress, docType, skipCount);
        if (headers.isEmpty())
            return skipCount;
        skipCount = reindexHeaders(fortress.getCompany(), headers, skipCount);
        return reindexByDocType(skipCount, fortress, docType);

    }

    private Long reindexHeaders(Company company, Collection<MetaHeader> headers, Long skipCount) {
        Collection<SearchChange> searchDocuments = new ArrayList<>(headers.size());
        for (MetaHeader header : headers) {
            TrackLog lastLog = trackService.getLastLog(header.getId());
            searchDocuments.add(searchService.rebuild(company, header, lastLog));
            skipCount++;
        }
        searchService.makeChangesSearchable(searchDocuments);
        return skipCount;
    }

    public TrackedSummaryBean getTrackedSummary(String metaKey) throws DatagioException {
        return getTrackedSummary(null, metaKey);
    }

    public TrackedSummaryBean getTrackedSummary(Company company, String metaKey) throws DatagioException {
        return trackService.getMetaSummary(company, metaKey);
    }

    public EsSearchResult<Collection<MetaHeader>> search(Company company, QueryParams queryParams) {

        StopWatch watch = new StopWatch(queryParams.toString());
        watch.start("Get ES Query Results");
        EsSearchResult<Collection<String>> esSearchResult = searchService.search(queryParams);
        watch.stop();
        watch.start("Get Graph Headers");
        Collection<MetaHeader> headers = trackService.getHeaders(company, esSearchResult.getResults());
        EsSearchResult<Collection<MetaHeader>> results = new EsSearchResult<>(esSearchResult);
        results.setResults(headers);
        watch.stop();
        logger.info(watch.prettyPrint());
        return results;
    }

    @Autowired
    WhatService whatService;

    public void purge(String fortressName, String apiKey) throws DatagioException {
        if (fortressName == null)
            throw new DatagioException("Illegal value for fortress name");
        SystemUser su = registrationService.getSystemUser(apiKey);
        if (su == null || su.getCompany() == null)
            throw new SecurityException("Unable to verify that the caller can work with the requested fortress");
        Fortress fortress = fortressService.findByName(su.getCompany(), fortressName);
        if (fortress == null)
            throw new DatagioException("Fortress [" + fortressName + "] does not exist");
        purge(fortress, su);
    }

    private void purge(Fortress fortress, SystemUser su) throws DatagioException {
        logger.info("Purging fortress [{}] on behalf of [{}]", fortress, su.getLogin());

        String indexName = "ab." + fortress.getCompany().getCode() + "." + fortress.getCode();
        trackService.purge(fortress);

        whatService.purge(indexName);
        fortressService.purge(fortress);
        engineConfig.resetCache();
        searchService.purge(indexName);
    }

    public void cancelLastLogSync(String metaKey) throws IOException, DatagioException {
        MetaSearchChange searchChange = trackService.cancelLastLogSync(metaKey);
        if (searchChange != null ){
            searchService.makeChangeSearchable(searchChange);
        } else {
            logger.info("ToDo: Delete the search document {}", metaKey);
        }
    }
}
