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

package org.flockdata.search.dao;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.text.Text;
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
import org.flockdata.helper.JsonUtils;
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

        if (params.getRelationships() == null || params.getRelationships().isEmpty()) {
            if (params.getTags() == null || params.getTags().isEmpty())
                return result;
            else {
                for (String tag : params.getTags())
                    result.add(parseTagCode("*", tag));
                return result;
            }
        }


        for (String relationship : params.getRelationships()) {
            if (params.getTags() == null || params.getTags().isEmpty())
                result.add(parseTagCode(relationship, "*"));
            else
                for (String tag : params.getTags()) {
                    result.add(parseTagCode(relationship, tag));
                    result.add(parseTagName(relationship, tag));
                }
        }

        return result;

    }

    private String parseTagCode(String relationship, String tag) {
        return EntitySearchSchema.TAG + "." + (relationship.toLowerCase().equals(tag.toLowerCase()) ? "" : relationship.toLowerCase() + ".") + tag.toLowerCase() + ".code.facet";
        //return EntitySearchSchema.TAG + "." + relationship.toLowerCase() + "."+tag.toLowerCase() + ".code.facet";
    }

    private String parseTagName(String relationship, String tag) {
        return EntitySearchSchema.TAG + "." + (relationship.toLowerCase().equals(tag.toLowerCase()) ? "" : relationship.toLowerCase() + ".") + tag.toLowerCase() + ".name.facet";
//        return EntitySearchSchema.TAG + "." + relationship.toLowerCase() + "."+tag.toLowerCase() + ".name.facet";
    }


    @Override
    public TagCloud getCloudTag(TagCloudParams tagCloudParams) throws NotFoundException {
        // Getting all tag and What fields


        Collection<String> whatAndTagFields = getTagArray(tagCloudParams);

        SearchRequestBuilder query = client.prepareSearch(EntitySearchSchema.parseIndex(tagCloudParams.getCompany(), tagCloudParams.getFortress()))
                .setTypes(tagCloudParams.getTypes());

        if (tagCloudParams.getRelationships() != null)
            tagCloudParams.getRelationships().clear();
        if (tagCloudParams.getTags() != null)
            tagCloudParams.getTags().clear();
        query.setExtraSource(QueryGenerator.getFilteredQuery(tagCloudParams, false));
        for (String whatAndTagField : whatAndTagFields) {
            query.addAggregation(AggregationBuilders.terms(whatAndTagField).field(whatAndTagField).size(50));
        }
//        query.setExtraSource(QueryGenerator.getSearchText(tagCloudParams.getSearchText(), false));
        //searchRequest.setQuer("*");

        // No hits, just the aggs
        SearchResponse response = query.execute().actionGet();

        TagCloud tagcloud = new TagCloud();
        Aggregations tagCloudFacet = response.getAggregations();
        if (tagCloudFacet == null) {
            // ToDo: support "ALL" tag fields
            return tagcloud;
        }
        Map<String, Aggregation> aggregates = resolveKeys(tagCloudFacet.getAsMap());
        for (String key : aggregates.keySet()) {
            InternalTerms terms = (InternalTerms) aggregates.get(key);
            for (Terms.Bucket bucket : terms.getBuckets()) {
                tagcloud.addTerm(bucket.getKey(), bucket.getDocCount());
            }
        }
        tagcloud.scale(); // Scale the results suitable for presentation
        return tagcloud;
    }

    /**
     * Indexed document tags always have a code but not always a name. Typically a code will
     * be a codified value so we favour human readable names.
     * <p>
     * If the code and the name are equal during indexing, then the value is stored
     * only as a code. Entity document tags always have a code value.
     * <p>
     * We want to return either the name or the code associated with the document. We don't want to
     * resort to a scripted field to achieve this so the action is being performed here.
     *
     * @param asMap ES results
     * @return Results to return to the caller
     */
    private Map<String, Aggregation> resolveKeys(Map<String, Aggregation> asMap) {
        Map<String, Aggregation> results = new HashMap<>();
        ArrayList<String> relationships = new ArrayList<>();

        for (String s : asMap.keySet()) {
            int pos = s.indexOf(".name.facet"); // Names by preference
            if (pos > 0) {

                InternalTerms terms = (InternalTerms) asMap.get(s);
                if (terms.getBuckets().size() != 0) {
                    String relationship = s.substring(0, pos);
                    relationships.add(relationship);
                    results.put(s, asMap.get(s));
                }
            }
        }
        // Pickup any Codes that don't have Name entries
        for (String s : asMap.keySet()) {
            int pos = s.indexOf(".code.facet"); // Names by preference
            if (pos > 0) {
                String relationship = s.substring(0, pos);
                if (!relationships.contains(relationship)) {
                    relationships.add(relationship);
                    results.put(s, asMap.get(s));
                }
            }
        }

        return results;
    }

    @Override
    public long getHitCount(String index) {
        SearchResponse response = client.prepareSearch(index)
                .execute()
                .actionGet();

        logger.debug("Searching index [{}] for hit counts", index);

        return response.getHits().getTotalHits();
    }

    public MetaKeyResults doMetaKeySearch(QueryParams queryParams) throws FlockException {
        String[] types = Strings.EMPTY_ARRAY;
        if (queryParams.getTypes() != null) {
            types = queryParams.getTypes();
        }
        SearchRequestBuilder query = client.prepareSearch(EntitySearchSchema.parseIndex(queryParams))
                .setTypes(types)
                .addField(EntitySearchSchema.META_KEY)
                .setExtraSource(QueryGenerator.getFilteredQuery(queryParams, false));
        if ( queryParams.getSize()!=null)
            query.setSize(queryParams.getSize());

        SearchResponse response;
        try {
            response = query.execute().get(50000l, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("MetaKeySearch query error ", e);
            throw new FlockException("MetaKeySearch query error ", e);
        }
        return getMetaKeyResults(response);
    }

    private MetaKeyResults getMetaKeyResults(SearchResponse response) {
        MetaKeyResults results = new MetaKeyResults();
        if (response == null || response.getHits().getTotalHits() == 0)
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
                .setExtraSource(QueryGenerator.getSimpleQuery(queryParams, false))
                .execute()
                .actionGet();

        //logger.debug("looking for {} in index {}", queryString, index);
        return result.toString();
    }

    @Override
    public void getTags(String indexName) {
        GetMappingsResponse fieldMappings = client
                .admin()
                .indices()
                .getMappings(new GetMappingsRequest())
                .actionGet();

        ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = fieldMappings.getMappings();

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
                .addField(EntitySearchSchema.UPDATED)
                .addField(EntitySearchSchema.CREATED)
                .addField(EntitySearchSchema.TIMESTAMP)
                .setExtraSource(QueryGenerator.getSimpleQuery(queryParams, highlightEnabled));

        if (queryParams.getSize()!=null )
            query.setSize(queryParams.getSize());

        if ( queryParams.getFrom()!=null )
            query.setFrom(queryParams.getFrom());

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
        searchResult.setStartedFrom(queryParams.getFrom()==null ?0:queryParams.getFrom());
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
                                getHitValue(searchHitFields.getFields().get(EntitySearchSchema.UPDATED)),
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
        EsSearchResult result ;
        if (queryParams.getQuery() != null || queryParams.getAggs()!=null) {

            String query = "{\"query\": " + JsonUtils.getJSON(queryParams.getQuery()) ;
            if ( queryParams.getFields()!=null){
                query = query +",\"fields\": "+JsonUtils.getJSON(queryParams.getFields());
            }
            if ( queryParams.getAggs()!=null )
                query = query + ",\"aggs\": " + JsonUtils.getJSON(queryParams.getAggs()) + "}";
            else
                query = query + "}";

            SearchRequestBuilder esQuery = client.prepareSearch(EntitySearchSchema.parseIndex(queryParams));

            if ( queryParams.getSize()!=null )
                esQuery.setSize(queryParams.getSize());

            if (queryParams.getFrom() != null)
                esQuery.setFrom(queryParams.getFrom());

            esQuery.setExtraSource( query );

            SearchResponse response = esQuery
                    .execute()
                    .actionGet();

             result = new EsSearchResult(response.toString().getBytes());
            result.setTotalHits(response.getHits().getTotalHits());


        } else {
            GetResponse response =
                    client.prepareGet(EntitySearchSchema.parseIndex(queryParams),
                            queryParams.getTypes()[0],
                            queryParams.getCallerRef())
                            .execute()
                            .actionGet();
            result = new EsSearchResult(response.getSourceAsBytes());

        }
        return result;

    }
}
