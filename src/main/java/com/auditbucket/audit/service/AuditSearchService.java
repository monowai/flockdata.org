package com.auditbucket.audit.service;

import com.auditbucket.audit.dao.IAuditChangeDao;
import com.auditbucket.audit.dao.IAuditQueryDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.repo.es.model.AuditChange;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User: mike
 * Date: 18/06/13
 * Time: 2:03 PM
 */
@Service
public class AuditSearchService {
    @Autowired
    private IAuditChangeDao auditChange;

    @Autowired
    private IAuditQueryDao auditQuery;


    public Long getHitCount(String index) {
        return auditQuery.getHitCount(index);
    }

    @Transactional
    void updateSearchableChange(IAuditHeader header, String existingKey, DateTime dateWhen, String what) {
        if (header.getFortress().isIgnoreSearchEngine())
            return;
        if (existingKey != null)
            auditChange.update(header, existingKey, what);
        else
            // Only happen if the fortress was previously not creating searchable key values
            createSearchableChange(header, dateWhen, what, "Create");
    }

    @Transactional
    IAuditChange createSearchableChange(IAuditHeader header, DateTime dateWhen, String what, String event) {
        if (header.getFortress().isIgnoreSearchEngine())
            return null;
        IAuditChange thisChange = new AuditChange(header);
        thisChange.setEvent(event);
        thisChange.setWhat(what);
        if (dateWhen != null)
            thisChange.setWhen(dateWhen.toDate());
        thisChange = auditChange.save(thisChange);
        return thisChange;
    }

    @Transactional
    public void delete(IAuditHeader auditHeader, String key) {
        auditChange.delete(auditHeader, key);

    }

    @Transactional
    public void update(IAuditHeader auditHeader, String key, String what) {
        auditChange.update(auditHeader, key, what);

    }
}
