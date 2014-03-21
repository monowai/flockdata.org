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
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.helper.TagException;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.TagService;
import org.joda.time.DateTime;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.concurrent.Future;

/**
 * Exists because calling makeChangeSearchable within the completed transaction
 * of auditService.createLog resulted in a "__TYPE__ not found" exception from Neo4J
 * <p/>
 * http://stackoverflow.com/questions/18072961/loosing-type-under-load
 * <p/>
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

    @Autowired
    private SecurityHelper securityHelper;

    private Logger logger = LoggerFactory.getLogger(AuditManagerService.class);

    public Company resolveCompany(String apiKey) throws AuditException {
        Company c;
        c = securityHelper.getCompany(apiKey);
        if (c == null)
            throw new AuditException("Invalid API Key");
        return c;
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

    @Async
    public Future<Integer> createHeadersAsync(AuditHeaderInputBean[] inputBeans, Company company, Fortress fortress) throws AuditException {
        return new AsyncResult<>(createHeaders(inputBeans, company, fortress));
    }

    public Integer createHeaders(AuditHeaderInputBean[] inputBeans, Company company, Fortress fortress) throws AuditException {
        if (inputBeans.length == 0)
            return 0;
        fortress.setCompany(company);
        Long id = DateTime.now().getMillis();
        StopWatch watch = new StopWatch();
        int processCount = 0;
        try {
            watch.start();
            logger.info("Starting Batch [{}] - size [{}]", id, inputBeans.length);
            boolean newMode = true;
            if (newMode) {
                logger.info("Processing in Batch Transaction mode");
                Iterable<AuditResultBean> resultBeans = auditService.createHeaders(inputBeans, company, fortress);
                processAuditLogs(resultBeans, company);

            } else {
                logger.info("Processing in slow Transaction mode");
                for (AuditHeaderInputBean inputBean : inputBeans) {
                    createHeader(inputBean, company, fortress);
                    processCount++;
                }
            }
            watch.stop();
        } catch (Exception e) {
            logger.error("Async Header error", e);
            throw new AuditException("Async error progressing Headers", e);
        }
        logger.info("Completed Batch [{}] - secs= {}, RPS={}", id, f.format(watch.getTotalTimeSeconds()), f.format(inputBeans.length / watch.getTotalTimeSeconds()));
        return processCount;
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

    public AuditResultBean createHeader(AuditHeaderInputBean inputBean, Company company, Fortress fortress) throws AuditException {
        if (inputBean == null)
            throw new AuditException("No input to process!");

        AuditResultBean resultBean = null;

        // Deadlock re-try fun
        int maxRetry = 10, retryCount = 0;
        while (retryCount < maxRetry) {
            try {
                if (resultBean == null || resultBean.getAuditId() == null) {
                    resultBean = auditService.createHeader(inputBean, company, fortress);
                }
                retryCount = maxRetry; // Exit the loop
            } catch (RuntimeException re) {
                // ToDo: Exceptions getting wrapped in a JedisException. Can't directly catch the DDE hence the instanceof check
                if (re.getCause() instanceof NotFoundException || re.getCause() instanceof DeadlockDetectedException || re.getCause() instanceof InvalidDataAccessResourceUsageException || re.getCause() instanceof DataRetrievalFailureException) {
                    logger.debug("Deadlock Detected. Entering retry fortress [{}], docType {}, callerRef [{}], rolling back. Cause = {}", inputBean.getFortress(), inputBean.getDocumentType(), inputBean.getCallerRef(), re.getCause());
                    Thread.yield();
                    retryCount++;
                    if (retryCount == maxRetry) {
                        // http://www.slideshare.net/neo4j/zephyr-neo4jgraphconnect-2013short
                        logger.error("Error creating Header for fortress [{}], docType {}, callerRef [{}], rolling back. Cause = {}", inputBean.getFortress(), inputBean.getDocumentType(), inputBean.getCallerRef(), re.getCause());
                        throw (re);
                    }
                } else if (re.getCause() instanceof TagException) {
                    // Carry on processing FixMe - log this to an error channel
                    logger.error("Error creating Tag. Input was fortress [{}], docType {}, callerRef [{}], rolling back. Cause = {}", inputBean.getFortress(), inputBean.getDocumentType(), inputBean.getCallerRef(), re.getCause());
                } else {
                    throw (re);
                }
            }
        }

        processAuditLog(resultBean, company);
        return resultBean;

    }

    @Async
    public Future<Void> processAuditLogs(Iterable<AuditResultBean> resultBeans, Company company) {
        for (AuditResultBean resultBean : resultBeans)
            processAuditLog(resultBean, company);
        return new AsyncResult<>(null);
    }

    private void processAuditLog(AuditResultBean resultBean, Company company) {
        AuditLogInputBean logBean = resultBean.getAuditLog();
        // Here on could be spun in to a separate thread. The log has to happen eventually
        //   and shouldn't fail.
        if (resultBean.getAuditLog() != null) {
            // Secret back door so that the log result can quickly get the
            logBean.setAuditId(resultBean.getAuditId());
            logBean.setAuditKey(resultBean.getAuditKey());
            logBean.setFortressUser(resultBean.getAuditInputBean().getFortressUser());
            logBean.setCallerRef(resultBean.getCallerRef());

            AuditLogResultBean logResult = createLog(company, resultBean.getAuditLog());
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

    public AuditLogResultBean createLog(Company company, AuditLogInputBean input) {
        AuditHeader header = auditService.getHeader(company, input.getAuditKey());
        return createLog(header, input);
    }

    public AuditLogResultBean createLog(AuditHeader header, AuditLogInputBean auditLogInputBean) throws AuditException {
        auditLogInputBean.setWhat(auditLogInputBean.getWhat());
        AuditLogResultBean resultBean = auditService.createLog(header, auditLogInputBean);
        if (resultBean != null && resultBean.getStatus() == AuditLogInputBean.LogStatus.OK)
            auditService.makeChangeSearchable(resultBean.getSearchDocument());

        return resultBean;

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
     * @return number of documents processed
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
        AuditSummaryBean summary = auditService.getAuditSummary(auditKey, company);
        return summary;
    }
}
