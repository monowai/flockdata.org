/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.engine.track.endpoint;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.servlet.http.HttpServletRequest;
import org.flockdata.data.EntityLog;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.tag.MediationFacade;
import org.flockdata.engine.track.service.EntityService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.EntityToEntityLinkInput;
import org.flockdata.track.bean.TrackRequestResult;
import org.flockdata.track.bean.TrackResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Write operations for Entities and Tags
 *
 * @author mholdsworth
 * @tag Track, Endpoint
 * @since 4/05/2013
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/track")
public class TrackEP {
  private final EntityService entityService;

  private final MediationFacade mediationFacade;

  @Autowired
  public TrackEP(EntityService entityService, MediationFacade mediationFacade) {
    this.entityService = entityService;
    this.mediationFacade = mediationFacade;
  }


  @RequestMapping(value = "/", consumes = "application/json", produces = "application/json", method = RequestMethod.PUT)
  public Collection<TrackRequestResult> trackEntities(@RequestBody List<EntityInputBean> inputBeans,
                                                      HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
    CompanyNode company = CompanyResolver.resolveCompany(request);

    return mediationFacade.trackEntities(company, inputBeans);
  }

  /**
   * Tracks an entity
   *
   * @param input   Entity input
   * @param request resolves authorised company to work with
   * @return TrackResultBean
   * @throws InterruptedException server shutting down
   * @throws FlockException       data errors
   * @throws ExecutionException   bad stuff
   */
  @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
  public ResponseEntity<TrackRequestResult> trackEntity(@RequestBody EntityInputBean input,
                                                        HttpServletRequest request) throws InterruptedException, FlockException, ExecutionException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    TrackResultBean trackResultBean;
    trackResultBean = mediationFacade.trackEntity(company, input);

    if (trackResultBean.entityExists()) {
      if (trackResultBean.getCurrentLog() != null && trackResultBean.isLogIgnored()) {
        return new ResponseEntity<>(new TrackRequestResult(trackResultBean), HttpStatus.NOT_MODIFIED);
      }
    }

    trackResultBean.addServiceMessage("OK");
    return new ResponseEntity<>(new TrackRequestResult(trackResultBean), HttpStatus.CREATED);

  }


  @RequestMapping(value = "/log", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
  public ResponseEntity<EntityLog> trackLog(@RequestBody ContentInputBean input,
                                            HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
    CompanyNode company = CompanyResolver.resolveCompany(request);

    TrackResultBean resultBean = mediationFacade.trackLog(company, input);
    ContentInputBean.LogStatus ls = resultBean.getLogStatus();
    if (ls.equals(ContentInputBean.LogStatus.FORBIDDEN)) {
      return new ResponseEntity<>(resultBean.getCurrentLog(), HttpStatus.FORBIDDEN);
    } else if (ls.equals(ContentInputBean.LogStatus.NOT_FOUND)) {
      throw new NotFoundException("Unable to locate the requested key");
    } else if (ls.equals(ContentInputBean.LogStatus.IGNORE)) {
      input.setFdMessage("Ignoring request to change as the 'data' has not changed");
      return new ResponseEntity<>(resultBean.getCurrentLog(), HttpStatus.NOT_MODIFIED);
    } else if (ls.equals(ContentInputBean.LogStatus.ILLEGAL_ARGUMENT)) {
      return new ResponseEntity<>(resultBean.getCurrentLog(), HttpStatus.NO_CONTENT);
    }

    return new ResponseEntity<>(resultBean.getCurrentLog(), HttpStatus.CREATED);
  }


  @RequestMapping(value = "/{fortress}/{recordType}/{code}", produces = "application/json", method = RequestMethod.PUT)
  public ResponseEntity<TrackRequestResult> trackByClientRef(@RequestBody EntityInputBean input,
                                                             @PathVariable("fortress") String fortress,
                                                             @PathVariable("recordType") String recordType,
                                                             @PathVariable("code") String code,
                                                             HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    TrackResultBean trackResultBean;
    input.setFortress(new FortressInputBean(fortress));
    input.setDocumentType(new DocumentTypeInputBean(recordType));
    input.setCode(code);
    input.setKey(null);
    trackResultBean = mediationFacade.trackEntity(company, input);
    trackResultBean.addServiceMessage("OK");
    return new ResponseEntity<>(new TrackRequestResult(trackResultBean), HttpStatus.OK);

  }

  @RequestMapping(value = "/{key}/{xRefName}/link", produces = "application/json", method = RequestMethod.POST)
  public @ResponseBody
  Collection<String> crossReference(@PathVariable("key") String key, Collection<String> keys, @PathVariable("xRefName") String relationshipName,
                                    HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    return entityService.crossReference(company, key, keys, relationshipName);
  }

  /**
   * Looks across all document types for the caller ref within the fortress. If the code is not unique or does not
   * exist then an exception is thrown.
   *
   * @param fortressName application
   * @param code         source
   * @param entities     targets
   * @param xRefName     name of the cross reference
   * @param request      used to resolve the company the user is authorised to work with
   * @return unresolvable caller references
   * @throws org.flockdata.helper.FlockException if not exactly one Entity for the code in the fortress
   */
  @RequestMapping(value = "/{fortress}/all/{code}/{xRefName}/link", produces = "application/json", method = RequestMethod.POST)
  public @ResponseBody
  Collection<EntityKeyBean> crossReferenceEntity(@PathVariable("fortress") String fortressName,
                                                 @PathVariable("code") String code,
                                                 @RequestBody Collection<EntityKeyBean> entities,
                                                 @PathVariable("xRefName") String xRefName,
                                                 HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    return entityService.linkEntities(company, new EntityKeyBean("*", fortressName, code), entities, xRefName);
  }


  @RequestMapping(value = "/link", produces = "application/json", method = RequestMethod.POST)
  public @ResponseBody
  Collection<EntityToEntityLinkInput> linkEntities(@RequestBody List<EntityToEntityLinkInput> entityToEntityLinkInputs,
                                                   HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);

    return entityService.linkEntities(company, entityToEntityLinkInputs);
  }


}