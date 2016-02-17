package org.flockdata.engine.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.flockdata.engine.integration.StorageDelta;
import org.flockdata.engine.integration.StorageReader;
import org.flockdata.engine.integration.StorageWriter;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.store.KvContent;
import org.flockdata.store.LogRequest;
import org.flockdata.store.bean.DeltaBean;
import org.flockdata.store.bean.KvContentBean;
import org.flockdata.track.bean.TrackResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Functional service point for interactions between fd-engine and fd-store
 *
 * Created by mike on 17/02/16.
 */
@Service
public class FdStorageProxy implements StorageProxy {

    @Autowired
    StorageReader.ReadStorageGateway storeRead;

    @Autowired
    StorageWriter.WriteStorageGateway storeWrite;

    @Autowired
    StorageDelta.DeltaGateway storeCompare;

    @Override
    public KvContent read(Entity entity, Log log) {
        return storeRead.read(new LogRequest(entity, log));
    }

    @Override
    public void write(TrackResultBean resultBean) {
        storeWrite.doStoreWrite(new KvContentBean(resultBean));
    }

    /**
     * Determines if the lastLog and the preparedLog are the same
     * @param entity
     * @param lastLog
     * @param preparedLog
     * @return
     */
    @Override
    public boolean compare(Entity entity, Log lastLog, Log preparedLog) {
        return  storeCompare.isSame(
                new DeltaBean(
                        new LogRequest(entity, lastLog),
                        preparedLog));
    }

    /**
     * Determine if the Log Content has changed
     *
     * @return false if different, true if same
     */
    @Override
    public boolean isSame(DeltaBean deltaBean) {
        if (deltaBean.getLogRequest().getLogId()== null)
            return false;

        KvContent content = storeRead.read(deltaBean.getLogRequest());

        if (content == null)
            return false;

        boolean sameContentType = deltaBean.getLogRequest().getContentType().equals(deltaBean.getPreparedLog().getContentType());

        return sameContentType &&
                (sameCheckSum(deltaBean.getLogRequest().getCheckSum(), deltaBean.getPreparedLog()) || deltaBean.getLogRequest().getContentType().equals("json") &&
                        sameJson(content, deltaBean.getPreparedLog().getContent()));

    }

    private boolean sameCheckSum(String compareFrom, Log compareTo) {
        return compareFrom.equals(compareTo.getChecksum());
    }

    private boolean sameJson(KvContent compareFrom, KvContent compareTo) {

        if (compareFrom.getData().size() != compareTo.getData().size())
            return false;
//        logger.trace("Comparing [{}] with [{}]", compareFrom, compareTo.getData());
        JsonNode jCompareFrom = FdJsonObjectMapper.getObjectMapper().valueToTree(compareFrom.getData());
        JsonNode jCompareWith = FdJsonObjectMapper.getObjectMapper().valueToTree(compareTo.getData());
        return !(jCompareFrom == null || jCompareWith == null) && jCompareFrom.equals(jCompareWith);

    }


}
