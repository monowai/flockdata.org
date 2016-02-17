package org.flockdata.engine.admin.service;

import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.store.KvContent;
import org.flockdata.store.bean.DeltaBean;
import org.flockdata.track.bean.TrackResultBean;

/**
 * Created by mike on 17/02/16.
 */
public interface StorageProxy {

    boolean compare(Entity entity, Log lastLog, Log preparedLog);

    KvContent read(Entity entity, Log lastChange);

    void write(TrackResultBean kvContentBean);

    public boolean isSame(DeltaBean deltaBean);
}
