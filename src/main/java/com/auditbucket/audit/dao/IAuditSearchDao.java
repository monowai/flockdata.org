package com.auditbucket.audit.dao;

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;

/**
 * User: mike
 * Date: 26/04/13
 * Time: 12:26 PM
 */
public interface IAuditSearchDao {
    IAuditChange save(IAuditChange auditChange);

    void update(IAuditChange auditChange);

    /**
     * Locates a specific key monitored by the header.
     * <p/>
     * If ID is null then the call is the same as findOne(header)
     * where the searchKey is taken to be AuditHeader.searchKey
     *
     * @return found audit change or null if none
     */
    byte[] findOne(IAuditHeader header, String id);

    /**
     * locates a document by AuditHeader.searchKey
     *
     * @param header auditHeader
     * @return document context as bytes
     */
    public byte[] findOne(IAuditHeader header);

    void delete(IAuditHeader header, String existingIndexKey);

}
