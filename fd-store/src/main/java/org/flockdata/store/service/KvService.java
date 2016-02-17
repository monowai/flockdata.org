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

package org.flockdata.store.service;

import org.flockdata.helper.FlockException;
import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.store.KvContent;
import org.flockdata.store.LogRequest;
import org.flockdata.store.bean.DeltaBean;
import org.flockdata.store.bean.KvContentBean;
import org.flockdata.track.bean.DeltaResultBean;
import org.flockdata.track.bean.TrackResultBean;

import java.io.IOException;

/**
 * User: mike
 * Date: 6/09/14
 * Time: 12:07 PM
 */
public interface KvService {
    String ping();

    void purge(String indexName);

    Log prepareLog(TrackResultBean content, Log log) throws IOException;

    KvContent getContent(LogRequest logRequest);

    void delete(Entity entity, Log change);

    boolean isSame(DeltaBean deltaBean);

    boolean sameJson(KvContent compareFrom, KvContent compareWith);

    DeltaResultBean getDelta(LogRequest logRequest, Log to);

    void doWrite( KvContentBean kvBean) throws FlockException;

//    // Resolves the kv store to use for a given trackResult
//    Store getKvStore(TrackResultBean trackResult);


}