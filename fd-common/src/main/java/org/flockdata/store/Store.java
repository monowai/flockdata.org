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

package org.flockdata.store;

import org.flockdata.model.DocumentType;
import org.flockdata.model.Entity;
import org.flockdata.model.FortressSegment;
import org.flockdata.model.Log;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.track.bean.TrackResultBean;

import java.io.IOException;

/**
 * ENUM and Log preparation helpers
 * Created by mike on 17/02/16.
 */
public enum Store {
    REDIS, RIAK, MEMORY, NONE ;

    public static Log prepareLog (Store defaultStore, TrackResultBean trackResult, Log log) throws IOException {
        Store storage = resolveStore(trackResult, defaultStore);
        StoredContent storedContent = new StorageBean(trackResult);
        storedContent.setStore(storage.name());
        log.setStorage(storage.name());
        log.setContent(storedContent);
        log.setChecksum(storedContent.getChecksum());
        return log;
    }

    public static Store resolveStore(TrackResultBean trackResult, Store defaultStore) {
        if ( trackResult.getDocumentType()== null)
            return Store.NONE;

        if (trackResult.getDocumentType().getVersionStrategy() == DocumentType.VERSION.ENABLE)
            return defaultStore;

        if (trackResult.getDocumentType().getVersionStrategy() == DocumentType.VERSION.DISABLE)
            return Store.NONE;

        Entity entity = trackResult.getEntity();
        FortressSegment segment = entity.getSegment();

        // Check against the fortress default
        Store storage;
        if (segment.getFortress().isStoreEnabled())
            storage = defaultStore;
        else
            storage = Store.NONE;
        return storage;
    }
}
