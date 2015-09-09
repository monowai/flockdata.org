package org.flockdata.search.service;

import org.flockdata.track.bean.SearchChange;

/**
 * Deals with Mapping and Settings for indexes
 *
 * Created by mike on 10/09/15.
 */
public interface IndexMappingService {

    boolean ensureIndexMapping(SearchChange change);
}
