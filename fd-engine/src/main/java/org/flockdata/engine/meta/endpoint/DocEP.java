/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.engine.meta.endpoint;

import org.flockdata.engine.query.service.QueryService;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.track.bean.ConceptResultBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Created by mike on 20/05/15.
 */

@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/doc")
public class DocEP {

    @Autowired
    QueryService queryService;

    @Autowired
    ConceptService conceptService;

    @RequestMapping(value = "/{fortress}", method = RequestMethod.GET)
    public Collection<DocumentResultBean> getFortressDocs(HttpServletRequest request, @PathVariable("fortress") String fortress) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Collection<String>fortresses = new ArrayList<>();
        fortresses.add(fortress)   ;
        return queryService.getDocumentsInUse(company, fortresses);
    }

    @RequestMapping(value = "/{fortress}/{docType}", method = RequestMethod.GET)
    public Collection<ConceptResultBean> getDocsLabels(HttpServletRequest request, @PathVariable("fortress") String fortress, @PathVariable("docType") String docType) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Collection<String>docNames = new ArrayList<>();
        docNames.add(docType);
        Set<DocumentResultBean> results = conceptService.findConcepts(company, docNames, true);
        for (DocumentResultBean result : results) {
            return result.getConcepts();
        }
        return new ArrayList<>();
    }


}
