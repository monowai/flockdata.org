package com.auditbucket.search.service;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;

/**
 * User: mike
 * Date: 8/09/14
 * Time: 10:55 AM
 */
public interface QueryService {
    Long getHitCount(String index);

    EsSearchResult metaKeySearch(QueryParams queryParams) throws DatagioException;

    String doSearch(QueryParams queryParams) throws DatagioException;
}
