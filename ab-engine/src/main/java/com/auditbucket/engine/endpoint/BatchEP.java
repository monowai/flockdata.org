package com.auditbucket.engine.endpoint;

import com.auditbucket.helper.CompanyResolver;
import com.auditbucket.helper.FlockException;
import com.auditbucket.helper.NotFoundException;
import com.auditbucket.profile.ImportProfile;
import com.auditbucket.profile.model.ProfileConfiguration;
import com.auditbucket.profile.service.ImportProfileService;
import com.auditbucket.registration.model.Company;
import com.auditbucket.track.service.MediationFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
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

    @RequestMapping(value = "/{fortress}/{document}/import", consumes = "application/json", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public void track(
            HttpServletRequest request, @PathVariable("fortress") String fortressCode, @PathVariable("document") String documentName, @RequestBody Map file) throws FlockException, InterruptedException, ExecutionException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Company company = CompanyResolver.resolveCompany(request);
        Object filename = file.get("file");
        if (filename == null)
            throw new NotFoundException("No file to process");

        profileService.validateArguments(company, fortressCode, documentName, filename.toString());
        profileService.process(company, fortressCode, documentName, filename.toString());
    }

    @RequestMapping(value = "/{fortress}/{document}", consumes = "application/json", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void putDocument(@RequestBody ImportProfile profile,
                            HttpServletRequest request, @PathVariable("fortress") String fortressCode, @PathVariable("document") String documentName) throws FlockException, InterruptedException, ExecutionException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Company company = CompanyResolver.resolveCompany(request);
        profileService.save(company, fortressCode, documentName, profile);
    }


    @RequestMapping(value = "/{fortress}/{document}", consumes = "application/json", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public ProfileConfiguration getDocument(HttpServletRequest request, @PathVariable("fortress") String fortressCode, @PathVariable("document") String documentName) throws FlockException, InterruptedException, ExecutionException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Company company = CompanyResolver.resolveCompany(request);
        return profileService.get(company, fortressCode, documentName);
    }

}
