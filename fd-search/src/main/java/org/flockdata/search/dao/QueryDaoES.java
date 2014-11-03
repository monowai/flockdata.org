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

    private String[] getTagFields(String[] concepts) {
        if (concepts==null || concepts.length == 0)
            return new String[]{EntitySearchSchema.TAG + ".*.code"};

        String[] result = new String[concepts.length];
        int i = 0;
        for (String concept : concepts) {
            result[i] = parseConcept(concept);
            i++;
        }
        return result;

    }
    private Collection<String> getTagArray(String[] concepts) {
        Collection<String> result = new ArrayList<>();
        if (concepts==null || concepts.length == 0)
            return result;

        for (String concept : concepts) {
            result.add(parseConcept(concept));
        }

        return result;

    }

    private String parseConcept(String tag ){
        return EntitySearchSchema.TAG + "." + tag.toLowerCase() + ".code";
    }

    @Override
    public TagCloud getCloudTag(TagCloudParams tagCloudParams) throws NotFoundException {
        // Getting all tag and What fields
        String index = EntitySearchSchema.parseIndex(tagCloudParams.getCompany(), tagCloudParams.getFortress());
//        GetFieldMappingsResponse esIndex;
//        try {
//            esIndex = client.admin()
//                    .indices()
//                    .prepareGetFieldMappings(index)
//                    .setTypes(getDocumentTypes(tagCloudParams.getType()))
//                    .setFields(getTagFields(tagCloudParams.getRelationships()))
//                    .get();
//        } catch (IndexMissingException ie) {
//            logger.error("Requested data from a missing index {}", index);
//            throw new NotFoundException("The requested index does not exist in the Search Service", ie);
//        }

//        ImmutableMap<String, ImmutableMap<String, GetFieldMappingsResponse.FieldMappingMetaData>>
//                mappings = esIndex.mappings().get(index);
        //List<String> whatAndTagFields = new ArrayList<>();
        Collection<String> whatAndTagFields = getTagArray(tagCloudParams.getRelationships());


//        for (String s : mappings.keySet()) {
//            ImmutableMap<String, GetFieldMappingsResponse.FieldMappingMetaData> var = mappings.get(s);
//            for (String field : var.keySet()) {
//                whatAndTagFields.add(field);
//            }
//        }

        SearchRequestBuilder searchRequest=
                client.prepareSearch(index)
                        .setTypes(tagCloudParams.getType().toLowerCase())
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
