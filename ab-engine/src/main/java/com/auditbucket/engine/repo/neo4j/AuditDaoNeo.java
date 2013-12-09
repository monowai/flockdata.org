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

package com.auditbucket.engine.repo.neo4j;

import com.auditbucket.audit.model.*;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.bean.AuditTXResult;
import com.auditbucket.dao.AuditDao;
import com.auditbucket.engine.repo.neo4j.model.*;
import com.auditbucket.helper.AuditException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.FortressUser;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
public class AuditDaoNeo implements AuditDao {
    private static final String LAST_CHANGE = "LAST_CHANGE";
    @Autowired
    AuditHeaderRepo auditRepo;

    @Autowired
    AuditLogRepo auditLogRepo;

    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(AuditDaoNeo.class);

    @Override
    @Caching(evict = {@CacheEvict(value = "auditHeaderId", key = "#p0.id"),
            @CacheEvict(value = "auditKey", key = "#p0.auditKey")})
    public AuditHeader save(AuditHeader auditHeader) {
        auditHeader.bumpUpdate();
        return auditRepo.save((AuditHeaderNode) auditHeader);
    }

    public TxRef save(TxRef tagRef) {
        return template.save((TxRefNode) tagRef);
    }

    @Cacheable(value = "auditKey")
    private AuditHeader getCachedHeader(String key) {
        return auditRepo.findByUID(key);
    }

    @Override
    public AuditHeader findHeader(String key, boolean inflate) {
        AuditHeader header = getCachedHeader(key);
        if (inflate) {
            fetch(header);
        }
        return header;
    }

    @Cacheable(value = "auditCallerKey", unless = "#result==null")
    public AuditHeader findHeaderByCallerRef(Long fortressId, @NotNull Long documentId, @NotNull String callerRef) {
        if (logger.isTraceEnabled())
            logger.trace("findByCallerRef fortress [" + fortressId + "] docType[" + documentId + "], callerRef[" + callerRef.toLowerCase() + "]");

        // This is pretty crappy, but Neo4J will throw an exception the first time you try to search if no index is in place.
        if (template.getGraphDatabaseService().index().existsForNodes("callerRef")) {
            String keyToFind = "" + fortressId + "." + documentId + "." + callerRef.toLowerCase();
            return auditRepo.findByCallerRef(keyToFind);
        }

        return null;
    }

    @Cacheable(value = "auditHeaderId", key = "p0.id")
    public AuditHeader fetch(AuditHeader header) {
        template.fetch(header.getCreatedBy());
        template.fetch(header.getLastUser());

        return header;
    }

    @Override
    public void fetch(AuditWhat what) {
        template.fetch(what);
    }

    @Override
    public Set<AuditHeader> findHeadersByTxRef(Long txRef) {
        return auditRepo.findHeadersByTxRef(txRef);
    }

    @Override
    public Set<AuditHeader> findHeaders(Long fortressId, Long skipTo) {
        return auditRepo.findHeadersFrom(fortressId, skipTo);
    }

    @Override
    public Set<AuditHeader> findHeaders(Long fortressId, Long docTypeId, Long skipTo) {

        return auditRepo.findHeadersFrom(fortressId, docTypeId, skipTo);
    }

    @Override
    public TxRef findTxTag(@NotEmpty String userTag, @NotNull Company company, boolean fetchHeaders) {
        return auditRepo.findTxTag(userTag, company.getId());
    }


    @Override
    public TxRef beginTransaction(String id, Company company) {

        TxRef tag = findTxTag(id, company, false);
        if (tag == null) {
            tag = new TxRefNode(id, company);
            template.save(tag);
        }
        return tag;
    }

    @Override
    public int getLogCount(Long id) {
        return auditLogRepo.getLogCount(id);
    }

    public AuditLog getLastAuditLog(Long auditHeaderID) {
        AuditLog when = auditLogRepo.getLastAuditLog(auditHeaderID);
        if (when != null) {
            //template.fetch(when.getAuditChange());
            logger.trace("Last Change {}", when);
        }

        return when;
    }

    public Set<AuditLog> getAuditLogs(Long auditLogID, Date from, Date to) {
        return auditLogRepo.getAuditLogs(auditLogID, from.getTime(), to.getTime());
    }

    public Set<AuditLog> getAuditLogs(Long auditHeaderID) {
        return auditLogRepo.findAuditLogs(auditHeaderID);
    }

    public Map<String, Object> findByTransaction(TxRef txRef) {
        //Example showing how to use cypher and extract

        String findByTagRef = "start tag =node({txRef}) " +
                "              match tag-[:AFFECTED]->auditLog<-[logs:LOGGED]-audit " +
                "             return logs, audit, auditLog " +
                "           order by logs.sysWhen";
        Map<String, Object> params = new HashMap<>();
        params.put("txRef", txRef.getId());


        Result<Map<String, Object>> exResult = template.query(findByTagRef, params);

        Iterator<Map<String, Object>> rows;
        rows = exResult.iterator();

        List<AuditTXResult> simpleResult = new ArrayList<>();
        int i = 1;
        //Result<Map<String, Object>> results =
        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            AuditLog log = template.convert(row.get("logs"), AuditLogRelationship.class);
            AuditChange change = template.convert(row.get("auditLog"), AuditChangeNode.class);
            AuditHeader audit = template.convert(row.get("audit"), AuditHeaderNode.class);
            simpleResult.add(new AuditTXResult(audit, change, log));
            i++;

        }
        Map<String, Object> result = new HashMap<String, Object>(i);
        result.put("txRef", txRef.getName());
        result.put("logs", simpleResult);

        return result;
    }

    @Override
    public AuditLog addLog(AuditHeader header, AuditChange al, DateTime fortressWhen) {
        return template.save(new AuditLogRelationship(header, al, fortressWhen));

    }

    public AuditLog save(AuditLog log) {
        return template.save((AuditLogRelationship) log);
    }

    @Override
    public String ping() {


        Map<String, Object> ab = new HashMap<>();
        ab.put("name", "AuditBucket");
        Node abNode = template.getGraphDatabase().getOrCreateNode("system", "name", "AuditBucket", ab);
        if (abNode == null) {
            return "Neo4J has problems";
        }
        return "Neo4J is OK";
    }

    @Override
    public AuditChange save(FortressUser fUser, AuditLogInputBean input) {
        return save(fUser, input, null, null);
    }

    @Override
    public AuditChange save(FortressUser fUser, AuditLogInputBean input, TxRef txRef, AuditChange previousChange) {
        AuditChange auditChange = new AuditChangeNode(fUser, input, txRef);
        auditChange.setEvent(input.getAuditEvent());
        auditChange.setPreviousChange(previousChange);
        return template.save(auditChange);
    }

    @Override
    public AuditHeader create(AuditHeaderInputBean inputBean, FortressUser fu, DocumentType documentType) throws AuditException {
        // AuditHeader ah = findHeaderByCallerRef(fu.getFortress().getId(), documentType.getId(), inputBean.getCallerRef());
        AuditHeader auditHeader = new AuditHeaderNode(inputBean.getAuditKey(), fu, inputBean, documentType);
        if (inputBean.isTrackSuppressed()) {
            auditHeader.setAuditKey(null);
            return auditHeader;
        } else
            return save(auditHeader);
    }

    @Override
    @Cacheable(value = "auditLog")
    public AuditLog getLog(Long logId) {
        Relationship change = template.getRelationship(logId);
        if (change != null)
            return (AuditLog) template.getDefaultConverter().convert(change, AuditLogRelationship.class);
        return null;
    }

    @Override
    public AuditWhat getWhat(Long whatId) {
        return template.findOne(whatId, AuditWhatNode.class);
    }

    @Override
    public AuditHeader getHeader(Long id) {
        return template.findOne(id, AuditHeaderNode.class);
    }

    @Override
    public AuditChange fetch(AuditChange lastChange) {
        return template.fetch(lastChange);
    }

    @Override
    public AuditLog getLastLog(Long headerId) {
        return auditLogRepo.getLastAuditLog(headerId);
    }


    @Override
    public void setLastChange(AuditHeader auditHeader, AuditChange toAdd, AuditChange toRemove) {

        Node header = template.getPersistentState(auditHeader);
        if (toRemove != null) {
            Node previous = template.getPersistentState(toRemove);
            logger.debug("Audit [{}], removing relationship [{}]", auditHeader.getId(), previous.getId());
            template.deleteRelationshipBetween(header, previous, LAST_CHANGE);
        }
        Node auditChange = template.getPersistentState(toAdd);
        template.createRelationshipBetween(header, auditChange, LAST_CHANGE, null);
        if (toRemove != null)
            logger.debug("Audit [{}], replaced Last Change relationship [{}] with [{}]", auditHeader.getId(), toRemove.getId(), auditChange.getId());
    }

    @Override
    public String save(AuditChange change, String jsonText, int version) {
        AuditWhatNode what = new AuditWhatNode(version);
        change.setWhat(what);
        change = template.save(change);
        return change.getWhat().getId();
    }

    @Override
    public AuditChange save(AuditChange change, Boolean compressed, int version) {
        AuditWhatNode what = new AuditWhatNode(version);
        what.setCompressed(compressed);
        change.setWhat(what);
        change = template.save(change);
        return change;
    }

}
