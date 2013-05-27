package com.auditbucket.audit.dao;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Set;

/**
 * User: mike
 * Date: 21/04/13
 * Time: 7:56 PM
 */
@Repository
public interface IAuditDao {
    public IAuditHeader save(IAuditHeader auditHeader);

    public IAuditLog save(IAuditLog auditLog);

    public IAuditHeader findHeader(String key);

    /**
     * @param id audit Header PK
     * @return count of log records for the PK
     */
    public int getLogCount(Long id);

    public IAuditLog getLastChange(Long auditHeaderID);

    Set<IAuditLog> getAuditLogs(Long headerKey, Date from, Date to);

    Set<IAuditLog> getAuditLogs(Long auditLogID);

    void delete(IAuditLog auditLog);

    void delete(IAuditHeader auditHeader);

    IAuditHeader findHeaderByClientRef(String clientRef, String fortressName, String companyName);
}
