/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.helper.AuditException;
import com.auditbucket.helper.Command;
import com.auditbucket.helper.DeadlockRetry;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.registration.service.TagService;
import com.google.common.collect.Lists;
import org.apache.commons.collections.FastArrayList;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/**
 *
 * Non transactional coordinator of mediation services
 *
 * User: Mike Holdsworth
 * Since: 28/08/13
 */
@Service
public class AuditManagerService {
    @Autowired
    AuditService auditService;

    @Autowired
    AuditTagService auditTagService;

    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    TagService tagService;

    private Logger logger = LoggerFactory.getLogger(AuditManagerService.class);

    @Autowired
    private RegistrationService registrationService;

    public Company resolveCompany(String apiKey) throws AuditException {
        return registrationService.resolveCompany(apiKey);
    }

    public Fortress resolveFortress(Company company, AuditHeaderInputBean inputBean) throws AuditException {
        return resolveFortress(company, inputBean, false);
    }

    public Fortress resolveFortress(Company company, AuditHeaderInputBean inputBean, boolean createIfMissing) throws AuditException {
        Fortress fortress = companyService.getCompanyFortress(company.getId(), inputBean.getFortress());
        if (fortress == null) {
            if (createIfMissing) {
                fortress = fortressService.registerFortress(new FortressInputBean(inputBean.getFortress(), false));
                logger.info("Automatically created fortress " + fortress.getName());
            }

            throw new AuditException("Fortress {" + inputBean.getFortress() + "} does not exist");
        }

        return fortress;
    }

    static DecimalFormat f = new DecimalFormat();

    /**
     * Process the AuditHeader input for a company asynchronously
     * @param inputBeans  data
     * @param company     for
     * @param fortress    system
     * @return process count - don't rely on it, why would you want it?
     * @throws AuditException
     */

    @Async
    public Future<Integer> createHeadersAsync(List<AuditHeaderInputBean> inputBeans, final Company company, final Fortress fortress) throws AuditException {
        // ToDo: Return strings which could contain only the caller ref data that failed.
        return new AsyncResult<>(createHeaders(inputBeans, company, fortress));
    }

    public Integer createHeaders(List<AuditHeaderInputBean> inputBeans, final Company company, final Fortress fortress) throws AuditException {

        fortress.setCompany(company);
        Long id = DateTime.now().getMillis();
        StopWatch watch = new StopWatch();
        watch.start();
        logger.info("Starting Batch [{}] - size [{}]", id, inputBeans.size());
        boolean newMode = true;
        if (newMode) {

            // Tune to balance against concurrency and batch transaction insert efficiency.
            List<List<AuditHeaderInputBean>> splitList = Lists.partition(inputBeans, 5);

            for (List<AuditHeaderInputBean> auditHeaderInputBeans : splitList) {

                class DLCommand implements Command {
                    ArrayList<AuditHeaderInputBean> headers = null;
                    DLCommand (List<AuditHeaderInputBean> processList){
                        headers = new FastArrayList(processList);
                    }
                    @Override
                    public Command execute() {
                        fortressService.registerFortress(new FortressInputBean(headers.iterator().next().getFortress()), company);
                        Iterable<AuditResultBean> resultBeans = auditService.createHeaders(headers, company, fortress);
                        processAuditLogsAsync(resultBeans, company);
                        return this;
                    }
                }
                DeadlockRetry.execute(new DLCommand(auditHeaderInputBeans), 10);
            }

        } else {
            logger.info("Processing in slow Transaction mode");
            for (AuditHeaderInputBean inputBean : inputBeans) {
                createHeader(inputBean, company, fortress);
            }
        }
        watch.stop();
        logger.info("Completed Batch [{}] - secs= {}, RPS={}", id, f.format(watch.getTotalTimeSeconds()), f.format(inputBeans.size() / watch.getTotalTimeSeconds()));
        return inputBeans.size();
    }

    public AuditResultBean createHeader(AuditHeaderInputBean inputBean, String apiKey) throws AuditException {
        if (inputBean == null)
            throw new AuditException("No input to process");
        AuditLogInputBean logBean = inputBean.getAuditLog();
        if (logBean != null) // Error as soon as we can
            logBean.setWhat(logBean.getWhat());

        Company company = resolveCompany(apiKey);
        Fortress fortress = resolveFortress(company, inputBean);
        fortress.setCompany(company);
        return createHeader(inputBean, company, fortress);
    }

    public AuditResultBean createHeader(final AuditHeaderInputBean inputBean, final Company company, final Fortress fortress) throws AuditException {
        if (inputBean == null)
            throw new AuditException("No input to process!");

        class DLCommand implements Command {
            AuditResultBean result = null;
            @Override
            public Command execute() {
                result = auditService.createHeader(inputBean, company, fortress);
                processAuditLog(result, company);

                return this;
            }
        }
        DLCommand c = new DLCommand();
        DeadlockRetry.execute(c , 10);
        return c.result;
    }

    @Async
    public Future<Void> processAuditLogsAsync(Iterable<AuditResultBean> resultBeans, Company company) {

        for (AuditResultBean resultBean : resultBeans) {
            processAuditLog(resultBean, company);
        }
        return new AsyncResult<>(null);
    }

    private void processAuditLog(AuditResultBean resultBean, Company company) {
        AuditLogInputBean logBean = resultBean.getAuditLog();
        AuditHeader header = resultBean.getAuditHeader();
        // Here on could be spun in to a separate thread. The log has to happen eventually
        //   and shouldn't fail.
        if (resultBean.getAuditLog() != null) {
            // Secret back door so that the log result can quickly get the
            logBean.setAuditId(resultBean.getAuditId());
            logBean.setAuditKey(resultBean.getAuditKey());
            logBean.setFortressUser(resultBean.getAuditInputBean().getFortressUser());
            logBean.setCallerRef(resultBean.getCallerRef());

            AuditLogResultBean logResult ;
            if ( header!= null )
                logResult =createLog(header, logBean);
            else
                logResult = createLog(company, resultBean);

            logResult.setAuditKey(null);// Don't duplicate the text as it's in the header
            logResult.setFortressUser(null);
            resultBean.setLogResult(logResult);

        } else {
            if (resultBean.getAuditInputBean().isTrackSuppressed())
                // If we aren't tracking in the graph, then we have to be searching
                // else why even call this service??
                auditService.makeHeaderSearchable(resultBean, resultBean.getAuditInputBean().getEvent(), resultBean.getAuditInputBean().getWhen(), company);
            else if (!resultBean.isDuplicate() &&
                    resultBean.getAuditInputBean().getEvent() != null && !"".equals(resultBean.getAuditInputBean().getEvent())) {
                auditService.makeHeaderSearchable(resultBean, resultBean.getAuditInputBean().getEvent(), resultBean.getAuditInputBean().getWhen(), company);
            }
        }
    }

    public AuditLogResultBean createLog(AuditLogInputBean input) {
        AuditHeader header = auditService.getHeader(null, input.getAuditKey());
        return createLog(header, input);
    }

    public AuditLogResultBean createLog(Company company, AuditResultBean resultBean) {
        AuditHeader header = resultBean.getAuditHeader();

        if (header == null) header = auditService.getHeader(company, resultBean.getAuditKey());
        return createLog(header, resultBean.getAuditLog());
    }

    public AuditLogResultBean createLog(final AuditHeader header, final AuditLogInputBean auditLogInputBean) throws AuditException {
        auditLogInputBean.setWhat(auditLogInputBean.getWhat());
        class DLCommand implements Command {
            AuditLogResultBean result = null;
            @Override
            public Command execute() {
                result = auditService.createLog(header, auditLogInputBean);
                return this;
            }
        }
        DLCommand c = new DLCommand();
        DeadlockRetry.execute(c , 10);

        if (c.result != null && c.result.getStatus() == AuditLogInputBean.LogStatus.OK)
            auditService.makeChangeSearchable(c.result.getSearchDocument());

        return c.result;

    }

    /**
     * Rebuilds all search documents for the supplied fortress
     *
     * @param fortressName name of the fortress to rebuild
     * @throws AuditException
     */
    public void reindex(String fortressName) throws AuditException {
        Fortress fortress = fortressService.findByName(fortressName);
        if (fortress == null)
            throw new AuditException("Fortress [" + fortress + "] could not be found");
        Long skipCount = 0l;
        long result = reindex(skipCount, fortress);
        logger.info("Reindex Search request completed. Processed [" + result + "] headers for [" + fortressName + "]");
    }

    private long reindex(Long skipCount, Fortress fortress) {

        Collection<AuditHeader> headers = auditService.getAuditHeaders(fortress, skipCount);
        if (headers.isEmpty())
            return skipCount;
        skipCount = reindexHeaders(skipCount, headers);
        return reindex(skipCount, fortress);

    }

    /**
     * Rebuilds all search documents for the supplied fortress of the supplied document type
     *
     * @param fortressName name of the fortress to rebuild
     * @throws AuditException
     */
    public void reindexByDocType(String fortressName, String docType) throws AuditException {
        Fortress fortress = fortressService.findByName(fortressName);
        if (fortress == null)
            throw new AuditException("Fortress [" + fortress + "] could not be found");
        Long skipCount = 0l;
        long result = reindexByDocType(skipCount, fortress, docType);
        logger.info("Reindex Search request completed. Processed [" + result + "] headers for [" + fortressName + "] and document type [" + docType + "]");
    }

    private long reindexByDocType(Long skipCount, Fortress fortress, String docType) {

        Collection<AuditHeader> headers = auditService.getAuditHeaders(fortress, docType, skipCount);
        if (headers.isEmpty())
            return skipCount;
        skipCount = reindexHeaders(skipCount, headers);
        return reindexByDocType(skipCount, fortress, docType);

    }

    private Long reindexHeaders(Long skipCount, Collection<AuditHeader> headers) {
        for (AuditHeader header : headers) {
            auditService.rebuild(header);
            skipCount++;
        }
        return skipCount;
    }

    public AuditSummaryBean getAuditSummary(String auditKey) {
        return getAuditSummary(auditKey, null);
    }

    public AuditSummaryBean getAuditSummary(String auditKey, Company company) {
        return auditService.getAuditSummary(auditKey, company);
    }
}
