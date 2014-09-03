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
import com.auditbucket.engine.repo.neo4j.model.LogNode;
import com.auditbucket.engine.repo.neo4j.model.LoggedRelationship;
import com.auditbucket.engine.repo.neo4j.model.MetaHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.TxRefNode;
import com.auditbucket.engine.service.TrackEventService;
import com.auditbucket.engine.service.WhatService;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.service.KeyGenService;
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
@SuppressWarnings("SpringJavaAutowiringInspection")
@Repository("auditDAO")
public class TrackDaoNeo implements TrackDao {
    @Autowired
    MetaDataRepo metaRepo;

    @Autowired
    TrackLogRepo trackLogRepo;

    @Autowired
    DocumentTypeRepo documentTypeRepo;

    @Autowired
    TrackEventService trackEventService;

    @Autowired
    WhatService whatService;

    @Autowired
    KeyGenService keyGenService;

    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(TrackDaoNeo.class);

    public MetaHeader save(MetaHeader metaHeader) {
        return save(metaHeader, null);
    }

    @Override
    public MetaHeader create(MetaInputBean inputBean, Fortress fortress, DocumentType documentType) throws DatagioException {
//        MetaHeader metaHeader = findByCallerRef(fortress.getId(), documentType.getId(), inputBean.getCallerRef());
//        if (metaHeader != null) {
//            logger.debug("Found existing MetaHeader during request to create - returning");
//            return metaHeader;
//        }
        String metaKey = ( inputBean.isTrackSuppressed()?null:keyGenService.getUniqueKey());
        MetaHeader metaHeader = new MetaHeaderNode(metaKey, fortress, inputBean, documentType);

        if (! inputBean.isTrackSuppressed()) {
            logger.debug("Creating {}", metaHeader);

            metaHeader = save(metaHeader, documentType);
            Node node = template.getPersistentState(metaHeader);
            node.addLabel(DynamicLabel.label(documentType.getName()));

        }
        return metaHeader;
    }

    //@Override
//    @Caching(evict = {@CacheEvict(value = "headerId", key = "#p0.id"),
//            @CacheEvict(value = "metaKey", key = "#p0.metaKey")}}

    public MetaHeader save(MetaHeader metaHeader, DocumentType documentType) {
        metaHeader.bumpUpdate();
        return metaRepo.save((MetaHeaderNode) metaHeader);

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
    public Collection<MetaHeader> findByCallerRef(Long fortressId, String callerRef) {
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
            throw new DatagioException("Unable to find exactly one record for the callerRef [" + callerRef + "]. Found " + count);

        return result;

    }

    //@Cacheable(value = "callerKey", unless = "#result==null")
    public MetaHeader findByCallerRef(Long fortressId, Long documentId, String callerRef) {
        if (logger.isTraceEnabled())
            logger.trace("findByCallerRef fortress [" + fortressId + "] docType[" + documentId + "], callerRef[" + callerRef + "]");

        String keyToFind = "" + fortressId + "." + documentId + "." + callerRef;
        return metaRepo.findBySchemaPropertyValue("callerKeyRef", keyToFind);
    }

    //@Cacheable(value = "headerId", key = "p0.id", unless = "#result==null")
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

        Iterator<Map<String, Object>> rows = result.iterator();
        Collection<MetaHeader> results = new ArrayList<>();

        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            results.add(template.projectTo(row.get("meta"), MetaHeaderNode.class));
        }

        return results;
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
            TrackLog log = template.convert(row.get("logs"), LoggedRelationship.class);
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
        logger.debug("Saving track log [{}] - Log ID [{}]", log, log.getLog().getId());
        return template.save((LoggedRelationship) log);
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
//    @Cacheable(value = "trackLog", unless = "#result==null")
    public TrackLog getLog(Long logId) {
        Relationship change = template.getRelationship(logId);
        if (change != null)
            try {
                return (TrackLog) template.getDefaultConverter().convert(change, LoggedRelationship.class);
            } catch (NotFoundException nfe) {
                // Occurs if ab-search has been down and the database is out of sync from multiple restarts
                logger.error("Error converting relationship to a LoggedRelationship");
                return null;
            }
        return null;
    }


    //    @Cacheable(value = "headerId", unless = "#result==null")
    @Override
    public MetaHeader getHeader(Long id) {
        return template.findOne(id, MetaHeaderNode.class);
    }

    @Override
    public Log fetch(Log lastChange) {
        return template.fetch(lastChange);
    }

    @Override
    public TrackLog addLog(MetaHeader metaHeader, Log newChange, DateTime fortressWhen, TrackLog existingLog) {

        newChange.setTrackLog(new LoggedRelationship(metaHeader, newChange, fortressWhen));

        if (metaHeader.getId() == null)// This occurs when tracking in ab-engine is suppressed and the caller is only creating search docs
            return newChange.getTrackLog();

        //template.fetch(newChange.getTrackLog());
        boolean moreRecent = (existingLog == null || existingLog.getFortressWhen() <= fortressWhen.getMillis());
        if (moreRecent) {
            if (metaHeader.getLastChange() != null)
                metaHeader = template.fetch(metaHeader);
            if (metaHeader.getLastUser() == null || (!metaHeader.getLastUser().getId().equals(newChange.getWho().getId()))) {
                metaHeader.setLastUser(newChange.getWho());
            }
            metaHeader.setFortressLastWhen(fortressWhen.getMillis());
            metaHeader.setLastChange(newChange);
            logger.debug("Saving more recent change, logid [{}]", newChange.getEvent());
            try {
                template.save(metaHeader);
            } catch (IllegalStateException e) {
                logger.error("ISE saving header {}", new Date(newChange.getTrackLog().getSysWhen()));
                logger.error("Unexpected", e);
            }

        } else {
            newChange = template.save(newChange);
        }
        logger.debug("Added Log - MetaHeader [{}], Log [{}], Change [{}]", metaHeader.getId(), newChange.getTrackLog(), newChange.getId());
        newChange.getTrackLog().setMetaHeader(metaHeader);
        return newChange.getTrackLog();
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
    public Map<String, MetaHeader> findHeaders(Company company, Collection<String> metaKeys) {
        logger.debug("Looking for {} headers for company [{}] ", metaKeys.size(), company);
        Collection<MetaHeader> foundHeaders = metaRepo.findHeaders(company.getId(), metaKeys);
        Map<String, MetaHeader> unsorted = new HashMap<>();
        for (MetaHeader foundHeader : foundHeaders) {
            unsorted.put(foundHeader.getMetaKey(), foundHeader);
        }
        return unsorted;
//        // DAT-86 The incoming collection is actually the sort order we want
//        // ToDo: Find a slicker way of dealing with this
//        Collection<MetaHeader> sortedResult = new ArrayList<>(unsorted.size());
//        for (String metaKey : metaKeys) {
//            MetaHeader header = unsorted.get(metaKey);
//            if (header != null)
//                sortedResult.add(header);
//        }
//        return sortedResult;
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
        documentTypeRepo.purgeFortressDocuments(fortress.getId());
    }

    public TrackLog getLastLog(Long metaHeaderId) {
        MetaHeader header = getHeader(metaHeaderId);
        Log lastChange = header.getLastChange();
        if (lastChange == null)
            return null;

        return trackLogRepo.getLastLog(lastChange.getId());
    }


}
