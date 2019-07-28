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

package org.flockdata.engine.integration.store;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.engine.query.service.EsHelper;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.IndexManager;
import org.flockdata.search.EsSearchRequestResult;
import org.flockdata.search.QueryParams;
import org.flockdata.search.SearchSchema;
import org.flockdata.store.AbstractStore;
import org.flockdata.store.LogRequest;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.track.bean.ContentInputBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Support for no storage engine. This will simply use elasticsearch as a store for
 * "current state" content, i.e. against the Entity.
 * <p>
 * If you require the storage of versions of content (Log content), then use
 * FdStorageProxy which deals with content for a Log
 */
@Service
@Profile( {"fd-server"})
public class EsRepo extends AbstractStore {

  @Autowired
  private EsStoreRequest.ContentStoreEs gateway;

  @Autowired
  private IndexManager indexManager;

  @Autowired
  private EsHelper esHelper;

  private Logger logger = LoggerFactory.getLogger(EsRepo.class);

  public void add(StoredContent contentBean) {

  }

  public StoredContent read(LogRequest logRequest) {
    String index = indexManager.toIndex(logRequest.getEntity());
    String type = indexManager.parseType(logRequest.getEntity());
    String id = logRequest.getEntity().getSearchKey();
    return read(index, type, id);
  }

  @Override
  public StoredContent read(String index, String type, String id) {
    QueryParams queryParams = new QueryParams(index, type, id);

    ContentInputBean contentInput = new ContentInputBean();
    EsSearchRequestResult result = null;
    try {
      result = gateway.getData(queryParams);
    } catch (HttpServerErrorException e) {
      logger.error(e.getMessage());
    }

    if (result != null) {
      try {

        result.setIndex(index);
        result.setEntityType(type);
        if (result.getJson() != null) {
          Map<String, Object> map = JsonUtils.toMap(result.getJson());
          failIfError(map);
          map = esHelper.extractData(map);
          contentInput.setData((Map<String, Object>) map.get(SearchSchema.DATA));
        }
      } catch (FlockException | IOException e) {
        logger.error("Json issue", e);
      }
    }
    return new StorageBean(id, contentInput);

  }

  private void failIfError(Map<String, Object> map) throws FlockException {
    if (map == null) {
      return;
    }
    Object error = map.get("__errors__");
    if (error != null) {
      throw new FlockException(error.toString());
    }
  }

  private Map<String, Object> unwrapData(HashMap<String, Object> map) {
    if (!map.containsKey("hits")) {
      return null;
    }
    Map<String, Object> hits = (Map<String, Object>) map.get("hits");
    return null;
  }

  public void delete(LogRequest logRequest) {
    // ToDo: delete from ES
  }

  @Override
  public void purge(String index) {
    // not supported.
  }

  @Override
  public String ping() {

    return "EsStorage is OK";
  }
}
