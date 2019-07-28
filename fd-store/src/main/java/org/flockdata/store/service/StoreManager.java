/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

import java.io.IOException;
import java.util.Map;
import org.flockdata.data.Entity;
import org.flockdata.data.Log;
import org.flockdata.helper.FlockServiceException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.integration.InMemoryRepo;
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

/**
 * Encapsulation of FlockData's store management functionality.
 * <p>
 * A simple wrapper with support
 * for various data stores that support put/get semantics.
 *
 * @author mholdsworth
 * @since 4/09/2013
 */
@Service
@Transactional
public class StoreManager implements StoreService {

  private RedisRepo redisRepo;
  private RiakRepo riakRepo;
  private InMemoryRepo inMemoryRepo;
  private Logger logger = LoggerFactory.getLogger(StoreManager.class);

  private FdStoreConfig storeConfig;

  @Autowired(required = false)
  void setRedisRepo(RedisRepo redisRepo) {
    this.redisRepo = redisRepo;
  }

  @Autowired(required = false)
  void setRiakRepo(RiakRepo riakRepo) {
    this.riakRepo = riakRepo;
  }

  @Autowired(required = false)
  void setInMemoryRepo(InMemoryRepo inMemoryRepo) {
    this.inMemoryRepo = inMemoryRepo;
  }

  @Autowired
  void setStoreConfig(FdStoreConfig storeConfig) {
    this.storeConfig = storeConfig;
  }

  @Override
  public String ping(Store store) {
    FdStoreRepo storeService = getStore(store);
    if (storeService == null) {
      return store.toString() + " is not enabled";
    }
    return storeService.ping();
  }

  @Override
  public StoredContent doRead(Store store, String index, String type, String id) {
    if (id == null) {
      return null;
    }
    logger.debug("Looking for {} value for {}", store.name(), id);
    StoredContent content = getStore(store).read(index, type, id);
    if (content == null) {
      throw new NotFoundException("Didn't locate the id " + id + " for type" + type);
    }
    return content;
//        return getStore(store).read(index, type, id);
  }

  /**
   * Persists the payload
   *
   * @param storeBean payload to write to a store
   */
  public void doWrite(StorageBean storeBean) throws FlockServiceException {

    if (storeBean.getStore().equals(Store.NONE.name())) {
      return; // This service does not write to ES, that is handled via fd-engine
    }

    if (storeBean.getType() == null) {
      throw new AmqpRejectAndDontRequeueException("Couldn't figure out the type for entity " + storeBean.getId());
    }

    try {
      logger.debug("Received request to add storeBean {}", storeBean);
      FdStoreRepo store = getStore(Store.valueOf(storeBean.getStore().toUpperCase()));
      if (store == null) {
        throw new AmqpRejectAndDontRequeueException("Configured store manager " + storeBean.getStore().toUpperCase() + " was configured but not available");
      }
      store.add(storeBean);

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
      if (redisRepo == null) {
        throw new AmqpRejectAndDontRequeueException("Redis store was requested but not enabled");
      }
      return redisRepo;
    } else if (store == Store.RIAK) {
      if (riakRepo == null) {
        throw new AmqpRejectAndDontRequeueException("Riak store was requested but not enabled");
      }
      return riakRepo;
    } else if (store == Store.MEMORY) {
      if (inMemoryRepo == null) {
        throw new AmqpRejectAndDontRequeueException("Non-persistent InMemory store was requested but not enabled");
      }

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

  public Map<String, String> health() {
    Map<String, String> result = storeConfig.health();
    String redis = ping(Store.REDIS);
    String riak = ping(Store.RIAK);
    result.put("redis.ping", redis);
    result.put("riak.ping", riak);
    return result;
  }
}
