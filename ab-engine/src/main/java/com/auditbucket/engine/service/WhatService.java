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

package com.auditbucket.engine.service;

import com.auditbucket.audit.bean.AuditDeltaBean;
import com.auditbucket.audit.model.ChangeLog;
import com.auditbucket.audit.model.LogWhat;
import com.auditbucket.audit.model.MetaHeader;
import com.auditbucket.dao.TrackDao;
import com.auditbucket.engine.repo.KvRepo;
import com.auditbucket.engine.repo.LogWhatData;
import com.auditbucket.engine.repo.redis.RedisRepo;
import com.auditbucket.engine.repo.riak.RiakRepo;
import com.auditbucket.helper.CompressionHelper;
import com.auditbucket.helper.CompressionResult;
import com.auditbucket.helper.DatagioException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * User: Mike Holdsworth
 * Since: 4/09/13
 */
@Service
@Transactional
public class WhatService {

    public String ping() {
        KvRepo repo = getKvRepo();
        return repo.ping();
    }

    public enum KV_STORE {REDIS, RIAK}
    private static final ObjectMapper om = new ObjectMapper();
    @Autowired(required = false)
    TrackDao trackDao = null;
    @Autowired
    RedisRepo redisRepo;
    @Autowired
    RiakRepo riakRepo;
    @Autowired
    EngineConfig engineAdmin;

    private Logger logger = LoggerFactory.getLogger(WhatService.class);

    public ChangeLog logWhat(MetaHeader metaHeader, ChangeLog change, String jsonText) throws DatagioException {
        // Compress the Value of JSONText
        CompressionResult dataBlock = CompressionHelper.compress(jsonText);
        Boolean compressed = (dataBlock.getMethod() == CompressionResult.Method.GZIP);

        change.setWhatStore(String.valueOf(engineAdmin.getKvStore()));
        change.setCompressed(compressed);
        // Store First all information In Neo4j
        change = trackDao.save(change, compressed);
        doKvWrite(metaHeader, change, dataBlock);

        return change;
    }

    @Async //Only public methods execute Async
    public Future<Void> doKvWrite(MetaHeader metaHeader, ChangeLog change, CompressionResult dataBlock) throws DatagioException {
        try {
            // ToDo: deal with this via spring integration??
            getKvRepo(change).add(metaHeader, change.getId(), dataBlock.getAsBytes());
        } catch (IOException | RuntimeException e) {
            logger.error("KV storage issue", e);
            throw new DatagioException("KV Storage Issue", e);
        }
        return null ;
    }

    private KvRepo getKvRepo(){
        return getKvRepo(String.valueOf(engineAdmin.getKvStore()));
    }
    private KvRepo getKvRepo(ChangeLog change) {
        return getKvRepo(change.getWhatStore());
    }

    private KvRepo getKvRepo(String kvStore){
        if (kvStore.equalsIgnoreCase(String.valueOf(KV_STORE.REDIS))) {
            return redisRepo;
        } else if (kvStore.equalsIgnoreCase(String.valueOf(KV_STORE.RIAK))) {
            return riakRepo ;
        } else {
            throw new IllegalStateException("The only supported KV Stores supported are redis & riak");
        }

    }
    public LogWhat getWhat(MetaHeader metaHeader, ChangeLog change) {
        if (change == null )
            return null;
        try {
            byte[] whatInformation = getKvRepo(change).getValue(metaHeader, change.getId());
            return new LogWhatData(whatInformation, change.isCompressed());
        } catch ( RuntimeException re){
            logger.error("KV Error Audit["+ metaHeader.getMetaKey() +"] change ["+change.getId()+"]", re);

            //throw (re);
        }
        return null;
    }

    public void delete(MetaHeader metaHeader, ChangeLog change) {

        getKvRepo(change).delete(metaHeader, change.getId());
    }



    /**
     * Locate and compare the two JSON What documents to determine if they have changed
     *
     *
     * @param metaHeader  thing being tracked
     * @param compareFrom existing change to compare from
     * @param compareWith new Change to compare with - JSON format
     * @return false if different, true if same
     */
    public boolean isSame(MetaHeader metaHeader, ChangeLog compareFrom, String compareWith) {
        if (compareFrom == null)
            return false;
        LogWhat what = getWhat(metaHeader, compareFrom);

        if (what == null)
            return false;

        String jsonThis = what.getWhat();
        if (jsonThis == null || compareWith == null)
            return false;

        if (jsonThis.length() != compareWith.length())
            return false;

        // Compare values
        JsonNode compareTo = null;
        JsonNode other = null;
        try {
            compareTo = om.readTree(jsonThis);
            other = om.readTree(compareWith);
        } catch (IOException e) {
            logger.error("Comparing JSON docs");
        }
        return !(compareTo == null || other == null) && compareTo.equals(other);

    }

    public AuditDeltaBean getDelta(MetaHeader header, ChangeLog from, ChangeLog to) {
        if ( header == null || from == null || to == null )
            throw new IllegalArgumentException("Unable to compute delta due to missing arguments");
        LogWhat source = getWhat(header, from);
        LogWhat dest = getWhat(header, to);
        MapDifference<String, Object> diffMap = Maps.difference(source.getWhatMap(), dest.getWhatMap());
        AuditDeltaBean result = new AuditDeltaBean();
        result.setAdded(new HashMap<>(diffMap.entriesOnlyOnRight()));
        result.setRemoved(new HashMap<>(diffMap.entriesOnlyOnLeft()));
        HashMap<String, Object> differences = new HashMap<>();
        Set<String> keys =diffMap.entriesDiffering().keySet();
        for (String key : keys) {
            differences.put(key, diffMap.entriesDiffering().get(key).toString());
        }
        result.setChanged(differences);
        result.setUnchanged(diffMap.entriesInCommon());
        return result;
    }


}
