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

package org.flockdata.company.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TimeZone;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mholdsworth
 * @tag EndPoint, Fortress
 * @since 4/05/2013
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/fortress")
public class FortressEP {

  private final ConceptService conceptService;

  private final FortressService fortressService;

  @Autowired
  public FortressEP(ConceptService conceptService, FortressService fortressService) {
    this.conceptService = conceptService;
    this.fortressService = fortressService;
  }

  @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.GET)
  public Collection<FortressResultBean> findFortresses(HttpServletRequest request) throws FlockException {
    // curl -u mike:123 -X GET  http://localhost:8080/fd/fortresses
    CompanyNode company = CompanyResolver.resolveCompany(request);
    return fortressService.findFortresses(company);
  }

  @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  public FortressResultBean registerFortress(@RequestBody FortressInputBean fortressInputBean, HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    FortressNode fortress = fortressService.registerFortress(company, fortressInputBean, true);
    return new FortressResultBean(fortress);

  }

  @RequestMapping(value = "/{code}", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
  public FortressResultBean updateFortress(@RequestBody FortressInputBean fortressInputBean, HttpServletRequest request, @PathVariable("code") String code) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    FortressNode existing = fortressService.findByCode(company, code);
    if (existing == null) {
      throw new NotFoundException("Unable to locate the fortress with the code " + code);
    }

    return new FortressResultBean(fortressService.updateFortress(company, existing, fortressInputBean));

  }

  @RequestMapping(value = "/{fortressName}/{docTypeName}", produces = "application/json", consumes = "application/json", method = RequestMethod.PUT)
  public DocumentResultBean registerDocumentType(HttpServletRequest request, @PathVariable("fortressName") String fortressName, @PathVariable("docTypeName") String docTypeName) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    Fortress fortress = fortressService.getFortress(company, fortressName);
    return new DocumentResultBean(conceptService.resolveByDocCode(fortress, docTypeName, Boolean.TRUE), fortress);

  }

  @RequestMapping(value = "/{fortressName}/doc", method = RequestMethod.POST, consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public DocumentResultBean registerDocumentType(@RequestBody DocumentTypeInputBean docType, @PathVariable("fortressName") String fortressName, HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    Fortress fortress = fortressService.getFortress(company, fortressName);
    if (fortress == null) {
      throw new NotFoundException("Unable to locate the fortress ");
    }
    return new DocumentResultBean(conceptService.findOrCreate(fortress, new DocumentNode(fortress.getDefaultSegment(), docType)), fortress);

  }

  @RequestMapping(value = "/{code:.*}", method = RequestMethod.GET)
  public FortressResultBean getFortress(@PathVariable("code") String fortressCode, HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    FortressNode fortress = fortressService.findByCode(company, fortressCode);
    if (fortress == null) {
      fortress = fortressService.findByCode(company, fortressCode);
    }

    if (fortress == null) {
      throw new FlockException("Unable to locate the fortress " + fortressCode);
    }

    return new FortressResultBean(fortress);
  }

  @RequestMapping(value = "/{code:.*}", method = RequestMethod.DELETE)
  public String delete(@PathVariable("code") String fortressCode, HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    return fortressService.delete(company, fortressCode);
  }

  @RequestMapping(value = "/{code:.*}/docs", method = RequestMethod.GET)
  public Collection<DocumentResultBean> getDocumentTypes(@PathVariable("code") String code, HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    return fortressService.getFortressDocumentsInUse(company, code);
  }

  @RequestMapping(value = "/{fortress:.*}/segments", method = RequestMethod.GET)
  public Collection<Segment> getFortressSegments(@PathVariable("fortress") String code, HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    FortressNode f = fortressService.findByCode(company, code);
    return fortressService.getSegments(f);
  }

  /**
   * Fortresses store data in segments. This call returns known segments for the fortress
   *
   * @param code    fortress name or code
   * @param doc     doc type to filter by or * for all
   * @param request internal use
   * @return Collection of DocumentResultBeans with Segment data
   * @throws FlockException business exception, i.e. fortress does not exist
   */
  @RequestMapping(value = "/{fortress:.*}/{doc}/segments", method = RequestMethod.GET)
  public Collection<DocumentResultBean> getFortressDocSegments(
      @PathVariable("fortress") String code,
      @PathVariable("doc") String doc,
      HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    FortressNode f = fortressService.findByCode(company, code);
    Collection<DocumentResultBean> results = new ArrayList<>();
    if (doc.equals("*")) {
      // All docs for the fortress
      Collection<DocumentResultBean> fortressDcouments = fortressService.getFortressDocumentsInUse(company, code);
      results.addAll(fortressDcouments.stream().map(fortressDoc
          -> conceptService.findDocumentTypeWithSegments(f, fortressDoc.getName())
      ).collect(Collectors.toList()));

    } else {
      results.add(conceptService.findDocumentTypeWithSegments(f, doc));
    }

    return results;
  }

  /**
   * @param request used to resolve the company the logged in user belongs to
   * @return Available timezones
   */
  @RequestMapping(value = "/timezones", method = RequestMethod.GET)
  public String[] getTimezones(HttpServletRequest request) {
    CompanyResolver.resolveCompany(request);
    return TimeZone.getAvailableIDs();
  }

  @RequestMapping(value = "/defaults", method = RequestMethod.GET)
  public FortressInputBean getDefaultFib() {
    return fortressService.createDefaultFortressInput();
  }

}