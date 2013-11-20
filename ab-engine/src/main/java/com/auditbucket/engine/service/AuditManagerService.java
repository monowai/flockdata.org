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

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.bean.*;
import com.auditbucket.helper.AuditException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

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
    private SecurityHelper securityHelper;

    private Company resolveCompany(String apiKey) throws AuditException {
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

    private Fortress resolveFortress(Company company, AuditHeaderInputBean inputBean) throws AuditException {
        Fortress fortress = companyService.getCompanyFortress(company.getId(), inputBean.getFortress());
        if (fortress == null) {
            throw new AuditException("Fortress {" + inputBean.getFortress() + "} does not exist");
        }

        return fortress;
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
        AuditResultBean resultBean = auditService.createHeader(inputBean, company, fortress);
        auditTagService.createTagValuesFuture(resultBean.getAuditHeader(), inputBean);

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
            // Make header searchable - metadata only
            if (inputBean.getEvent() != null && !"".equals(inputBean.getEvent())) {
                // Tracking an event only
                auditService.makeHeaderSearchable(resultBean, inputBean.getEvent(), inputBean.getWhen(), company.getId());
            }
        }
        return resultBean;

    }

    public AuditLogResultBean createLog(AuditLogInputBean auditLogInputBean) throws AuditException {
        return createLog(null, auditLogInputBean);
    }

    public AuditLogResultBean createLog(com.auditbucket.audit.model.AuditHeader header, AuditLogInputBean auditLogInputBean) throws AuditException {
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
    public long rebuild(String fortressName) throws AuditException {
        // ToDo: Rebuild by SearchDocument
        Fortress fortress = fortressService.findByName(fortressName);
        if (fortress == null)
            throw new AuditException("Fortress [" + fortress + "] could not be found");
        Long skipCount = 0l;
        return rebuild(skipCount, fortress);
    }

    private long rebuild(Long skipCount, Fortress fortress) {

        Set<AuditHeader> headers = auditService.getAuditHeaders(fortress, skipCount);
        if (headers.isEmpty())
            return skipCount;
        for (AuditHeader header : headers) {
            auditService.rebuild(header);
            skipCount++;
        }
        return rebuild(skipCount, fortress);

    }


    public AuditSummaryBean getAuditSummary(String auditKey) {
        AuditSummaryBean summary = auditService.getAuditSummary(auditKey);
        return summary;
    }
}
