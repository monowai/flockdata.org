/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.repo.neo4j.dao;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.IAuditWhen;
import com.auditbucket.audit.model.ITxRef;
import com.auditbucket.bean.AuditTXResult;
import com.auditbucket.dao.IAuditDao;
import com.auditbucket.engine.repo.neo4j.AuditHeaderRepo;
import com.auditbucket.engine.repo.neo4j.AuditLogRepo;
import com.auditbucket.engine.repo.neo4j.model.AuditHeader;
import com.auditbucket.engine.repo.neo4j.model.AuditLog;
import com.auditbucket.engine.repo.neo4j.model.AuditWhen;
import com.auditbucket.engine.repo.neo4j.model.TxRef;
import com.auditbucket.registration.model.ICompany;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * User: Mike Holdsworth
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
    private Logger log = LoggerFactory.getLogger(AuditDaoNeo.class);

    @Override
    public IAuditHeader save(IAuditHeader auditHeader) {
        auditHeader.bumpUpdate();
        return auditRepo.save((AuditHeader) auditHeader);
    }

    @Override
    public IAuditLog save(IAuditLog auditLog) {
        return template.save((AuditLog) auditLog);
    }

    public ITxRef save(ITxRef tagRef) {
        return template.save((TxRef) tagRef);
    }


    public IAuditHeader findHeader(String key) {
        return findHeader(key, false);
    }

    @Override
    public IAuditHeader findHeader(String key, boolean inflate) {
        IAuditHeader header = auditRepo.findByUID(key);
        if (inflate) {
            fetch(header);
        }
        return header;
    }

    public IAuditHeader findHeaderByCallerRef(Long fortressId, @NotNull String documentType, @NotNull String callerRef) {
        if (log.isDebugEnabled())
            log.debug("findByCallerRef fortress [" + fortressId + "] docType[" + documentType + "], callerRef[" + callerRef.toLowerCase() + "]");
        // This is pretty crappy, but Neo4J will throw an exception the first time you try to search if no index is in place.
        if (template.getGraphDatabaseService().index().existsForNodes("callerRef"))
            return auditRepo.findByCallerRef(fortressId, documentType, callerRef.toLowerCase());

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
        template.fetch(header.getFortress());
        template.fetch(header.getTagValues());

        return header;
    }

    @Override
    public ITxRef findTxTag(@NotEmpty String userTag, @NotNull ICompany company, boolean fetchHeaders) {
        ITxRef txRef = auditRepo.findTxTag(userTag, company.getId());
        return txRef;
    }


    @Override
    public ITxRef beginTransaction(String id, ICompany company) {

        ITxRef tag = findTxTag(id, company, false);
        if (tag == null) {
            tag = new TxRef(id, company);
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

    public IAuditWhen getChange(Long auditHeaderID, long sysWhen) {
        IAuditWhen log = auditLogRepo.getChange(auditHeaderID, sysWhen);
        return log;
    }


    public Set<IAuditLog> getAuditLogs(Long auditLogID, Date from, Date to) {
        return auditLogRepo.getAuditLogs(auditLogID, from.getTime(), to.getTime());
    }

    public Set<IAuditLog> getAuditLogs(Long auditHeaderID) {
        return auditLogRepo.findAuditLogs(auditHeaderID);
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

    public Map<String, Object> findByTransaction(ITxRef txRef) {
        //Example showing how to use cypher and extract

        String findByTagRef = "start tag =node({txRef}) " +
                "              match tag-[:txIncludes]->auditLog<-[logs:logged]-audit " +
                "             return logs, audit, auditLog " +
                "           order by logs.sysWhen";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("txRef", txRef.getId());

        Iterator<Map<String, Object>> rows;
        Result<Map<String, Object>> exResult = template.query(findByTagRef, params);

        rows = exResult.iterator();

        List<AuditTXResult> simpleResult = new ArrayList<AuditTXResult>();
        int i = 1;
        //Result<Map<String, Object>> results =
        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            IAuditWhen when = template.convert(row.get("logs"), AuditWhen.class);
            IAuditLog log = template.convert(row.get("auditLog"), AuditLog.class);
            IAuditHeader audit = template.convert(row.get("audit"), AuditHeader.class);
            simpleResult.add(new AuditTXResult(audit, log, when));
            i++;

        }
        Map<String, Object> result = new HashMap<String, Object>(i);
        result.put("txRef", txRef.getName());
        result.put("logs", simpleResult);

        return result;
    }

    @Override
    public void addChange(IAuditHeader header, IAuditLog al, DateTime dateWhen) {
        AuditWhen aWhen = new AuditWhen(header, al);
        template.save(aWhen);
        header.getAuditKey();

    }

    @Override
    public void save(IAuditWhen log) {
        template.save((AuditWhen) log);
    }
}
