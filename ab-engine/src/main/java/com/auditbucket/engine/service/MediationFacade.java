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
import com.auditbucket.helper.NotFoundException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.MetaSearchChange;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.bean.TrackedSummaryBean;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.SearchChange;
import com.auditbucket.track.model.TrackLog;
import com.auditbucket.track.service.*;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
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

    @Autowired
    LogService logService;

    private Logger logger = LoggerFactory.getLogger(MediationFacade.class);

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    EngineConfig engineConfig;

    static DecimalFormat f = new DecimalFormat();

    /**
     * Process the MetaHeader input for a company asynchronously
     *
     * @param fortress   system
     * @param inputBeans data
     * @return process count - don't rely on it, why would you want it?
     * @throws com.auditbucket.helper.DatagioException
     */

    @Async
    public Future<Collection<TrackResultBean>> trackHeadersAsync(final Fortress fortress, List<MetaInputBean> inputBeans) throws DatagioException, IOException, ExecutionException, InterruptedException {
        // ToDo: Return strings which contain only the caller ref data that failed.
        return new AsyncResult<>(trackHeaders(fortress, inputBeans, 10));
    }

    public Collection<TrackResultBean> trackHeaders(final Fortress fortress, final List<MetaInputBean> inputBeans, int listSize) throws DatagioException, IOException, ExecutionException, InterruptedException {
        Long id = DateTime.now().getMillis();
        StopWatch watch = new StopWatch();
        watch.start();
        logger.debug("Starting Batch [{}] - size [{}]", id, inputBeans.size());
        // Tune to balance against concurrency and batch transaction insert efficiency.
        List<List<MetaInputBean>> splitList = Lists.partition(inputBeans, listSize);
        Collection<TrackResultBean> results = new ArrayList<>();
        for (List<MetaInputBean> metaInputBeans : splitList) {

            @Deprecated
                    // We should favour spring-retry for this kind of activity
            class DLCommand implements Command {
                Iterable<MetaInputBean> headers = null;
                Iterable<TrackResultBean> resultBeans;

                DLCommand(List<MetaInputBean> processList) {
                    this.headers = new CopyOnWriteArrayList<>(processList);
                }

                @Override
                public Command execute() throws DatagioException, IOException, ExecutionException, InterruptedException {
                    // ToDo: DAT-169 This needs to be dealt with via SpringIntegration and persistent messaging
                    //       weirdly, the integration is with ab-engine
                    // DLCommand and DeadLockRetry need to be removed

                    // This happens before we create headers to minimize IO on the graph
                    schemaService.createDocTypes(headers, fortress);
                    // Ensure the headers and tags are created
                    // this routine is prone to deadlocks under load
                    resultBeans = trackService.createHeaders(fortress, headers);
                    resultBeans = logService.processLogsSync(fortress.getCompany(), resultBeans);

                    return this;
                }
            }
            DLCommand dlc = new DLCommand(metaInputBeans);
            DeadlockRetry.execute(dlc, "creating headers", 50);
            for (TrackResultBean resultBean : dlc.resultBeans) {
                results.add(resultBean);
            }

        }

        watch.stop();
        logger.debug("Completed Batch [{}] - secs= {}, RPS={}", id, f.format(watch.getTotalTimeSeconds()), f.format(inputBeans.size() / watch.getTotalTimeSeconds()));
        return results;
    }

    public TrackResultBean trackHeader(Company company, MetaInputBean inputBean) throws DatagioException, IOException, ExecutionException, InterruptedException {
        Fortress fortress = fortressService.findByName(company, inputBean.getFortress());
        if (fortress == null)
            fortress = fortressService.registerFortress(company,
                    new FortressInputBean(inputBean.getFortress(), false)
                            .setTimeZone(inputBean.getTimezone()));
        fortress.setCompany(company);
        return trackHeader(fortress, inputBean);
    }


    /**
     * tracks a header and creates logs. Distributes changes to KV stores and search engine.
     * <p/>
     * This is synchronous and blocks until completed
     *
     * @param fortress  - system that owns the data
     * @param inputBean - input
     * @return non-null
     * @throws DatagioException illegal input
     * @throws IOException      json processing exception
     */
    public TrackResultBean trackHeader(final Fortress fortress, final MetaInputBean inputBean) throws DatagioException, IOException, ExecutionException, InterruptedException {
        List<MetaInputBean>inputs = new ArrayList<>(1);
        inputs.add(inputBean);
        Collection<TrackResultBean>results =trackHeaders(fortress, inputs, 1);
        return results.iterator().next();
    }


    @Transactional
    public TrackResultBean processLog(Company company, LogInputBean input) throws DatagioException, IOException, ExecutionException, InterruptedException {
        MetaHeader metaHeader;
        if (input.getMetaKey() != null)
            metaHeader = trackService.getHeader(company, input.getMetaKey());
        else
            metaHeader = trackService.findByCallerRef(input.getFortress(), input.getDocumentType(), input.getCallerRef());
        if (metaHeader == null)
            throw new DatagioException("Unable to resolve the MetaHeader");
        return logService.writeLog(metaHeader, input);
    }

    /**
     * Rebuilds all search documents for the supplied fortress
     *
     * @param fortressName name of the fortress to rebuild
     * @throws com.auditbucket.helper.DatagioException
     */
    @Secured({"ROLE_AB_ADMIN"})
    public Long reindex(Company company, String fortressName) throws DatagioException {
        Fortress fortress = fortressService.findByCode(company, fortressName);
        Future<Long> result = reindexAsnc(fortress);

        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Unexpected", e);
            return -1l;
        }


    }

    @Async
    public Future<Long> reindexAsnc(Fortress fortress) throws DatagioException {
        if (fortress == null)
            throw new DatagioException("No fortress to reindex was supplied");
        Long skipCount = 0l;
        long result = reindex(fortress, skipCount);
        logger.info("Reindex Search request completed. Processed [" + result + "] headers for [" + fortress.getName() + "]");
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
     */
    @Async
    @Secured({"ROLE_AB_ADMIN"})
    public void reindexByDocType(Company company, String fortressName, String docType) throws DatagioException {
        Fortress fortress = fortressService.findByName(company, fortressName);
        if (fortress == null)
            throw new DatagioException("Fortress [" + fortressName + "] could not be found");
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

    public EsSearchResult search(Company company, QueryParams queryParams) {
        StopWatch watch = new StopWatch(queryParams.toString());
        watch.start("Get ES Query Results");
        queryParams.setCompany(company.getName());
        EsSearchResult esSearchResult = searchService.search(queryParams);
        watch.stop();
        logger.info(watch.prettyPrint());

        return esSearchResult;
    }

    @Autowired
    WhatService whatService;

    @Secured({"ROLE_AB_ADMIN"})
    public void purge(String fortressName, String apiKey) throws DatagioException {
        if (fortressName == null)
            throw new DatagioException("Illegal value for fortress name");
        SystemUser su = registrationService.getSystemUser(apiKey);
        if (su == null || su.getCompany() == null)
            throw new SecurityException("Unable to verify that the caller can work with the requested fortress");
        Fortress fortress = fortressService.findByName(su.getCompany(), fortressName);
        if (fortress == null)
            throw new NotFoundException("Fortress [" + fortressName + "] does not exist");
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

    public void cancelLastLogSync(Company company, String metaKey) throws IOException, DatagioException {
        MetaSearchChange searchChange = trackService.cancelLastLogSync(company, metaKey);
        if (searchChange != null) {
            searchService.makeChangeSearchable(searchChange);
        } else {
            logger.info("ToDo: Delete the search document {}", metaKey);
        }
    }


}
