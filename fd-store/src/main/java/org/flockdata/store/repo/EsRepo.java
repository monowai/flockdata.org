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

package org.flockdata.store.repo;

import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.search.IndexManager;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.flockdata.store.LogRequest;
import org.flockdata.store.StoreContent;
import org.flockdata.store.bean.StoreBean;
import org.flockdata.store.common.repos.AbstractStore;
import org.flockdata.store.integration.EsStoreRequest;
import org.flockdata.track.bean.ContentInputBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Support for no storage engine. This will simply use elasticsearch as a store for
 * current state content
 */
@Component
public class EsRepo extends AbstractStore {

    @Autowired
    EsStoreRequest.ContentStoreEs esStore;

    @Autowired
    IndexManager indexHelper;

    private Logger logger = LoggerFactory.getLogger(EsRepo.class);

    public void add(StoreContent contentBean) {

    }

    public StoreContent getValue(LogRequest logRequest)  {
        QueryParams queryParams = new QueryParams(indexHelper.parseIndex(logRequest.getEntity())
                , IndexManager.parseType(logRequest.getEntity())
                , logRequest.getEntity().getSearchKey() );

        ContentInputBean contentInput = new ContentInputBean();
        // DAT-347
        if ( logRequest.getEntity().getSearchKey() != null ){
            EsSearchResult result = esStore.getData(queryParams);

            if (result!=null )
                try {
                    // DAT-419 - there is a problem with UTF-8 conversion of SI using http, or at least the
                    //           way we have it configured. It throws a deep exception for the odd incoming payload
                    //           complaining that it's not valid UTF-8 text. This approach we're now using works.
                    if ( result.getJson() !=null ) {
                        HashMap map = JsonUtils.toObject(result.getJson(), HashMap.class);
                        contentInput.setData((Map<String, Object>) map.get(EntitySearchSchema.DATA));
                    }
                    //contentInput.setData(JsonUtils.getAsMap(result.getJson()));
                } catch (FlockException |IOException e) {
                    logger.error("Json issue", e);
                }


        }
        return new StoreBean(logRequest.getLogId(), contentInput);
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
