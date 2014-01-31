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
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.TagService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.text.DecimalFormat;
import java.util.*;
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
        if (apiKey == null) {
            // Find by logged in user name
            c = securityHelper.getCompany();
        } else {
            c = companyService.findByApiKey(apiKey);
        }
        if (c == null)
            throw new AuditException("Unable to find the requested API Key");
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

    public void createHeaders(AuditHeaderInputBean[] inputBeans) throws AuditException {
        AuditHeaderInputBean args = inputBeans[0];
        Company company = resolveCompany(args.getApiKey());
        Fortress fortress = resolveFortress(company, args);
        fortress.setCompany(company);


        for (AuditHeaderInputBean inputBean : inputBeans) {
            createHeadersAsync(inputBeans, company, fortress);
        }
        logger.debug("Batch Request processed");
    }

    public void createTagStructure(AuditHeaderInputBean[] inputBeans, Company company) {
        Map<String, Object> tagProcessed = new HashMap<>(); // Map to track tags we've created
        for (AuditHeaderInputBean inputBean : inputBeans) {
            if (inputBean.getAssociatedTags() != null)
                auditTagService.createTagStructure(inputBean.getAssociatedTags(), company);

            Map<String, Object> tags = inputBean.getTagValues();
            Collection<TagInputBean> tagSet = new ArrayList<>();

            for (String tag : tags.keySet()) {
                if (tagProcessed.get(tag) == null) {
                    //logger.info(tag);
                    tagSet.add(new TagInputBean(tag)); // Create Me!
                    tagProcessed.put(tag, true); // suppress duplicates
                }
            }
            if (!tagSet.isEmpty()) // Anything new to add?
                tagService.processTags(tagSet, company);
        }
    }

    static DecimalFormat f = new DecimalFormat();

    @Async
    public Future<Integer> createHeadersAsync(AuditHeaderInputBean[] inputBeans, Company company, Fortress fortress) throws AuditException {
        if (inputBeans.length == 0)
            return null;
        fortress.setCompany(company);
        Long id = DateTime.now().getMillis();
        StopWatch watch = new StopWatch();
        int processCount = 0;
        try {
            watch.start();
            logger.info("Starting Batch [{}] - size [{}]", id, inputBeans.length);
            for (AuditHeaderInputBean inputBean : inputBeans) {
                createHeader(inputBean, company, fortress, false);
                processCount++;
            }

            watch.stop();
        } catch (Exception e) {
            logger.error("Async Header error", e);
            throw new AuditException("Async error progressing Headers", e);
        }
        logger.info("Completed Batch [{}] - secs= {}, RPS={}", id, f.format(watch.getTotalTimeSeconds()), f.format(inputBeans.length / watch.getTotalTimeSeconds()));
        return new AsyncResult<>(processCount);
    }

    public AuditResultBean createHeader(AuditHeaderInputBean inputBean) throws AuditException {
        if (inputBean == null)
            throw new AuditException("No input to process");
        AuditLogInputBean logBean = inputBean.getAuditLog();
        if (logBean != null) // Error as soon as we can
            logBean.setWhat(logBean.getWhat());

        Company company = resolveCompany(inputBean.getApiKey());
        Fortress fortress = resolveFortress(company, inputBean);
        fortress.setCompany(company);
        return createHeader(inputBean, company, fortress, false);
    }

    public AuditResultBean createHeader(AuditHeaderInputBean inputBean, Company company, Fortress fortress, boolean tagsProcessed) throws AuditException {
        // Establish directed tag structure
        if (!tagsProcessed) {
            AuditHeaderInputBean[] inputBeans = new AuditHeaderInputBean[1];
            inputBeans[0] = inputBean;
            createTagStructure(inputBeans, company);
        }
        AuditResultBean resultBean = null;

        // Deadlock re-try fun
        int retries = 4, retryCount = 0;
        while (resultBean == null)
            try {
                resultBean = auditService.createHeader(inputBean, company, fortress);

                // Don't recreate tags if we already handled this -ToDiscuss!!
                if (!resultBean.isDuplicate()) {
                    retryCount = 0;
                    if (inputBean.isTrackSuppressed())
                        // We need to get the "tags" across to ElasticSearch, so we mock them ;)
                        resultBean.setTags(auditTagService.associateTags(resultBean.getAuditHeader(), inputBean.getTagValues()));
                    else
                        // Write the associations to the graph
                        auditTagService.associateTags(resultBean.getAuditHeader(), inputBean.getTagValues());
                }
            } catch (RuntimeException re) {
                logger.debug("Deadlock Detected. Entering retry");
                retryCount++;
                if (retryCount == retries) {
                    // Deadlock retry
                    // ToDo: A Map<String,AuditHeader> keyed by Tag may reduce potential deadlocks as fewer tags are associated with more headers
                    // http://www.slideshare.net/neo4j/zephyr-neo4jgraphconnect-2013short
                    logger.error("Error creating Header, rolling back", re);
                    throw (re);
                }

            }

        AuditLogInputBean logBean = inputBean.getAuditLog();
        // Here on could be spun in to a separate thread. The log has to happen eventually
        //   and can't fail.
        if (inputBean.getAuditLog() != null) {
            // Secret back door so that the log result can quickly get the
            logBean.setAuditId(resultBean.getAuditId());
            logBean.setAuditKey(resultBean.getAuditKey());
            logBean.setFortressUser(inputBean.getFortressUser());
            logBean.setCallerRef(resultBean.getCallerRef());

            AuditLogResultBean logResult = createLog(inputBean.getAuditLog());
            logResult.setAuditKey(null);// Don't duplicate the text as it's in the header
            logResult.setFortressUser(null);
            resultBean.setLogResult(logResult);
        } else {
            if (inputBean.isTrackSuppressed())
                // If we aren't tracking in the graph, then we have to be searching
                // else why even call this service??
                auditService.makeHeaderSearchable(resultBean, inputBean.getEvent(), inputBean.getWhen(), company);
            else if (!resultBean.isDuplicate() &&
                    inputBean.getEvent() != null && !"".equals(inputBean.getEvent())) {
                auditService.makeHeaderSearchable(resultBean, inputBean.getEvent(), inputBean.getWhen(), company);
            }
        }
        return resultBean;

    }

    public AuditLogResultBean createLog(AuditLogInputBean auditLogInputBean) throws AuditException {
        return createLog(null, auditLogInputBean);
    }

    AuditLogResultBean createLog(AuditHeader header, AuditLogInputBean auditLogInputBean) throws AuditException {
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
     * @return number of documents processed
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

        Set<AuditHeader> headers = auditService.getAuditHeaders(fortress, skipCount);
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

        Set<AuditHeader> headers = auditService.getAuditHeaders(fortress, docType, skipCount);
        if (headers.isEmpty())
            return skipCount;
        skipCount = reindexHeaders(skipCount, headers);
        return reindexByDocType(skipCount, fortress, docType);

    }

    private Long reindexHeaders(Long skipCount, Set<AuditHeader> headers) {
        for (AuditHeader header : headers) {
            auditService.rebuild(header);
            skipCount++;
        }
        return skipCount;
    }

    public AuditSummaryBean getAuditSummary(String auditKey) throws AuditException {
        AuditSummaryBean summary = auditService.getAuditSummary(auditKey);
        return summary;
    }


}
