/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.profile.endpoint;

import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.profile.ContentProfileImpl;
import org.flockdata.profile.ContentProfileResult;
import org.flockdata.profile.ContentValidationRequest;
import org.flockdata.profile.ContentValidationResults;
import org.flockdata.profile.model.ContentProfile;
import org.flockdata.profile.model.ImportFile;
import org.flockdata.profile.service.ContentProfileService;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.service.FortressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Since: 8/11/13
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/content")
public class ContentProfileEP {

    @Autowired
    ContentProfileService profileService;

    @Autowired
    FortressService fortressService;

    @Autowired
    ConceptService conceptService;

    @RequestMapping(value = "/",
            produces = "application/json",
            method = RequestMethod.GET)
    public Collection<ContentProfileResult> getProfiles (HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return profileService.find(company);
    }

    @RequestMapping(value = "/{key}",
            produces = "application/json",
            method = RequestMethod.GET)
    public ContentProfileResult getProfileKey(HttpServletRequest request, @PathVariable("key") String key) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return profileService.find(company,key);
    }

    @RequestMapping(value = "/{fortressCode}/{docTypeName}",
            produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ContentProfile getContentProfile (
            HttpServletRequest request,
            @PathVariable("fortressCode") String fortressCode,
            @PathVariable("docTypeName") String docTypeName) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        Fortress fortress = fortressService.getFortress(company, fortressCode);
        if ( fortress == null )
            throw new IllegalArgumentException("Unable to locate the fortress " + fortressCode);

        DocumentType documentType = conceptService.resolveByDocCode(fortress, docTypeName, Boolean.FALSE);
        if ( documentType == null )
            throw new IllegalArgumentException("Unable to locate the document " + docTypeName);

        return profileService.get(company, fortress, documentType);

    }

    @RequestMapping(value = "/{fortressCode}/{docTypeName}",
                    produces = "application/json",
                    consumes = "application/json",
                    method = RequestMethod.POST)
    public ContentProfileResult storeContentProfile (HttpServletRequest request,
                                        @PathVariable("fortressCode") String fortressCode,
                                        @PathVariable("docTypeName") String docTypeName,
                                        @RequestBody ContentProfileImpl contentProfile) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        Fortress fortress = fortressService.getFortress(company, fortressCode);
        if ( fortress == null )
            throw new IllegalArgumentException("Unable to locate the fortress " + fortressCode);

        DocumentType documentType = conceptService.resolveByDocCode(fortress, docTypeName, Boolean.FALSE);
        if ( documentType == null )
            throw new IllegalArgumentException("Unable to locate the document " + docTypeName);
        return profileService.saveFortressContentType(company, fortress, documentType, contentProfile);

    }

    @RequestMapping(value = "/validate",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.POST)
    public ContentValidationResults validateContent (HttpServletRequest request,
                                                     @RequestBody ContentValidationRequest contentRequest) throws FlockException {
        CompanyResolver.resolveCompany(request);

        return profileService.validate(contentRequest);

    }

    @RequestMapping(value = "/default/{fortress}/{docType}",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.POST)
    public ContentProfile defaultContentProfile(HttpServletRequest request,
                                                @RequestBody ContentValidationRequest contentRequest,
                                                @PathVariable("fortress") String fortress,
                                                @PathVariable("docType") String docType)
            throws FlockException {
        CompanyResolver.resolveCompany(request);
        ContentProfile result = profileService.createDefaultContentProfile(contentRequest);
        result.setDocumentType( new DocumentTypeInputBean(docType));
        result.setContentType(ImportFile.ContentType.CSV);
        result.setHeader(true);
        result.setFortress(new FortressInputBean(fortress));
        return result;
    }


}
