package org.flockdata.test.engine;

import org.flockdata.engine.admin.service.FdStorageProxy;
import org.flockdata.store.KvContent;
import org.flockdata.store.LogRequest;
import org.flockdata.store.bean.KvContentBean;
import org.flockdata.store.common.repos.MapRepo;
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
    MapRepo mapRepo;

    @Override
    public void write(TrackResultBean trackResult) {
        KvContent content = new KvContentBean(trackResult);
        mapRepo.add(content);
    }

    @Override
    public KvContent read(LogRequest logRequest) {
        return mapRepo.getValue( logRequest);
    }

}
