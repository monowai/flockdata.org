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

package org.flockdata.engine.query.service;

import org.flockdata.authentication.FdRoles;
import org.flockdata.engine.integration.EsStoreRequest;
import org.flockdata.engine.integration.FdMetaKeyQuery;
import org.flockdata.engine.integration.FdViewQuery;
import org.flockdata.engine.integration.TagCloudRequest;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.search.model.*;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.service.EntityTagService;
import org.flockdata.track.service.FortressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Query parameter support functionality
 * Centralises methods that will support options to use on MatrixService etc.
 * <p/>
 * User: mike
 * Date: 14/06/14
 * Time: 9:43 AM
 */
@Service
@PreAuthorize(FdRoles.EXP_EITHER)
public class QueryService {

    private Logger logger = LoggerFactory.getLogger(QueryService.class);

    @Autowired
    FortressService fortressService;

    @Autowired
    EntityTagService tagService;

    @Autowired
    ConceptService conceptService;

    @Autowired
    TagCloudRequest.TagCloudGateway tagCloudGateway;

    @Autowired
    FdViewQuery.FdViewGateway fdViewQuery;

    @Autowired
    EsStoreRequest.ContentStoreEs esStore;

    public Collection<DocumentResultBean> getDocumentsInUse(Company abCompany, Collection<String> fortresses) throws FlockException {
        ArrayList<DocumentResultBean> docs = new ArrayList<>();

        // ToDo: Optimize via Cypher, not a java loop
        //match (f:Fortress) -[:FORTRESS_DOC]-(d) return f,d
        if (fortresses == null) {
            Collection<FortressResultBean> forts = fortressService.findFortresses(abCompany);
            for (FortressResultBean fort : forts) {
                docs.addAll(fortressService.getFortressDocumentsInUse(abCompany, fort.getName()));
            }

        } else {
            for (String fortress : fortresses) {
                Collection<DocumentResultBean> documentTypes = fortressService.getFortressDocumentsInUse(abCompany, fortress);
                docs.addAll(documentTypes);
            }
        }
        return docs;

    }

    public Set<DocumentResultBean> getConceptsWithRelationships(Company company, Collection<String> documents) {
        return conceptService.findConcepts(company, documents, true);

    }

    public Set<DocumentResultBean> getConcepts(Company company, Collection<String> documents, boolean withRelationships) {
        return conceptService.findConcepts(company, documents, withRelationships);

    }

    public Set<DocumentResultBean> getConcepts(Company abCompany, Collection<String> documents) {
        //match (a:Orthopedic) -[r]-(:_Tag) return distinct type(r) as typeName  order by typeName;

        return getConcepts(abCompany, documents, false);
    }

    public EsSearchResult search(Company company, QueryParams queryParams) {

//        StopWatch watch = new StopWatch(queryParams.toString());
//        watch.start("Get ES Query Results");
        queryParams.setCompany(company.getName());
        EsSearchResult esSearchResult;

        // ToDo: FixMe fdstore refactor
        if ( queryParams.getQuery() !=null ) {
            esSearchResult = esStore.getData(queryParams);
        } else {
            esSearchResult = fdViewQuery.fdSearch(queryParams);
        }


        //watch.stop();
        //logger.info("Hit Count {}, Results {}",esSearchResult.getTotalHits(), (esSearchResult.getResults() == null ? 0 : esSearchResult.getResults().size()));
        //logger.info(watch.prettyPrint());

        return esSearchResult;

    }

    public TagCloud getTagCloud(Company company, TagCloudParams tagCloudParams) throws NotFoundException {
        Fortress fortress = fortressService.findByCode(company, tagCloudParams.getFortress());
        if (fortress == null)
            throw new NotFoundException("Fortress [" + tagCloudParams.getFortress() + "] does not exist");
        tagCloudParams.setFortress(fortress.getCode());
        tagCloudParams.setCompany(company.getCode());
        return tagCloudGateway.getTagCloud(tagCloudParams);
    }

    @Autowired
    FdMetaKeyQuery.FdMetaKeyGateway mkGateway;

    public MetaKeyResults getMetaKeys(Company company, QueryParams queryParams) {
        queryParams.setCompany(company.getName());
        return mkGateway.metaKeys(queryParams);
    }
}
