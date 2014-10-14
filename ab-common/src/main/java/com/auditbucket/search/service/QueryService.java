package com.auditbucket.search.service;

import com.auditbucket.helper.FlockException;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.search.model.TagCloud;
import com.auditbucket.search.model.TagCloudParams;

/**
 * User: mike
 * Date: 8/09/14
 * Time: 10:55 AM
 */
public interface QueryService {
    TagCloud getTagCloud(TagCloudParams tagCloudParams);

    Long getHitCount(String index);

    EsSearchResult metaKeySearch(QueryParams queryParams) throws FlockException;

    String doSearch(QueryParams queryParams) throws FlockException;
}
