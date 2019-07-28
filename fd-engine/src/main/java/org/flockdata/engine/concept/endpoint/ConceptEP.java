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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.track.bean.ConceptResultBean;
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
 * @since 26/04/2016
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/concept")
public class ConceptEP {

  private final ConceptService conceptService;

  @Autowired
  public ConceptEP(ConceptService conceptService) {
    this.conceptService = conceptService;
  }

  @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
  public Set<DocumentResultBean> getConceptsAllDocs(@RequestBody(required = false) Collection<String> documents, HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);

    return conceptService.findConcepts(company, documents, false);
  }


  @RequestMapping(value = "/{fortress}", method = RequestMethod.GET, consumes = "application/json", produces = "application/json")
  public Set<DocumentResultBean> getConceptsForFortress(HttpServletRequest request, @PathVariable("fortress") String fortress) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    Collection<String> fortresses = new ArrayList<>();
    fortresses.add(fortress);
    Collection<DocumentResultBean> docs = conceptService.getDocumentsInUse(company, fortresses);
    Collection<String> documents = new ArrayList<>();

    for (DocumentResultBean doc : docs) {
      documents.add(doc.getName());
    }

    return conceptService.findConcepts(company, documents, false);
  }

  @RequestMapping(value = "/{fortress}/structure", method = RequestMethod.GET)
  public MatrixResults getConceptStructure
      (@PathVariable("fortress") String fortress, HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    return conceptService.getContentStructure(company, fortress);
  }


  @RequestMapping(value = "/relationships", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
  public Set<DocumentResultBean> getRelationships(@RequestBody(required = false) Collection<String> documents, HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    // Todo: DAT-100 Sherry's comment. Should be Concepts, not Doc Types
    return conceptService.findConcepts(company, documents, true);
  }

  @RequestMapping(value = "/{docType}/values", method = RequestMethod.GET)
  public Collection<ConceptResultBean> getConceptsFOrDocument(HttpServletRequest request, @PathVariable("docType") String docType) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    Collection<String> docNames = new ArrayList<>();
    docNames.add(docType);
    Set<DocumentResultBean> results = conceptService.findConcepts(company, docNames, true);
    for (DocumentResultBean result : results) {
      return result.getConcepts();
    }
    return new ArrayList<>();
  }

}
