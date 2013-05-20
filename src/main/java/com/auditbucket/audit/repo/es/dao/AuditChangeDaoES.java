package com.auditbucket.audit.repo.es.dao;

import com.auditbucket.audit.dao.IAuditChangeDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.repo.es.model.AuditChange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetResponse;
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
    private Client client;

    ObjectMapper om = new ObjectMapper();

    private Log log = LogFactory.getLog(AuditChangeDaoES.class);

    public String save(IAuditChange auditChange) {

        try {

            if (log.isTraceEnabled())
                log.trace("Saving " + auditChange.getIndexName() + "/" + auditChange.getDataType());
            // Look at index structure. Company:Fortress, TableType
            IndexResponse ir =
                    client.prepareIndex(auditChange.getIndexName(), auditChange.getDataType())
                            .setSource(om.writeValueAsString(auditChange))
                            .execute()
                            .actionGet();

            if (log.isDebugEnabled())
                log.trace("Saved [" + ir.id() + "] to " + auditChange.getIndexName());

            String parent = ir.id();

            client.prepareIndex(auditChange.getIndexName(), auditChange.getDataType())
                    .setSource(auditChange.getWhat())
                    .setParent(parent)
                    .execute()
                    .actionGet();

            return parent;
        } catch (IOException e) {
            log.fatal("*** Error saving [" + auditChange.getIndexName() + "], [" + auditChange.getDataType() + "]", e);
        }

        return null;


    }

    @Override
    public IAuditChange findOne(IAuditLog auditLog) {
        String indexName = auditLog.getHeader().getIndexName();
        String recordType = auditLog.getHeader().getDataType();
        String id = auditLog.getKey();
        return findOne(indexName, recordType, id);

    }

    @Override
    public IAuditChange findOne(String indexName, String recordType, String id) {
        try {
            if (log.isTraceEnabled())
                log.trace("Looking for " + indexName + "/" + recordType + "/" + id);

            GetResponse response = client.prepareGet(indexName, recordType, id)
                    .execute()
                    .actionGet();
            IAuditChange ac = convert(response);
            if (log.isTraceEnabled())
                log.trace("Found! " + response.id());
            return ac;
        } catch (IOException e) {
            log.fatal("*** Error saving [" + indexName + "], [" + recordType + "]", e);
        }
        return null;
    }

    @Override
    public void delete(String indexName, String recordType, IAuditLog auditLog) {
        client.prepareDelete(indexName, recordType, auditLog.getKey())
                .execute()
                .actionGet();

    }

    private IAuditChange convert(GetResponse response) throws IOException {
        if (response == null)
            throw new IllegalArgumentException("Response does not exist for NULL");
        if (response.isSourceEmpty())
            return null;
        IAuditChange ac = om.readValue(response.getSourceAsBytes(), AuditChange.class);
        if (ac != null)
            ac.setId(response.id());
        return ac;

    }


}
