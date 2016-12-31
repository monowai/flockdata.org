/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.Segment;
import org.flockdata.track.bean.TrackResultBean;

/**
 * @author mike
 * @tag
 * @since 4/01/17
 */
public class StoreHelper {

    public static Store resolveStore(TrackResultBean trackResult, Store defaultStore) {
        if ( trackResult.getDocumentType()== null)
            return Store.NONE;

        if (trackResult.getDocumentType().getVersionStrategy() == Document.VERSION.ENABLE)
            return defaultStore;

        if (trackResult.getDocumentType().getVersionStrategy() == Document.VERSION.DISABLE)
            return Store.NONE;

        Entity entity = trackResult.getEntity();
        Segment segment = entity.getSegment();

        // Check against the fortress default
        Store storage;
        if (segment.getFortress().isStoreEnabled())
            storage = defaultStore;
        else
            storage = Store.NONE;
        return storage;
    }
}
