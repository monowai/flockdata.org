package com.auditbucket.audit.service;

import com.auditbucket.audit.dao.IAuditChangeDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.repo.es.model.AuditChange;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * User: mike
 * Date: 18/06/13
 * Time: 2:03 PM
 */
@Service
public class AuditSearchService {
    @Autowired
    private IAuditChangeDao auditChange;


    void updateSearchableChange(IAuditHeader header, String existingKey, DateTime dateWhen, String what) {
        if (header.getFortress().isIgnoreSearchEngine())
            return;
        if (existingKey != null)
            auditChange.update(header, existingKey, what);
        else
            // Only happen if the fortress was previously not creating searchable key values
            createSearchableChange(header, dateWhen, what, "Create");
    }

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

    public void delete(IAuditHeader auditHeader, String key) {
        auditChange.delete(auditHeader, key);

    }

    public void update(IAuditHeader auditHeader, String key, String what) {
        auditChange.update(auditHeader, key, what);

    }
}
