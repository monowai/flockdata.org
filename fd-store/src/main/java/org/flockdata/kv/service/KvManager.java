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
import org.flockdata.helper.FlockServiceException;
import org.flockdata.kv.*;
import org.flockdata.kv.bean.KvContentBean;
import org.flockdata.kv.memory.MapRepo;
import org.flockdata.kv.redis.RedisRepo;
import org.flockdata.kv.riak.RiakRepo;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.DeltaBean;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityContent;
import org.flockdata.track.model.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

/**
 * Encapsulation of FlockData's KV management functionality. A simple wrapper with support
 * for various KV stores. Also provides retry and integration capabilities
 * <p/>
 * User: Mike Holdsworth
 * Since: 4/09/13
 */
@Service
@Transactional
public class KvManager implements KvService {

    @Autowired
    KvGateway kvGateway;  // Used for async loose coupling between this service and the db

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

    @Override
    public String ping() {
        KvRepo repo = getKvRepo();
        return repo.ping();
    }


    @Override
    public void purge(String indexName) {
        getKvRepo().purge(indexName);
    }

    /**
     * Activated via an integration channel. This method goes through retry logic to handle
     * temporary failures. If the kvBean is not processed then the message is left on the queue
     * for retry
     *
     * @param kvBean content
     * @throws FlockServiceException - problem with the underlying
     */
    @ServiceActivator(inputChannel = "doKvWrite", adviceChain = {"retrier"}, requiresReply = "false")
    public Boolean asyncWrite(KvContentBean kvBean) throws FlockServiceException {
        try {
            // ToDo: Retry or CircuitBreaker?
            logger.debug("Received request to add kvBean {}", kvBean);
            getKvRepo().add(kvBean);

        } catch (IOException e) {
            String errorMsg = String.format("Error writing to the %s KvStore.", kvConfig.getKvStore());
            logger.error(errorMsg); // Hopefully an ops team will monitor for this event and
            //           resolve the underlying DB problem
            throw new FlockServiceException(errorMsg, e); // Keep the message on the queue
        }
        return Boolean.TRUE;
    }

    /**
     * Persists the payload
     * <p/>
     * if fd-engine.kv.async== true, then this will be handed off to an integration gateway
     * for guaranteed delivery. Otherwise the write call will be performed immediately and the caller
     * will have to deal with any errors
     *
     * @param kvBean payload for the KvStore
     */
    public void doKvWrite(KvContentBean kvBean) throws FlockServiceException {
        if (kvConfig.isAsyncWrite()) {
            // Via the Gateway
            logger.trace("Async write begins");
            kvGateway.doKvWrite(kvBean);
        } else {

            asyncWrite(kvBean);
        }
    }

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
        } else if (kvStore.equalsIgnoreCase(String.valueOf(KV_STORE.MEMORY))) {
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

        logger.trace("Comparing [{}] with [{}]", compareFrom, compareTo.getWhat());
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
