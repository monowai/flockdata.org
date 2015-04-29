/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.search.dao;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.InternalTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.highlight.HighlightField;
import org.flockdata.dao.QueryDao;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.search.helper.QueryGenerator;
import org.flockdata.search.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private Collection<String> getTagArray(TagCloudParams params) {
        Collection<String> result = new ArrayList<>();

        if (params.getRelationships()== null || params.getRelationships().length == 0) {
            if (params.getTags() == null || params.getTags().length == 0)
                return result;
            else {
                for (String tag : params.getTags())
                    result.add(parseConcept("*", tag));
                return result;
            }
        }


        for (String relationship : params.getRelationships()) {
            if (params.getTags() == null || params.getTags().length ==0)
                result.add(parseConcept(relationship, "*"));
            else
                for ( String tag : params.getTags() )
                    result.add(parseConcept(relationship, tag));
        }

        return result;

    }

    private String parseConcept(String relationship, String tag) {
        return EntitySearchSchema.TAG + "." + relationship.toLowerCase() + "."+tag.toLowerCase() + ".code.facet";
    }

    @Override
    public TagCloud getCloudTag(TagCloudParams tagCloudParams) throws NotFoundException {
        // Getting all tag and What fields
        String index = EntitySearchSchema.parseIndex(tagCloudParams.getCompany(), tagCloudParams.getFortress());

        Collection<String> whatAndTagFields = getTagArray(tagCloudParams);

        SearchRequestBuilder searchRequest =
                client.prepareSearch(index)
                        .setTypes(tagCloudParams.getTypes())
                        .setQuery(
                                QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), null)
                        );

        for (String whatAndTagField : whatAndTagFields) {
            searchRequest.addAggregation(AggregationBuilders.terms(whatAndTagField).field(whatAndTagField).size(50));
        }
        //SearchRequestBuilder searchRequest ;
        SearchResponse response = searchRequest.setSize(0).get();

        TagCloud tagcloud = new TagCloud();
        Aggregations tagCloudFacet = response.getAggregations();
        if (tagCloudFacet == null) {
            // ToDo: support "ALL" tag fields
            return tagcloud;
        }
        Map<String, Aggregation> aggregates = tagCloudFacet.getAsMap();
        for (String key : aggregates.keySet()) {
            InternalTerms terms = (InternalTerms) aggregates.get(key);
            for (Terms.Bucket bucket : terms.getBuckets()) {
                // ToDo: Figure out date handling. When writing the tag, we've lost the datatype
                //       we could autodetect
                tagcloud.addTerm(bucket.getKey(), bucket.getDocCount());
            }
        }
        tagcloud.scale(); // Scale the results suitable for presentation
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

    public MetaKeyResults doMetaKeySearch ( QueryParams queryParams ) throws FlockException{
        String[] types = Strings.EMPTY_ARRAY;
        if (queryParams.getTypes() != null) {
            types = queryParams.getTypes();
        }
        SearchRequestBuilder query = client.prepareSearch(EntitySearchSchema.parseIndex(queryParams))
                .setTypes(types)
                .addField(EntitySearchSchema.META_KEY)
                .setSize(queryParams.getRowsPerPage())
                .setExtraSource(QueryGenerator.getSimpleQuery(queryParams.getSimpleQuery(), false));

        SearchResponse response ;
        try {
            response = query.execute().get(50000l, TimeUnit.MILLISECONDS);
        } catch (InterruptedException |ExecutionException | TimeoutException e) {
            logger.error("MetaKeySearch query error ", e);
            throw new FlockException("MetaKeySearch query error ", e);
        }
        return getMetaKeyResults(response);
    }

    private MetaKeyResults getMetaKeyResults(SearchResponse response) {
        MetaKeyResults results = new MetaKeyResults();
        if ( response == null || response.getHits().getTotalHits() == 0)
            return results;

        for (SearchHit searchHitFields : response.getHits().getHits()) {
            Object o = searchHitFields.getFields().get(EntitySearchSchema.META_KEY).getValues().iterator().next();
            results.add(o);
        }
        return results;
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
        logger.debug("Processing [{}] SearchResults from ElasticSearch", results.size());
        for (SearchHit searchHitFields : response.getHits().getHits()) {
            if (!searchHitFields.getFields().isEmpty()) { // DAT-83
                // This function returns only information tracked by FD which will always have  a metaKey
                SearchHitField metaKeyCol = searchHitFields.getFields().get(EntitySearchSchema.META_KEY);
                if (metaKeyCol != null) {
                    Object metaKey = metaKeyCol.getValue();
                    if (metaKey != null) {
                        Map<String, String[]> fragments = convertHighlightToMap(searchHitFields.getHighlightFields());

                        SearchResult sr = new SearchResult(
                                searchHitFields.getId(),
                                metaKey.toString(),
                                getHitValue(searchHitFields.getFields().get(EntitySearchSchema.FORTRESS)),
                                getHitValue(searchHitFields.getFields().get(EntitySearchSchema.LAST_EVENT)),
                                searchHitFields.getType(),
                                getHitValue(searchHitFields.getFields().get(EntitySearchSchema.WHO)),
                                getHitValue(searchHitFields.getFields().get(EntitySearchSchema.WHEN)),
                                getHitValue(searchHitFields.getFields().get(EntitySearchSchema.CREATED)),
                                getHitValue(searchHitFields.getFields().get(EntitySearchSchema.TIMESTAMP)),
                                fragments);

                        sr.setDescription(getHitValue(searchHitFields.getFields().get(EntitySearchSchema.DESCRIPTION)));

                        sr.setCallerRef(getHitValue(searchHitFields.getFields().get(EntitySearchSchema.CALLER_REF)));
                        if (queryParams.getData() != null) {
                            for (String field : queryParams.getData()) {
                                sr.addFieldValue(field, getHitValue(searchHitFields.getFields().get(field)));
                            }
                        }
                        results.add(sr);

                    }
                }
            } else {
                logger.debug("Skipping row due to no column");
            }
        }
    }

    private String getHitValue(SearchHitField field) {
        if (field == null || field.getValue() == null)
            return null;

        return field.getValue().toString();
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

    @Override
    public EsSearchResult doWhatSearch(QueryParams queryParams) throws FlockException {

        GetResponse response =
                client.prepareGet(EntitySearchSchema.parseIndex(queryParams),
                        queryParams.getTypes()[0],
                        queryParams.getCallerRef())
                        .execute()
                        .actionGet();

        return new EsSearchResult(response.getSourceAsMap());
    }
}
