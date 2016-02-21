package org.flockdata.engine.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.flockdata.engine.integration.StorageGateway;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.shared.IndexManager;
import org.flockdata.store.LogRequest;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;
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

    @Autowired (required = false) // Functional tests don't require gateways
    StorageGateway storageGateway;

    @Autowired
    IndexManager indexManager;

    @Override
    public void write(TrackResultBean resultBean) {
        storageGateway.write(new StorageBean(resultBean));
    }

    @Override
    public StoredContent read(Entity entity, Log log) {
        return read(new LogRequest(entity, log));
    }

    @Override
    public StoredContent read(LogRequest logRequest) {
        String index = indexManager.parseIndex(logRequest.getStore(), logRequest.getEntity());
        String type = indexManager.parseType(logRequest.getEntity());
        String key =  indexManager.resolveKey(logRequest.getStore(), logRequest);
        StoredContent contentResult = storageGateway.read(logRequest.getStore(),
                index,
                type,
                key);

        return contentResult;
    }

    /**
     * Determines if the lastLog and the incomingLog are the same
     * @param entity        owner of the log
     * @param existingLog   persisted log
     * @param incomingLog   notional log
     * @return true if different false if the same
     */
    @Override
    public boolean compare(Entity entity, Log existingLog, Log incomingLog) {
        LogRequest logRequest = new LogRequest(entity, existingLog);
        StoredContent existingContent = read(logRequest);

        return existingContent != null && isSame(logRequest, incomingLog, existingContent);

    }

    /**
     * Determine if the Log Content has changed
     *
     * @return false if different, true if same
     */
    boolean isSame(LogRequest logRequest, Log compareTo, StoredContent existingContent) {
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

    private boolean sameJson(StoredContent compareFrom, StoredContent compareTo) {

        if (compareFrom.getData().size() != compareTo.getData().size())
            return false;
//        logger.trace("Comparing [{}] with [{}]", compareFrom, compareTo.getData());
        JsonNode jCompareFrom = FdJsonObjectMapper.getObjectMapper().valueToTree(compareFrom.getData());
        JsonNode jCompareWith = FdJsonObjectMapper.getObjectMapper().valueToTree(compareTo.getData());
        return !(jCompareFrom == null || jCompareWith == null) && jCompareFrom.equals(jCompareWith);

    }


}
