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

package org.flockdata.engine.track.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.data.FortressUser;
import org.flockdata.engine.admin.service.StorageProxy;
import org.flockdata.engine.configure.EngineConfig;
import org.flockdata.engine.data.dao.EntityDaoNeo;
import org.flockdata.engine.data.graph.EntityLog;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.LogNode;
import org.flockdata.helper.FlockException;
import org.flockdata.store.StoredContent;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mholdsworth
 * @since 21/06/2014
 */
@Service
@Transactional
public class LogServiceNeo4j implements LogService {
  private final StorageProxy storageProxy;
  private final EntityDaoNeo entityDao;
  private final LogRetryService logRetryService;
  private Logger logger = LoggerFactory.getLogger(LogServiceNeo4j.class);

  @Autowired
  public LogServiceNeo4j(EngineConfig engineConfig, StorageProxy storageProxy, EntityDaoNeo entityDao, LogRetryService logRetryService) {
    this.storageProxy = storageProxy;
    this.entityDao = entityDao;
    this.logRetryService = logRetryService;
  }

  @Override
  @Async("fd-log")
  @Transactional(timeout = 4000)
  public Future<Collection<TrackResultBean>> processLogs(Fortress fortress, Collection<TrackResultBean> resultBeans) throws FlockException, ExecutionException, InterruptedException {
    // ToDo - ServiceActivator
    //Collection<TrackResultBean> logResults = new ArrayList<>();
    for (TrackResultBean resultBean : resultBeans) {
      processLogFromResult(fortress, resultBean);
    }
    return new AsyncResult<>(resultBeans);
  }

  @Override
  @Transactional(timeout = 4000)
  public Collection<TrackResultBean> processLogsSync(Fortress fortress, Collection<TrackResultBean> resultBeans) throws FlockException, InterruptedException, ExecutionException {
    // ToDo - ServiceActivator
    for (TrackResultBean resultBean : resultBeans) {
      processLogFromResult(fortress, resultBean);
    }
    return resultBeans;
  }

  private void processLogFromResult(Fortress fortress, TrackResultBean resultBean) throws FlockException, ExecutionException, InterruptedException {
    // ToDo: Service Activator
    if (resultBean.getContentInput() == null) {
      return;
    }

    ContentInputBean contentInputBean = resultBean.getContentInput();
    logger.debug("writeLog {}", contentInputBean);
    logRetryService.writeLog((FortressNode) fortress, resultBean);
    if (resultBean.getLogStatus() == ContentInputBean.LogStatus.NOT_FOUND) {
      throw new FlockException("Unable to find Entity ");
    }

    if (resultBean.getContentInput() != null && !resultBean.isLogIgnored()) {
      // Log is now prepared (why not just get KvContent??
      if (fortress.isStoreEnabled()) {
        storageProxy.write(resultBean);
      }
    }
  }

  @Override
  public TrackResultBean writeLog(Document documentType, Entity entity, ContentInputBean input, FortressUser fu) throws FlockException, IOException, ExecutionException, InterruptedException {

    TrackResultBean resultBean = new TrackResultBean(entity, documentType);

    resultBean.setContentInput(input);
    ArrayList<TrackResultBean> logs = new ArrayList<>();
    logs.add(resultBean);
    Collection<TrackResultBean> results = processLogs(entity.getFortress(), logs).get();
    return results.iterator().next();
  }


  @Override
  @Transactional
  public EntityLog getLastLog(Entity entity) throws FlockException {
    if (entity == null || entity.getId() == null) {
      return null;
    }
    logger.trace("Getting lastLog MetaID [{}]", entity.getId());
    return entityDao.getLastLog(entity.getId());
  }

  @Override
  public StoredContent getContent(Entity entity, LogNode log) {
    return storageProxy.read(entity, log);
  }


}
