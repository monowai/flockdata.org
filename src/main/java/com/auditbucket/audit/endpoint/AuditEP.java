package com.auditbucket.audit.endpoint;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditLogInputBean;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.service.AuditService;
import com.auditbucket.helper.SecurityHelper;
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

    @Autowired
    SecurityHelper securityHelper;

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    @ResponseBody
    public String get() throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/audit/ping
        return "Ping";
    }

    @RequestMapping(value = "/header/new", produces = "application/json", consumes = "application/json", method = RequestMethod.PUT)
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @ResponseBody
    public ResponseEntity<AuditHeaderInputBean> createHeader(@RequestBody AuditHeaderInputBean input) throws Exception {
        // curl -u mike:123 -X PUT http://localhost:8080/ab/audit/header/new/ -d '"fortress":"MyFortressName", "fortressUser": "yoursystemuser", "recordType":"Company","when":"2012-11-10"}'
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

    @RequestMapping(value = "/log/new", consumes = "application/json", method = RequestMethod.PUT)
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @ResponseBody
    public ResponseEntity<String> createLog(@RequestBody AuditLogInputBean input) throws Exception {
        // curl -u mike:123 -H "Content-Type:application/json" -X PUT http://localhost:8080/ab/audit/log/new -d '{"eventType":"change","auditKey":"c27ec2e5-2e17-4855-be18-bd8f82249157","fortressUser":"miketest","when":"2012-11-10", "what": "{\"name\": \"val\"}" }'
        try {

            AuditService.LogStatus ls = auditService.createLog(input);
            if (ls.equals(AuditService.LogStatus.FORBIDDEN))
                return new ResponseEntity<String>("", HttpStatus.FORBIDDEN);
            else if (ls.equals(AuditService.LogStatus.NOT_FOUND))
                return new ResponseEntity<String>("Illegal Audit Key", HttpStatus.NOT_FOUND);
            else if (ls.equals(AuditService.LogStatus.IGNORE))
                return new ResponseEntity<String>("Ignoring request to change as the 'what' has not changed", HttpStatus.NOT_MODIFIED);
            else if (ls.equals(AuditService.LogStatus.ILLEGAL_ARGUMENT))
                return new ResponseEntity<String>(input.getMessage(), HttpStatus.NO_CONTENT);


            return new ResponseEntity<String>("OK", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
        }
    }


    @RequestMapping(value = "/{auditKey}", method = RequestMethod.GET)
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