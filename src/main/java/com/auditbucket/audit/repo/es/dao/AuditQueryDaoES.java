package com.auditbucket.audit.repo.es.dao;

import com.auditbucket.audit.dao.IAuditQueryDao;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: mike
 * Date: 28/04/13
 * Time: 2:23 PM
 */
@Repository
public class AuditQueryDaoES implements IAuditQueryDao {

    @Autowired
    private Client client;

    @Override
    public long getHitCount(String index) {
        SearchResponse response = client.prepareSearch(index)
                .execute()
                .actionGet();
        return response.getHits().getTotalHits();

    }

}
