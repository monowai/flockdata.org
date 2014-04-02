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

import com.auditbucket.audit.bean.*;
import com.auditbucket.audit.model.MetaHeader;
import com.auditbucket.helper.Command;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.DeadlockRetry;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.registration.service.TagService;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

/**
 *
 * Non transactional coordinator for mediation services
 *
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
    TagService tagService;

    private Logger logger = LoggerFactory.getLogger(MediationFacade.class);

    @Autowired
    private RegistrationService registrationService;

    static DecimalFormat f = new DecimalFormat();

    /**
     * Process the MetaHeader input for a company asynchronously
     *
     * @param company     for
     * @param fortress    system
     * @param inputBeans  data
     * @return process count - don't rely on it, why would you want it?
     * @throws com.auditbucket.helper.DatagioException
     */

    @Async
    public Future<Integer> createHeadersAsync(final Company company, final Fortress fortress, List<MetaInputBean> inputBeans) throws DatagioException {
        // ToDo: Return strings which could contain only the caller ref data that failed.
        return new AsyncResult<>(createHeaders(company, fortress, inputBeans));
    }

    public Integer createHeaders(final Company company, final Fortress fortress, final List<MetaInputBean> inputBeans) throws DatagioException {
        fortress.setCompany(company);
        Long id = DateTime.now().getMillis();
        StopWatch watch = new StopWatch();
        watch.start();
        logger.info("Starting Batch [{}] - size [{}]", id, inputBeans.size());
        boolean newMode = true;
        if (newMode) {

            // Tune to balance against concurrency and batch transaction insert efficiency.
            List<List<MetaInputBean>> splitList = Lists.partition(inputBeans, 5);

            for (List<MetaInputBean> metaInputBeans : splitList) {

                class DLCommand implements Command {
                    Iterable<MetaInputBean> headers = null;
                    DLCommand (List<MetaInputBean> processList){
                        this.headers = new CopyOnWriteArrayList<>(processList);
                    }
                    @Override
                    public Command execute() {
                        //fortressService.registerFortress(company, new FortressInputBean(headers.iterator().next().getFortress()), true);
                        Iterable<TrackResultBean> resultBeans = trackService.createHeaders(headers, company, fortress);
                        processLogs(company, resultBeans);
                        return this;
                    }
                }
                DeadlockRetry.execute(new DLCommand(metaInputBeans), "creating headers", 20);
            }

        } else {
            logger.info("Processing in slow Transaction mode");
            for (MetaInputBean inputBean : inputBeans) {
                createHeader(company, fortress, inputBean);
            }
        }
        watch.stop();
        logger.info("Completed Batch [{}] - secs= {}, RPS={}", id, f.format(watch.getTotalTimeSeconds()), f.format(inputBeans.size() / watch.getTotalTimeSeconds()));
        return inputBeans.size();
    }

    public TrackResultBean createHeader(MetaInputBean inputBean, String apiKey) throws DatagioException {
        if (inputBean == null)
            throw new DatagioException("No input to process");
        LogInputBean logBean = inputBean.getLog();
        if (logBean != null) // Error as soon as we can
            logBean.setWhat(logBean.getWhat());

        Company company = registrationService.resolveCompany(apiKey);
        Fortress fortress = fortressService.registerFortress(company, new FortressInputBean(inputBean.getFortress(), true));
        fortress.setCompany(company);
        return createHeader(company, fortress, inputBean);
    }

    public TrackResultBean createHeader(final Company company, final Fortress fortress, final MetaInputBean inputBean) throws DatagioException {
        if (inputBean == null)
            throw new DatagioException("No input to process!");

        class HeaderDeadlockRetry implements Command {
            TrackResultBean result = null;
            @Override
            public Command execute() {
                result = trackService.createHeader(inputBean, company, fortress);
                processLogFromResult(company, result);

                return this;
            }
        }

        HeaderDeadlockRetry c = new HeaderDeadlockRetry();
        com.auditbucket.helper.DeadlockRetry.execute(c, "create header", 10);
        return c.result;
    }

    @Async
    public Future<Void> processLogs(Company company, Iterable<TrackResultBean> resultBeans) {

        for (TrackResultBean resultBean : resultBeans) {
            processLogFromResult(company, resultBean);
        }
        return new AsyncResult<>(null);
    }

    public LogResultBean processLog(LogInputBean input) {
        MetaHeader header = trackService.getHeader(null, input.getMetaKey());
        return processLogForHeader(header, input);
    }

    private LogResultBean processCompanyLog(Company company, TrackResultBean resultBean) {
        MetaHeader header = resultBean.getMetaHeader();
        if (header == null) header = trackService.getHeader(company, resultBean.getMetaKey());
        return processLogForHeader(header, resultBean.getLog());
    }

    private void processLogFromResult(Company company, TrackResultBean resultBean) {
        LogInputBean logBean = resultBean.getLog();
        MetaHeader header = resultBean.getMetaHeader();
        // Here on could be spun in to a separate thread. The log has to happen eventually
        //   and shouldn't fail.
        if (resultBean.getLog() != null) {
            // Secret back door so that the log result can quickly get the auditid
            logBean.setMetaId(resultBean.getAuditId());
            logBean.setMetaKey(resultBean.getMetaKey());
            logBean.setFortressUser(resultBean.getMetaInputBean().getFortressUser());
            logBean.setCallerRef(resultBean.getCallerRef());

            LogResultBean logResult ;
            if ( header!= null )
                logResult = processLogForHeader(header, logBean);
            else
                logResult = processCompanyLog(company, resultBean);

            logResult.setMetaKey(null);// Don't duplicate the text as it's in the header
            logResult.setFortressUser(null);
            resultBean.setLogResult(logResult);

        } else {
            if (resultBean.getMetaInputBean().isTrackSuppressed())
                // If we aren't tracking in the graph, then we have to be searching
                // else why even call this service??
                trackService.makeHeaderSearchable(company, resultBean, resultBean.getMetaInputBean().getEvent(), resultBean.getMetaInputBean().getWhen());
            else if (!resultBean.isDuplicate() &&
                    resultBean.getMetaInputBean().getEvent() != null && !"".equals(resultBean.getMetaInputBean().getEvent())) {
                trackService.makeHeaderSearchable(company, resultBean, resultBean.getMetaInputBean().getEvent(), resultBean.getMetaInputBean().getWhen());
            }
        }
    }

    /**
     * Will locate the audit header from the supplied input
     * @param company valid company the caller can operate on
     * @param input   payload containing at least the metaKey
     * @return result of the log
     */
    public LogResultBean processLogForCompany(Company company, LogInputBean input) {
        MetaHeader header = trackService.getHeader(company, input.getMetaKey());
        if (header == null )
            throw new DatagioException("Unable to find the request auditHeader "+input.getMetaKey());
        return processLogForHeader(header, input);
    }

    /**
     * Deadlock safe processor that creates the log then indexes the change to the search service if necessary
     *
     * @param header Header that the caller is authorised to work with
     * @param logInputBean log details to apply to the authorised header
     * @return result details
     * @throws com.auditbucket.helper.DatagioException
     */
    public LogResultBean processLogForHeader(final MetaHeader header, final LogInputBean logInputBean) throws DatagioException {
        logInputBean.setWhat(logInputBean.getWhat());
        class DeadLockCommand implements Command {
            LogResultBean result = null;
            @Override
            public Command execute() {
                result = trackService.createLog(header, logInputBean);
                return this;
            }
        }
        DeadLockCommand c = new DeadLockCommand();
        DeadlockRetry.execute(c, "processing log for header", 20);

        if (c.result != null && c.result.getStatus() == LogInputBean.LogStatus.OK)
            trackService.makeChangeSearchable(c.result.getSearchDocument());

        return c.result;

    }

    /**
     * Rebuilds all search documents for the supplied fortress
     *
     * @param fortressName name of the fortress to rebuild
     * @throws com.auditbucket.helper.DatagioException
     */
    @Async
    public void reindex(Company company, String fortressName) throws DatagioException {
        Fortress fortress = fortressService.findByName(company, fortressName);
        if (fortress == null)
            throw new DatagioException("Fortress [" + fortress + "] could not be found");
        Long skipCount = 0l;
        long result = reindex(skipCount, fortress);
        logger.info("Reindex Search request completed. Processed [" + result + "] headers for [" + fortressName + "]");
    }

    private long reindex(Long skipCount, Fortress fortress) {

        Collection<MetaHeader> headers = trackService.getHeaders(fortress, skipCount);
        if (headers.isEmpty())
            return skipCount;
        skipCount = reindexHeaders(headers, skipCount);
        return reindex(skipCount, fortress);

    }

    /**
     * Rebuilds all search documents for the supplied fortress of the supplied document type
     *
     * @param fortressName name of the fortress to rebuild
     * @throws com.auditbucket.helper.DatagioException
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
        skipCount = reindexHeaders(headers, skipCount);
        return reindexByDocType(skipCount, fortress, docType);

    }

    private Long reindexHeaders(Collection<MetaHeader> headers, Long skipCount) {
        for (MetaHeader header : headers) {
            trackService.rebuild(header);
            skipCount++;
        }
        return skipCount;
    }

    public TrackedSummaryBean getTrackedSummary(String metaKey) {
        return getTrackedSummary(null, metaKey);
    }

    public TrackedSummaryBean getTrackedSummary(Company company, String metaKey) {
        return trackService.getMetaSummary(company, metaKey);
    }

}
