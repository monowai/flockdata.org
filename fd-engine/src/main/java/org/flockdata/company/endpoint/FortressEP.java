/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.company.endpoint;

import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.model.FortressSegment;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.service.FortressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/fortress")
public class FortressEP {

    @Autowired
    CompanyService companyService;

    @Autowired
    ConceptService conceptService;

    @Autowired
    FortressService fortressService;

    @Autowired
    SecurityHelper securityHelper;

    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.GET)
    public Collection<FortressResultBean> findFortresses(HttpServletRequest request) throws FlockException {
        // curl -u mike:123 -X GET  http://localhost:8080/fd/fortresses
        Company company = CompanyResolver.resolveCompany(request);
        return fortressService.findFortresses(company);
    }

    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public FortressResultBean registerFortress( @RequestBody FortressInputBean fortressInputBean, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Fortress fortress = fortressService.registerFortress(company, fortressInputBean, true);
        return new FortressResultBean(fortress);

    }

    @RequestMapping(value = "/{fortressCode}/{docTypeName}", produces = "application/json", consumes = "application/json", method = RequestMethod.PUT)
    public DocumentResultBean registerDocumentType (HttpServletRequest request, @PathVariable("fortressCode") String fortressCode, @PathVariable("docTypeName") String docTypeName) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Fortress fortress = fortressService.getFortress(company, fortressCode);
        return new DocumentResultBean(conceptService.resolveByDocCode(fortress, docTypeName, Boolean.TRUE));

    }

    @RequestMapping(value = "/{fortressCode}/docs", method = RequestMethod.POST, consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public Collection<DocumentResultBean> registerDocumentType(@RequestBody List<DocumentTypeInputBean> docTypes, @PathVariable("fortressCode") String fortressCode, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Fortress fortress = fortressService.getFortress(company, fortressCode);
        if ( fortress == null  )
            throw new IllegalArgumentException("Unable to locate the fortress ");
        Collection<DocumentResultBean>results = new ArrayList<>();
        for (DocumentTypeInputBean docType : docTypes) {
            DocumentType result = conceptService.findOrCreate(fortress, new DocumentType(fortress, docType));
            results.add(new DocumentResultBean(result));
        }

        return  results;
    }

    @RequestMapping(value = "/{code}", method = RequestMethod.GET)
    public FortressResultBean getFortress(@PathVariable("code") String fortressCode, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if ( fortress == null)
            fortress = fortressService.findByCode(company, fortressCode);

        if (fortress == null)
           throw new FlockException("Unable to locate the fortress "+ fortressCode);

        return new FortressResultBean(fortress);
    }

    @RequestMapping(value = "/{code}", method = RequestMethod.DELETE)
    public String delete(@PathVariable("code") String fortressCode, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return fortressService.delete(company, fortressCode);
    }

    @RequestMapping(value = "/{code}/docs", method = RequestMethod.GET)
    public Collection<DocumentResultBean> getDocumentTypes(@PathVariable("code") String code, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return  fortressService.getFortressDocumentsInUse(company, code);
    }

    @RequestMapping(value = "/{code}/segments", method = RequestMethod.GET)
    public Collection<FortressSegment> getFortressSegments(@PathVariable("code") String code, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Fortress f = fortressService.findByCode(company, code);
        return  fortressService.getSegments(f);
    }

    @RequestMapping(value = "/timezones", method = RequestMethod.GET)
    public String[] getTimezones(HttpServletRequest request) throws FlockException {
        CompanyResolver.resolveCompany(request);
        return TimeZone.getAvailableIDs();
    }

}