package com.auditbucket.engine.endpoint;

import com.auditbucket.authentication.handler.ApiKeyInterceptor;
import com.auditbucket.engine.service.EngineConfig;
import com.auditbucket.helper.CompanyResolver;
import com.auditbucket.helper.FlockException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.track.service.MediationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Engine admin
 * User: mike
 * Date: 15/04/14
 * Time: 9:09 PM
 * To change this template use File | Settings | File Templates.
 */
@RestController
@RequestMapping("/admin")
public class AdminEP {

    @Qualifier("mediationFacadeNeo4j")
    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    EngineConfig engineConfig;

    private static Logger logger = LoggerFactory.getLogger(AdminEP.class);

    @RequestMapping(value = "/cache", method = RequestMethod.DELETE)
    public void resetCache() {
        engineConfig.resetCache();

    }


    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public String getPing() {
        // curl -X GET http://localhost:8081/ab-engine/v1/track/ping
        return "Pong!";
    }


    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public Map<String, String> getHealth(HttpServletRequest request) throws FlockException {
        Object o = request.getAttribute(ApiKeyInterceptor.API_KEY);
        String apiKey = "";
        if (o != null )
            apiKey = o.toString();

        if ( "".equals(apiKey))
            apiKey = null;
        if ( request.getAttribute(ApiKeyInterceptor.COMPANY) == null &&
                 apiKey == null )
            throw new SecurityException("You are not authorized to perform this request");
        return engineConfig.getHealth();
    }


    @RequestMapping(value = "/{fortressName}/rebuild", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public ResponseEntity<String> rebuildSearch(@PathVariable("fortressName") String fortressName,
                                                HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        logger.info("Reindex command received for " + fortressName + " from [" + securityHelper.getLoggedInUser() + "]");
        mediationFacade.reindex(company, fortressName);
        return new ResponseEntity<>("Request to reindex has been received", HttpStatus.ACCEPTED);
    }


    @RequestMapping(value = "/{fortressName}/{docType}/rebuild", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public ResponseEntity<String> rebuildSearch(@PathVariable("fortressName") String fortressName, @PathVariable("docType") String docType,
                                                HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        logger.info("Reindex command received for " + fortressName + " & docType " + docType + " from [" + securityHelper.getLoggedInUser() + "]");
        mediationFacade.reindexByDocType(company, fortressName, docType);
        return new ResponseEntity<>("Request to reindex fortress document type has been received", HttpStatus.ACCEPTED);
    }

    @RequestMapping(value = "/{fortressName}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public ResponseEntity<String> purgeFortress(@PathVariable("fortressName") String fortressName,
                                                HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        mediationFacade.purge(company, fortressName);
        return new ResponseEntity<>("Purged " + fortressName, HttpStatus.ACCEPTED);

    }


}
