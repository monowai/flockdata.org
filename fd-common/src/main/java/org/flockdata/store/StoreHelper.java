/*
 *  Copyright 2012-2017 the original author or authors.
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
    if (trackResult.getDocumentType() == null) {
      return Store.NONE;
    }

    if (trackResult.getDocumentType().getVersionStrategy() == Document.VERSION.ENABLE) {
      return defaultStore;
    }

    if (trackResult.getDocumentType().getVersionStrategy() == Document.VERSION.DISABLE) {
      return Store.NONE;
    }

    Entity entity = trackResult.getEntity();
    Segment segment = entity.getSegment();

    // Check against the fortress default
    Store storage;
    if (segment.getFortress().isStoreEnabled()) {
      storage = defaultStore;
    } else {
      storage = Store.NONE;
    }
    return storage;
  }

  public static boolean isMockable(Entity entity, Document documentType) {
    return documentType.getVersionStrategy() == Document.VERSION.DISABLE || (documentType.getVersionStrategy() == Document.VERSION.FORTRESS && !entity.getFortress().isStoreEnabled());
  }

}
