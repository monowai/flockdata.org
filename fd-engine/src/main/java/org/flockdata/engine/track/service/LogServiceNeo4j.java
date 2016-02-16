/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.track.service;

import org.flockdata.authentication.registration.service.CompanyService;
import org.flockdata.engine.dao.EntityDaoNeo;
import org.flockdata.helper.FlockException;
import org.flockdata.kv.bean.KvContentBean;
import org.flockdata.kv.service.KvService;
import org.flockdata.model.*;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.service.FortressService;
import org.flockdata.track.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 21/06/14
 * Time: 12:56 PM
 */
@Service
@Transactional
public class LogServiceNeo4j implements LogService {
    private Logger logger = LoggerFactory.getLogger(LogServiceNeo4j.class);

    @Autowired
    private KvService kvManager;

    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    ConceptService conceptService;

    @Autowired
    EntityDaoNeo entityDao;

    @Autowired
    LogRetryService logRetryService;

    @Override
    @Async ("fd-log")
    @Transactional (timeout = 4000)
    public Future<Collection<TrackResultBean>> processLogs(Fortress fortress, Collection<TrackResultBean> resultBeans) throws FlockException, IOException, ExecutionException, InterruptedException {
        // ToDo - ServiceActivator
        //Collection<TrackResultBean> logResults = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            processLogFromResult(fortress, resultBean);
        }
        return new AsyncResult<>(resultBeans);
    }

    @Override
    @Transactional (timeout = 4000)
    public Collection<TrackResultBean> processLogsSync(Fortress fortress, Collection<TrackResultBean> resultBeans) throws FlockException, InterruptedException, ExecutionException, IOException {
        // ToDo - ServiceActivator
        //Collection<TrackResultBean> logResults = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            processLogFromResult(fortress, resultBean);
        }
        return resultBeans;
    }

    private void processLogFromResult(Fortress fortress, TrackResultBean resultBean) throws FlockException, IOException, ExecutionException, InterruptedException {
        // ToDo: Service Activator
        if (resultBean.getContentInput() == null)
            return ;

        ContentInputBean contentInputBean = resultBean.getContentInput();
        logger.debug("writeLog {}", contentInputBean);
        logRetryService.writeLog(fortress, resultBean);
        if (resultBean.getLogStatus() == ContentInputBean.LogStatus.NOT_FOUND)
            throw new FlockException("Unable to find Entity ");

        if (resultBean.getContentInput() != null
                && !resultBean.isLogIgnored()) {
            //if ( resultBean.getEntityInputBean() == null || !resultBean.getEntityInputBean().isTrackSuppressed()) {
                KvContentBean kvContentBean = new KvContentBean(resultBean);
                kvManager.doWrite(resultBean, kvContentBean);
            //}
        }
    }

    @Override
    public TrackResultBean writeLog(DocumentType documentType, Entity entity, ContentInputBean input, FortressUser fu) throws FlockException, IOException, ExecutionException, InterruptedException {

        TrackResultBean resultBean = new TrackResultBean(entity,documentType);

        resultBean.setContentInput(input);
        ArrayList<TrackResultBean> logs = new ArrayList<>();
        logs.add(resultBean);
        Collection<TrackResultBean> results = processLogs(entity.getFortress(), logs).get();
        return results.iterator().next();
    }


    @Override
    @Transactional
    public EntityLog getLastLog(Entity entity) throws FlockException {
        if (entity == null || entity.getId() == null)
            return null;
        logger.trace("Getting lastLog MetaID [{}]", entity.getId());
        return entityDao.getLastLog(entity.getId());
    }

}
