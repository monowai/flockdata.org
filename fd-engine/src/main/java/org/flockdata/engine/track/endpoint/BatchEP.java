/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.engine.track.endpoint;

import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.service.ImportProfileService;
import org.flockdata.track.service.MediationFacade;
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
@RequestMapping("${org.fd.engine.system.api:api}/v1/batch")
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
        boolean async = false;
        Object value = file.get("async");
        if ( value != null )
            async = Boolean.parseBoolean(value.toString());

        if ( async)
            profileService.processAsync(company, fortressCode, documentName, filename.toString());
        else
            profileService.process(company, fortressCode, documentName, filename.toString(), async);
    }

    @RequestMapping(value = "/{fortress}/{document}", consumes = "application/json", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void putDocument(@RequestBody ContentProfileImpl profile,
                            HttpServletRequest request, @PathVariable("fortress") String fortressCode, @PathVariable("document") String documentName) throws FlockException, InterruptedException, ExecutionException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Company company = CompanyResolver.resolveCompany(request);
        profileService.save(company, fortressCode, documentName, profile);
    }


    @RequestMapping(value = "/{fortress}/{document}", consumes = "application/json", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public ContentProfile getDocument(HttpServletRequest request, @PathVariable("fortress") String fortressCode, @PathVariable("document") String documentName) throws FlockException, InterruptedException, ExecutionException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        Company company = CompanyResolver.resolveCompany(request);
        return profileService.get(company, fortressCode, documentName);
    }

}
