/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
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

import com.auditbucket.audit.bean.AuditTXResult;
import com.auditbucket.audit.bean.LogInputBean;
import com.auditbucket.audit.bean.MetaInputBean;
import com.auditbucket.audit.model.*;
import com.auditbucket.dao.TrackDao;
import com.auditbucket.engine.repo.LogWhatData;
import com.auditbucket.engine.repo.neo4j.model.ChangeLogNode;
import com.auditbucket.engine.repo.neo4j.model.MetaHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.TrackLogRelationship;
import com.auditbucket.engine.repo.neo4j.model.TxRefNode;
import com.auditbucket.engine.service.TrackEventService;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.FortressUser;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.neo4j.graphdb.*;
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
public class TrackDaoNeo implements TrackDao {
    @Autowired
    MetaDataRepo metaRepo;

    @Autowired
    TrackLogRepo trackLogRepo;

    @Autowired
    TrackEventService trackEventService;

    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(TrackDaoNeo.class);

    public MetaHeader save(MetaHeader metaHeader) {
        return save(metaHeader, null);
    }

    @Override
    @Caching(evict = {@CacheEvict(value = "headerId", key = "#p0.id"),
            @CacheEvict(value = "metaKey", key = "#p0.metaKey")}
    )
    public MetaHeader save(MetaHeader metaHeader, DocumentType documentType) {
        metaHeader.bumpUpdate();
        MetaHeader header = metaRepo.save((MetaHeaderNode) metaHeader);
        if (header != null && documentType != null) {
            Node node = template.getPersistentState(header);
            node.addLabel(DynamicLabel.label(documentType.getName()));
        }

        return header;
    }

    public TxRef save(TxRef tagRef) {
        return template.save((TxRefNode) tagRef);
    }

    @Cacheable(value = "metaKey", unless = "#result==null")
    private MetaHeader getCachedHeader(String key) {
        return metaRepo.findBySchemaPropertyValue(MetaHeaderNode.UUID_KEY, key);
    }

    @Override
    public MetaHeader findHeader(String key, boolean inflate) {
        MetaHeader header = getCachedHeader(key);
        if (inflate && header != null) {
            fetch(header);
        }
        return header;
    }

    @Cacheable(value = "callerKey", unless = "#result==null")
    public MetaHeader findHeaderByCallerKey(Long fortressId, Long documentId, String callerRef) {
        if (logger.isTraceEnabled())
            logger.trace("findByCallerRef fortress [" + fortressId + "] docType[" + documentId + "], callerRef[" + callerRef + "]");

        String keyToFind = "" + fortressId + "." + documentId + "." + callerRef;
        return metaRepo.findBySchemaPropertyValue("callerKeyRef", keyToFind);
    }

    @Cacheable(value = "headerId", key = "p0.id", unless = "#result==null")
    public MetaHeader fetch(MetaHeader header) {
        template.fetch(header.getCreatedBy());
        template.fetch(header.getLastUser());

        return header;
    }

    @Override
    public void fetch(LogWhat what) {
        template.fetch(what);
    }

    @Override
    public Set<MetaHeader> findHeadersByTxRef(Long txRef) {
        return metaRepo.findHeadersByTxRef(txRef);
    }

    @Override
    public Collection<MetaHeader> findHeaders(Long fortressId, Long skipTo) {
        return metaRepo.findHeadersFrom(fortressId, skipTo);
    }

    @Override
    public Collection<MetaHeader> findHeaders(Long fortressId, String label, Long skipTo) {
        //ToDo: Should this pass in timestamp it got to??
        String cypher = "match (f:Fortress)-[:TRACKS]->(audit:`" + label + "`) where id(f)={fortress} return audit ORDER BY audit.dateCreated ASC skip {skip} limit 100 ";
        Map<String, Object> args = new HashMap<>();
        args.put("fortress", fortressId);
        args.put("skip", skipTo);
        Result<Map<String, Object>> result = template.query(cypher, args);
        if (!((Result) result).iterator().hasNext())
            return new ArrayList<>();

        Iterator<Map<String, Object>> rows = result.iterator();

        Collection<MetaHeader> results = new ArrayList<>();

        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            results.add(template.projectTo(row.get("audit"), MetaHeaderNode.class));
        }
        //
        return results;
        //return metaRepo.findHeadersFrom(fortressId, docTypeId, skipTo);
    }

    @Override
    public void delete(ChangeLog currentChange) {
        trackLogRepo.delete((ChangeLogNode) currentChange);
    }

    @Override
    public TxRef findTxTag(@NotEmpty String txTag, @NotNull Company company, boolean fetchHeaders) {
        return metaRepo.findTxTag(txTag, company.getId());
    }


    @Override
    public TxRef beginTransaction(String id, Company company) {

        TxRef txTag = findTxTag(id, company, false);
        if (txTag == null) {
            txTag = new TxRefNode(id, company);
            template.save(txTag);
        }
        return txTag;
    }

    @Override
    public int getLogCount(Long id) {
        return trackLogRepo.getLogCount(id);
    }

    public Set<TrackLog> getLogs(Long auditLogID, Date from, Date to) {
        return trackLogRepo.getAuditLogs(auditLogID, from.getTime(), to.getTime());
    }

    public Set<TrackLog> getLogs(Long auditHeaderID) {
        return trackLogRepo.findAuditLogs(auditHeaderID);
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
            TrackLog log = template.convert(row.get("logs"), TrackLogRelationship.class);
            ChangeLog change = template.convert(row.get("auditLog"), ChangeLogNode.class);
            MetaHeader audit = template.convert(row.get("audit"), MetaHeaderNode.class);
            simpleResult.add(new AuditTXResult(audit, change, log));
            i++;

        }
        Map<String, Object> result = new HashMap<>(i);
        result.put("txRef", txRef.getName());
        result.put("logs", simpleResult);

        return result;
    }

    public TrackLog save(TrackLog log) {
        logger.debug("Saving audit log [{}] - ChangeLog ID [{}]", log, log.getChange().getId());
        return template.save((TrackLogRelationship) log);
    }

    @Override
    public String ping() {


//        Map<String, Object> ab = new HashMap<>();
//        ab.put("name", "AuditBucket");
//        ArrayList labels = new ArrayList();
//        labels.add("ABPing");
//        Node abNode = template.getGraphDatabase().getOrCreateNode("system", "name", "ab", "AuditBucket", ab, labels);
//        if (abNode == null) {
//            return "Neo4J has problems";
//        }
        return "Neo4J is OK";
    }

    @Override
    public ChangeLog save(FortressUser fUser, LogInputBean input) {
        return save(fUser, input, null, null);
    }

    @Override
    public ChangeLog save(FortressUser fUser, LogInputBean input, TxRef txRef, ChangeLog previousChange) {
        ChangeEvent event = trackEventService.processEvent(fUser.getFortress().getCompany(), input.getEvent());
        ChangeLog changeLog = new ChangeLogNode(fUser, input, txRef);
        changeLog.setEvent(event);
        changeLog.setPreviousChange(previousChange);
        return template.save(changeLog);
    }

    @Override
    public MetaHeader create(MetaInputBean inputBean, FortressUser fu, DocumentType documentType) throws DatagioException {
        MetaHeader metaHeader = new MetaHeaderNode(inputBean.getMetaKey(), fu, inputBean, documentType);
        if (inputBean.isTrackSuppressed()) {
            metaHeader.setMetaKey(null);
            return metaHeader;
        } else
            return save(metaHeader, documentType);
    }

    @Override
    @Cacheable(value = "trackLog", unless = "#result==null")
    public TrackLog getLog(Long logId) {
        Relationship change = template.getRelationship(logId);
        if (change != null)
            return (TrackLog) template.getDefaultConverter().convert(change, TrackLogRelationship.class);
        return null;
    }

    @Override
    public LogWhat getWhat(Long whatId) {
        return template.findOne(whatId, LogWhatData.class);
    }


    @Cacheable(value = "headerId",unless = "#result==null")
    @Override
    public MetaHeader getHeader(Long id) {
        return template.findOne(id, MetaHeaderNode.class);
    }

    @Override
    public ChangeLog fetch(ChangeLog lastChange) {
        return template.fetch(lastChange);
    }

    enum LastChange implements RelationshipType {
        LAST_CHANGE
    }

    private static final String LAST_CHANGE = "LAST_CHANGE";

    @Override
    public TrackLog addLog(MetaHeader metaHeader, ChangeLog newChange, DateTime fortressWhen, TrackLog existingLog) {
        TrackLog newLog = template.save(new TrackLogRelationship(metaHeader, newChange, fortressWhen));
        boolean moreRecent = (existingLog == null || existingLog.getFortressWhen() <= newLog.getFortressWhen());
        if (moreRecent) {
            removeLastChange(metaHeader, existingLog);
            makeLastChange(metaHeader, newChange);
        }

        logger.debug("Added Log - MetaHeader [{}], Log [{}], Change [{}]", metaHeader.getId(), newLog, newChange.getId());
        return newLog;

    }

    private void removeLastChange(MetaHeader metaHeader, TrackLog existingLog) {
        if (existingLog != null) {
            Node auditNode = template.getPersistentState(metaHeader);
            if (existingLog.getChange() != null) {
                Node changeNode = template.getPersistentState(existingLog.getChange());
                Relationship r = template.getRelationshipBetween(auditNode, changeNode, LAST_CHANGE);
                if (r != null) {
                    logger.debug("removeLastChange MetaHeader[{}], [{}]", metaHeader.getId(), r);
                    r.delete();
                    r = auditNode.getSingleRelationship(LastChange.LAST_CHANGE, Direction.OUTGOING);
                    if (r != null) r.delete();
                } else {
                    logger.debug("No change to remove for MetaHeader [{}]", metaHeader.getId());
                }
            }
        }
    }

    @Override
    public void makeLastChange(MetaHeader metaHeader, ChangeLog lastChange) {
        Node auditNode = template.getPersistentState(metaHeader);
        Node changeNode = template.getPersistentState(lastChange);
        Relationship r = template.createRelationshipBetween(auditNode, changeNode, LAST_CHANGE, null);
        logger.debug("makeLastChange - MetaHeader [{}], LAST_CHANGE [{}], auditChange [{}]", metaHeader.getId(), r.getId(), changeNode.getId());
    }


    public TrackLog getLastLog(Long auditHeaderID) {
        TrackLogRelationship log = null;

        Relationship r = template.getNode(auditHeaderID).getSingleRelationship(LastChange.LAST_CHANGE, Direction.BOTH);
        if (r != null) {
            trackLogRepo.getLastAuditLog(auditHeaderID);


            log = trackLogRepo.getLastAuditLog(r.getEndNode().getId());
//            int count = 0;
//
//            for (TrackLogRelationship when : logs) {
//                count++;
//                log = when;
//                logger.debug("getLastLog for MetaHeader [{}], found RLX [{}], changeId [{}]", auditHeaderID, when, when.getAuditChange().getId());
//            }
//
//            if (log != null) {
//                if (count > 1)
//                    logger.debug("*** MetaHeader [{}] - Found more than [{}] LAST_CHANGE Log, should only be 1", auditHeaderID, count);
//
//            }
        }
        return log;
    }

    @Override
    public ChangeLog save(ChangeLog change, Boolean compressed) {
        logger.debug("Saving Audit Change [{}]", change);
        return template.save(change);
    }

}
