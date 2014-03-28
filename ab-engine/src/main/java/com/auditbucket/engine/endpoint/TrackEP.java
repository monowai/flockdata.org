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

package com.auditbucket.engine.endpoint;

import com.auditbucket.audit.bean.*;
import com.auditbucket.audit.model.*;
import com.auditbucket.engine.service.*;
import com.auditbucket.helper.ApiKeyHelper;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.registration.service.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@Controller
@RequestMapping("/track")
public class TrackEP {
    @Autowired
    TrackService trackService;

    @Autowired
    EngineConfig engineConfig;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    FortressService fortressService;

    @Autowired
    TagTrackService tagTrackService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    CompanyService companyService;

    @Autowired
    WhatService whatService;

    private static Logger logger = LoggerFactory.getLogger(TrackEP.class);
    @Autowired
    private RegistrationService registrationService;

    @ResponseBody
    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public String get() {
        // curl -X GET http://localhost:8081/ab-engine/v1/track/ping
        return "Pong!";
    }

    @ResponseBody
    @RequestMapping(value = "/health", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public Map<String, String> getHealth() {
        return engineConfig.getHealth();
    }

    @ResponseBody
    @RequestMapping(value = "/", consumes = "application/json", method = RequestMethod.PUT)

    public void trackHeaders(@RequestBody List<MetaInputBean> inputBeans,
                             String apiKey,
                             @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        trackHeadersAsync(inputBeans, true, ApiKeyHelper.resolveKey(apiKey, apiHeaderKey));
    }


    public Future<Integer> trackHeadersAsync(List<MetaInputBean> inputBeans, boolean async, String apiKey) throws DatagioException {
        Company company = registrationService.resolveCompany(apiKey);
        Fortress fortress = fortressService.registerFortress(company, new FortressInputBean(inputBeans.iterator().next().getFortress()), true);
        if (async) {
            Future<Integer> batch = mediationFacade.createHeadersAsync(inputBeans, company, fortress);
            Thread.yield();
            return batch;

        } else {
            return new AsyncResult<>(mediationFacade.createHeaders(inputBeans, company, fortress));
        }
    }

    /**
     * Creates a header
     *
     * @param input meta header input
     * @return TrackResultBean
     * @throws com.auditbucket.helper.DatagioException
     */
    @ResponseBody
    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    public ResponseEntity<TrackResultBean> trackHeader(@RequestBody MetaInputBean input,
                                                       String apiKey,
                                                       @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        // curl -u mike:123 -H "Content-Type:application/json" -X POST http://localhost:8081/ab-engine/track/track/ -d '"fortress":"MyFortressName", "fortressUser": "yoursystemuser", "documentType":"CompanyNode","when":"2012-11-10"}'

        TrackResultBean trackResultBean;
        trackResultBean = mediationFacade.createHeader(input, ApiKeyHelper.resolveKey(apiKey, apiHeaderKey));
        trackResultBean.setStatus("OK");
        return new ResponseEntity<>(trackResultBean, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/log/", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    public ResponseEntity<LogResultBean> trackLog(@RequestBody LogInputBean input, String apiKey,
                                                       @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {

        // If we have a valid company we are good to go.
        Company company = getCompany(apiKey, apiHeaderKey);

        LogResultBean resultBean = mediationFacade.processLogForCompany(company, input);
        LogInputBean.LogStatus ls = resultBean.getStatus();
        if (ls.equals(LogInputBean.LogStatus.FORBIDDEN))
            return new ResponseEntity<>(resultBean, HttpStatus.FORBIDDEN);
        else if (ls.equals(LogInputBean.LogStatus.NOT_FOUND)) {
            input.setAbMessage("Illegal meta key");
            return new ResponseEntity<>(resultBean, HttpStatus.NOT_FOUND);
        } else if (ls.equals(LogInputBean.LogStatus.IGNORE)) {
            input.setAbMessage("Ignoring request to change as the 'what' has not changed");
            return new ResponseEntity<>(resultBean, HttpStatus.NOT_MODIFIED);
        } else if (ls.equals(LogInputBean.LogStatus.ILLEGAL_ARGUMENT)) {
            return new ResponseEntity<>(resultBean, HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(resultBean, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/{fortress}/{recordType}/{callerRef}", method = RequestMethod.PUT)
    @Secured({"ROLE_USER"})
    public ResponseEntity<TrackResultBean> trackByClientRef(@RequestBody MetaInputBean input,
                                                            @PathVariable("fortress") String fortress,
                                                            @PathVariable("recordType") String recordType,
                                                            @PathVariable("callerRef") String callerRef) throws DatagioException {
        TrackResultBean trackResultBean;
        input.setFortress(fortress);
        input.setDocumentType(recordType);
        input.setCallerRef(callerRef);
        input.setMetaKey(null);
        trackResultBean = mediationFacade.createHeader(input, null);
        trackResultBean.setStatus("OK");
        return new ResponseEntity<>(trackResultBean, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{fortress}/{recordType}/{callerRef}", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<MetaHeader> getByClientRef(@PathVariable("fortress") String fortress,
                                                                                  @PathVariable("recordType") String recordType,
                                                                                  @PathVariable("callerRef") String callerRef) {
        Fortress f = fortressService.findByName(fortress);
        MetaHeader result = trackService.findByCallerRef(f, recordType, callerRef);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}", method = RequestMethod.GET)
    public ResponseEntity<MetaHeader> getAudit(@PathVariable("metaKey") String metaKey, String apiKey,
                                                @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        // curl -u mike:123 -H "Content-Type:application/json" -X PUT http://localhost:8081/ab-engine/track/{metaKey}/ -d '{"eventType":"change","metaKey":"c27ec2e5-2e17-4855-be18-bd8f82249157","fortressUser":"miketest","when":"2012-11-10", "what": "{\"name\": \"val\"}" }'
        Company company = getCompany(apiKey, apiHeaderKey);
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}
        MetaHeader result = trackService.getHeader(company, metaKey, true);
        if (result == null )
            throw new DatagioException("Unable to resolve requested meta key [" + metaKey + "]. Company is " +(company==null?"Invalid":"Valid"));

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private Company getCompany(String apiRequestKey, String apiHeaderKey) {
        Company company = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiRequestKey, apiHeaderKey));
        if ( company == null )
            throw new DatagioException( "Unable to resolve supplied API key to a valid company");
        return company;
    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/logs", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public Set<TrackLog> getLogs(@PathVariable("metaKey") String metaKey) throws DatagioException {
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}/logs
        return trackService.getLogs(metaKey);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/summary", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<TrackedSummaryBean> getAuditSummary(@PathVariable("metaKey") String metaKey, String apiKey,
                                                            @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiKey, apiHeaderKey);
        return new ResponseEntity<>(mediationFacade.getTrackedSummary(metaKey, company), HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/lastlog", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<TrackLog> getLastChange(@PathVariable("metaKey") String metaKey) throws DatagioException {
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/c27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
        TrackLog changed = trackService.getLastLog(metaKey);
        if (changed != null)
            return new ResponseEntity<>(changed, HttpStatus.OK);

        return new ResponseEntity<>((TrackLog) null, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/lastlog/what", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<LogWhat> getLastChangeWhat(@PathVariable("metaKey") String metaKey) throws DatagioException {
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/trackc27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
        MetaHeader header = trackService.getHeader(metaKey);
        if (header != null) {
            TrackLog changed = trackService.getLastLog(header);
            LogWhat what = whatService.getWhat(header, changed.getChange());

            if (changed != null)
                return new ResponseEntity<>(what, HttpStatus.OK);
        }

        return new ResponseEntity<>((LogWhat) null, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/{logId}/delta/{withId}", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<AuditDeltaBean> getDelta(@PathVariable("metaKey") String metaKey, @PathVariable("logId") Long logId, @PathVariable("withId") Long withId) {
        MetaHeader header = trackService.getHeader(metaKey);

        if (header != null) {
            TrackLog left = trackService.getLogForHeader(header, logId);
            TrackLog right = trackService.getLogForHeader(header, withId);
            if (left != null && right != null) {
                AuditDeltaBean deltaBean = whatService.getDelta(header, left.getChange(), right.getChange());

                if (deltaBean != null)
                    return new ResponseEntity<>(deltaBean, HttpStatus.OK);
            }
        }

        return new ResponseEntity<>((AuditDeltaBean) null, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/{logId}", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<LogDetailBean> getFullLog(@PathVariable("metaKey") String metaKey, @PathVariable("logId") Long logId) {

        LogDetailBean change = trackService.getFullDetail(metaKey, logId);

        if (change != null)
            return new ResponseEntity<>(change, HttpStatus.OK);

        return new ResponseEntity<>((LogDetailBean) null, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/{logId}/what", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<LogWhat> getLogWhat(@PathVariable("metaKey") String metaKey, @PathVariable("logId") Long logId) {

        MetaHeader header = trackService.getHeader(metaKey);
        if (header != null) {
            TrackLog log = trackService.getLogForHeader(header, logId);
            if (log != null)
                return new ResponseEntity<>(whatService.getWhat(header, log.getChange()), HttpStatus.OK);
        }

        return new ResponseEntity<>((LogWhat) null, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{metaKey}/tags", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<Set<TrackTag>> getAuditTags(@PathVariable("metaKey") String metaKey) {
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}
        MetaHeader result = trackService.getHeader(metaKey);
        return new ResponseEntity<>(tagTrackService.findTrackTags(result), HttpStatus.OK);
    }


    @ResponseBody
    @RequestMapping(value = "/tx/{txRef}", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<TxRef> getAuditTx(@PathVariable("txRef") String txRef) {
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}
        TxRef result;
        result = trackService.findTx(txRef);
        return new ResponseEntity<>(result, HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/tx/{txRef}/headers", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<Map<String, Object>> getAuditTxHeaders(@PathVariable("txRef") String txRef) {
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/{metaKey}
        Set<MetaHeader> headers;
        Map<String, Object> result = new HashMap<>(2);
        headers = trackService.findTxHeaders(txRef);
        result.put("txRef", txRef);
        result.put("headers", headers);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/tx/{txRef}/logs", produces = "application/json", method = RequestMethod.GET)
    @Secured({"ROLE_USER"})
    public ResponseEntity<Map> getAuditTxLogs(@PathVariable("txRef") String txRef) {
        // curl -u mike:123 -X GET http://localhost:8081/ab-engine/track/tx/{txRef}/logs
        Map<String, Object> result;
        result = trackService.findByTXRef(txRef);
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
    public ResponseEntity<String> rebuildSearch(@PathVariable("fortressName") String fortressName) throws DatagioException {
        logger.info("Reindex command received for " + fortressName + " from [" + securityHelper.getLoggedInUser() + "]");
        mediationFacade.reindex(fortressName);
        return new ResponseEntity<>("Request to reindex has been received", HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/{fortressName}/{docType}/rebuild", produces = "application/json", method = RequestMethod.POST)
    @Secured({"ROLE_USER"})
    public ResponseEntity<String> rebuildSearch(@PathVariable("fortressName") String fortressName, @PathVariable("docType") String docType) throws DatagioException {
        logger.info("Reindex command received for " + fortressName + " & docType " + docType + " from [" + securityHelper.getLoggedInUser() + "]");
        mediationFacade.reindexByDocType(fortressName, docType);
        return new ResponseEntity<>("Request to reindex has been received", HttpStatus.OK);
    }

}