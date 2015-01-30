/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.kv.memory;

import org.flockdata.kv.KvRepo;
import org.flockdata.kv.bean.KvContentBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple map to hold Key Values. Non-persistent and for testing purposes only
 */
@Component
public class MapRepo implements KvRepo {

    private static Logger logger = LoggerFactory.getLogger(MapRepo.class);

    Map<Long, byte[]> map = new HashMap <>();

    public void add(KvContentBean contentBean) {
        map.put(contentBean.getLogId(), contentBean.getEntityContent());
    }

    public byte[] getValue(Entity entity, Log forLog) {

        return map.get(forLog.getId());
    }

    public void delete(Entity entity, Log log) {
        map.remove(log.getId());
    }

    @Override
    public void purge(String index) {
        map.clear() ;
    }

    @Override
    public String ping() {

        return "MemMap is OK";
    }
}
