package org.flockdata.engine.admin.service;

import org.flockdata.engine.integration.StorageDelta;
import org.flockdata.engine.integration.StorageReader;
import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.store.KvContent;
import org.flockdata.store.bean.DeltaBean;
import org.flockdata.store.bean.ReadContentBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by mike on 17/02/16.
 */
@Service
public class StorageProxy {

    @Autowired
    StorageDelta.DeltaGateway deltaGateway;

    @Autowired
    StorageReader.ReadStorageGateway storageGateway;

    public boolean isSame (Entity entity, Log lastLog, Log preparedLog) {
        return  deltaGateway.isSame(new DeltaBean(entity, lastLog, preparedLog));
    }



    public KvContent getContent(Entity entity, Log lastChange) {
        return storageGateway.read(new ReadContentBean(entity, lastChange));
    }
}
