package com.auditbucket.audit.repo.neo4j.dao;

import com.auditbucket.audit.dao.IAuditDao;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.repo.neo4j.AuditHeaderRepo;
import com.auditbucket.audit.repo.neo4j.AuditLogRepo;
import com.auditbucket.audit.repo.neo4j.model.AuditHeader;
import com.auditbucket.audit.repo.neo4j.model.AuditLog;
import com.auditbucket.registration.model.IFortressUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;

/**
 * User: mike
 * Date: 21/04/13
 * Time: 8:00 PM
 */
@Repository("auditDAO")
public class AuditDaoNeo implements IAuditDao {
    @Autowired
    AuditHeaderRepo auditRepo;

    @Autowired
    AuditLogRepo auditLogRepo;

    @Autowired
    Neo4jTemplate template;


    @Override
    public IAuditHeader save(IAuditHeader auditHeader) {
        auditHeader.bumpUpdate();
        return auditRepo.save((AuditHeader) auditHeader);
    }

    @Override
    public IAuditLog save(IAuditLog auditLog) {

        return auditLogRepo.save((AuditLog) auditLog);
    }

    @Override
    public IAuditHeader findHeader(String key) {
        return auditRepo.findByPropertyValue(AuditHeader.UUID_KEY, key);
    }

    public IAuditHeader findHeaderByClientRef(@NotNull String clientRef, @NotNull String fortressName, @NotNull String companyName) {

        // This is pretty crappy, but Neo4J will throw an exception the first time you try to search if no index is in place.
        if (template.getGraphDatabaseService().index().existsForNodes("clientRef"))
            return auditRepo.findByClientRef(clientRef.toLowerCase(), fortressName, companyName);

        return null;
    }

    @Override
    public void removeLastChange(IAuditHeader header, IFortressUser fu) {
        // Remove the lastChange relationship
        template.deleteRelationshipBetween(header, fu, "lastChangedBy");
        //auditLogRepo.delete(lastChange);
    }

    @Override
    public int getLogCount(Long id) {
        return auditLogRepo.getLogCount(id);
    }

    public IAuditLog getLastChange(Long auditHeaderID) {
        return auditLogRepo.getLastChange(auditHeaderID);
    }

    public Set<IAuditLog> getAuditLogs(Long auditLogID, Date from, Date to) {
        return auditLogRepo.getAuditLogs(auditLogID, from.getTime(), to.getTime());
    }

    public Set<IAuditLog> getAuditLogs(Long auditLogID) {
        return auditLogRepo.getAuditLogs(auditLogID);
    }

    @Override
    public void delete(IAuditLog auditLog) {
        auditLogRepo.delete((AuditLog) auditLog);
    }

    @Override
    public void delete(IAuditHeader auditHeader) {
        auditRepo.delete((AuditHeader) auditHeader);
    }

}
