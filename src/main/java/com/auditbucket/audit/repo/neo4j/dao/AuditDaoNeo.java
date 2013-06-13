package com.auditbucket.audit.repo.neo4j.dao;

import com.auditbucket.audit.dao.IAuditDao;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.ITagRef;
import com.auditbucket.audit.repo.neo4j.AuditHeaderRepo;
import com.auditbucket.audit.repo.neo4j.AuditLogRepo;
import com.auditbucket.audit.repo.neo4j.model.AuditHeader;
import com.auditbucket.audit.repo.neo4j.model.AuditLog;
import com.auditbucket.audit.repo.neo4j.model.TagRef;
import com.auditbucket.registration.model.ICompany;
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
        return template.save((AuditLog) auditLog);
    }

    public ITagRef save(ITagRef tagRef) {
        return template.save((TagRef) tagRef);
    }

    @Override
    public Set<IAuditLog> findByTag(@NotNull String tagName, @NotNull ICompany company) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public IAuditHeader findHeader(String key) {
        return findHeader(key, false);
    }

    @Override
    public IAuditHeader findHeader(String key, boolean inflate) {
        IAuditHeader header = auditRepo.findByPropertyValue(AuditHeader.UUID_KEY, key);
        if (inflate) {
            template.fetch(header);
            template.fetch(header.getTags());
        }
        return header;
    }

    public IAuditHeader findHeaderByClientRef(@NotNull String clientRef, @NotNull String fortressName, @NotNull String companyName) {

        // This is pretty crappy, but Neo4J will throw an exception the first time you try to search if no index is in place.
        if (template.getGraphDatabaseService().index().existsForNodes("clientRef"))
            return auditRepo.findByClientRef(clientRef, fortressName, companyName);

        return null;
    }

    @Override
    public void removeLastChange(IAuditHeader header) {
        // Remove the lastChange relationship
        template.deleteRelationshipBetween(header, header.getLastUser(), "lastChangedBy");
    }

    @Override
    public IAuditHeader fetch(IAuditHeader header) {
        template.fetch(header);
        return header;
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

    public Set<IAuditLog> getAuditLogs(Long auditHeaderID) {
        IAuditHeader header = auditRepo.findOne(auditHeaderID);
        template.fetch(header);
        return header.getAuditLogs();
    }

    @Override
    public void delete(IAuditLog auditLog) {
        auditLogRepo.delete((AuditLog) auditLog);
    }

    @Override
    public void delete(IAuditHeader auditHeader) {
        //ToDo: Remove all the logs
        auditRepo.delete((AuditHeader) auditHeader);
    }

}
