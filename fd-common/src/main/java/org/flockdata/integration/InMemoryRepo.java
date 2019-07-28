/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.integration;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.flockdata.store.AbstractStore;
import org.flockdata.store.LogRequest;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.track.bean.ContentInputBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Simple map to hold Key Values. Non-persistent and for testing purposes only
 *
 * @tag Store
 */
@Component
@Profile( {"dev", "memstore"})
public class InMemoryRepo extends AbstractStore {

  Map<Object, ContentInputBean> map = new HashMap<>();
  private Logger logger = LoggerFactory.getLogger(InMemoryRepo.class);

  @PostConstruct
  void status() {
    LoggerFactory.getLogger("configuration").info("**** Deploying InMemory non-persistent repo manager");
  }

  public void add(StoredContent contentBean) {
    map.put(getKey(contentBean.getType(), contentBean.getId()), contentBean.getContent());
  }

  @Override
  public StoredContent read(String index, String type, String id) {
    ContentInputBean value = map.get(getKey(type, id));
    if (value == null) {
      return null;   // Emulate a NULL result on not found in other KV stores
    }

    return new StorageBean(id, value);
  }

  public String getKey(String type, Object id) {
//        return id.toString();
    return type.toLowerCase() + "." + id;
  }

  public StoredContent read(LogRequest logRequest) {

    return read("", logRequest.getType(), logRequest.getLogId().toString());
  }

  public void delete(LogRequest logRequest) {
    map.remove(logRequest.getLogId());
  }

  @Override
  public void purge(String index) {
    map.clear();
  }

  @Override
  public String ping() {
    return "OK - MemMap";
  }
}
