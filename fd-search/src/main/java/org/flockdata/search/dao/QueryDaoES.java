/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.search.dao;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Strings;
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
import org.flockdata.search.configure.SearchConfig;
import org.flockdata.search.helper.QueryGenerator;
import org.flockdata.search.model.*;
import org.flockdata.shared.IndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.io.IOException;
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

    public static final String ES_FIELD_SEP = ".";
    public static final String CODE_FACET = ".code.facet";
    public static final String NAME_FACET = ".name.facet";

    @Autowired
    private SearchConfig esClient;
    @Autowired
    IndexManager indexManager;

    @Value("${highlight.enabled:true}")
    Boolean highlightEnabled;

    private static Logger logger = LoggerFactory.getLogger(QueryDaoES.class);

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
        return SearchSchema.TAG + ES_FIELD_SEP + (relationship.toLowerCase().equals(tag.toLowerCase()) ? "" : relationship.toLowerCase() + ES_FIELD_SEP) + tag.toLowerCase() + CODE_FACET;
    }

    private String parseTagName(String relationship, String tag) {
        return SearchSchema.TAG + ES_FIELD_SEP + (relationship.toLowerCase().equals(tag.toLowerCase()) ? "" : relationship.toLowerCase() + ES_FIELD_SEP) + tag.toLowerCase() + "-name-facet";
    }

    public String[] getIndexes(TagCloudParams tagCloudParams) {
        return indexManager.getIndexesToQuery(tagCloudParams.getCompany(), tagCloudParams.getFortress(), null, tagCloudParams.getTypes());

    }

    @Override
    public TagCloud getCloudTag(TagCloudParams tagCloudParams) throws NotFoundException {
        // Getting all tag and What fields


        Collection<String> whatAndTagFields = getTagArray(tagCloudParams);

        SearchRequestBuilder query = esClient.elasticSearchClient().prepareSearch(
                getIndexes(tagCloudParams))
                ;

        if (tagCloudParams.getRelationships() != null)
            tagCloudParams.getRelationships().clear();
        if (tagCloudParams.getTags() != null)
            tagCloudParams.getTags().clear();

        query.setTypes(tagCloudParams.getTypes());

        query.setExtraSource(QueryGenerator.getFilteredQuery(tagCloudParams, false));
        for (String whatAndTagField : whatAndTagFields) {
            query.addAggregation(AggregationBuilders.terms(whatAndTagField).field(whatAndTagField).size(50));
        }

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
            for (Object object : terms.getBuckets()) {

                org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket bucket = (Terms.Bucket) object;
                tagcloud.addTerm(bucket.getKey(), bucket.getDocCount());
            }
//            for (Terms.Bucket bucket : terms.getBuckets()) {

  //          }
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
            int pos = s.indexOf(NAME_FACET); // Names by preference
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
            int pos = s.indexOf(CODE_FACET); // Names by preference
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
        SearchResponse response = esClient.elasticSearchClient().prepareSearch(index)
                .execute()
                .actionGet();

        logger.debug("Searching index [{}] for hit counts", index);

        return response.getHits().getTotalHits();
    }

    public EntityKeyResults doEntityKeySearch(QueryParams queryParams) throws FlockException {
        String[] types = Strings.EMPTY_ARRAY;
        if (queryParams.getTypes() != null) {
            types = queryParams.getTypes();
        }
        SearchRequestBuilder query = esClient.elasticSearchClient().prepareSearch(indexManager.getIndexesToQuery(queryParams))
                .setTypes(types)
                .addField(SearchSchema.KEY)
                .setExtraSource(QueryGenerator.getFilteredQuery(queryParams, false));
        if ( queryParams.getSize()!=null)
            query.setSize(queryParams.getSize());

        SearchResponse response;
        try {
            response = query.execute().get(10000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("KeySearch query error ", e);
            throw new FlockException("KeySearch query error ", e);
        }
        return getKeyResults(response);
    }

    private EntityKeyResults getKeyResults(SearchResponse response) {
        EntityKeyResults results = new EntityKeyResults();
        if (response == null || response.getHits().getTotalHits() == 0)
            return results;

        for (SearchHit searchHitFields : response.getHits().getHits()) {
            Object o = searchHitFields.getFields().get(SearchSchema.KEY).getValues().iterator().next();
            results.add(o);
        }
        return results;
    }

    @Override
    public String doSearch(QueryParams queryParams) throws FlockException {
        SearchResponse result = esClient.elasticSearchClient().prepareSearch(indexManager.getIndexesToQuery(queryParams))
                .setExtraSource(QueryGenerator.getSimpleQuery(queryParams, false))
                .execute()
                .actionGet();

        //logger.debug("looking for {} in index {}", queryString, index);
        return result.toString();
    }

    @Override
    public void getTags(String indexName) {
//        GetMappingsResponse fieldMappings = esClient
//                .admin()
//                .indices()
//                .getMappings(new GetMappingsRequest())
//                .actionGet();

//        ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = fieldMappings.getMappings();

    }

    @Override
    public EsSearchResult doEntitySearch(QueryParams queryParams) throws FlockException {
        StopWatch watch = new StopWatch();

        watch.start(queryParams.toString());

        SearchRequestBuilder query = esClient.elasticSearchClient().prepareSearch(indexManager.getIndexesToQuery(queryParams))
                .addField(SearchSchema.KEY)
                .addField(SearchSchema.FORTRESS)
                .addField(SearchSchema.LAST_EVENT)
                .addField(SearchSchema.DESCRIPTION)
                .addField(SearchSchema.CODE)
                .addField(SearchSchema.WHO)
                .addField(SearchSchema.UPDATED)
                .addField(SearchSchema.CREATED)
                .addField(SearchSchema.TIMESTAMP)
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
        logger.debug("ES Query. Results [{}] took [{}]", results.size(), watch.prettyPrint());
        return searchResult;
    }

    private void getEntityResults(Collection<SearchResult> results, SearchResponse response, QueryParams queryParams) {
        logger.debug("Processing [{}] SearchResults from ElasticSearch", results.size());
        for (SearchHit searchHitFields : response.getHits().getHits()) {
            if (!searchHitFields.getFields().isEmpty()) { // DAT-83
                // This function returns only information tracked by FD which will always have  a key
                SearchHitField keyCol = searchHitFields.getFields().get(SearchSchema.KEY);
                if (keyCol != null) {
                    Object key = keyCol.getValue();
                    if (key != null) {
                        Map<String, String[]> fragments = convertHighlightToMap(searchHitFields.getHighlightFields());

                        SearchResult sr = new SearchResult(
                                searchHitFields.getId(),
                                key.toString(),
                                getHitValue(searchHitFields.getFields().get(SearchSchema.FORTRESS)),
                                getHitValue(searchHitFields.getFields().get(SearchSchema.LAST_EVENT)),
                                searchHitFields.getType(),
                                getHitValue(searchHitFields.getFields().get(SearchSchema.WHO)),
                                getHitValue(searchHitFields.getFields().get(SearchSchema.UPDATED)),
                                getHitValue(searchHitFields.getFields().get(SearchSchema.CREATED)),
                                getHitValue(searchHitFields.getFields().get(SearchSchema.TIMESTAMP)),
                                fragments);

                        sr.setDescription(getHitValue(searchHitFields.getFields().get(SearchSchema.DESCRIPTION)));

                        sr.setCode(getHitValue(searchHitFields.getFields().get(SearchSchema.CODE)));
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

            String query = "{\"query\": " + JsonUtils.toJson(queryParams.getQuery()) ;
            if ( queryParams.getFields()!=null){
                query = query +",\"fields\": "+JsonUtils.toJson(queryParams.getFields());
            }
            if ( queryParams.getAggs()!=null )
                query = query + ",\"aggs\": " + JsonUtils.toJson(queryParams.getAggs()) + "}";
            else
                query = query + "}";

            SearchRequestBuilder esQuery = esClient
                    .elasticSearchClient().prepareSearch(indexManager.getIndexesToQuery(queryParams))
                    .setTypes(queryParams.getTypes());

            if ( queryParams.getSize()!=null )
                esQuery.setSize(queryParams.getSize());

            if (queryParams.getFrom() != null)
                esQuery.setFrom(queryParams.getFrom());

            esQuery.setExtraSource( query );

            try {
                SearchResponse response = esQuery
                        .execute()
                        .actionGet();

                result = new EsSearchResult(response.toString().getBytes());
                result.setTotalHits(response.getHits().getTotalHits());
            } catch ( ElasticsearchException e){
                Map<String,Object>error = new HashMap<>();
                error.put("errors", parseException(e.getRootCause().getMessage()));

                try {
                    result = new EsSearchResult(JsonUtils.toJsonBytes(error));
                } catch (IOException e1) {
                    throw new FlockException("Json error", e1);
                }
            }


        } else {
            String index = queryParams.getIndex();
            if ( index == null )
                index = indexManager.parseIndex(queryParams);

            GetResponse response =
                    esClient.elasticSearchClient().prepareGet(index,
                            queryParams.getTypes()[0],
                            queryParams.getCode())
                            .execute()
                            .actionGet();
            result = new EsSearchResult(response.getSourceAsBytes());

        }
        return result;

    }

    private Collection<String> parseException(String message) {

        Collection<String> results = new ArrayList<>();
        String[] failures = StringUtils.delimitedListToStringArray(message, "Parse Failure ");
        if (failures.length == 0)
            results.add( message);
        else {
            for (String failure : failures) {
                // Exclude duplicates and query source
                if ( !results.contains(failure) && ! failure.startsWith("[Failed to parse source"))
                    results.add(failure);
            }
        }


        return results;
    }
}
