/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.store.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.FlockServiceException;
import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.store.KvContent;
import org.flockdata.store.Store;
import org.flockdata.store.bean.KvContentBean;
import org.flockdata.store.repos.*;
import org.flockdata.track.bean.DeltaResultBean;
import org.flockdata.track.bean.TrackResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
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
//@Profile({"integration","production"})
public class StoreManager implements KvService {

    private static final ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

    @Autowired
    AbstractStore redisRepo;

    @Autowired
    RiakRepo riakRepo;

    @Autowired
    MapRepo mapRepo;

    @Autowired
    EsRepo defaultStore;

    @Autowired
    FdStoreConfig kvConfig;

    private Logger logger = LoggerFactory.getLogger(StoreManager.class);

    @Override
    public String ping() {
        FdStoreRepo repo = getKvRepo();
        return repo.ping();
    }


    @Override
    public void purge(String indexName) {
        getKvRepo().purge(indexName);
    }

    /**
     * Persists the payload
     * <p/>
     * if fd-engine.kv.async== true, then this will be handed off to an integration gateway
     * for guaranteed delivery. Otherwise the write call will be performed immediately and the caller
     * will have to deal with any errors
     *
     * @param kvBean          payload for the KvStore
     */
    public void doWrite(KvContentBean kvBean) throws FlockServiceException {
        // Code smell - we're resolving the storage twice
        // ToDO: Fix me if an error occurs
//        if (kvBean.getStorage() == null) {
//            kvBean.setStorage(getKvStore(trackResultBean).name());
//        }
        if (kvBean.getStorage().equals(Store.NONE.name()))
            return;

        kvBean.setBucket(kvBean.getBucket());

//        if (kvConfig.isAsyncWrite()) {
        // Via the Gateway
        //          logger.debug("Async write begins {}", kvBean);
        //        kvGateway.doKvWrite(kvBean);
        //  } else {
        logger.debug("Sync write begins {}", kvBean);
        // ToDo: Extract in to a standalone class
        try {
            // ToDo: Retry or CircuitBreaker?
            logger.debug("Received request to add kvBean {}", kvBean);
            getKvRepo(Store.valueOf(kvBean.getStorage())).add(kvBean);

        } catch (IOException e) {
            String errorMsg = String.format("Error writing to the %s KvStore.", kvConfig.kvStore().name());
            logger.error(errorMsg); // Hopefully an ops team will monitor for this event and
            //           resolve the underlying DB problem
            throw new AmqpRejectAndDontRequeueException(errorMsg, e); // Keep the message on the queue
        }
        //}
    }

    /**
     * adds what store details to the log that will be index in Neo4j
     * Subsequently, this data will make it to a KV store if enabled
     * <p/>
     * If fortress storage is disabled, then the storage is set to KV_STORE.NONE
     *
     * @param trackResult Escaped Json
     * @param log         Log
     * @return logChange
     * @throws IOException
     */
    @Override
    public Log prepareLog(TrackResultBean trackResult, Log log) throws IOException {
        // ToDo: KVStore's need to be aligned between services
//        Store storage = getKvStore(trackResult);
        return Store.prepareLog(kvConfig.kvStore(), trackResult, log);
    }

//    public Store getKvStore(TrackResultBean trackResult) {
//        if (trackResult.getDocumentType().getVersionStrategy() == DocumentType.VERSION.ENABLE)
//            return kvConfig.kvStore();
//
//        if (trackResult.getDocumentType().getVersionStrategy() == DocumentType.VERSION.DISABLE)
//            return Store.NONE;
//
//        Entity entity = trackResult.getEntity();
//        FortressSegment segment = entity.getSegment();
//
//        // Check against the fortress default
//        Store storage;
//        if (segment.getFortress().isStoreEnabled())
//            storage = kvConfig.kvStore();
//        else
//            storage = Store.NONE;
//        return storage;
//    }

    // Returns the system default kv store
    FdStoreRepo getKvRepo() {
        return getKvRepo(kvConfig.kvStore());
    }

    // Returns the kvstore based on log.storage
    FdStoreRepo getKvRepo(Log log) {
        return getKvRepo(Store.valueOf(log.getStorage()));
    }

    FdStoreRepo getKvRepo(Store kvStore) {
        if (kvStore == Store.REDIS) {
            return redisRepo;
        } else if (kvStore == Store.RIAK) {
            return riakRepo;
        } else if (kvStore == Store.MEMORY) {
            return mapRepo;
        } else if (kvStore == Store.NONE) {
            return defaultStore;
        } else {
            logger.info("The only supported persistent KV Stores supported are redis & riak. Returning a non-persistent memory based map");
            return mapRepo;
        }

    }

    @Override
    public KvContent getContent(Entity entity, Log log) {
        if (log == null)
            return null;
        try {
            return getKvRepo(log).getValue(entity, log);

        } catch (FlockServiceException re) {
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
     * @param entity      thing being tracked
     * @param compareFrom existing change to compare from
     * @param compareTo   new Change to compare with
     * @return false if different, true if same
     */
    @Override
    public boolean isSame(Entity entity, Log compareFrom, Log compareTo) {
        if (compareFrom == null)
            return false;

        // ToDo: Retryable - what if KV store is down?
        KvContent content = getContent(entity, compareFrom);

        if (content == null)
            return false;

        logger.debug("Content found [{}]", content);
        boolean sameContentType = compareFrom.getContentType().equals(compareTo.getContentType());

        return sameContentType &&
                (sameCheckSum(compareFrom, compareTo) || compareFrom.getContentType().equals("json") &&
                        sameJson(content, compareTo.getContent()));

    }

    private boolean sameCheckSum(Log compareFrom, Log compareTo) {
        return compareFrom.getChecksum().equals(compareTo.getChecksum());
    }

    @Override
    public boolean sameJson(KvContent compareFrom, KvContent compareTo) {
//        if ( compareTo == null )
//            return false;
        if (compareFrom.getData().size() != compareTo.getData().size())
            return false;
        logger.trace("Comparing [{}] with [{}]", compareFrom, compareTo.getData());
        JsonNode jCompareFrom = om.valueToTree(compareFrom.getData());
        JsonNode jCompareWith = om.valueToTree(compareTo.getData());
        return !(jCompareFrom == null || jCompareWith == null) && jCompareFrom.equals(jCompareWith);

    }

    @Override
    public DeltaResultBean getDelta(Entity entity, Log from, Log to) {
        if (entity == null || from == null || to == null)
            throw new IllegalArgumentException("Unable to compute delta due to missing arguments");
        KvContent source = getContent(entity, from);
        KvContent dest = getContent(entity, to);
        MapDifference<String, Object> diffMap = Maps.difference(source.getData(), dest.getData());
        DeltaResultBean result = new DeltaResultBean();
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
