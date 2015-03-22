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
        queryParams.setCallerRef(entity.getCallerRef());

        // DAT-347
        EsSearchResult result = esGateway.get(queryParams);
        ContentInputBean contentInput = new ContentInputBean();
        if (result!=null )
            try {
                contentInput.setWhat((Map<String, Object>) result.getWhat().get(EntitySearchSchema.WHAT));
            } catch (FlockException e) {
                logger.error("Json issue", e);
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
