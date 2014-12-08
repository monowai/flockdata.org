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

package org.flockdata.kv;

import org.flockdata.kv.bean.KvContentBean;
import org.flockdata.track.bean.EntityBean;
import org.flockdata.track.model.Log;

import java.io.IOException;

/**
 * User: Mike Holdsworth
 * Since: 31/01/14
 */
public interface KvRepo {
    public void add(KvContentBean contentBean) throws IOException;

    public byte[] getValue(EntityBean entity, Log forLog);

    public void delete(EntityBean entity, Log log);

    public void purge(String index);

    String ping();
}