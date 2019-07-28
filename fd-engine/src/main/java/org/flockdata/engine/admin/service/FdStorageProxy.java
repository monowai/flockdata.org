/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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
import org.flockdata.data.Entity;
import org.flockdata.data.Log;
import org.flockdata.engine.integration.store.EsRepo;
import org.flockdata.engine.integration.store.StorageReader;
import org.flockdata.engine.integration.store.StorageWriter;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.NotFoundException;
import org.flockdata.integration.IndexManager;
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
 * @author mholdsworth
 * @since 17/02/2016
 */
@Service
@Profile( {"fd-server"})
public class FdStorageProxy implements StorageProxy {

  private StorageReader.StorageReaderGateway readGateway;
  private StorageWriter.StorageWriterGateway writeGateway;
  private IndexManager indexManager;
  private EsRepo esRepo;
  private Logger logger = LoggerFactory.getLogger(FdStorageProxy.class);

  @Autowired(required = false)
  public void setFdStorageProxy(StorageReader.StorageReaderGateway readGateway) {
    this.readGateway = readGateway;
  }

  @Autowired(required = false)
  void setStorageWriter(StorageWriter.StorageWriterGateway writer) {
    this.writeGateway = writer;
  }

  @Autowired(required = false)
  void setEsRepo(EsRepo esRepo) {
    this.esRepo = esRepo;
  }

  @Autowired
  void setIndexManager(IndexManager indexManager) {
    this.indexManager = indexManager;
  }

  @Override
  @Retryable
  public void write(TrackResultBean resultBean) {
    writeGateway.write(new StorageBean(resultBean));
  }

  @Override
  public StoredContent read(Entity entity, Log log) {
    return read(new LogRequest(entity, log));
  }

  @Override
  public StoredContent read(LogRequest logRequest) {
    String index = indexManager.toStoreIndex(logRequest.getStore(), logRequest.getEntity());
    String type = indexManager.parseType(logRequest.getEntity());
    String key;
    try {
      key = indexManager.resolveKey(logRequest);
    } catch (NotFoundException e) {
      logger.error(e.getMessage());
      return null;
    }
    StoredContent contentResult;
    if (logRequest.getStore() == Store.NONE) {
      contentResult = esRepo.read(index, type, key);
    } else {
      try {
        contentResult = readGateway.read(logRequest.getStore(),
            index,
            type,
            key);
      } catch (HttpClientErrorException nfe) {
        logger.debug("Request caused error - {} - for {}/{}/{} was not found", nfe.getMessage(), index, type, key);
        return null;
      }
    }

    return contentResult;
  }

  /**
   * Determines if the lastLog and the incomingLog are the same
   *
   * @param entity      owner of the log
   * @param existingLog persisted log
   * @param incomingLog notional log
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
   * @tag Delta, Storage
   */
  private boolean isSame(LogRequest logRequest, Log compareTo, StoredContent existingContent) {
    if (logRequest.getLogId() == null) {
      return false;
    }

    boolean sameContentType = logRequest.getContentType().equals(compareTo.getContentType());

    return sameContentType &&
        (sameCheckSum(logRequest.getCheckSum(), compareTo) || logRequest.getContentType().equals("json") &&
            sameJson(existingContent, compareTo.getContent()));

  }

  private boolean sameCheckSum(String compareFrom, Log compareTo) {
    return compareFrom.equals(compareTo.getChecksum());
  }

  private boolean sameJson(StoredContent compareFrom, StoredContent compareTo) {

    if (compareFrom.getData().size() != compareTo.getData().size()) {
      return false;
    }
//        logger.trace("Comparing [{}] with [{}]", compareFrom, compareTo.getData());
    JsonNode jCompareFrom = FdJsonObjectMapper.getObjectMapper().valueToTree(compareFrom.getData());
    JsonNode jCompareWith = FdJsonObjectMapper.getObjectMapper().valueToTree(compareTo.getData());
    return !(jCompareFrom == null || jCompareWith == null) && jCompareFrom.equals(jCompareWith);

  }


}
