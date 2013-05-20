package com.auditbucket.audit.repo.es.dao;

import com.auditbucket.audit.dao.IAuditQueryDao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: mike
 * Date: 28/04/13
 * Time: 2:23 PM
 */
@Repository
public class AuditQueryDaoES implements IAuditQueryDao {
    private Log log = LogFactory.getLog(AuditQueryDaoES.class);

    @Autowired
    private Client client;

    @Override
    public long getHitCount(String index){
        SearchResponse response = client.prepareSearch(index)
                .execute()
                .actionGet();
        return response.getHits().getTotalHits();

    }

}
