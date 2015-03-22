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

import org.flockdata.kv.AbstractKvRepo;
import org.flockdata.kv.bean.KvContentBean;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.KvContent;
import org.flockdata.track.model.Log;
import org.springframework.stereotype.Component;

/**
 * Support for no storage engine. This will simply use elasticsearch as a store for content
 */
@Component
public class EsRepo extends AbstractKvRepo{

    public void add(KvContent contentBean) {

    }

    public KvContent getValue(Entity entity, Log forLog) {
        // ElasticSearch query
        return new KvContentBean(forLog, new ContentInputBean());
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
