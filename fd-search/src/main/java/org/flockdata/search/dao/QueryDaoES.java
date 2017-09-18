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

package org.flockdata.search.dao;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.InternalTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.NotFoundException;
import org.flockdata.integration.IndexManager;
import org.flockdata.search.*;
import org.flockdata.search.helper.QueryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * @author mholdsworth
 * @since 28/04/2013
 * @tag Query, Search, ElasticSearch
 */
@Repository
public class QueryDaoES  {

    public static final String ES_FIELD_SEP = ".";
    public static final String CODE_KEYWORD = ".code";
    public static final String NAME_KEYWORD = ".name";
    private static Logger logger = LoggerFactory.getLogger(QueryDaoES.class);
    private final Client elasticSearchClient;
    private final IndexManager indexManager;
    @Value("${highlight.enabled:true}")
    Boolean highlightEnabled;

    @Autowired
    public QueryDaoES(IndexManager indexManager, Client elasticSearchClient) {
        this.indexManager = indexManager;
        this.elasticSearchClient = elasticSearchClient;
    }

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
        return SearchSchema.TAG + ES_FIELD_SEP + (relationship.toLowerCase().equals(tag.toLowerCase()) ? "" : relationship.toLowerCase() + ES_FIELD_SEP) + tag.toLowerCase() + CODE_KEYWORD;
    }

    private String parseTagName(String relationship, String tag) {
        return SearchSchema.TAG + ES_FIELD_SEP + (relationship.toLowerCase().equals(tag.toLowerCase()) ? "" : relationship.toLowerCase() + ES_FIELD_SEP) + tag.toLowerCase() + "-name-keyword";
    }

    public String[] getIndexes(TagCloudParams tagCloudParams) {
        return indexManager.getIndexesToQuery(tagCloudParams.getCompany(), tagCloudParams.getFortress(), tagCloudParams.getTypes(), null);

    }

    public TagCloud getCloudTag(TagCloudParams tagCloudParams) throws NotFoundException {
        // Getting all tag and What fields


        Collection<String> whatAndTagFields = getTagArray(tagCloudParams);

        SearchRequestBuilder query = elasticSearchClient.prepareSearch(
                getIndexes(tagCloudParams))
                ;

        if (tagCloudParams.getRelationships() != null)
            tagCloudParams.getRelationships().clear();
        if (tagCloudParams.getTags() != null)
            tagCloudParams.getTags().clear();

        query.setTypes(tagCloudParams.getTypes());

//        query.setExtraSource(QueryGenerator.getFilteredQuery(tagCloudParams, false));
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
     *
     * If the code and the name are equal during indexing, then the value is stored
     * only as a code. Entity document tags always have a code value.
     *
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
            int pos = s.indexOf(NAME_KEYWORD); // Names by preference
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
            int pos = s.indexOf(CODE_KEYWORD); // Names by preference
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

    public long getHitCount(String index) {
        SearchResponse response = elasticSearchClient.prepareSearch(index)
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

        SearchRequestBuilder query = elasticSearchClient.prepareSearch(indexManager.getIndexesToQuery(queryParams))
                .setTypes(types)
                .addDocValueField(SearchSchema.KEY)
                .setQuery(QueryBuilders.matchQuery(SearchSchema.KEY, queryParams.getKey()));
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

    public String doSearch(QueryParams queryParams) throws FlockException {
        SearchResponse result = elasticSearchClient.prepareSearch(indexManager.getIndexesToQuery(queryParams))
                .setQuery(QueryBuilders.wrapperQuery(QueryGenerator.getSimpleQuery(queryParams, false)))
                .execute()
                .actionGet();

        return result.toString();
    }

    public void getTags(String indexName) {
//        GetMappingsResponse fieldMappings = elasticSearchClient
//                .admin()
//                .indices()
//                .getMappings(new GetMappingsRequest())
//                .actionGet();

//        ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = fieldMappings.getMappings();

    }

    public EsSearchRequestResult doEntitySearch(QueryParams queryParams) throws FlockException {
        StopWatch watch = new StopWatch();

        watch.start(queryParams.toString());
        String[] indexes = indexManager.getIndexesToQuery(queryParams);
        String queryString = (queryParams.getSearchText()==null? queryParams.getCode(): queryParams.getSearchText());
        SearchRequestBuilder query = elasticSearchClient.prepareSearch(indexes)
                .addDocValueField(SearchSchema.KEY)
                .addDocValueField(SearchSchema.FORTRESS)
                .addDocValueField(SearchSchema.LAST_EVENT)
                .addDocValueField(SearchSchema.NAME )
                .addStoredField(SearchSchema.DESCRIPTION)
                .addDocValueField(SearchSchema.CODE)
                .addDocValueField(SearchSchema.WHO)
                .addDocValueField(SearchSchema.UPDATED)
                .addDocValueField(SearchSchema.CREATED)
                .addDocValueField(SearchSchema.TIMESTAMP)
                .setQuery(boolQuery().should(queryStringQuery(queryString)));
//                .setQuery(QueryBuilders.simpleQueryStringQuery(QueryGenerator.getSimpleQuery(queryParams, highlightEnabled))) ;

        if (queryParams.getSize()!=null )
            query.setSize(queryParams.getSize());

        if ( queryParams.getFrom()!=null )
            query.setFrom(queryParams.getFrom());

        // Add user requested fields
        if (queryParams.getData() != null){
            for (String field : queryParams.getData()) {
                query.addDocValueField(field);
            }
        }


        ListenableActionFuture<SearchResponse> future = query.execute();


        SearchResponse response;
        try {
            response = future.get();
        } catch (ExecutionException e ){
            logger.debug(e.getCause().getMessage() +"\n"+queryParams.toString() + " computed indexes"+ Arrays.toString(indexes));
            return  new EsSearchRequestResult("Error looking for entities "+ parseException(e));

        }catch (InterruptedException  e) {
            logger.error("Search Exception processing query", e);
            // ToDo: No sensible error being returned to the caller
            return new EsSearchRequestResult(e.getMessage());
        }

        Collection<SearchResult> results = convert(response, queryParams);
        EsSearchRequestResult searchResult = new EsSearchRequestResult(results);
        searchResult.setTotalHits(response.getHits().getTotalHits());
        searchResult.setStartedFrom(queryParams.getFrom()==null ?0:queryParams.getFrom());
        watch.stop();
        logger.debug("ES Query. Results [{}] took [{}]", results.size(), watch.prettyPrint());
        return searchResult;
    }

    private String parseException(ExecutionException e) {
        if ( e.getCause()!=null){
            return (e.getCause().getCause()!=null ? e.getCause().getCause().getMessage() : e.getCause().getMessage());
        }
        return e.getMessage();

    }

    private Collection<SearchResult> convert( SearchResponse response, QueryParams queryParams) {

        Collection<SearchResult> results = new ArrayList<>();
        for (SearchHit searchHitFields : response.getHits().getHits()) {
            if (!searchHitFields.getFields().isEmpty()) { // DAT-83
                // This function returns only information tracked by FD which will always have  a key
                SearchHitField keyCol = searchHitFields.getFields().get(SearchSchema.KEY);
                if (keyCol != null) {
                    Object key = keyCol.getValue();
                    if (key != null) {
//                        Map<String, HighlightField> fragments = convertHighlightToMap(searchHitFields.getHighlightFields());

                        EsSearchResult sr = new EsSearchResult(
                                searchHitFields.getId(),
                                key.toString(),
                                getHitValue(searchHitFields.getFields().get(SearchSchema.FORTRESS)),
                                getHitValue(searchHitFields.getFields().get(SearchSchema.LAST_EVENT)),
                                searchHitFields.getType(),
                                getHitValue(searchHitFields.getFields().get(SearchSchema.WHO)),
                                getHitValue(searchHitFields.getFields().get(SearchSchema.UPDATED)),
                                getHitValue(searchHitFields.getFields().get(SearchSchema.CREATED)),
                                getHitValue(searchHitFields.getFields().get(SearchSchema.TIMESTAMP))
                        );

                        sr.setDescription(getHitValue(searchHitFields.getFields().get(SearchSchema.DESCRIPTION)));
                        sr.setName(getHitValue(searchHitFields.getFields().get(SearchSchema.NAME)));
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
        return results;
    }

    private String getHitValue(SearchHitField field) {
        if (field == null || field.getValue() == null)
            return null;

        return field.getValue().toString();
    }

    private Map<String, HighlightField> convertHighlightToMap(Map<String, HighlightField> highlightFields) {
        Map<String, HighlightField> highlights = new HashMap<>();
        for (String key : highlightFields.keySet()) {
            Text[] esFrag = highlightFields.get(key).getFragments();
            highlights.put(key, new HighlightField(key, esFrag));
        }
        return highlights;
    }

    public EsSearchRequestResult doEsQuery(QueryParams queryParams) throws FlockException {
        String index = queryParams.getIndex();
        if ( index == null )
            index = indexManager.toIndex(queryParams);


        SearchRequestBuilder esQuery = elasticSearchClient
                .prepareSearch(index)
                .setQuery(termQuery(SearchSchema.CODE, queryParams.getCode()))
                .setTypes(queryParams.getTypes());

        SearchResponse response = esQuery.execute().actionGet();
        return wrapResponseResult(response);

    }

    public EsSearchRequestResult wrapResponseResult(SearchResponse response) {
        EsSearchRequestResult result = new EsSearchRequestResult(response.toString().getBytes());
        result.setTotalHits(response.getHits().getTotalHits());
        return result;
    }

    public EsSearchRequestResult doParametrizedQuery(QueryParams queryParams) throws FlockException {
        EsSearchRequestResult result ;
//        if (queryParams.isMatchAll() || queryParams.getCode() != null || queryParams.getAggs()!=null) {
            // Default ES query if not otherwise supplied

            SearchRequestBuilder esQuery = elasticSearchClient
                    .prepareSearch(indexManager.getIndexesToQuery(queryParams));

            BoolQueryBuilder boolQuery =null;
            MatchAllQueryBuilder matchAll = null;
            
            if ( queryParams.isMatchAll())
                matchAll = QueryBuilders.matchAllQuery();

            if ( queryParams.getCode()!=null ) {
                boolQuery =QueryBuilders.boolQuery();
                boolQuery.must(termQuery(SearchSchema.CODE, queryParams.getCode()));
            }

            if ( queryParams.getSearchText() !=null ){
                if ( boolQuery == null )
                    boolQuery =QueryBuilders.boolQuery();
                boolQuery.should(queryStringQuery(queryParams.getSearchText()));
            }


            if ( !queryParams.getTerms().isEmpty()){
                if ( boolQuery == null )
                    boolQuery =QueryBuilders.boolQuery();
                for (String key : queryParams.getTerms().keySet()) {
                    boolQuery.must(termQuery(key, queryParams.getTerms().get(key))) ;
                }
            }

            if ( queryParams.getFields()!=null){
                for (String field : queryParams.getFields()) {
                    esQuery.addDocValueField(field);
                }
            }

            if ( queryParams.getAggs()!=null ) {
                // ToDo: Fix Aggs
//                query = query + ",\"aggs\": " + JsonUtils.toJson(queryParams.getAggs()) + "}";
            }

            if ( queryParams.getTypes()!=null )
                    esQuery.setTypes(queryParams.getTypes());

            if ( queryParams.getSize()!=null )
                esQuery.setSize(queryParams.getSize());
            if (queryParams.getFrom() != null)
                esQuery.setFrom(queryParams.getFrom());

            if ( boolQuery !=null )
                esQuery.setQuery(boolQuery);
            else
                esQuery.setQuery(matchAll);
            
            try {
                SearchResponse response = esQuery
                        .execute()
                        .actionGet();

                return wrapResponseResult(response);
            } catch ( ElasticsearchException e){
                Map<String,Object>error = new HashMap<>();
                error.put("errors", parseException(e.getRootCause().getMessage()));

                try {
                    result = new EsSearchRequestResult(JsonUtils.toJsonBytes(error));
                } catch (IOException e1) {
                    throw new FlockException("Json error", e1);
                }
            }


//        }
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
