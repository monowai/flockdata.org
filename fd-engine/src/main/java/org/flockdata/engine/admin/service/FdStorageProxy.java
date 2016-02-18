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
import org.flockdata.store.bean.KvContentBean;
import org.flockdata.track.bean.TrackResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Functional service point for interactions between fd-engine and fd-store
 *
 * Created by mike on 17/02/16.
 */
@Service
@Profile({"integration","production"})
public class FdStorageProxy implements StorageProxy {

    @Autowired (required = false) // Functional tests don't neeed these gateways
    StorageReader.ReadStorageGateway storeRead;

    @Autowired (required = false) // Functional tests don't neeed these gateways
    StorageWriter.WriteStorageGateway storeWrite;

    @Autowired (required = false) // Functional tests don't neeed these gateways
    StorageDelta.DeltaGateway storeCompare;

    @Override
    public void write(TrackResultBean resultBean) {
        storeWrite.doStoreWrite(new KvContentBean(resultBean));
    }

    @Override
    public KvContent read(Entity entity, Log log) {
        return read(new LogRequest(entity, log));
    }

    @Override
    public KvContent read(LogRequest logRequest) {
        return storeRead.read(logRequest);
    }

    /**
     * Determines if the lastLog and the incomingLog are the same
     * @param entity        owner of the log
     * @param existingLog   persisted log
     * @param incomingLog   notional log
     * @return
     */
    @Override
    public boolean compare(Entity entity, Log existingLog, Log incomingLog) {
        LogRequest logRequest = new LogRequest(entity, existingLog);
        KvContent existingContent = read(logRequest);

        return existingContent != null && isSame(logRequest, incomingLog, existingContent);

    }

    /**
     * Determine if the Log Content has changed
     *
     * @return false if different, true if same
     */
    boolean isSame(LogRequest logRequest, Log compareTo, KvContent existingContent) {
        if (logRequest.getLogId()== null)
            return false;


        boolean sameContentType = logRequest.getContentType().equals(compareTo.getContentType());

        return sameContentType &&
                (sameCheckSum(logRequest.getCheckSum(), compareTo) || logRequest.getContentType().equals("json") &&
                        sameJson(existingContent, compareTo.getContent()));

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
