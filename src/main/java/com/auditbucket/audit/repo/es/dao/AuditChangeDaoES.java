package com.auditbucket.audit.repo.es.dao;

import com.auditbucket.audit.dao.IAuditChangeDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.io.IOException;

/**
 * User: mike
 * Date: 26/04/13
 * Time: 12:00 PM
 */
@Repository("esAuditChange")
public class AuditChangeDaoES implements IAuditChangeDao {
    public static final String PARENT = "_parent";
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

            IndexResponse ir = esClient.prepareIndex(indexName, recordType + PARENT)
                    .setSource(om.writeValueAsBytes(auditChange))
                    .setRouting(auditChange.getName())
                    .execute()
                    .actionGet();

            if (log.isDebugEnabled())
                log.debug("Added parent [" + ir.getId() + "] to " + indexName + "/" + recordType + PARENT);
            String parent = ir.getId();
            auditChange.setParent(parent);

            IndexResponse cr = esClient.prepareIndex(indexName, recordType)
                    .setSource(auditChange.getWhat())
                    .setParent(parent)
                    .execute()
                    .actionGet();

            auditChange.setChild(cr.getId());
            if (log.isDebugEnabled())
                log.debug("Added child [" + cr.getId() + "] to " + indexName + "/" + recordType);
            return auditChange;
        } catch (IOException e) {
            log.fatal("*** Error saving [" + auditChange.getIndexName() + "], [" + auditChange.getRecordType() + "]", e);
        }

        return null;


    }

    public byte[] findOne(IAuditHeader header, String id) {
        return findOne(header, id, false);
    }

    @Override
    public byte[] findOne(IAuditHeader header, String id, boolean parent) {
        String indexName = header.getIndexName();
        String recordType = (parent ? header.getDataType() + PARENT : header.getDataType());
        if (log.isDebugEnabled())
            log.debug("Looking for [" + id + "] in " + indexName + "/" + recordType);

        GetResponse response = esClient.prepareGet(indexName, recordType, id)
                .setRouting(header.getUID())
                .execute()
                .actionGet();

        if (response != null && response.isExists() && !response.isSourceEmpty())
            return response.getSourceAsBytes();
        log.info("Unable to find response data for [" + id + "] in " + indexName + "/" + recordType);

        return null;

    }

    @Override
    public void delete(IAuditHeader header, String indexKey) {
        //ToDo: remove from the _parent index
        String indexName = header.getIndexName();
        String recordType = header.getDataType();
        if (log.isDebugEnabled())
            log.debug("Removing [" + indexKey + "] from " + indexName + "/" + recordType);

        esClient.prepareDelete(indexName, recordType, indexKey)
                .setRouting(header.getUID())
                .execute()
                .actionGet();

    }

    @Override
    public void update(IAuditHeader header, @NotNull @NotEmpty String existingKey, String what) {
        delete(header, existingKey);

        IndexRequestBuilder update = esClient.
                prepareIndex(header.getIndexName(), header.getDataType(), existingKey)
                .setRouting(header.getUID())
                .setOperationThreaded(false);

        IndexResponse ur = update.setSource(what).
                execute().
                actionGet();

        if (log.isDebugEnabled())
            log.debug("Updated [" + existingKey + "] for " + header + " to version " + ur.getVersion());

    }


}