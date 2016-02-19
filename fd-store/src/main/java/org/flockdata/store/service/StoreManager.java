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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.FlockServiceException;
import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.store.KvContent;
import org.flockdata.store.LogRequest;
import org.flockdata.store.Store;
import org.flockdata.store.bean.KvContentBean;
import org.flockdata.store.common.repos.AbstractStore;
import org.flockdata.store.common.repos.FdStoreRepo;
import org.flockdata.store.common.repos.MapRepo;
import org.flockdata.store.repo.EsRepo;
import org.flockdata.store.repo.RiakRepo;
import org.flockdata.track.bean.TrackResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Encapsulation of FlockData's KV management functionality. A simple wrapper with support
 * for various KV stores. Also provides retry and integration capabilities
 * <p/>
 * User: Mike Holdsworth
 * Since: 4/09/13
 */
@Service
@Transactional
public class StoreManager implements StoreService {

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

        if (kvBean.getStorage().equals(Store.NONE.name()))
            return;

        kvBean.setBucket(kvBean.getBucket());

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
        return Store.prepareLog(kvConfig.kvStore(), trackResult, log);
    }

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
    public KvContent getContent(LogRequest logRequest) {
        if (logRequest.getLogId() == null)
            return null;
        try {
            return getKvRepo(logRequest.getStore()).getValue(logRequest);

        } catch (FlockServiceException re) {
            logger.error("KV Error Entity[" + logRequest+ "]", re);
        }
        return null;
    }

    @Override
    public void delete(Entity entity, Log change) {

        getKvRepo(change).delete(new LogRequest(entity, change));
    }



}
