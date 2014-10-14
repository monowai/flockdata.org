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
import com.auditbucket.helper.FlockException;
import com.auditbucket.search.helper.QueryGenerator;
import com.auditbucket.search.model.*;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.elasticsearch.search.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;

import java.util.*;
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

    @Value("${highlight.enabled:true}")
    Boolean highlightEnabled;

    private Logger logger = LoggerFactory.getLogger(QueryDaoES.class);

    @Override
    public TagCloud getCloudTag(TagCloudParams tagCloudParams) {
        // Getting all tag and What fields
        String index = EntitySearchSchema.parseIndex(tagCloudParams.getCompany(),tagCloudParams.getFortress());
        List<String> whatAndTagFields = new ArrayList<>();
        ImmutableMap<String, ImmutableMap<String, GetFieldMappingsResponse.FieldMappingMetaData>>
                mappings = client.admin()
                .indices()
                .prepareGetFieldMappings(index)
                .setTypes(tagCloudParams.getType())
                .setFields("@tag.*.code")
                .get().mappings().get(index);
        for (String s : mappings.keySet()) {
            ImmutableMap<String, GetFieldMappingsResponse.FieldMappingMetaData> var = mappings.get(s);
            //for (String key : var.keySet()) {
                for (String field: var.keySet()) {
                    whatAndTagFields.add(field);
                    //logger.info(field);
                }
        //    }
        }

//        ImmutableMap<String, GetFieldMappingsResponse.FieldMappingMetaData> responseFieldsMapping =
//                client.admin()
//                .indices()
//                .prepareGetFieldMappings(index)
//                .setTypes("*")
//                .setFields("@tag.*.code")
//                .get()
//                .mappings()
//                .get(index)
//                .get("*");
//        for (String what : responseFieldsMapping.keySet()) {
//            whatAndTagFields.add(what);
//        }

        //settings().prepareSearch(index).

        TermsFacetBuilder builder = FacetBuilders.termsFacet("tagcloud")
                .fields(whatAndTagFields.toArray(new String[whatAndTagFields.size()]))
                .size(100);

        SearchResponse response = client.prepareSearch(index)

                .addFacet(builder)
                .execute()
                .actionGet();
        TagCloud tagcloud = new TagCloud();
        TermsFacet tagCloudFacet = (TermsFacet) response.getFacets().getFacets().get("tagcloud");
        for (TermsFacet.Entry entry : tagCloudFacet.getEntries()) {
            tagcloud.addTerm(entry.getTerm().string(), entry.getCount());
        }
        return tagcloud;
    }

    @Override
    public long getHitCount(String index) {
        SearchResponse response = client.prepareSearch(index)
                .execute()
                .actionGet();

        logger.debug("Searching index [{}] for hit counts", index);

        return response.getHits().getTotalHits();
    }

    @Override
    public String doSearch(QueryParams queryParams) throws FlockException {
        SearchResponse result = client.prepareSearch(EntitySearchSchema.parseIndex(queryParams))
                .setExtraSource(QueryGenerator.getSimpleQuery(queryParams.getSimpleQuery(), false))
                .execute()
                .actionGet();

        //logger.debug("looking for {} in index {}", queryString, index);
        return result.toString();
    }

    @Override
    public EsSearchResult doEntitySearch(QueryParams queryParams) throws FlockException {
        StopWatch watch = new StopWatch();

        watch.start(queryParams.toString());
        String[] types = Strings.EMPTY_ARRAY;
        if (queryParams.getTypes() != null) {
            types = queryParams.getTypes();
        }
        SearchRequestBuilder query = client.prepareSearch(EntitySearchSchema.parseIndex(queryParams))
                .setTypes(types)
                .addField(EntitySearchSchema.META_KEY)
                .addField(EntitySearchSchema.FORTRESS)
                .addField(EntitySearchSchema.LAST_EVENT)
                .addField(EntitySearchSchema.DESCRIPTION)
                .addField(EntitySearchSchema.CALLER_REF)
                .addField(EntitySearchSchema.WHO)
                .addField(EntitySearchSchema.WHEN)
                .addField(EntitySearchSchema.CREATED)
                .addField(EntitySearchSchema.TIMESTAMP)
                .setSize(queryParams.getRowsPerPage())
                .setFrom(queryParams.getStartFrom())
                .setExtraSource(QueryGenerator.getSimpleQuery(queryParams.getSimpleQuery(), highlightEnabled));

        // Add user requested fields
        if (queryParams.getData() != null)
            query.addFields(queryParams.getData());

        ListenableActionFuture<SearchResponse> future = query.execute();
        Collection<SearchResult> results = new ArrayList<>();

        SearchResponse response;
        try {
            response = future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Search Exception processing query", e);
            // ToDo: No sensible error being returned to the caller
            return new EsSearchResult();
        }

        getEntityResults(results, response, queryParams);
        EsSearchResult searchResult = new EsSearchResult(results);
        searchResult.setTotalHits(response.getHits().getTotalHits());
        searchResult.setStartedFrom(queryParams.getStartFrom());
        watch.stop();
        logger.info("ES Query. Results [{}] took [{}]", results.size(), watch.prettyPrint());
        return searchResult;
    }

    private void getEntityResults(Collection<SearchResult> results, SearchResponse response, QueryParams queryParams) {
        for (SearchHit searchHitFields : response.getHits().getHits()) {
            if (!searchHitFields.getFields().isEmpty()) { // DAT-83
                // This function returns only information tracked by AB which will always have  a metaKey
                SearchHitField metaKeyCol = searchHitFields.getFields().get(EntitySearchSchema.META_KEY);
                if (metaKeyCol != null) {
                    Object metaKey = metaKeyCol.getValue();
                    if (metaKey != null) {
                        Map<String, String[]> fragments = convertHighlightToMap(searchHitFields.getHighlightFields());

                        SearchResult sr = new SearchResult(
                                searchHitFields.getId(),
                                metaKey.toString(),
                                searchHitFields.getFields().get(EntitySearchSchema.FORTRESS).getValue().toString(),
                                searchHitFields.getFields().get(EntitySearchSchema.LAST_EVENT).getValue().toString(),
                                searchHitFields.getType(),
                                searchHitFields.getFields().get(EntitySearchSchema.WHO).getValue().toString(),
                                searchHitFields.getFields().get(EntitySearchSchema.WHEN).getValue().toString(),
                                searchHitFields.getFields().get(EntitySearchSchema.CREATED).getValue().toString(),
                                searchHitFields.getFields().get(EntitySearchSchema.TIMESTAMP).getValue().toString(),
                                fragments);
                        SearchHitField esField = searchHitFields.getFields().get(EntitySearchSchema.DESCRIPTION);
                        if (esField != null)
                            sr.setDescription(esField.getValue().toString());

                        esField = searchHitFields.getFields().get(EntitySearchSchema.CALLER_REF);
                        if (esField != null)
                            sr.setCallerRef(esField.getValue().toString());
                        if (queryParams.getData() != null) {
                            for (String field : queryParams.getData()) {
                                esField = searchHitFields.getFields().get(field);
                                if (esField != null)
                                    sr.addFieldValue(field, esField.getValue());
                            }
                        }
                        results.add(sr);

                    }
                }
            }
        }
    }

    private Map<String, String[]> convertHighlightToMap(Map<String, HighlightField> highlightFields) {
        Map<String, String[]> highlights = new HashMap<>();
        for (String key : highlightFields.keySet()) {
            Text[] esFrag = highlightFields.get(key).getFragments();
            String[] frags = new String[esFrag.length];
            int i = 0;
            for (Text text : esFrag) {
                frags[i] = text.string();
                i++;
            }
            highlights.put(key, frags);
        }
        return highlights;
    }

}
