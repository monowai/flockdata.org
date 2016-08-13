/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.flockdata.engine.integration.store.EsRepo;
import org.flockdata.engine.integration.store.StorageReader;
import org.flockdata.engine.integration.store.StorageWriter;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Entity;
import org.flockdata.model.Log;
import org.flockdata.shared.IndexManager;
import org.flockdata.store.LogRequest;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.track.bean.TrackResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Functional service point for interactions between fd-engine and fd-store
 *
 * Created by mike on 17/02/16.
 */
@Service
@Profile({"fd-server"})
public class FdStorageProxy implements StorageProxy {

    @Autowired (required = false)
    StorageReader.StorageReaderGateway readGateway;

    @Autowired (required = false)
    StorageWriter.StorageWriterGateway writeGateway;

    private Logger logger = LoggerFactory.getLogger(FdStorageProxy.class);

    @Autowired
    IndexManager indexManager;

    @Autowired   (required = false)
    EsRepo esRepo;

    @Override
    @Retryable
    public void write(TrackResultBean resultBean) {
        StorageBean storageBean = new StorageBean(resultBean);
        // If there is no store to write to then don't !
        if ( !storageBean.getStore().equals( Store.NONE.name()))
            writeGateway.write(storageBean);
    }

    @Override
    public StoredContent read(Entity entity, Log log) {
        return read(new LogRequest(entity, log));
    }

    @Override
    public StoredContent read(LogRequest logRequest) {
        String index = indexManager.toStoreIndex(logRequest.getStore(), logRequest.getEntity());
        String type  = indexManager.parseType(logRequest.getEntity());
        String key   ;
        try {
            key = indexManager.resolveKey(logRequest);
        } catch (NotFoundException e) {
            logger.error ( e.getMessage());
            return null;
        }
        StoredContent contentResult;
        if ( logRequest.getStore() == Store.NONE){
            contentResult = esRepo.read(index,type,key);
        } else {
            try {
                contentResult = readGateway.read(logRequest.getStore(),
                        index,
                        type,
                        key);
            } catch (HttpClientErrorException nfe){
                logger.debug("Request caused error - {} - for {}/{}/{} was not found", nfe.getMessage(), index,type,key);
                return null;
            }
        }

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
