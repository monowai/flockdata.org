package com.auditbucket.engine.endpoint;

import com.auditbucket.engine.service.EngineConfig;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.helper.ApiKeyHelper;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.service.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Engine admin
 * User: mike
 * Date: 15/04/14
 * Time: 9:09 PM
 * To change this template use File | Settings | File Templates.
 */
@Controller
@RequestMapping("/admin")

public class AdminEP {

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    EngineConfig engineConfig;

    private static Logger logger = LoggerFactory.getLogger(AdminEP.class);

    @RequestMapping(value = "/cache", method = RequestMethod.DELETE)
    public void resetCache (){
        engineConfig.resetCache();

    }

    @ResponseBody
    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public String get() {
        // curl -X GET http://localhost:8081/ab-engine/v1/track/ping
        return "Pong!";
    }

    @ResponseBody
    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public Map<String, String> getHealth(String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        String user = registrationService.getSystemUser(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey)).getLogin();
        if(user == null ||(user.equalsIgnoreCase(RegistrationService.GUEST.getLogin()) || user.equalsIgnoreCase("anonymousUser")))
            return null;
        return engineConfig.getHealth();
    }

    @ResponseBody
    @RequestMapping(value = "/{fortressName}/rebuild", method = RequestMethod.POST)
    public ResponseEntity<String> rebuildSearch(@PathVariable("fortressName") String fortressName,
                                                String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = getCompany(apiHeaderKey, apiKey);
        logger.info("Reindex command received for " + fortressName + " from [" + securityHelper.getLoggedInUser() + "]");
        mediationFacade.reindex(company, fortressName);
        return new ResponseEntity<>("Request to reindex has been received", HttpStatus.ACCEPTED);
    }

    @ResponseBody
    @RequestMapping(value = "/{fortressName}/{docType}/rebuild", method = RequestMethod.POST)
    public ResponseEntity<String> rebuildSearch(@PathVariable("fortressName") String fortressName, @PathVariable("docType") String docType
            , String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {

        Company company = getCompany(apiHeaderKey, apiKey);

        logger.info("Reindex command received for " + fortressName + " & docType " + docType + " from [" + securityHelper.getLoggedInUser() + "]");
        mediationFacade.reindexByDocType(company, fortressName, docType);
        return new ResponseEntity<>("Request to reindex fortress document type has been received", HttpStatus.ACCEPTED);
    }
    private Company getCompany(String apiHeaderKey, String apiRequestKey) throws DatagioException {
        Company company = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiRequestKey));
        if (company == null)
            throw new DatagioException("Unable to resolve supplied API key to a valid company");
        return company;
    }

    @ResponseBody
    @RequestMapping(value = "/{fortressName}", method = RequestMethod.DELETE)
    public ResponseEntity<String> purgeFortress(@PathVariable("fortressName") String fortressName,
                                      String apiKey,
                                      @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {

        mediationFacade.purge(fortressName, ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        return new ResponseEntity<>( "Purged " + fortressName, HttpStatus.OK);

    }



}
