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

package org.flockdata.engine.query.service;

import org.flockdata.authentication.FdRoles;
import org.flockdata.data.Company;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.integration.search.EntityKeyQuery.EntityKeyGateway;
import org.flockdata.engine.integration.search.FdViewQuery.FdViewQueryGateway;
import org.flockdata.engine.integration.search.TagCloudRequest.TagCloudGateway;
import org.flockdata.engine.integration.store.EsStoreRequest.ContentStoreEs;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.helper.FlockServiceException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.integration.IndexManager;
import org.flockdata.search.EntityKeyResults;
import org.flockdata.search.EsSearchRequestResult;
import org.flockdata.search.QueryParams;
import org.flockdata.search.TagCloud;
import org.flockdata.search.TagCloudParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

/**
 * Query parameter support functionality
 * Centralises methods that will support options to use on MatrixService etc.
 *
 * @author mholdsworth
 * @tag Query
 * @since 14/06/2014
 */
@Service
@PreAuthorize(FdRoles.EXP_EITHER)
public class QueryService {

  private final FortressService fortressService;
  private final IndexManager indexManager;
  private Logger logger = LoggerFactory.getLogger(QueryService.class);
  private TagCloudGateway tagCloudGateway;
  private EntityKeyGateway entityKeyGateway;

  private ContentStoreEs esStore;

  private FdViewQueryGateway fdViewQueryGateway;

  @Autowired
  public QueryService(IndexManager indexManager, FortressService fortressService) {
    this.indexManager = indexManager;
    this.fortressService = fortressService;
  }

  @Autowired(required = false)
// Functional tests don't require gateways
  void setTagCloudGateway(TagCloudGateway tagCloudGateway) {
    this.tagCloudGateway = tagCloudGateway;
  }

  @Autowired(required = false)
    // Functional tests don't require gateways
  void setEntityKeyGateway(EntityKeyGateway entityKeyGateway) {
    this.entityKeyGateway = entityKeyGateway;
  }

  @Autowired(required = false)
  void setFdViewQueryGateway(FdViewQueryGateway fdViewQueryGateway) {
    this.fdViewQueryGateway = fdViewQueryGateway;
  }

  @Autowired(required = false)
  void setContentStoreEs(ContentStoreEs contentStoreEs) {
    this.esStore = contentStoreEs;
  }

  public EsSearchRequestResult search(Company company, QueryParams queryParams) {

    queryParams.setCompany(company.getName());
    EsSearchRequestResult esSearchRequestResult;
    if (queryParams.isSearchTagsOnly()) {
      // Set the index
      queryParams.setIndex(indexManager.toIndex(queryParams));
    }

    if (queryParams.isMatchAll() || queryParams.isSearchTagsOnly() || queryParams.getQuery() != null || queryParams.getAggs() != null) {
      esSearchRequestResult = esStore.getData(queryParams);
    } else {
      if (fdViewQueryGateway == null) {
        logger.info("fdViewQueryGateway is not available");
        return null;
      } else {
        try {
          esSearchRequestResult = fdViewQueryGateway.fdSearch(queryParams);
        } catch (ResourceAccessException e) {
          throw new FlockServiceException("The search service is not currently available");
        }
      }
    }

    return esSearchRequestResult;

  }

  public TagCloud getTagCloud(Company company, TagCloudParams tagCloudParams) throws NotFoundException {
    FortressNode fortress = fortressService.findByCode(company, tagCloudParams.getFortress());
    if (fortress == null) {
      throw new NotFoundException("Fortress [" + tagCloudParams.getFortress() + "] does not exist");
    }
    tagCloudParams.setFortress(fortress.getCode());
    tagCloudParams.setCompany(company.getCode());
    return tagCloudGateway.getTagCloud(tagCloudParams);
  }

  public EntityKeyResults getKeys(Company company, QueryParams queryParams) {
    queryParams.setCompany(company.getName());
    return entityKeyGateway.keys(queryParams);
  }
}
