package com.auditbucket.audit.repo.es.dao;

import com.auditbucket.audit.dao.IAuditChangeDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
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
public class AuditChangeDaoES implements IAuditChangeDao {
    @Autowired
    private Client esClient;

    private Logger log = LoggerFactory.getLogger(AuditChangeDaoES.class);

    /**
     * @param auditChange object containing changes
     * @return key value of the child document
     */
    public IAuditChange save(IAuditChange auditChange) {
        String indexName = auditChange.getIndexName();
        String documentType = auditChange.getDocumentType();


        Map<String, Object> indexMe = auditChange.getWhat();
        indexMe.put("auditKey", auditChange.getAuditKey());
        indexMe.put("who", auditChange.getWho());

        IndexResponse ir = esClient.prepareIndex(indexName, documentType)
                .setSource(indexMe)
                .setRouting(auditChange.getAuditKey())
                .execute()
                .actionGet();

        auditChange.setSearchKey(ir.getId());
        if (log.isDebugEnabled())
            log.debug("Added Document [" + ir.getId() + "] to " + indexName + "/" + documentType);
        return auditChange;

    }

    @Override
    public void delete(IAuditHeader header, String existingIndexKey) {
        String indexName = header.getIndexName();
        String recordType = header.getDocumentType();

//        if ( findOne(header, existingIndexKey)== null)
//            return;
//
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
        String documentType = header.getDocumentType();
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
