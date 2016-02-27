/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
