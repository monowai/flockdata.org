/*
 * Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.shared;

import org.flockdata.store.AbstractStore;
import org.flockdata.store.LogRequest;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.track.bean.ContentInputBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple map to hold Key Values. Non-persistent and for testing purposes only
 */
@Component
@Profile("dev")
public class InMemoryRepo extends AbstractStore {

    Map<Object, ContentInputBean> map = new HashMap <>();

    public void add(StoredContent contentBean) {
        map.put(getKey(contentBean.getType(), contentBean.getId()), contentBean.getContent());
    }

    @Override
    public StoredContent read(String index, String type, Object id) {
        return new StorageBean(id, map.get(getKey(type, id)));
    }

    public String getKey(String type, Object id) {
//        return id.toString();
        return type.toLowerCase()+"."+id;
    }

    public StoredContent read(LogRequest logRequest) {

        return read("", logRequest.getType(), logRequest.getLogId());
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
