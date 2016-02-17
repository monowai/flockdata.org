package org.flockdata.engine.admin.service;

import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.store.KvContent;
import org.flockdata.store.bean.KvContentBean;

/**
 * Created by mike on 17/02/16.
 */
public interface StorageProxy {

    boolean isSame (Entity entity, Log lastLog, Log preparedLog);

    KvContent getContent(Entity entity, Log lastChange);

    void doStoreWrite(KvContentBean kvContentBean);
}
