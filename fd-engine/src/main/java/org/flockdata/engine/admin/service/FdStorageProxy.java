package org.flockdata.engine.admin.service;

import org.flockdata.engine.integration.StorageDelta;
import org.flockdata.engine.integration.StorageReader;
import org.flockdata.engine.integration.StorageWriter;
import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.store.KvContent;
import org.flockdata.store.LogRequest;
import org.flockdata.store.bean.DeltaBean;
import org.flockdata.store.bean.KvContentBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by mike on 17/02/16.
 */
@Service
public class FdStorageProxy implements StorageProxy {

    @Autowired
    StorageDelta.DeltaGateway storeCompare;

    @Autowired
    StorageReader.ReadStorageGateway storeReader;

    @Autowired
    StorageWriter.StorageGateway storeWriter;

    @Override
    public boolean isSame(Entity entity, Log lastLog, Log preparedLog) {
        return  storeCompare.isSame(new DeltaBean(new LogRequest(entity, lastLog), preparedLog));
    }

    @Override
    public KvContent getContent(Entity entity, Log lastChange) {
        return storeReader.read(new LogRequest(entity, lastChange));
    }

    @Override
    public void doStoreWrite(KvContentBean kvContentBean) {
        storeWriter.doStoreWrite(kvContentBean);
    }
}
