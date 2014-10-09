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

import com.auditbucket.engine.repo.KvContentData;
import com.auditbucket.engine.repo.riak.RiakRepo;
import com.auditbucket.helper.CompressionResult;
import com.auditbucket.kv.service.KvService;
import com.auditbucket.track.bean.ContentInputBean;
import com.auditbucket.track.bean.DeltaBean;
import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.EntityContent;
import com.auditbucket.engine.repo.EntityContentData;
import com.auditbucket.engine.repo.KvRepo;
import com.auditbucket.engine.repo.redis.RedisRepo;
import com.auditbucket.helper.CompressionHelper;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.Log;
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
public class KvManager implements KvService {

    @Override
    public String ping() {
        KvRepo repo = getKvRepo();
        return repo.ping();
    }

    @Override
    public void purge(String indexName) {
        getKvRepo().purge(indexName);
    }

    @Override
    public void doKvWrite(TrackResultBean resultBean) throws IOException {
        if (resultBean.getContentInput() != null && resultBean.getContentInput().getStatus() != ContentInputBean.LogStatus.TRACK_ONLY)
            doKvWrite(resultBean.getEntity(), resultBean.getLogResult().getWhatLog());
    }

    private static final ObjectMapper om = new ObjectMapper();

    @Autowired
    RedisRepo redisRepo;

    @Autowired
    RiakRepo riakRepo;

    @Autowired
    EngineConfig engineAdmin;

    private Logger logger = LoggerFactory.getLogger(KvManager.class);

    /**
     * adds what store details to the log that will be index in Neo4j
     * Subsequently, this data will make it to a KV store
     *
     *
     * @param log      Log
     * @param content Escaped Json
     * @return logChange
     * @throws IOException
     */
    @Override
    public Log prepareLog(Log log, ContentInputBean content) throws IOException {
        // Compress the Value of JSONText
        CompressionResult compressionResult = CompressionHelper.compress(new KvContentData(content));
        Boolean compressed = (compressionResult.getMethod() == CompressionResult.Method.GZIP);
        log.setWhatStore(String.valueOf(engineAdmin.getKvStore()));
        log.setCompressed(compressed);
        log.setChecksum(compressionResult.getChecksum());
        log.setEntityContent(compressionResult.getAsBytes());

        return log;
    }



    private void doKvWrite(Entity entity, Log log) throws IOException {
        // ToDo: deal with this via spring integration??
        if (log == null) {
            return;
        }
        getKvRepo(log).add(entity, log);
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

    @Override
    public EntityContent getContent(Entity entity, Log log) {
        if (log == null)
            return null;
        try {
            byte[] entityContent = getKvRepo(log).getValue(entity, log);
            if (entityContent != null)
                return new EntityContentData(entityContent, log);

        } catch (RuntimeException re) {
            logger.error("KV Error Entity[" + entity.getMetaKey() + "] change [" + log.getId() + "]", re);
        }
        return null;
    }

    @Override
    public void delete(Entity entity, Log change) {

        getKvRepo(change).delete(entity, change);
    }


    /**
     * Determine if the Log Content has changed
     *
     * @param entity        thing being tracked
     * @param compareFrom   existing change to compare from
     * @param compareTo     new Change to compare with
     * @return false if different, true if same
     */
    @Override
    public boolean isSame(Entity entity, Log compareFrom, Log compareTo) {
        if (compareFrom == null)
            return false;
        EntityContent content = null;
        int count = 0;
        int timeout = 10;
        while ( content ==null && count < timeout){
            count++;
            content = getContent(entity, compareFrom);
        }

        if ( count >= timeout)
            logger.error("Timeout looking for KV What data for [{}]", entity);

        if (content == null)
            return false;

        logger.debug("Value found [{}]", content);
        boolean sameContentType = compareFrom.getContentType().equals(compareTo.getContentType());
        if ( !sameContentType )
            return false;

        if ( compareFrom.getContentType().equals("json"))
            return sameJson(content, new EntityContentData(compareTo.getEntityContent(), compareTo));
        else
            return sameCheckSum(compareFrom, compareTo);
    }

    private boolean sameCheckSum(Log compareFrom, Log compareTo) {
        return compareFrom.getChecksum().equals(compareTo.getChecksum()) ;
    }


    @Override
    public boolean sameJson(EntityContent compareFrom, EntityContent compareTo) {

        logger.debug ("Comparing [{}] with [{}]", compareFrom, compareTo.getWhat());
        JsonNode jCompareFrom = om.valueToTree(compareFrom.getWhat());
        JsonNode jCompareWith = om.valueToTree(compareTo.getWhat());
        return !(jCompareFrom == null || jCompareWith == null) && jCompareFrom.equals(jCompareWith);

    }

    @Override
    public DeltaBean getDelta(Entity entity, Log from, Log to) {
        if (entity == null || from == null || to == null)
            throw new IllegalArgumentException("Unable to compute delta due to missing arguments");
        EntityContent source = getContent(entity, from);
        EntityContent dest = getContent(entity, to);
        MapDifference<String, Object> diffMap = Maps.difference(source.getWhat(), dest.getWhat());
        DeltaBean result = new DeltaBean();
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
