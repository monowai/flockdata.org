/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.kv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.flockdata.helper.CompressionHelper;
import org.flockdata.helper.CompressionResult;
import org.flockdata.helper.FlockDataJsonFactory;
import org.flockdata.helper.FlockException;
import org.flockdata.kv.*;
import org.flockdata.kv.bean.KvContentBean;
import org.flockdata.kv.redis.RedisRepo;
import org.flockdata.kv.riak.RiakRepo;
import org.flockdata.kv.memory.MapRepo;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DeltaBean;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityContent;
import org.flockdata.track.model.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.scheduling.annotation.EnableAsync;
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
@EnableAsync
public class KvManager implements KvService {

    @Autowired
    KvGateway kvGateway;  // Used for async loose coupling between this service and the db

    @Override
    public String ping() {
        KvRepo repo = getKvRepo();
        return repo.ping();
    }

    Boolean asyncWrites = true;

    @Value("${fd-engine.kv.async:@null}")
    protected void setAsyncWrites(String kvAsync) {
        if (!"@null".equals(kvAsync))
            this.asyncWrites = Boolean.parseBoolean(kvAsync);
    }


    @Override
    public void purge(String indexName) {
        getKvRepo().purge(indexName);
    }

    @ServiceActivator(inputChannel = "doKvWrite")
    public void asyncWrite(KvContentBean kvBean) throws FlockException {
        try {
            getKvRepo().add(kvBean);
        } catch (IOException e) {
            throw new FlockException ("Error writing to the KvStore ", e);
        }
    }

    /**
     * Persists the payload
     *
     * if fd-engine.kv.async== true, then this will be done via an integration gateway
     * otherwise it will be done immediately with no guarantees around delivery.
     *
     * @param kvBean payload for the KvStore
     */
    public void doKvWrite(KvContentBean kvBean) throws FlockException {
        if (asyncWrites) {
            // Via the Gateway
            kvGateway.doKvWrite(kvBean);
        } else {
            asyncWrite(kvBean);
        }
    }

    private static final ObjectMapper om = FlockDataJsonFactory.getObjectMapper();

    @Autowired
    RedisRepo redisRepo;

    @Autowired
    RiakRepo riakRepo;

    @Autowired
    MapRepo mapRepo;

    @Autowired
    FdKvConfig kvConfig;

    private Logger logger = LoggerFactory.getLogger(KvManager.class);

    /**
     * adds what store details to the log that will be index in Neo4j
     * Subsequently, this data will make it to a KV store
     *
     * @param log     Log
     * @param content Escaped Json
     * @return logChange
     * @throws IOException
     */
    @Override
    public Log prepareLog(Log log, ContentInputBean content) throws IOException {
        // Compress the Value of JSONText
        CompressionResult compressionResult = CompressionHelper.compress(new KvContentData(content));
        Boolean compressed = (compressionResult.getMethod() == CompressionResult.Method.GZIP);
        log.setWhatStore(String.valueOf(kvConfig.getKvStore()));
        log.setCompressed(compressed);
        log.setChecksum(compressionResult.getChecksum());
        log.setEntityContent(compressionResult.getAsBytes());

        return log;
    }

    private KvRepo getKvRepo() {
        return getKvRepo(String.valueOf(kvConfig.getKvStore()));
    }

    private KvRepo getKvRepo(Log change) {
        return getKvRepo(change.getWhatStore());
    }

    private KvRepo getKvRepo(String kvStore) {
        if (kvStore.equalsIgnoreCase(String.valueOf(KV_STORE.REDIS))) {
            return redisRepo;
        } else if (kvStore.equalsIgnoreCase(String.valueOf(KV_STORE.RIAK))) {
            return riakRepo;
        } else if ( kvStore.equalsIgnoreCase(String.valueOf(KV_STORE.MEMORY))) {
            return mapRepo;
        } else {
            logger.info("The only supported persistent KV Stores supported are redis & riak. Returning a non-persistent memory based map");
            return mapRepo;
        }

    }

    @Override
    public EntityContent getContent(Entity entity, Log log) {
        if (log == null)
            return null;
        try {
            byte[] entityContent = getKvRepo(log).getValue(new EntityBean(entity), log);
            if (entityContent != null)
                return new EntityContentData(entityContent, log);

        } catch (RuntimeException re) {
            logger.error("KV Error Entity[" + entity.getMetaKey() + "] change [" + log.getId() + "]", re);
        }
        return null;
    }

    @Override
    public void delete(Entity entity, Log change) {

        getKvRepo(change).delete(new EntityBean(entity), change);
    }


    /**
     * Determine if the Log Content has changed
     *
     * @param entity      thing being tracked
     * @param compareFrom existing change to compare from
     * @param compareTo   new Change to compare with
     * @return false if different, true if same
     */
    @Override
    public boolean isSame(Entity entity, Log compareFrom, Log compareTo) {
        if (compareFrom == null)
            return false;
        EntityContent content = null;
        int count = 0;
        int timeout = 10;
        while (content == null && count < timeout) {
            count++;
            content = getContent(entity, compareFrom);
        }

        if (count >= timeout)
            logger.error("Timeout looking for KV What data for [{}] [{}]", entity, compareFrom);

        if (content == null)
            return false;

        logger.debug("Value found [{}]", content);
        boolean sameContentType = compareFrom.getContentType().equals(compareTo.getContentType());
        if (!sameContentType)
            return false;

        if (compareFrom.getContentType().equals("json"))
            return sameJson(content, new EntityContentData(compareTo.getEntityContent(), compareTo));
        else
            return sameCheckSum(compareFrom, compareTo);
    }

    private boolean sameCheckSum(Log compareFrom, Log compareTo) {
        return compareFrom.getChecksum().equals(compareTo.getChecksum());
    }


    @Override
    public boolean sameJson(EntityContent compareFrom, EntityContent compareTo) {

        logger.debug("Comparing [{}] with [{}]", compareFrom, compareTo.getWhat());
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
