/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

import org.flockdata.engine.concept.service.TxService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.model.EntityLog;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.track.bean.*;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.service.EntityService;
import org.flockdata.track.service.MediationFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Write operations for Entities and Tags
 *
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@RestController
@RequestMapping("/track")
public class TrackEP {
    @Autowired
    EntityService entityService;

    @Autowired
    MediationFacade mediationFacade;

    @Autowired
    CompanyService companyService;

    @Autowired
    TxService txService;

    //private static Logger logger = LoggerFactory.getLogger(TrackEP.class);


    @RequestMapping(value = "/", consumes = "application/json", method = RequestMethod.PUT)
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public void trackEntities(@RequestBody List<EntityInputBean> inputBeans,
                              HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
        //Company company = CompanyResolver.resolveCompany(request);

        mediationFacade.trackEntities(inputBeans, CompanyResolver.resolveCallerApiKey(request));
    }

    /**
     * Tracks an entity
     *
     * @param input Entity input
     * @return TrackResultBean
     * @throws org.flockdata.helper.FlockException
     */
    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    public
    ResponseEntity<TrackResultBean> trackEntity(@RequestBody EntityInputBean input,
                                                HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        TrackResultBean trackResultBean;
        trackResultBean = mediationFacade.trackEntity(company, input);

        if ( trackResultBean.entityExists())
            if ( trackResultBean.getCurrentLog()!= null && trackResultBean.isLogIgnored())
                return new ResponseEntity<>(trackResultBean, HttpStatus.NOT_MODIFIED);

        trackResultBean.addServiceMessage("OK");
        return new ResponseEntity<>(trackResultBean, HttpStatus.CREATED);

    }


    @RequestMapping(value = "/log/", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    public ResponseEntity<EntityLog> trackLog(@RequestBody ContentInputBean input ,
                                              HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);

        TrackResultBean resultBean = mediationFacade.trackLog(company, input);
        ContentInputBean.LogStatus ls = resultBean.getLogStatus();
        if (ls.equals(ContentInputBean.LogStatus.FORBIDDEN))
            return new ResponseEntity<>(resultBean.getCurrentLog(), HttpStatus.FORBIDDEN);
        else if (ls.equals(ContentInputBean.LogStatus.NOT_FOUND)) {
            throw new NotFoundException("Unable to locate the requested metaKey");
        } else if (ls.equals(ContentInputBean.LogStatus.IGNORE)) {
            input.setFdMessage("Ignoring request to change as the 'what' has not changed");
            return new ResponseEntity<>(resultBean.getCurrentLog(), HttpStatus.NOT_MODIFIED);
        } else if (ls.equals(ContentInputBean.LogStatus.ILLEGAL_ARGUMENT)) {
            return new ResponseEntity<>(resultBean.getCurrentLog(), HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(resultBean.getCurrentLog(), HttpStatus.CREATED);
    }


    @RequestMapping(value = "/{fortress}/{recordType}/{callerRef}", produces = "application/json", method = RequestMethod.PUT)
    public ResponseEntity<TrackResultBean> trackByClientRef(@RequestBody EntityInputBean input,
                                                            @PathVariable("fortress") String fortress,
                                                            @PathVariable("recordType") String recordType,
                                                            @PathVariable("callerRef") String callerRef ,
                                                            HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        TrackResultBean trackResultBean;
        input.setFortress(fortress);
        input.setDocumentName(recordType);
        input.setCallerRef(callerRef);
        input.setMetaKey(null);
        trackResultBean = mediationFacade.trackEntity(company, input);
        trackResultBean.addServiceMessage("OK");
        return new ResponseEntity<>(trackResultBean, HttpStatus.OK);

    }

    @RequestMapping(value = "/{metaKey}/{xRefName}/xref", produces = "application/json", method = RequestMethod.POST)
    public @ResponseBody Collection<String> crossReference(@PathVariable("metaKey") String metaKey, Collection<String> metaKeys, @PathVariable("xRefName") String relationshipName,
                                                           HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return entityService.crossReference(company, metaKey, metaKeys, relationshipName);
    }

    /**
     * Looks across all document types for the caller ref within the fortress. If the callerRef is not unique or does not
     * exist then an exception is thown.
     *
     * @param fortressName application
     * @param callerRef    source
     * @param entities   targets
     * @param xRefName     name of the cross reference
     * @return unresolvable caller references
     * @throws org.flockdata.helper.FlockException if not exactly one Entity for the callerRef in the fortress
     */
    @RequestMapping(value = "/{fortress}/all/{callerRef}/{xRefName}/xref", produces = "application/json", method = RequestMethod.POST)
    public @ResponseBody  List<EntityKeyBean> crossReferenceEntity(@PathVariable("fortress") String fortressName,
                                                                   @PathVariable("callerRef") String callerRef,
                                                                   @RequestBody Collection<EntityKeyBean> entities,
                                                                   @PathVariable("xRefName") String xRefName,
                                                                   HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return entityService.crossReferenceEntities(company, new EntityKeyBean(fortressName, "*", callerRef), entities, xRefName);
    }


    @RequestMapping(value = "/xref", produces = "application/json", method = RequestMethod.POST)
    public @ResponseBody List<CrossReferenceInputBean> crossReferenceEntities(@RequestBody List<CrossReferenceInputBean> crossReferenceInputBeans,
                                                                              HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);

        return entityService.crossReferenceEntities(company, crossReferenceInputBeans);
        //return crossReferenceInputBeans;
    }



}