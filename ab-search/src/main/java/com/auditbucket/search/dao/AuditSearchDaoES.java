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

import com.auditbucket.audit.model.AuditSearchDao;
import com.auditbucket.audit.model.SearchChange;
import com.auditbucket.audit.model.AuditHeader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 27/04/13
 * Time: 12:00 PM
 */
@Repository("esAuditChange")
public class AuditSearchDaoES implements AuditSearchDao {
    @Autowired
    private Client esClient;

    private Logger log = LoggerFactory.getLogger(AuditSearchDaoES.class);

    private String makeIndexJson(SearchChange auditChange) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> index = makeIndexDocument(auditChange);
        try {
            return mapper.writeValueAsString(index);
        } catch (JsonProcessingException e) {

            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * Converts a user requested auditChange in to a standardised document to index
     *
     * @param auditChange incoming
     * @return document to index
     */
    private Map<String, Object> makeIndexDocument(SearchChange auditChange) {
        Map<String, Object> indexMe = new HashMap<String, Object>();
        indexMe.put("@what", auditChange.getWhat());
        indexMe.put("@auditKey", auditChange.getAuditKey());
        indexMe.put("@who", auditChange.getWho());
        indexMe.put("@lastEvent", auditChange.getEvent());
        indexMe.put("@when", auditChange.getWhen());
        indexMe.put("@timestamp", new Date(auditChange.getSysWhen())); // Kibana should be able to search on this
        // https://github.com/monowai/auditbucket/issues/21

        indexMe.put("@fortress", auditChange.getFortressName());
        indexMe.put("@docType", auditChange.getDocumentType());
        indexMe.put("@callerRef", auditChange.getCallerRef());
        indexMe.put("@tags", auditChange.getTagValues());

        return indexMe;
    }

    /**
     * @param auditChange object containing changes
     * @return key value of the child document
     */
    public SearchChange save(SearchChange auditChange) {
        String indexName = auditChange.getIndexName();
        String documentType = auditChange.getDocumentType();


        //Map<String, Object> indexMe = makeIndexDocument(auditChange);
        String source = makeIndexJson(auditChange);
        IndexResponse ir = esClient.prepareIndex(indexName, documentType)
                .setSource(source)
                .setRouting(auditChange.getAuditKey())
                .execute()
                .actionGet();

        auditChange.setSearchKey(ir.getId());
        if (log.isDebugEnabled())
            log.debug("Added Document [" + ir.getId() + "] to " + indexName + "/" + documentType);
        return auditChange;

    }

    @Override
    public void delete(AuditHeader header, String existingIndexKey) {
        String indexName = header.getIndexName();
        String recordType = header.getDocumentType();

        if (existingIndexKey == null)
            existingIndexKey = header.getSearchKey();

        DeleteResponse dr = esClient.prepareDelete(indexName, recordType, existingIndexKey)
                .setRouting(header.getAuditKey())
                .execute()
                .actionGet();

        if (log.isDebugEnabled()) {
            if (dr.isNotFound())
                log.debug("Didn't find the document to remove [" + existingIndexKey + "] from " + indexName + "/" + recordType);
            else
                log.debug("Removed document [" + existingIndexKey + "] from " + indexName + "/" + recordType);
        }

    }

    @Override
    public Map<String, Object> ping() {
        Map<String, Object> results = new HashMap<String, Object>();
        ClusterHealthRequest request = new ClusterHealthRequest();
        ClusterHealthResponse response = esClient.admin().cluster().health(request).actionGet();
        if (response == null) {
            results.put("status", "error!");
            return results;
        }
        results.put("abStatus", "ok");
        results.put("health", response.getStatus().name());
        results.put("dataNodes", response.getNumberOfDataNodes());
        results.put("nodes", response.getNumberOfNodes());
        results.put("clusterName", response.getClusterName());

        return results;
    }

    @Override
    public void update(SearchChange incoming) {

        String source = makeIndexJson(incoming);

        GetResponse response =
                esClient.prepareGet(incoming.getIndexName(),
                        incoming.getDocumentType(),
                        incoming.getSearchKey())
                        .setRouting(incoming.getAuditKey())
                        .execute()
                        .actionGet();
        if (response.isExists() && !response.isSourceEmpty()) {
            // Messages can be received out of sequence
            // Check to ensure we don't accidentally overwrite a more current
            // document with an older one. We assume the calling fortress understands
            // what the most recent doc is.
            Object o = response.getSource().get("@when"); // Users view of WHEN, not AuditBuckets!
            if (o != null) {
                Long existingWhen = (Long) o;
                if (existingWhen > incoming.getWhen())
                    return; // Don't overwrite the most current doc!
            }
        }

        // Update the existing document with the incoming change
        IndexRequestBuilder update = esClient
                .prepareIndex(incoming.getIndexName(), incoming.getDocumentType(), incoming.getSearchKey())
                .setRouting(incoming.getAuditKey());

        // ToDo: Do we care about waiting for the response? I doubt it.
        IndexResponse ur = update.setSource(source).
                execute().
                actionGet();

        if (log.isDebugEnabled())
            log.debug("Updated [" + incoming.getSearchKey() + "] for " + incoming + " to version " + ur.getVersion());

    }

    public byte[] findOne(AuditHeader header) {
        return findOne(header, null);
    }

    public byte[] findOne(AuditHeader header, String id) {
        String indexName = header.getIndexName();
        String documentType = header.getDocumentType();
        if (id == null)
            id = header.getSearchKey();
        if (log.isDebugEnabled())
            log.debug("Looking for [" + id + "] in " + indexName + "/" + documentType);

        GetResponse response = esClient.prepareGet(indexName, documentType, id)
                .setRouting(header.getAuditKey())
                .execute()
                .actionGet();

        if (response != null && response.isExists() && !response.isSourceEmpty())
            return response.getSourceAsBytes();

        log.info("Unable to find response data for [" + id + "] in " + indexName + "/" + documentType);
        return null;
    }

}
