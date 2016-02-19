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
import org.flockdata.store.LogRequest;
import org.flockdata.store.Store;
import org.flockdata.store.StoreContent;
import org.flockdata.store.bean.StoreBean;
import org.flockdata.store.common.repos.FdStoreRepo;
import org.flockdata.store.common.repos.MapRepo;
import org.flockdata.store.repo.EsRepo;
import org.flockdata.store.repo.RedisRepo;
import org.flockdata.store.repo.RiakRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Encapsulation of FlockData's store management functionality. A simple wrapper with support
 * for various data stores that support put/get semantics.
 * <p/>
 * User: Mike Holdsworth
 * Since: 4/09/13
 */
@Service
@Transactional
public class StoreManager implements StoreService {

    private static final ObjectMapper om = FdJsonObjectMapper.getObjectMapper();

    @Autowired (required = false)
    RedisRepo redisRepo;

    @Autowired (required = false)
    RiakRepo riakRepo;

    @Autowired (required = false)
    MapRepo mapRepo;

    @Autowired
    EsRepo defaultStore;

    @Autowired
    FdStoreConfig storeConfig;

    private Logger logger = LoggerFactory.getLogger(StoreManager.class);

    @Override
    public String ping(Store store) {
        return getStore(store).ping();
    }


    /**
     * Persists the payload
     * <p/>
     *
     * @param storeBean          payload to write to a store
     */
    public void doWrite(StoreBean storeBean) throws FlockServiceException {

        if (storeBean.getStore().equals(Store.NONE.name()))
            return;

        storeBean.setBucket(storeBean.getBucket());

        logger.debug("Sync write begins {}", storeBean);
        // ToDo: Extract in to a standalone class
        try {
            // ToDo: Retry or CircuitBreaker?
            logger.debug("Received request to add storeBean {}", storeBean);
            getStore(Store.valueOf(storeBean.getStore())).add(storeBean);

        } catch (IOException e) {
            String errorMsg = String.format("Error writing to the %s Store.", storeBean.getStore());
            logger.error(errorMsg); // Hopefully an ops team will monitor for this event and
            //           resolve the underlying DB problem
            throw new AmqpRejectAndDontRequeueException(errorMsg, e); // Keep the message on the queue
        }
        //}
    }

    // Returns the kvstore based on log.storage
    FdStoreRepo getStore(Log log) {
        return getStore(Store.valueOf(log.getStorage()));
    }

    FdStoreRepo getStore(Store kvStore) {
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
    public StoreContent getContent(LogRequest logRequest) {
        if (logRequest.getLogId() == null)
            return null;
        try {
            return getStore(logRequest.getStore()).getValue(logRequest);

        } catch (FlockServiceException re) {
            logger.error("KV Error Entity[" + logRequest+ "]", re);
        }
        return null;
    }

    @Override
    public void delete(Entity entity, Log change) {

        getStore(change).delete(new LogRequest(entity, change));
    }



}
