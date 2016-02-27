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

package org.flockdata.engine.admin.service;

import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.store.LogRequest;
import org.flockdata.store.StoredContent;
import org.flockdata.track.bean.TrackResultBean;

/**
 * Created by mike on 17/02/16.
 */
public interface StorageProxy {

    void write(TrackResultBean trackResult);

    StoredContent read(Entity entity, Log lastChange);

    StoredContent read(LogRequest logRequest) ;

    boolean compare(Entity entity, Log lastLog, Log preparedLog);


}
