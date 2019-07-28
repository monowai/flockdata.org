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

package org.flockdata.store.repo;

import java.io.IOException;
import java.util.Date;
import javax.annotation.PostConstruct;
import org.flockdata.helper.JsonUtils;
import org.flockdata.store.AbstractStore;
import org.flockdata.store.LogRequest;
import org.flockdata.store.StoredContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @tag Redis, Store
 */

@Service
@Profile("redis")
public class RedisRepo extends AbstractStore {

  private static Logger logger = LoggerFactory.getLogger(AbstractStore.class);
  private final RedisTemplate<Object, byte[]> template;

  @Autowired
  public RedisRepo(RedisTemplate<Object, byte[]> template) {
    this.template = template;
  }

  public void add(StoredContent storedContent) throws IOException {

    template.opsForValue().set(storedContent.getId(), JsonUtils.toJsonBytes(storedContent));
  }

  @Override
  public StoredContent read(String index, String type, String id) {
    Long key = Long.decode(id);
    byte[] bytes = template.opsForValue().get(key);

    try {
      return JsonUtils.toObject(bytes, StoredContent.class);
      //return getContent(key, oResult);
    } catch (IOException e) {
      logger.error("Error extracting content for " + key, e);
    }
    return null;

  }

  public StoredContent read(LogRequest logRequest) {
    return read("", "", logRequest.getLogId().toString());
  }

  public void delete(LogRequest logRequest) {
    template.opsForValue().getOperations().delete(logRequest.getLogId());
  }

  @Override
  public void purge(String index) {
    logger.debug("Purge not supported for REDIS. Ignoring this request");
  }

  @Override
  public String ping() {
    Date when = new Date();
    template.opsForValue().setIfAbsent(-99999l, when.toString().getBytes());
    template.opsForValue().getOperations().delete(-99999l);
    return "OK - Redis";
  }

  @PostConstruct
  void status() {
    Logger logger = LoggerFactory.getLogger("configuration");
    logger.info("**** Deploying Redis repo manager");
  }

}
