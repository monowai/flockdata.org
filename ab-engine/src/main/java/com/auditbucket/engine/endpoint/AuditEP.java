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

package com.auditbucket.engine.endpoint;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditLog;
import com.auditbucket.audit.model.AuditTag;
import com.auditbucket.audit.model.TxRef;
import com.auditbucket.bean.*;
import com.auditbucket.engine.service.AuditManagerService;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.engine.service.AuditTagService;
import com.auditbucket.engine.service.EngineAdmin;
import com.auditbucket.helper.AuditException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@Controller
@RequestMapping("/audit")
@MessageEndpoint
public class AuditEP {
    @Autowired
    AuditService auditService;

    @Autowired
    EngineAdmin auditAdmin;

    @Autowired
    AuditManagerService auditManager;

    @Autowired
    FortressService fortressService;

    @Autowired
    AuditTagService auditTagService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    CompanyService companyService;

    private static Logger logger = LoggerFactory.getLogger(AuditEP.class);

    @ResponseBody
    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public String get() {
        // curl -X GET http://auditbucketdemo.entiviti.com:8081/ab-engine/v1/audit/ping
        return "Pong!";
    }

    @ResponseBody
    @RequestMapping(value = "/health", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public Map<String, String> getHealth() {
        return auditAdmin.getHealth();
    }

    @ResponseBody
    @RequestMapping(value = "/", consumes = "application/json", method = RequestMethod.PUT)
    @Secured({"ROLE_USER"})
    public void createHeaders(@RequestBody AuditHeaderInputBean[] inputBeans) throws AuditException {
        createHeadersF(inputBeans, false);
    }

    public void createHeadersF(AuditHeaderInputBean[] inputBeans, boolean waitForFinish) throws AuditException {
        Company company = auditManager.resolveCompany(inputBeans[0].getApiKey());
        Fortress fortress = auditManager.resolveFortress(company, inputBeans[0], true);
        boolean async = true;
        //auditManager.createTagStructure(inputBeans, company);

        if (async) {

            Future<Integer> am = auditManager.createHeadersAsync(inputBeans, company, fortress);
            if (waitForFinish)
                while (!am.isDone()) {
                    //
                }

        } else {

            for (AuditHeaderInputBean inputBean : inputBeans) {
                auditManager.createHeader(inputBean, company, fortress, true);
            }
        }
    }

    /**
     * Creates a header
     *
     * @param input audit header input
     * @return AuditResultBean
     * @throws AuditException
     */
    @ResponseBody
    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    @Secured({"ROLE_USER"})
    public ResponseEntity<AuditResultBean> createHeader(@RequestBody AuditHeaderInputBean input) throws AuditException {
        // curl -u mike:123 -H "Content-Type:application/json" -X POST http://localhost:8080/ab/audit/header/new/ -d '"fortress":"MyFortressName", "fortressUser": "yoursystemuser", "documentType":"CompanyNode","when":"2012-11-10"}'
        AuditResultBean auditResultBean;
        auditResultBean = auditManager.createHeader(input);
        auditResultBean.setStatus("OK");
        return new ResponseEntity<>(auditResultBean, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/log/", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    @Secured({"ROLE_USER"})
    public ResponseEntity<AuditLogResultBean> createLog(@RequestBody AuditLogInputBean input) throws AuditException {
        // curl -u mike:123 -H "Content-Type:application/json" -X PUT http://localhost:8080/ab/audit/log/ -d '{"eventType":"change","auditKey":"c27ec2e5-2e17-4855-be18-bd8f82249157","fortressUser":"miketest","when":"2012-11-10", "what": "{\"name\": \"val\"}" }'

        AuditLogResultBean resultBean = auditManager.createLog(input);
        AuditLogInputBean.LogStatus ls = input.getAbStatus();
        if (ls.equals(AuditLogInputBean.LogStatus.FORBIDDEN))
            return new ResponseEntity<>(resultBean, HttpStatus.FORBIDDEN);
        else if (ls.equals(AuditLogInputBean.LogStatus.NOT_FOUND)) {
            input.setAbMessage("Illegal audit key");
            return new ResponseEntity<>(resultBean, HttpStatus.NOT_FOUND);
        } else if (ls.equals(AuditLogInputBean.LogStatus.IGNORE)) {
            input.setAbMessage("Ignoring request to change as the 'what' has not changed");
            return new ResponseEntity<>(resultBean, HttpStatus.NOT_MODIFIED);
        } else if (ls.equals(AuditLogInputBean.LogStatus.ILLEGAL_ARGUMENT)) {
            return new ResponseEntity<>(resultBean, HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(resultBean, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/{fortress}/{recordType}/{callerRef}", method = RequestMethod.PUT)
    @Async
    @Secured({"ROLE_USER"})
    public ResponseEntity<AuditResultBean> putByClientRef(@RequestBody AuditHeaderInputBean input,
                                                          @PathVariable("fortress") String fortress,
                                                          @PathVariable("recordType") String recordType,
                                                          @PathVariable("callerRef") String callerRef) throws AuditException {
        AuditResultBean auditResultBean;
        input.setFortress(fortress);
        input.setDocumentType(recordType);
        input.setCallerRef(callerRef);
        input.setAuditKey(null);
        auditResultBean = auditManager.createHeader(input);
        auditResultBean.setStatus("OK");
        return new ResponseEntity<>(auditResultBean, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{fortress}/{recordType}/{callerRef}", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<com.auditbucket.audit.model.AuditHeader> getByClientRef(@PathVariable("fortress") String fortress,
                                                                                  @PathVariable("recordType") String recordType,
                                                                                  @PathVariable("callerRef") String callerRef) {
        Fortress f = fortressService.findByName(fortress);
        AuditHeader result = auditService.findByCallerRef(f, recordType, callerRef);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/{auditKey}", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<com.auditbucket.audit.model.AuditHeader> getAudit(@PathVariable("auditKey") String auditKey) {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        com.auditbucket.audit.model.AuditHeader result = auditService.getHeader(auditKey, true);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/{auditKey}/logs", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public Set<AuditLog> getAuditLogs(@PathVariable("auditKey") String auditKey) throws AuditException {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/c27ec2e5-2e17-4855-be18-bd8f82249157/logs
        return auditService.getAuditLogs(auditKey);

    }

    @ResponseBody
    @RequestMapping(value = "/{auditKey}/summary", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<AuditSummaryBean> getAuditSummary(@PathVariable("auditKey") String auditKey) {
        return new ResponseEntity<>(auditManager.getAuditSummary(auditKey), HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{auditKey}/lastlog", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<AuditLog> getLastChange(@PathVariable("auditKey") String auditKey) throws AuditException {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/c27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
        AuditLog changed = auditService.getLastLog(auditKey);
        if (changed != null)
            return new ResponseEntity<>(changed, HttpStatus.OK);

        return new ResponseEntity<>((AuditLog) null, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{auditKey}/{logId}", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<AuditLogDetailBean> getFullLog(@PathVariable("auditKey") String auditKey, @PathVariable("logId") Long logId) {

        AuditLogDetailBean change = auditService.getFullDetail(auditKey, logId);

        if (change != null)
            return new ResponseEntity<>(change, HttpStatus.OK);

        return new ResponseEntity<>((AuditLogDetailBean) null, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{auditKey}/tags", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<Set<AuditTag>> getAuditTags(@PathVariable("auditKey") String auditKey) {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        com.auditbucket.audit.model.AuditHeader result = auditService.getHeader(auditKey);
        return new ResponseEntity<>(auditTagService.findAuditTags(result), HttpStatus.OK);
    }


    @ResponseBody
    @RequestMapping(value = "/tx/{txRef}", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<TxRef> getAuditTx(@PathVariable("txRef") String txRef) {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        TxRef result;
        result = auditService.findTx(txRef);
        return new ResponseEntity<>(result, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/tx/{txRef}/headers", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<Map<String, Object>> getAuditTxHeaders(@PathVariable("txRef") String txRef) {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        Set<AuditHeader> headers;
        Map<String, Object> result = new HashMap<>(2);
        headers = auditService.findTxHeaders(txRef);
        result.put("txRef", txRef);
        result.put("headers", headers);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/tx/{txRef}/logs", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<Map> getAuditTxLogs(@PathVariable("txRef") String txRef) {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        Map<String, Object> result;
        result = auditService.findByTXRef(txRef);
        if (result == null) {
            result = new HashMap<>(1);
            result.put("txRef", "Not a valid transaction identifier");
            return new ResponseEntity<>((Map) result, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<Map>(result, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/{fortressName}/rebuild", produces = "application/json", method = RequestMethod.POST)
    @Secured({"ROLE_USER"})
    public ResponseEntity<String> rebuildSearch(@PathVariable("fortressName") String fortressName) throws AuditException {
        logger.info("Reindex command received for " + fortressName + " from [" + securityHelper.getLoggedInUser() + "]");
        auditManager.reindex(fortressName);
        return new ResponseEntity<>("Request to reindex has been received", HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/{fortressName}/{docType}/rebuild", produces = "application/json", method = RequestMethod.POST)
    @Secured({"ROLE_USER"})
    public ResponseEntity<String> rebuildSearch(@PathVariable("fortressName") String fortressName, @PathVariable("docType") String docType) throws AuditException {
        logger.info("Reindex command received for " + fortressName + " & docType " + docType + " from [" + securityHelper.getLoggedInUser() + "]");
        auditManager.reindexByDocType(fortressName, docType);
        return new ResponseEntity<>("Request to reindex has been received", HttpStatus.OK);
    }

}