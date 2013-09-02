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

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditLog;
import com.auditbucket.audit.model.TxRef;
import com.auditbucket.bean.*;
import com.auditbucket.engine.service.AuditManagerService;
import com.auditbucket.engine.service.AuditService;
import com.auditbucket.engine.service.EngineAdmin;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@Controller
@RequestMapping("/")
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
    CompanyService companyService;

    @ResponseBody
    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public String get() throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/ping
        return "Pong!";
    }

    @ResponseBody
    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public Map<String, String> getHealth() throws Exception {
        return auditAdmin.getHealth();
    }

    @ResponseBody
    @RequestMapping(value = "/header/new", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    public ResponseEntity<AuditResultBean> createHeader(@RequestBody AuditHeaderInputBean input) throws Exception {
        // curl -u mike:123 -H "Content-Type:application/json" -X POST http://localhost:8080/ab/audit/header/new/ -d '"fortress":"MyFortressName", "fortressUser": "yoursystemuser", "documentType":"CompanyNode","when":"2012-11-10"}'
        AuditResultBean auditResultBean;
        try {
            auditResultBean = auditManager.createHeader(input);
            auditResultBean.setStatus("OK");
            return new ResponseEntity<>(auditResultBean, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            auditResultBean = new AuditResultBean(e.getMessage());
            return new ResponseEntity<>(auditResultBean, HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            auditResultBean = new AuditResultBean("Forbidden Request " + e.getMessage());
            return new ResponseEntity<>(auditResultBean, HttpStatus.FORBIDDEN);
        }
    }

    @ResponseBody
    @RequestMapping(value = "/log/new", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    public ResponseEntity<AuditLogResultBean> createLog(@RequestBody AuditLogInputBean input) throws Exception {
        // curl -u mike:123 -H "Content-Type:application/json" -X PUT http://localhost:8080/ab/audit/log/new -d '{"eventType":"change","auditKey":"c27ec2e5-2e17-4855-be18-bd8f82249157","fortressUser":"miketest","when":"2012-11-10", "what": "{\"name\": \"val\"}" }'
        AuditLogResultBean resultBean = null;
        try {

            resultBean = auditManager.createLog(input);
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
        } catch (IllegalArgumentException e) {
            input.setAbMessage(e.getMessage());
            return new ResponseEntity<>(resultBean, HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            input.setAbMessage(e.getMessage());
            return new ResponseEntity<>(resultBean, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            input.setAbMessage(e.getMessage());
            return new ResponseEntity<>(resultBean, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ResponseBody
    @RequestMapping(value = "/tx/{txRef}/logs", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<Map> getAuditTxLogs(@PathVariable("txRef") String txRef) throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        Map<String, Object> result;
        try {
            result = auditService.findByTXRef(txRef);
            if (result == null) {
                result = new HashMap<>(1);
                result.put("txRef", "Not a valid transaction identifier");
                return new ResponseEntity<>((Map) result, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<Map>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>((Map) null, HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<>((Map) null, HttpStatus.FORBIDDEN);
        }
    }

    @ResponseBody
    @RequestMapping(value = "/tx/{txRef}/headers", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getAuditTxHeaders(@PathVariable("txRef") String txRef) throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        Set<AuditHeader> headers;
        Map<String, Object> result = new HashMap<>(2);
        try {
            headers = auditService.findTxHeaders(txRef);
            result.put("txRef", txRef);
            result.put("headers", headers);
            return new ResponseEntity<>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
        }
    }

    @ResponseBody
    @RequestMapping(value = "/tx/{txRef}", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<TxRef> getAuditTx(@PathVariable("txRef") String txRef) throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        TxRef result;
        try {
            result = auditService.findTx(txRef);
            return new ResponseEntity<>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>((TxRef) null, HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<>((TxRef) null, HttpStatus.FORBIDDEN);
        }
    }

    @ResponseBody
    @RequestMapping(value = "/{auditKey}", method = RequestMethod.GET)
    public ResponseEntity<AuditHeader> getAudit(@PathVariable("auditKey") String auditKey) throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        try {
            AuditHeader result = auditService.getHeader(auditKey, true);
            return new ResponseEntity<>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>((AuditHeader) null, HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<>((AuditHeader) null, HttpStatus.FORBIDDEN);
        }
    }

    @ResponseBody
    @RequestMapping(value = "/find/{fortress}/{recordType}/{callerRef}", method = RequestMethod.GET)
    public ResponseEntity<AuditHeader> getByClientRef(@PathVariable("fortress") String fortress,
                                                      @PathVariable("recordType") String recordType,
                                                      @PathVariable("callerRef") String callerRef) throws Exception {
        try {
            Fortress f = fortressService.find(fortress);
            AuditHeader result = auditService.findByCallerRef(f.getId(), recordType, callerRef);
            return new ResponseEntity<>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>((AuditHeader) null, HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<>((AuditHeader) null, HttpStatus.FORBIDDEN);
        }
    }

    @ResponseBody
    @RequestMapping(value = "/{auditKey}/logs", produces = "application/json", method = RequestMethod.GET)
    public Set<AuditLog> getAuditLogs(@PathVariable("auditKey") String auditKey) throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/c27ec2e5-2e17-4855-be18-bd8f82249157/logs
        return auditService.getAuditLogs(auditKey);

    }

    @ResponseBody
    @RequestMapping(value = "/{auditKey}/summary", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<AuditSummaryBean> getAuditSummary(@PathVariable("auditKey") String auditKey) throws Exception {


        return new ResponseEntity<>(auditManager.getAuditSummary(auditKey), HttpStatus.OK);

    }

    @ResponseBody
    @RequestMapping(value = "/{auditKey}/lastchange", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<AuditChange> getLastChange(@PathVariable("auditKey") String auditKey) throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/c27ec2e5-2e17-4855-be18-bd8f82249157/lastchange
        try {
            AuditChange changed = auditService.getLastChange(auditKey);
            if (changed != null)
                return new ResponseEntity<>(changed, HttpStatus.OK);

            return new ResponseEntity<>((AuditChange) null, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>((AuditChange) null, HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<>((AuditChange) null, HttpStatus.FORBIDDEN);
        }

    }
}