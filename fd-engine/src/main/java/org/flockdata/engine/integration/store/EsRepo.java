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

package org.flockdata.engine.integration.store;

import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.flockdata.search.model.SearchSchema;
import org.flockdata.shared.IndexManager;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Support for no storage engine. This will simply use elasticsearch as a store for
 * "current state" content, i.e. against the Entity.
 *
 * If you require the storage of versions of content (Log content), then use
 * FdStorageProxy which deals with content for a Log
 *
 */
@Service
@Profile({"fd-server"})
public class EsRepo extends AbstractStore {

    @Autowired
    EsStoreRequest.ContentStoreEs gateway;

    @Autowired
    IndexManager indexManager;

    private Logger logger = LoggerFactory.getLogger(EsRepo.class);

    public void add(StoredContent contentBean) {

    }

    public StoredContent read(LogRequest logRequest) {
        String index = indexManager.parseIndex(logRequest.getEntity());
        String type = indexManager.parseType(logRequest.getEntity());
        String id = logRequest.getEntity().getSearchKey();
        return read (index, type, id);
    }

    @Override
    public StoredContent read(String index, String type, String id) {
        QueryParams queryParams = new QueryParams(index, type, id);

        ContentInputBean contentInput = new ContentInputBean();

        EsSearchResult result = gateway.getData(queryParams);

        if (result != null)
            try {

                result.setIndex (index);
                result.setEntityType (type);
                if (result.getJson() != null) {
                    HashMap map = JsonUtils.toObject(result.getJson(), HashMap.class);
                    contentInput.setData((Map<String, Object>) map.get(SearchSchema.DATA));
                    if( map.get("name")!=null)
                        contentInput.getData().put("_name", map.get("name"));
                    if ( map.get("description")!=null)
                        contentInput.getData().put("_description", map.get("description"));
                }
            } catch (FlockException | IOException e) {
                logger.error("Json issue", e);
            }
        return new StorageBean(id, contentInput);

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
