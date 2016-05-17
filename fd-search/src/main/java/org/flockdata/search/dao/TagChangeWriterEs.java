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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.NoShardAvailableActionException;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.model.Tag;
import org.flockdata.search.base.TagChangeWriter;
import org.flockdata.search.configure.SearchConfig;
import org.flockdata.search.model.SearchSchema;
import org.flockdata.search.model.TagSearchChange;
import org.flockdata.shared.IndexManager;
import org.flockdata.track.bean.AliasResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates search docs based on Tag content
 * Created by mike on 16/05/16.
 */
@Service
public class TagChangeWriterEs implements TagChangeWriter {

    private Logger logger = LoggerFactory.getLogger(TagChangeWriterEs.class);

    @Autowired
    SearchConfig searchConfig;

    @Autowired
    IndexManager indexManager;

    @Override
    public TagSearchChange handle(TagSearchChange searchChange) throws IOException {

        String source = getJsonToIndex(searchChange);

        if (searchChange.getSearchKey() == null || searchChange.getSearchKey().equals("")) {
            searchChange.setSearchKey((searchChange.getCode() == null ? searchChange.getKey() : searchChange.getCode()));
            logger.debug("No search key, creating as a new document [{}]", searchChange.getKey());
            return save(searchChange, source);
        }

        try {
            logger.debug("Update request for searchKey [{}], key[{}]", searchChange.getSearchKey(), searchChange.getKey());

            GetRequestBuilder request =
                    searchConfig.elasticSearchClient().prepareGet(searchChange.getIndexName(),
                            indexManager.parseType(searchChange.getDocumentType()),
                            searchChange.getSearchKey());

            GetResponse response = request.execute()
                    .actionGet();

            if (response.isExists() && !response.isSourceEmpty()) {
                logger.debug("Document exists!");
                // Messages can be received out of sequence
                // Check to ensure we don't accidentally overwrite a more current
                // document with an older one. We assume the calling fortress understands
                // what the most recent doc is.
            } else {
                // No response, to a search key we expect to exist. Create a new one
                // Likely to be in response to rebuilding an ES index from Graph data.
                logger.debug("About to create in response to an update request for {}", searchChange.toString());
                return save(searchChange, source);
            }

            // Update the existing document with the searchChange change
            IndexRequestBuilder update = searchConfig
                    .elasticSearchClient().prepareIndex(searchChange.getIndexName(), indexManager.parseType(searchChange.getDocumentType()), searchChange.getSearchKey());

            ListenableActionFuture<IndexResponse> ur = update.setSource(source).
                    execute();

            if (logger.isDebugEnabled()) {
                IndexResponse indexResponse = ur.actionGet();
                logger.debug("Updated [{}] logId=[{}] for [{}] to version [{}]", searchChange.getSearchKey(), searchChange.getLogId(), searchChange, indexResponse.getVersion());
            }
        } catch (NoShardAvailableActionException e) {
            return save(searchChange, source);
        }
        return searchChange;
    }

    private TagSearchChange save(TagSearchChange searchChange, String source) {
        String indexName = searchChange.getIndexName();
        String documentType = indexManager.parseType(searchChange.getDocumentType());
        logger.debug("Received request to Save [{}] SearchKey [{}]", searchChange.getKey(), searchChange.getSearchKey());

        // Rebuilding a document after a reindex - preserving the unique key.
        IndexRequestBuilder irb =
                searchConfig.elasticSearchClient()
                        .prepareIndex(searchChange.getIndexName(), documentType)
                        .setSource(source);

        irb.setId(searchChange.getSearchKey());
        irb.setRouting(searchChange.getCode());

        try {
            irb.execute().actionGet();

            return searchChange;
        } catch (MapperParsingException e) {
            logger.error("Parsing error {} - callerRef [{}], key [{}], [{}]", indexName, searchChange.getCode(), searchChange.getKey(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Parsing error - callerRef [" + searchChange.getCode() + "], key [" + searchChange.getKey() + "], " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Writing to index {} produced an error [{}]", indexName, e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Parsing error - callerRef [" + searchChange.getCode() + "], key [" + searchChange.getKey() + "], " + e.getMessage(), e);
        }

    }

    private String getJsonToIndex(TagSearchChange searchChange) {
        ObjectMapper mapper = FdJsonObjectMapper.getObjectMapper();
        Map<String, Object> index = getMapFromChange(searchChange);
        try {
            return mapper.writeValueAsString(index);
        } catch (JsonProcessingException e) {

            logger.error(e.getMessage());
        }
        return null;

    }

    private Map<String, Object> getMapFromChange(TagSearchChange searchChange) {

        Map<String, Object> indexMe = new HashMap<>();
        indexMe.put(SearchSchema.DOC_TYPE, searchChange.getDocumentType());
        indexMe.put(SearchSchema.CODE, searchChange.getCode());
        indexMe.put(SearchSchema.KEY, searchChange.getKey());
        if ( searchChange.getName()!=null)
            indexMe.put(SearchSchema.NAME, searchChange.getName());
        if ( searchChange.getDescription()!=null)
            indexMe.put(SearchSchema.DESCRIPTION, searchChange.getDescription());

        if ( !searchChange.getProps().isEmpty())
            indexMe.put(SearchSchema.PROPS, searchChange.getProps());

        if (!searchChange.getAliases().isEmpty()) {
            Map<String, String> alases = new HashMap<>();
            for (AliasResultBean aliasResultBean : searchChange.getAliases()) {
                alases.put(aliasResultBean.getDescription().toLowerCase(), aliasResultBean.getName());
            }
            indexMe.put("aka", alases);
        }

        return indexMe;


    }

    @Override
    public Map<String, Object> findOne(Tag tag) {
        return null;
    }

    @Override
    public void purgeCache() {

    }

    @Override
    public Map<String, Object> findOne(Tag tag, String id) {
        return null;
    }

    @Override
    public boolean delete(TagSearchChange searchChange) {
        return false;
    }

    @Override
    public void deleteIndex(String index) {

    }
}
