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

package org.flockdata.kv.none;

import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.kv.AbstractKvRepo;
import org.flockdata.kv.bean.KvContentBean;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.KvContent;
import org.flockdata.track.model.Log;
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
public class EsRepo extends AbstractKvRepo{

    @Autowired
    EsGateway esGateway;

    private Logger logger = LoggerFactory.getLogger(EsRepo.class);

    public void add(KvContent contentBean) {

    }

    public KvContent getValue(Entity entity, Log forLog)  {
        QueryParams queryParams = new QueryParams();
        queryParams.setCompany(entity.getFortress().getCompany().getName());
        queryParams.setTypes(entity.getDocumentType());
        queryParams.setFortress(entity.getFortress().getName());
        queryParams.setCallerRef(entity.getSearchKey());
        ContentInputBean contentInput = new ContentInputBean();
        // DAT-347
        if ( entity.getSearchKey() != null ){
            EsSearchResult result = esGateway.get(queryParams);

            if (result!=null )
                try {
                    // DAT-419 - there is a problem with UTF-8 conversion of SI using http, or at least the
                    //           way we have it configured. It throws a deep exception for the odd incoming payload
                    //           complaining that it's not valid UTF-8 text. This approach we're now using works.
                    HashMap map =JsonUtils.getBytesAsObject(result.getJson(), HashMap.class);
                    contentInput.setWhat((Map<String, Object>) map.get(EntitySearchSchema.WHAT));
                    //contentInput.setWhat(JsonUtils.getAsMap(result.getJson()));
                } catch (FlockException |IOException e) {
                    logger.error("Json issue", e);
                }


        }
        return new KvContentBean(forLog, contentInput);
    }

    public void delete(Entity entity, Log log) {
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
