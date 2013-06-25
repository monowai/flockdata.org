package com.auditbucket.audit.endpoint;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.ITxRef;
import com.auditbucket.audit.service.AuditService;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: mike
 * Date: 1/05/13
 * Time: 8:23 PM
 */
@Controller
@RequestMapping("/")
public class AuditEP {
    @Autowired
    AuditService auditService;

    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    @ResponseBody
    public String get() throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/ping
        return "Ping";
    }

    @RequestMapping(value = "/header/new", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @ResponseBody
    public ResponseEntity<AuditHeaderInputBean> createHeader(@RequestBody AuditHeaderInputBean input) throws Exception {
        // curl -u mike:123 -H "Content-Type:application/json" -X POST http://localhost:8080/ab/audit/header/new/ -d '"fortress":"MyFortressName", "fortressUser": "yoursystemuser", "documentType":"Company","when":"2012-11-10"}'
        try {
            input = auditService.createHeader(input);
            input.setLastMessage("OK");
            return new ResponseEntity<AuditHeaderInputBean>(input, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            input.setLastMessage(e.getMessage());
            return new ResponseEntity<AuditHeaderInputBean>(input, HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            input.setLastMessage(e.getMessage());
            return new ResponseEntity<AuditHeaderInputBean>(input, HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(value = "/log/new", consumes = "application/json", produces = "application/json", method = RequestMethod.PUT)
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @ResponseBody
    public ResponseEntity<AuditLogInputBean> createLog(@RequestBody AuditLogInputBean input) throws Exception {
        // curl -u mike:123 -H "Content-Type:application/json" -X PUT http://localhost:8080/ab/audit/log/new -d '{"eventType":"change","auditKey":"c27ec2e5-2e17-4855-be18-bd8f82249157","fortressUser":"miketest","when":"2012-11-10", "what": "{\"name\": \"val\"}" }'
        try {

            input = auditService.createLog(input);
            AuditService.LogStatus ls = input.getLogStatus();
            if (ls.equals(AuditService.LogStatus.FORBIDDEN))
                return new ResponseEntity<AuditLogInputBean>(input, HttpStatus.FORBIDDEN);
            else if (ls.equals(AuditService.LogStatus.NOT_FOUND)) {
                input.setMessage("Illegal audit key");
                return new ResponseEntity<AuditLogInputBean>(input, HttpStatus.NOT_FOUND);
            } else if (ls.equals(AuditService.LogStatus.IGNORE)) {
                input.setMessage("Ignoring request to change as the 'what' has not changed");
                return new ResponseEntity<AuditLogInputBean>(input, HttpStatus.NOT_MODIFIED);
            } else if (ls.equals(AuditService.LogStatus.ILLEGAL_ARGUMENT)) {
                return new ResponseEntity<AuditLogInputBean>(input, HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<AuditLogInputBean>(input, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            input.setMessage(e.getMessage());
            return new ResponseEntity<AuditLogInputBean>(input, HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            input.setMessage(e.getMessage());
            return new ResponseEntity<AuditLogInputBean>(input, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            input.setMessage(e.getMessage());
            return new ResponseEntity<AuditLogInputBean>(input, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/tx/{txRef}/logs", produces = "application/json", method = RequestMethod.GET)
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @ResponseBody
    public ResponseEntity<Map> getAuditTxLogs(@PathVariable("txRef") String txRef) throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        Map<String, Object> result;
        try {
            result = auditService.findByTXRef(txRef);
            if (result == null) {
                result = new HashMap<String, Object>(1);
                result.put("txRef", "Not a valid transaction identifier");
                return new ResponseEntity<Map>((Map) result, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<Map>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<Map>((Map) null, HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<Map>((Map) null, HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(value = "/tx/{txRef}/headers", produces = "application/json", method = RequestMethod.GET)
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAuditTxHeaders(@PathVariable("txRef") String txRef) throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        Set<IAuditHeader> headers;
        Map<String, Object> result = new HashMap<String, Object>(2);
        try {
            headers = auditService.findTxHeaders(txRef);
            result.put("txRef", txRef);
            result.put("headers", headers);
            return new ResponseEntity<Map<String, Object>>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<Map<String, Object>>(result, HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<Map<String, Object>>(result, HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(value = "/tx/{txRef}", produces = "application/json", method = RequestMethod.GET)
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @ResponseBody
    public ResponseEntity<ITxRef> getAuditTx(@PathVariable("txRef") String txRef) throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        ITxRef result;
        try {
            result = auditService.findTx(txRef);
            return new ResponseEntity<ITxRef>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<ITxRef>((ITxRef) null, HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<ITxRef>((ITxRef) null, HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(value = "/{auditKey}", method = RequestMethod.GET)
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @ResponseBody
    public ResponseEntity<IAuditHeader> getAudit(@PathVariable("auditKey") String auditKey) throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/{audit-key}
        try {
            IAuditHeader result = auditService.getHeader(auditKey);
            return new ResponseEntity<IAuditHeader>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<IAuditHeader>((IAuditHeader) null, HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<IAuditHeader>((IAuditHeader) null, HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(value = "/find/{fortress}/{recordType}/{clientRef}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<IAuditHeader> getByClientRef(@PathVariable("fortress") String fortress,
                                                       @PathVariable("recordType") String recordType,
                                                       @PathVariable("clientRef") String clientRef) throws Exception {
        try {
            IFortress f = fortressService.find(fortress);
            IAuditHeader result = auditService.findByName(f.getId(), recordType, clientRef);
            return new ResponseEntity<IAuditHeader>(result, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<IAuditHeader>((IAuditHeader) null, HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<IAuditHeader>((IAuditHeader) null, HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(value = "/{auditKey}/logs", method = RequestMethod.GET)
    @ResponseBody
    public Set<IAuditLog> getAuditLogs(@PathVariable("auditKey") String auditKey) throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/c27ec2e5-2e17-4855-be18-bd8f82249157/logs
        return auditService.getAuditLogs(auditKey);

    }

    @RequestMapping(value = "/{auditKey}/lastchange", method = RequestMethod.GET)
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @ResponseBody
    public ResponseEntity<IAuditLog> getLastChange(@PathVariable("auditKey") String auditKey) throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/c27ec2e5-2e17-4855-be18-bd8f82249157/logs
        try {
            IAuditHeader header = auditService.getHeader(auditKey);
            return new ResponseEntity<IAuditLog>(auditService.getLastChange(header), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<IAuditLog>((IAuditLog) null, HttpStatus.NOT_FOUND);
        } catch (SecurityException e) {
            return new ResponseEntity<IAuditLog>((IAuditLog) null, HttpStatus.FORBIDDEN);
        }

    }
}