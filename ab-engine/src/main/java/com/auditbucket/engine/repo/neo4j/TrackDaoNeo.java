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

import com.auditbucket.dao.TrackDao;
import com.auditbucket.engine.repo.LogWhatData;
import com.auditbucket.engine.repo.neo4j.model.LogNode;
import com.auditbucket.engine.repo.neo4j.model.MetaHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.TrackLogRelationship;
import com.auditbucket.engine.repo.neo4j.model.TxRefNode;
import com.auditbucket.engine.service.TrackEventService;
import com.auditbucket.engine.service.WhatService;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.track.bean.AuditTXResult;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.*;
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
import java.io.IOException;
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
    SchemaTypeRepo schemaTypeRepo;

    @Autowired
    TrackEventService trackEventService;

    @Autowired
    WhatService whatService;

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

    //    @Cacheable(value = "metaKey", unless = "#result==null")
    private MetaHeader getCachedHeader(String key) {
        if (key == null)
            return null;
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

    @Override
    public Iterable<MetaHeader> findByCallerRef(Long fortressId, String callerRef) {
        return metaRepo.findByCallerRef(fortressId, callerRef);

    }

    @Override
    public MetaHeader findByCallerRefUnique(Long fortressId, String callerRef) throws DatagioException {
        int count = 0;
        Iterable<MetaHeader> headers = findByCallerRef(fortressId, callerRef);
        MetaHeader result = null;
        for (MetaHeader header : headers) {
            count++;
            result = header;
            if (count > 1) break;
        }
        if (count > 1)
            throw new DatagioException("Unable to find exactly one record for the callerRef [" + callerRef + "]");

        return result;

    }

    //@Cacheable(value = "callerKey", unless = "#result==null")
    public MetaHeader findByCallerRef(Long fortressId, Long documentId, String callerRef) {
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
        String cypher = "match (f:Fortress)-[:TRACKS]->(meta:`" + label + "`) where id(f)={fortress} return meta ORDER BY meta.dateCreated ASC skip {skip} limit 100 ";
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
            results.add(template.projectTo(row.get("meta"), MetaHeaderNode.class));
        }
        //
        return results;
        //return metaRepo.findHeadersFrom(fortressId, docTypeId, skipTo);
    }

    @Override
    public void delete(Log currentChange) {
        trackLogRepo.delete((LogNode) currentChange);
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
        return trackLogRepo.getLogs(auditLogID, from.getTime(), to.getTime());
    }

    public Set<TrackLog> getLogs(Long auditHeaderID) {
        return trackLogRepo.findLogs(auditHeaderID);
    }

    public Map<String, Object> findByTransaction(TxRef txRef) {
        //Example showing how to use cypher and extract

        String findByTagRef = "start tag =node({txRef}) " +
                "              match tag-[:AFFECTED]->auditLog<-[logs:LOGGED]-track " +
                "             return logs, track, auditLog " +
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
            Log change = template.convert(row.get("auditLog"), LogNode.class);
            MetaHeader audit = template.convert(row.get("track"), MetaHeaderNode.class);
            simpleResult.add(new AuditTXResult(audit, change, log));
            i++;

        }
        Map<String, Object> result = new HashMap<>(i);
        result.put("txRef", txRef.getName());
        result.put("logs", simpleResult);

        return result;
    }

    public TrackLog save(TrackLog log) {
        logger.debug("Saving track log [{}] - Log ID [{}]", log, log.getChange().getId());
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
    public Log prepareLog(FortressUser fUser, LogInputBean input, TxRef txRef, Log previousChange) throws DatagioException {
        ChangeEvent event = trackEventService.processEvent(fUser.getFortress().getCompany(), input.getEvent());
        Log changeLog = new LogNode(fUser, input, txRef);
        changeLog.setEvent(event);
        changeLog.setPreviousLog(previousChange);
        try {
            changeLog = whatService.prepareLog(changeLog, input.getWhat());
        } catch (IOException e) {
            throw new DatagioException("Unexpected error talking to What Service", e);
        }
        return changeLog;
    }

    @Override
    public MetaHeader create(MetaInputBean inputBean, Fortress fortress, DocumentType documentType) throws DatagioException {
        MetaHeader metaHeader = new MetaHeaderNode(inputBean.getMetaKey(), fortress, inputBean, documentType);
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


    @Cacheable(value = "headerId", unless = "#result==null")
    @Override
    public MetaHeader getHeader(Long id) {
        return template.findOne(id, MetaHeaderNode.class);
    }

    @Override
    public Log fetch(Log lastChange) {
        return template.fetch(lastChange);
    }

    enum LastChange implements RelationshipType {
        LAST_CHANGE
    }

    private static final String LAST_CHANGE = "LAST_CHANGE";

    @Override
    public TrackLog addLog(MetaHeader metaHeader, Log newChange, DateTime fortressWhen, TrackLog existingLog) {
        newChange = template.save(newChange);
        TrackLog newLog = template.save(new TrackLogRelationship(metaHeader, newChange, fortressWhen));
        boolean moreRecent = (existingLog == null || existingLog.getFortressWhen() <= fortressWhen.getMillis());
        if (moreRecent) {
            metaHeader.setLastChange(newChange);
            template.save(metaHeader);

            //  removeLastChange(metaHeader, existingLog);
//            makeLastChange(metaHeader, newChange);
        }
        logger.debug("Added Log - MetaHeader [{}], Log [{}], Change [{}]", metaHeader.getId(), newLog, newChange.getId());
        newLog.setMetaHeader(metaHeader);
        return newLog;

    }

    private void removeLastChange(MetaHeader metaHeader, TrackLog existingLog) {
        if (existingLog != null) {
            Node auditNode = template.getPersistentState(metaHeader);
            if (existingLog.getChange() != null) {
                Node logNode = template.getPersistentState(existingLog.getChange());
                Relationship r = template.getRelationshipBetween(auditNode, logNode, LAST_CHANGE);
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
    public void makeLastChange(MetaHeader metaHeader, Log lastChange) {
        //Node metaNode = template.getPersistentState(metaHeader);
        //Node logNode = template.getPersistentState(lastChange);
        //Relationship r = template.createRelationshipBetween(metaNode, logNode, LAST_CHANGE, null);
        logger.debug("makeLastChange - MetaHeader [{}], LAST_CHANGE [{}]", metaHeader.getId(), lastChange.getId());
        metaHeader.setLastChange(lastChange);
        template.save(metaHeader);
    }

    @Override
    public void crossReference(MetaHeader header, Collection<MetaHeader> targets, String refName) {
        Node source = template.getPersistentState(header);
        for (MetaHeader target : targets) {
            Node dest = template.getPersistentState(target);
            template.createRelationshipBetween(source, dest, refName, null);
        }
    }

    @Override
    public Map<String, Collection<MetaHeader>> getCrossReference(Company company, MetaHeader header, String xRefName) {
        Node n = template.getPersistentState(header);

        RelationshipType r = DynamicRelationshipType.withName(xRefName);
        Iterable<Relationship> rlxs = n.getRelationships(r);
        Map<String, Collection<MetaHeader>> results = new HashMap<>();
        Collection<MetaHeader> headers = new ArrayList<>();
        results.put(xRefName, headers);
        for (Relationship rlx : rlxs) {
            headers.add(template.projectTo(rlx.getEndNode(), MetaHeaderNode.class));
        }
        return results;

    }

    @Override
    public Collection<MetaHeader> findHeaders(Company company, Collection<String> toFind) {
        logger.debug("Looking for {} headers for company [{}] ", toFind.size(), company);
        return metaRepo.findHeaders(company.getId(), toFind);
    }

    @Override
    public void purgeTagRelationships(Fortress fortress) {
        // ToDo: Check if this works with huge datasets
        trackLogRepo.purgeTagRelationships(fortress.getId());
    }

    @Override
    public void purgeFortressLogs(Fortress fortress) {
        trackLogRepo.purgeFortressLogs(fortress.getId());
    }

    @Override
    public void purgePeopleRelationships(Fortress fortress) {
        metaRepo.purgePeopleRelationships(fortress.getId());
    }

    @Override
    public void purgeHeaders(Fortress fortress) {
        metaRepo.purgeHeaders(fortress.getId());
    }

    @Override
    public void purgeFortressDocuments(Fortress fortress) {
        schemaTypeRepo.purgeFortressDocuments(fortress.getId());
    }


    public TrackLog getLastLog(Long metaHeaderId) {
        TrackLogRelationship log = null;
        Iterable<Relationship> rlxs = template.getNode(metaHeaderId).getRelationships(LastChange.LAST_CHANGE, Direction.OUTGOING);
        int count = 0;
        for (Relationship rlx : rlxs) {
            if (count > 0) {
                logger.error("Multiple relationships found for {} - returning the first found - {}", metaHeaderId, log.getId());
            } else {
                log = trackLogRepo.getLastLog(rlx.getEndNode().getId());
                count++;
            }
        }
        return log;
    }


}
