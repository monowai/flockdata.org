package com.auditbucket.audit.repo.es.dao;

import com.auditbucket.audit.dao.IAuditChangeDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * User: mike
 * Date: 26/04/13
 * Time: 12:00 PM
 */
@Repository("esAuditChange")
public class AuditChangeDaoES implements IAuditChangeDao {
    @Autowired
    private Client esClient;

    ObjectMapper om = new ObjectMapper();

    private Log log = LogFactory.getLog(AuditChangeDaoES.class);

    /**
     * @param auditChange object containing changes
     * @return key value of the child document
     */
    public IAuditChange save(IAuditChange auditChange) {
        String indexName = auditChange.getIndexName();
        String recordType = auditChange.getRecordType();


        Map<String, Object> indexMe = auditChange.getWhat();
        indexMe.put("auditKey", auditChange.getName());
        indexMe.put("who", auditChange.getWho());

        IndexResponse ir = esClient.prepareIndex(indexName, recordType)
                .setSource(indexMe)
                .setRouting(auditChange.getName())
                .execute()
                .actionGet();

        auditChange.setSearchKey(ir.getId());
        if (log.isDebugEnabled())
            log.debug("Added Document [" + ir.getId() + "] to " + indexName + "/" + recordType);
        return auditChange;

    }

    @Override
    public void delete(IAuditHeader header, String existingIndexKey) {
        String indexName = header.getIndexName();
        String recordType = header.getDataType();

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

    public byte[] findOne(IAuditHeader header, String id) {
        String indexName = header.getIndexName();
        String recordType = header.getDataType();
        if (log.isDebugEnabled())
            log.debug("Looking for [" + id + "] in " + indexName + "/" + recordType);

        GetResponse response = esClient.prepareGet(indexName, recordType, id)
                .setRouting(header.getAuditKey())
                .execute()
                .actionGet();

        if (response != null && response.isExists() && !response.isSourceEmpty())
            return response.getSourceAsBytes();
        log.info("Unable to find response data for [" + id + "] in " + indexName + "/" + recordType);

        return null;
    }

}