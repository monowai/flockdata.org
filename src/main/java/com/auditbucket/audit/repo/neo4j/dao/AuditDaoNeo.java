package com.auditbucket.audit.repo.neo4j.dao;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.bean.AuditTXResult;
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
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.*;

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
    public IAuditHeader save(IAuditHeader auditHeader, AuditHeaderInputBean inputBean) {
        auditHeader.bumpUpdate();
        return auditRepo.save((AuditHeader) auditHeader);
    }

    @Override
    public IAuditHeader save(IAuditHeader auditHeader, String txTag) {
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


    public IAuditHeader findHeader(String key) {
        return findHeader(key, false);
    }

    @Override
    public IAuditHeader findHeader(String key, boolean inflate) {
        IAuditHeader header = auditRepo.findByPropertyValue(AuditHeader.UUID_KEY, key);
        if (inflate) {
            fetch(header);
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
        template.deleteRelationshipBetween(header, header.getLastUser(), "lastChanged");
    }

    @Override
    public IAuditHeader fetch(IAuditHeader header) {
        template.fetch(header);
        template.fetch(header.getTxTags());
        template.fetch(header.getFortress());

        return header;
    }

    @Override
    public ITagRef findTxTag(String userTag, ICompany company) {
        return auditRepo.findTxTag(userTag, company.getId());
    }

    @Override
    public IAuditHeader save(IAuditHeader auditHeader) {
        return save(auditHeader, (String) null);
    }

    @Override
    public ITagRef beginTransaction(String id, ICompany company) {

        ITagRef tag = findTxTag(id, company);
        if (tag == null) {
            tag = new TagRef(id, company);
            template.save(tag);
        }
        return tag;
    }

    @Override
    public int getLogCount(Long id) {
        return auditLogRepo.getLogCount(id);
    }

    public IAuditLog getLastChange(Long auditHeaderID) {
        IAuditLog log = auditLogRepo.getLastChange(auditHeaderID);
        if (log != null)
            template.fetch(log.getWho());
        return log;
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

    public Map<String, Object> findByTransactionx(ITagRef txRef) {
        //ExecutionEngine engine = new ExecutionEngine( template.getGraphDatabaseService());
        if (txRef.getHeaders().size() == 0)
            template.fetch(txRef);

        //template.fetch(txRef.getHeaders());
        if (txRef.getHeaders().size() == 0)
            return null;
        Collection<Long> keys = new ArrayList<Long>(10);
        for (IAuditHeader iAuditHeader : txRef.getHeaders()) {
            keys.add(iAuditHeader.getId());
        }
        EndResult<Map<String, Object>> logs = auditLogRepo.findLogs(keys, txRef.getName());
        Iterator<Map<String, Object>> rows = logs.iterator();

        List<AuditTXResult> simpleResult = new ArrayList<AuditTXResult>();
        int i = 1;
        //Result<Map<String, Object>> results =
        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            IAuditLog log = template.convert(row.get("log"), AuditLog.class);

            AuditTXResult aresult = new AuditTXResult(log);
            simpleResult.add(aresult);
            i++;
        }
        Map<String, Object> result = new HashMap<String, Object>(i);
        result.put("txRef", txRef.getName());
        result.put("logs", simpleResult);

        return result;
    }

    public Map<String, Object> findByTransaction(ITagRef txRef) {
        //Example showing how to use cypher and extract

        String findByTagRef = "start tag =node({txRef}) " +
                "              match tag-[:txIncludes]->audit<-[logs:changed]-fortressUser " +
                "              where logs.txRef! = tag.name " +
                "             return logs " +
                "           order by logs.sysWhen";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("txRef", txRef.getId());

        Iterator<Map<String, Object>> rows;
        Result<Map<String, Object>> exResult = template.query(findByTagRef, params);
        Map<Long, IAuditHeader> headers = new HashMap<Long, IAuditHeader>();

        rows = exResult.iterator();

        List<AuditTXResult> simpleResult = new ArrayList<AuditTXResult>();
        int i = 1;
        //Result<Map<String, Object>> results =
        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            IAuditLog log = template.convert(row.get("logs"), AuditLog.class);

            IAuditHeader audit = headers.get(log.getHeader().getId());

            if (audit == null) {
                audit = template.findOne(log.getHeader().getId(), AuditHeader.class);
                //template.fetch(audit.getFortress());
                headers.put(audit.getId(), audit);
            }
            simpleResult.add(new AuditTXResult(audit, log));
            //aResult[i] = new AuditTXResult(tag, audit, logs);
            i++;

        }
        Map<String, Object> result = new HashMap<String, Object>(i);
        result.put("txRef", txRef.getName());
        result.put("logs", simpleResult);

        return result;
    }
}
