package com.auditbucket.audit.dao;

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;

/**
 * User: mike
 * Date: 26/04/13
 * Time: 12:26 PM
 */
public interface IAuditChangeDao {
    String save(IAuditChange auditChange);

    /**
     * generally for unit testing. Prefer to use the IAuditLog version.
     *
     * @param indexKey   Lucene Index to use
     * @param recordType Record type
     * @param id         Lucene Key
     * @return found audit change or null if none
     */
    byte[] findOne(String indexKey, String recordType, String id);

    void delete(String indexName, String recordType, String indexKey);

    void update(String existingKey, IAuditHeader header, String what);
}
