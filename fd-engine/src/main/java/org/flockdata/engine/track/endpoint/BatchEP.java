/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.track.endpoint;

import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.profile.service.ImportProfileService;
import org.flockdata.registration.model.Company;
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
