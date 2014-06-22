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

import com.auditbucket.engine.repo.KvRepo;
import com.auditbucket.engine.repo.LogWhatData;
import com.auditbucket.engine.repo.redis.RedisRepo;
import com.auditbucket.engine.repo.riak.RiakRepo;
import com.auditbucket.helper.CompressionHelper;
import com.auditbucket.helper.CompressionResult;
import com.auditbucket.track.bean.AuditDeltaBean;
import com.auditbucket.track.bean.LogInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Log;
import com.auditbucket.track.model.LogWhat;
import com.auditbucket.track.model.MetaHeader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

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

    public void purge(String indexName) {
        getKvRepo().purge(indexName);
    }

    public void doKvWrite(Iterable<TrackResultBean> resultBeans) throws IOException {
        int count = 0;
        for (TrackResultBean resultBean : resultBeans) {
            doKvWrite(resultBean);
            count ++;
        }
        logger.debug("KV Service handled [{}] requests", count);
    }

    public void doKvWrite(TrackResultBean resultBean) throws IOException {
        if (resultBean.getLog() != null && resultBean.getLog().getStatus() != LogInputBean.LogStatus.TRACK_ONLY)
            doKvWrite(resultBean.getMetaHeader(), resultBean.getLogResult().getWhatLog());
    }

    public enum KV_STORE {REDIS, RIAK}

    private static final ObjectMapper om = new ObjectMapper();

    @Autowired
    RedisRepo redisRepo;

    @Autowired
    RiakRepo riakRepo;

    @Autowired
    EngineConfig engineAdmin;

    private Logger logger = LoggerFactory.getLogger(WhatService.class);

    /**
     * adds what store details to the log that will be index in Neo4j
     * Subsequently, this data will make it to a KV store
     *
     * @param log      Log
     * @param jsonText Escaped Json
     * @return logChange
     * @throws IOException
     */
    public Log prepareLog(Log log, String jsonText) throws IOException {
        // Compress the Value of JSONText
        CompressionResult dataBlock = CompressionHelper.compress(jsonText);
        Boolean compressed = (dataBlock.getMethod() == CompressionResult.Method.GZIP);
        log.setWhatStore(String.valueOf(engineAdmin.getKvStore()));
        log.setCompressed(compressed);
        log.setDataBlock(dataBlock.getAsBytes());

        return log;
    }

    private void doKvWrite(MetaHeader metaHeader, Log log) throws IOException {
        // ToDo: deal with this via spring integration??
        if (log == null) {
            return;
        }
        byte[] dataBlock = log.getDataBlock();
        getKvRepo(log).add(metaHeader, log.getId(), dataBlock);
    }

    private KvRepo getKvRepo() {
        return getKvRepo(String.valueOf(engineAdmin.getKvStore()));
    }

    private KvRepo getKvRepo(Log change) {
        return getKvRepo(change.getWhatStore());
    }

    private KvRepo getKvRepo(String kvStore) {
        if (kvStore.equalsIgnoreCase(String.valueOf(KV_STORE.REDIS))) {
            return redisRepo;
        } else if (kvStore.equalsIgnoreCase(String.valueOf(KV_STORE.RIAK))) {
            return riakRepo;
        } else {
            throw new IllegalStateException("The only supported KV Stores supported are redis & riak");
        }

    }

    public LogWhat getWhat(MetaHeader metaHeader, Log log) {
        if (log == null)
            return null;
        try {
            byte[] whatInformation = getKvRepo(log).getValue(metaHeader, log.getId());
            if (whatInformation != null)
                return new LogWhatData(whatInformation, log.isCompressed());
            else {
                //logger.error("Unable to obtain What data from {}", log.getWhatStore());
                return new LogWhatData(null, false);
            }
        } catch (RuntimeException re) {
            logger.error("KV Error Audit[" + metaHeader.getMetaKey() + "] change [" + log.getId() + "]", re);

            //throw (re);
        }
        return null;
    }

    public void delete(MetaHeader metaHeader, Log change) {

        getKvRepo(change).delete(metaHeader, change.getId());
    }


    /**
     * Locate and compare the two JSON What documents to determine if they have changed
     *
     * @param metaHeader  thing being tracked
     * @param compareFrom existing change to compare from
     * @param compareWith new Change to compare with - JSON format
     * @return false if different, true if same
     */
    public boolean isSame(MetaHeader metaHeader, Log compareFrom, String compareWith) {
        if (compareFrom == null)
            return false;
        LogWhat what = null;
        int count = 0;
        int timeout = 10;
        while ( what ==null && count < timeout){
            count++;
            what = getWhat(metaHeader, compareFrom);
        }

        if ( count >= timeout)
            logger.error("Timeout looking for KV What data for [{}]", metaHeader);

        if (what == null)
            return false;

        String jsonThis = what.getWhatString();
        return isSame(jsonThis, compareWith);
    }

    public boolean isSame(String compareFrom, String compareWith) {

        if (compareFrom == null || compareWith == null)
            return false;

        if (compareFrom.length() != compareWith.length())
            return false;

        // Compare values
        JsonNode jCompareFrom = null;
        JsonNode jCompareWith = null;
        try {
            jCompareFrom = om.readTree(compareFrom);
            jCompareWith = om.readTree(compareWith);
        } catch (IOException e) {
            logger.error("Comparing JSON docs", e);
        }
        return !(jCompareFrom == null || jCompareWith == null) && jCompareFrom.equals(jCompareWith);

    }

    public AuditDeltaBean getDelta(MetaHeader header, Log from, Log to) {
        if (header == null || from == null || to == null)
            throw new IllegalArgumentException("Unable to compute delta due to missing arguments");
        LogWhat source = getWhat(header, from);
        LogWhat dest = getWhat(header, to);
        MapDifference<String, Object> diffMap = Maps.difference(source.getWhat(), dest.getWhat());
        AuditDeltaBean result = new AuditDeltaBean();
        result.setAdded(new HashMap<>(diffMap.entriesOnlyOnRight()));
        result.setRemoved(new HashMap<>(diffMap.entriesOnlyOnLeft()));
        HashMap<String, Object> differences = new HashMap<>();
        Set<String> keys = diffMap.entriesDiffering().keySet();
        for (String key : keys) {
            differences.put(key, diffMap.entriesDiffering().get(key).toString());
        }
        result.setChanged(differences);
        result.setUnchanged(diffMap.entriesInCommon());
        return result;
    }


}
