package com.auditbucket.audit.service;

import com.auditbucket.audit.bean.SearchDocumentBean;
import com.auditbucket.audit.dao.IAuditChangeDao;
import com.auditbucket.audit.dao.IAuditQueryDao;
import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.repo.es.model.AuditChange;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Map;

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
    IAuditChange updateSearchableChange(IAuditHeader header, String existingKey, DateTime dateWhen, Map<String, Object> what, String event) {
        if (header.getFortress().isIgnoreSearchEngine())
            return null;
        if (existingKey != null)
            auditChange.delete(header, existingKey);

        return createSearchableChange(header, dateWhen, what, event);
    }

    @Transactional
    IAuditChange createSearchableChange(IAuditHeader header, DateTime dateWhen, Map<String, Object> what, String event) {
        if (header.getFortress().isIgnoreSearchEngine())
            return null;
        IAuditChange thisChange = new AuditChange(header, event, what);
        thisChange.setWho(header.getLastUser().getName());
        if (dateWhen != null)
            thisChange.setWhen(dateWhen.toDate());
        thisChange = auditChange.save(thisChange);
        return thisChange;
    }

    @Transactional
    public void delete(IAuditHeader auditHeader, @NotNull @NotEmpty String key) {
        auditChange.delete(auditHeader, key);

    }

    public IAuditChange createSearchableChange(SearchDocumentBean searchDocumentBean) {
        return createSearchableChange(searchDocumentBean.getAuditHeader(), searchDocumentBean.getDateTime(), searchDocumentBean.getWhat(), searchDocumentBean.getEvent());
    }
}
