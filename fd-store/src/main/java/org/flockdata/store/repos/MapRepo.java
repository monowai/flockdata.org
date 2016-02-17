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

package org.flockdata.store.repos;

import org.flockdata.store.KvContent;
import org.flockdata.store.LogRequest;
import org.flockdata.store.bean.KvContentBean;
import org.flockdata.track.bean.ContentInputBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple map to hold Key Values. Non-persistent and for testing purposes only
 */
@Component
public class MapRepo extends AbstractStore {

    Map<Long, ContentInputBean> map = new HashMap <>();

    public void add(KvContent contentBean) {
        map.put(contentBean.getId(), contentBean.getContent());
    }

    public KvContent getValue(LogRequest logRequest) {

        return new KvContentBean(logRequest.getLogId(), map.get(logRequest.getLogId()));
    }

    public void delete(LogRequest logRequest) {
        map.remove(logRequest.getLogId());
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
