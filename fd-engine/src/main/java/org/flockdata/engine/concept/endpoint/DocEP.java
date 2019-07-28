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

package org.flockdata.engine.concept.endpoint;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.flockdata.data.Fortress;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.track.bean.DocumentResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mholdsworth
 * @tag Endpoint, Concept, DocumentType
 * @since 20/05/2015
 */

@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/doc")
public class DocEP {

  private final ConceptService conceptService;
  private final FortressService fortressService;

  @Autowired
  public DocEP(ConceptService conceptService, FortressService fortressService) {
    this.conceptService = conceptService;
    this.fortressService = fortressService;
  }

  @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
  public Collection<DocumentResultBean> getDocumentsInUse(@RequestBody(required = false) Collection<String> fortresses, HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    return conceptService.getDocumentsInUse(company, fortresses);
  }

  @RequestMapping(value = "/{fortress}", method = RequestMethod.GET)
  public Collection<DocumentResultBean> getFortressDocs(HttpServletRequest request, @PathVariable("fortress") String fortress) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    return conceptService.getDocumentsInUse(company, fortress);
  }

  @RequestMapping(value = "/{fortress}/{docType}", method = RequestMethod.GET)
  public DocumentResultBean getDocument(HttpServletRequest request, @PathVariable("fortress") String fortress, @PathVariable("docType") String docType) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    Fortress f = fortressService.findByName(company, fortress);
    return new DocumentResultBean(conceptService.findDocumentType(f, docType), f);
  }


}
