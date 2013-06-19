package com.auditbucket.audit.dao;

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;

/**
 * User: mike
 * Date: 26/04/13
 * Time: 12:26 PM
 */
public interface IAuditChangeDao {
    IAuditChange save(IAuditChange auditChange);

    /**
     * generally for unit testing. Prefer to use the IAuditLog version.
     * This will locate a document in the child index, i.e. source system What information
     *
     * @param id Lucene Key
     * @return found audit change or null if none
     */
    byte[] findOne(IAuditHeader header, String id);

    void delete(IAuditHeader header, String existingIndexKey);

}
