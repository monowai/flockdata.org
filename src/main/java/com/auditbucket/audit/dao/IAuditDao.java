package com.auditbucket.audit.dao;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.ITagRef;
import com.auditbucket.registration.model.ICompany;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
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

    public ITagRef save(ITagRef tagRef);

    public Set<IAuditLog> findByTag(@NotNull String txName, @NotNull ICompany company);

    /**
     * @param key GUID
     * @return AuditHeader in "default" state
     */
    public IAuditHeader findHeader(String key);

    /**
     * @param key     GUID
     * @param inflate should all relationships be loaded
     * @return header in inflated or "default" state
     */
    public IAuditHeader findHeader(String key, boolean inflate);

    /**
     * @param id audit Header PK
     * @return count of log records for the PK
     */
    public int getLogCount(Long id);

    public IAuditLog getLastChange(Long auditHeaderID);

    Set<IAuditLog> getAuditLogs(Long headerKey, Date from, Date to);

    Set<IAuditLog> getAuditLogs(Long auditHeaderID);

    void delete(IAuditLog auditLog);

    void delete(IAuditHeader auditHeader);

    IAuditHeader findHeaderByClientRef(String clientRef, String fortressName, String companyName);

    public void removeLastChange(IAuditHeader header);

    IAuditHeader fetch(IAuditHeader header);
}
