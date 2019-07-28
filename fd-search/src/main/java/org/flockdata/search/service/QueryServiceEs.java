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

package org.flockdata.search.service;

import java.io.IOException;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.search.EntityKeyResults;
import org.flockdata.search.EsSearchRequestResult;
import org.flockdata.search.QueryParams;
import org.flockdata.search.TagCloud;
import org.flockdata.search.TagCloudParams;
import org.flockdata.search.base.QueryService;
import org.flockdata.search.dao.QueryDaoES;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author mholdsworth
 * @tag Query, Search, ElasticSearch
 * @since 18/06/2013
 */
@Service("queryServiceEs")
public class QueryServiceEs implements QueryService {

  private final QueryDaoES queryDao;

  @Autowired
  public QueryServiceEs(QueryDaoES queryDao) {
    this.queryDao = queryDao;
  }

  @Override
  public TagCloud getTagCloud(TagCloudParams tagCloudParams) throws NotFoundException, FlockException {
    return queryDao.getCloudTag(tagCloudParams);
  }

  @Override
  public Long getHitCount(String index) throws IOException {
    return queryDao.doHitCountQuery(index);
  }

  @Override
  public EsSearchRequestResult doFdViewSearch(QueryParams queryParams) throws FlockException {
    return queryDao.doFdViewSearch(queryParams);
  }

  public EntityKeyResults doKeyQuery(QueryParams queryParams) throws FlockException {
    return queryDao.doEntityKeySearch(queryParams);
  }

  @Override
  public EsSearchRequestResult doParametrizedQuery(QueryParams queryParams) throws FlockException {
    return queryDao.doParametrizedQuery(queryParams);
  }

  @Override
  public String doSearch(QueryParams queryParams) throws FlockException {
    return queryDao.doSearch(queryParams);
  }

  @Override
  public void getTags(String indexName) {
    queryDao.getTags(indexName);
  }

}
