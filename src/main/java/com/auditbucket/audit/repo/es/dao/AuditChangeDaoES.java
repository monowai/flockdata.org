package com.auditbucket.audit.repo.es.dao;

import com.auditbucket.audit.dao.IAuditChangeDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.repo.es.model.AuditChange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;

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
        try {
            String indexName = auditChange.getIndexName();
            String recordType = auditChange.getRecordType();
            if (log.isDebugEnabled())
                log.debug("Saving to " + indexName + "/" + recordType);

            IndexResponse ir = esClient.prepareIndex(indexName, recordType)
                    .setSource(om.writeValueAsString(auditChange))
                    .execute()
                    .actionGet();

            if (log.isDebugEnabled())
                log.debug("Added what [" + ir.getId() + "] to " + indexName);
            String parent = ir.getId();
            auditChange.setParent(parent);

            IndexResponse cr = esClient.prepareIndex(indexName, recordType)
                    .setSource(auditChange.getWhat())
                    .setParent(parent)
                    .execute()
                    .actionGet();

            auditChange.setChild(cr.getId());
            if (log.isDebugEnabled())
                log.debug("Wrote [" + cr.getId() + "] to " + indexName + "/" + recordType);
            return auditChange;
        } catch (IOException e) {
            log.fatal("*** Error saving [" + auditChange.getIndexName() + "], [" + auditChange.getRecordType() + "]", e);
        }

        return null;


    }

    @Override
    public byte[] findOne(String indexName, String recordType, String id) {
        if (log.isDebugEnabled())
            log.debug("Looking for [" + id + "] in " + indexName + "/" + recordType);

        GetResponse response = esClient.prepareGet(indexName, recordType, id)
                .execute()
                .actionGet();
        //IAuditChange ac = convert(response);
        if (response != null && response.isExists() && !response.isSourceEmpty())
            return response.getSourceAsBytes();

        return null;

    }

    @Override
    public void delete(String indexName, String recordType, String indexKey) {
        esClient.prepareDelete(indexName, recordType, indexKey)
                .execute()
                .actionGet();

    }

    @Override
    public void update(String existingKey, IAuditHeader header, String what) {
        delete(header.getIndexName(), header.getDataType(), existingKey);
        IndexRequestBuilder update = esClient.prepareIndex(header.getIndexName(), header.getDataType(), existingKey);
        IndexResponse ur = update.setSource(what).execute().actionGet();
        if (log.isDebugEnabled())
            log.debug("Updated [" + existingKey + "] for " + header + " to version " + ur.getVersion());
        //To what body of implemented methods use File | Settings | File Templates.
    }

    private IAuditChange convert(GetResponse response) throws IOException {
        if (response == null)
            throw new IllegalArgumentException("Response does not exist for NULL");
        if (response.isSourceEmpty())
            return null;
        IAuditChange ac = om.readValue(response.getSourceAsBytes(), AuditChange.class);
        if (ac != null)
            ac.setId(response.getId());
        return ac;

    }


}