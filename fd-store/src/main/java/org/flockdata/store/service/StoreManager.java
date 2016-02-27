/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.store.service;

import org.flockdata.helper.FlockServiceException;
import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.shared.InMemoryRepo;
import org.flockdata.store.FdStoreRepo;
import org.flockdata.store.LogRequest;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;
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
 * Encapsulation of FlockData's store management functionality.
 *
 * A simple wrapper with support
 * for various data stores that support put/get semantics.
 *
 *
 * <p/>
 * User: Mike Holdsworth
 * Since: 4/09/13
 */
@Service ("fdStoreManager")
@Transactional
public class StoreManager implements StoreService {

    @Autowired (required = false)
    RedisRepo redisRepo;

    @Autowired (required = false)
    RiakRepo riakRepo;

    @Autowired (required = false)
    InMemoryRepo inMemoryRepo;

//    @Autowired
//    EsRepo defaultStore;

    @Autowired
    FdStoreConfig storeConfig;

    private Logger logger = LoggerFactory.getLogger(StoreManager.class);

    @Override
    public String ping(Store store) {
        return getStore(store).ping();
    }

    @Override
    public StoredContent doRead(Store store, String index, String type, String id) {
        if ( id == null )
            return null;

        return getStore(store).read(index, type, id);
    }

    /**
     * Persists the payload
     * <p/>
     *
     * @param storeBean          payload to write to a store
     */
    public void doWrite(StorageBean storeBean) throws FlockServiceException {

        if (storeBean.getStore().equals(Store.NONE.name()))
            return; // This service does not write to ES, that is handled via fd-engine

        try {
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

    FdStoreRepo getStore(Store store) {
        if (store == Store.REDIS) {
            return redisRepo;
        } else if (store == Store.RIAK) {
            return riakRepo;
        } else if (store == Store.MEMORY) {
            return inMemoryRepo;
        } else {
            logger.info("The only supported persistent KV Stores supported are redis & riak. Returning a non-persistent memory based map");
            return inMemoryRepo;
        }

    }

    @Override
    public void delete(Entity entity, Log change) {

        getStore(change).delete(new LogRequest(entity, change));
    }



}
