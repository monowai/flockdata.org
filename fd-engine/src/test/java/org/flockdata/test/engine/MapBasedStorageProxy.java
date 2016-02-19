package org.flockdata.test.engine;

import org.flockdata.engine.admin.service.FdStorageProxy;
import org.flockdata.store.LogRequest;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.store.common.repos.InMemoryRepo;
import org.flockdata.track.bean.TrackResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 *
 * Simple map based storage proxy for testing. Overrides methods and imports the basic MapRepo from kv-store
 *
 * Created by mike on 18/02/16.
 */
@Service
@Profile({"dev"})

public class MapBasedStorageProxy extends FdStorageProxy {
    @Autowired
    InMemoryRepo inMemoryRepo;

    @Override
    public void write(TrackResultBean trackResult) {
        StoredContent content = new StorageBean(trackResult);
        inMemoryRepo.add(content);
    }

    @Override
    public StoredContent read(LogRequest logRequest) {
        return inMemoryRepo.read( logRequest);
    }

}
