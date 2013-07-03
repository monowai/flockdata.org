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

package com.auditbucket.audit.repo.es.dao;

import com.auditbucket.audit.dao.IAuditSearchDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * User: mike
 * Date: 26/04/13
 * Time: 12:00 PM
 */
@Repository("esAuditChange")
public class AuditSearchDaoES implements IAuditSearchDao {
    @Autowired
    private Client esClient;

    private Logger log = LoggerFactory.getLogger(AuditSearchDaoES.class);

    /**
     * @param auditChange object containing changes
     * @return key value of the child document
     */
    public IAuditChange save(IAuditChange auditChange) {
        String indexName = auditChange.getIndexName();
        String documentType = auditChange.getDocumentType();


        Map<String, Object> indexMe = getIndexDocument(auditChange);

        IndexResponse ir = esClient.prepareIndex(indexName, documentType)
                .setSource(indexMe)
                .setRouting(auditChange.getRoutingKey())
                .execute()
                .actionGet();

        auditChange.setSearchKey(ir.getId());
        if (log.isDebugEnabled())
            log.debug("Added Document [" + ir.getId() + "] to " + indexName + "/" + documentType);
        return auditChange;

    }

    private Map<String, Object> getIndexDocument(IAuditChange auditChange) {
        Map<String, Object> indexMe = auditChange.getWhat();
        indexMe.put("auditKey", auditChange.getAuditKey());
        indexMe.put("who", auditChange.getWho());
        indexMe.put("docType", auditChange.getDocumentType());
        return indexMe;
    }

    @Override
    public void delete(IAuditHeader header, String existingIndexKey) {
        String indexName = header.getIndexName();
        String recordType = header.getDocumentType();

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
    public void update(IAuditChange change) {

        Map<String, Object> indexMe = getIndexDocument(change);

        IndexRequestBuilder update = esClient
                .prepareIndex(change.getIndexName(), change.getDocumentType(), change.getSearchKey())
                .setRouting(change.getRoutingKey())
                .setOperationThreaded(false);

        IndexResponse ur = update.setSource(indexMe).
                execute().
                actionGet();


        if (log.isDebugEnabled())
            log.debug("Updated [" + change.getSearchKey() + "] for " + change + " to version " + ur.getVersion());

    }

    public byte[] findOne(IAuditHeader header) {
        return findOne(header, null);
    }

    public byte[] findOne(IAuditHeader header, String id) {
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
