package com.auditbucket.engine.endpoint;

import com.auditbucket.helper.CompanyResolver;
import com.auditbucket.helper.FlockException;
import com.auditbucket.profile.service.ImportProfileService;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.service.MediationFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 7/10/14
 * Time: 2:05 PM
 */
@RestController
@RequestMapping("/batch")
public class BatchEP {
    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    ImportProfileService profileService;

    @RequestMapping(value = "/{fortress}/{document}/{file}", consumes = "application/json", method = RequestMethod.PUT)
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public void track(
            HttpServletRequest request, @PathVariable("fortress") String fortressCode, @PathVariable("document") String documentName, @PathVariable("file") String file) throws FlockException, InterruptedException, ExecutionException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Company company = CompanyResolver.resolveCompany(request);
        profileService.process(company, fortressCode, documentName, file);
    }

}
