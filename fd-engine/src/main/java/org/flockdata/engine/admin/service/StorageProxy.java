package org.flockdata.engine.admin.service;

import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.store.LogRequest;
import org.flockdata.store.StoreContent;
import org.flockdata.track.bean.TrackResultBean;

/**
 * Created by mike on 17/02/16.
 */
public interface StorageProxy {

    void write(TrackResultBean trackResult);

    StoreContent read(Entity entity, Log lastChange);

    StoreContent read(LogRequest logRequest) ;

    boolean compare(Entity entity, Log lastLog, Log preparedLog);


}
