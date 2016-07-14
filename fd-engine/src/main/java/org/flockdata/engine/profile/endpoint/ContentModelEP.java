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
import org.flockdata.profile.ContentModelResult;
import org.flockdata.profile.ContentValidationRequest;
import org.flockdata.profile.ContentValidationResults;
import org.flockdata.profile.model.ContentModel;
import org.flockdata.profile.service.ContentModelService;
import org.flockdata.track.service.FortressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Since: 8/11/13
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/model")
public class ContentModelEP {

    @Autowired
    ContentModelService contentModelService;

    @Autowired
    FortressService fortressService;

    @Autowired
    ConceptService conceptService;

    @RequestMapping(value = "/",
            produces = "application/json",
            method = RequestMethod.GET)
    public Collection<ContentModelResult> getModels(HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return contentModelService.find(company);
    }

    @RequestMapping(value = "/",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.POST)
    public Collection<ContentModelResult> storeModels(@RequestBody Collection<ContentModel> contentModels, HttpServletRequest request) throws FlockException {

        Company company = CompanyResolver.resolveCompany(request);
        Collection<ContentModelResult> results = new ArrayList<>();
        for (ContentModel contentModel : contentModels) {
            ContentModelResult result;
            if ( contentModel.isTagModel())
                result = makeContentModel(request, contentModel.getCode(), contentModel);
            else {
                Fortress fortress = fortressService.registerFortress(company, contentModel.getFortress());
                conceptService.save(new DocumentType(fortress.getDefaultSegment(), contentModel.getDocumentType()));
                result = makeContentModel(request, contentModel.getFortress().getName(), contentModel.getDocumentType().getName(), contentModel);
            }
            results.add(result);
        }
        return results;
    }


    @RequestMapping(value = "/{key}",
            produces = "application/json",
            method = RequestMethod.DELETE)
    public void deleteModelKey(HttpServletRequest request, @PathVariable("key") String key) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        contentModelService.delete(company, key);
    }

    @RequestMapping(value = "/{key}",
            produces = "application/json",
            method = RequestMethod.GET)
    public ContentModelResult getModelKey(HttpServletRequest request, @PathVariable("key") String key) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return contentModelService.find(company,key);
    }


    @RequestMapping(value = "/{fortressCode}/{docTypeName}",
            produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ContentModel getContentModel(
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

        return contentModelService.get(company, fortress, documentType);

    }

    @RequestMapping(value = "/tag/{code}",
            produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public ContentModel getContentModel(
            HttpServletRequest request,
            @PathVariable("code") String code) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);


        return contentModelService.getTagModel(company, code);

    }

    @RequestMapping(value = "/{fortressCode}/{docTypeName}",
                    produces = "application/json",
                    consumes = "application/json",
                    method = RequestMethod.POST)
    public ContentModelResult makeContentModel(HttpServletRequest request,
                                               @PathVariable("fortressCode") String fortressCode,
                                               @PathVariable("docTypeName") String docTypeName,
                                               @RequestBody ContentModel contentModel) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        Fortress fortress = fortressService.getFortress(company, fortressCode);
        if ( fortress == null )
            throw new IllegalArgumentException("Unable to locate the fortress " + fortressCode);

        DocumentType documentType = conceptService.resolveByDocCode(fortress, docTypeName, Boolean.FALSE);
        if ( documentType == null )
            throw new IllegalArgumentException("Unable to locate the document " + docTypeName);
        return contentModelService.saveEntityModel(company, fortress, documentType, contentModel);

    }

    @RequestMapping(value = "/tag/{code}",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.POST)
    public ContentModelResult makeContentModel(HttpServletRequest request,
                                               @PathVariable("code") String code,
                                               @RequestBody ContentModel contentModel) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        if (code== null || code.equals(""))
            throw new IllegalArgumentException("No key code was provided for the model");


        return contentModelService.saveTagModel(company, code, contentModel);

    }


    @RequestMapping(value = "/validate",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.POST)
    public ContentValidationResults validateContent (HttpServletRequest request,
                                                     @RequestBody ContentValidationRequest contentRequest) throws FlockException {
        CompanyResolver.resolveCompany(request);

        return contentModelService.validate(contentRequest);

    }

    @RequestMapping(value = "/default",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.POST)
    public ContentModel defaultContentModel(HttpServletRequest request,
                                            @RequestBody ContentValidationRequest contentRequest)
            throws FlockException {
        CompanyResolver.resolveCompany(request);
        return  contentModelService.createDefaultContentModel(contentRequest);
    }


}
