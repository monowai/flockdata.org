/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.search.dao;

import com.auditbucket.dao.QueryDao;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.MetaSearchSchema;
import com.auditbucket.search.model.QueryParams;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * User: Mike Holdsworth
 * Date: 28/04/13
 * Time: 2:23 PM
 */
@Repository
public class QueryDaoES implements QueryDao {

    @Autowired
    private Client client;

    private Logger logger = LoggerFactory.getLogger(QueryDaoES.class);

    @Override
    public long getHitCount(String index) {
        SearchResponse response = client.prepareSearch(index)
                .execute()
                .actionGet();

        logger.debug("Searching index [{}] for hit counts", index);

        return response.getHits().getTotalHits();
    }

    @Override
    public String doSearch(QueryParams queryParams) throws DatagioException {
        SearchResponse result = client.prepareSearch(MetaSearchSchema.parseIndex(queryParams))
                .setExtraSource(getSimpleQuery(queryParams.getSimpleQuery()))
                .execute()
                .actionGet();

        //logger.debug("looking for {} in index {}", queryString, index);
        return result.toString();
    }

    @Override
    public EsSearchResult doMetaKeySearch(QueryParams queryParams) throws DatagioException {
        StopWatch watch = new StopWatch();
        EsSearchResult<Collection<String>> searchResult = new EsSearchResult<>();
        watch.start(queryParams.toString());
        String[] types = Strings.EMPTY_ARRAY;
        if (queryParams.getTypes() != null) {
            types = queryParams.getTypes();
        }
        ListenableActionFuture<SearchResponse> future = client.prepareSearch(MetaSearchSchema.parseIndex(queryParams))
                .setTypes(types)
                .addField(MetaSearchSchema.META_KEY)
                .setSize(queryParams.getRowsPerPage())
                .setFrom(queryParams.getStartFrom())
                .setExtraSource(getSimpleQuery(queryParams.getSimpleQuery()))
                .execute();

        Collection<String> results = new ArrayList<>();

        SearchResponse response;
        try {
            response = future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Search Exception processing query", e);
            // ToDo: No sensible error being returned to the caller
            return searchResult;
        }

        for (SearchHit searchHitFields : response.getHits().getHits()) {
            if ( !searchHitFields.getFields().isEmpty()) { // DAT-83
                Object hit = searchHitFields.getFields().get(MetaSearchSchema.META_KEY).getValue();
                if (hit != null)
                    results.add(hit.toString());
            }
        }
        searchResult.setTotalHits(response.getHits().getTotalHits());
        searchResult.setStartedFrom(queryParams.getStartFrom());
        searchResult.setResults(results);
        watch.stop();
        logger.info("ES Query. Results [{}] took [{}]", results.size(), watch.prettyPrint());
        return searchResult;
    }

    private String getSimpleQuery(String queryString) {
        logger.debug("getSimpleQuery {}", queryString);
        return "{\n" +
                "      \"query\": {\n" +
                "        \"bool\": {\n" +
                "          \"should\": [\n" +
                "            {\n" +
                "              \"query_string\": {\n" +
                "                \"query\": \""+queryString+"\"\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      }\n" +
                "  }";
    }


}
