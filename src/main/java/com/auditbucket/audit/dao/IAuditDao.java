package com.auditbucket.audit.dao;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.ITxRef;
import com.auditbucket.registration.model.ICompany;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * User: mike
 * Date: 21/04/13
 * Time: 7:56 PM
 */
@Repository
public interface IAuditDao {

    public IAuditHeader save(IAuditHeader auditHeader, AuditHeaderInputBean inputBean);

    public IAuditLog save(IAuditLog auditLog);

    public ITxRef save(ITxRef tagRef);

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

    IAuditHeader findHeaderByCallerRef(Long fortressId, String documentType, String callerRef);

    public void removeLastChange(IAuditHeader header);

    IAuditHeader fetch(IAuditHeader header);

    ITxRef findTxTag(String txTag, ICompany company, boolean fetchHeaders);

    IAuditHeader save(IAuditHeader auditHeader);

    public ITxRef beginTransaction(String id, ICompany company);

    public Map<String, Object> findByTransaction(ITxRef txRef);

    void addChange(IAuditHeader header, IAuditLog al, DateTime dateWhen);
}
