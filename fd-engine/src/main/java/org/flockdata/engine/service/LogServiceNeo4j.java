/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.engine.service;

import org.flockdata.engine.repo.neo4j.EntityDaoNeo;
import org.flockdata.helper.FlockException;
import org.flockdata.kv.service.KvService;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityLog;
import org.flockdata.track.service.LogService;
import org.flockdata.track.service.SchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

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
    private KvService kvService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SearchServiceFacade searchService;

    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    EntityDaoNeo entityDao;

    @Autowired
    LogRetryService logRetryService;

    @Override
    public Collection<TrackResultBean> processLogsSync(Fortress fortress, Iterable<TrackResultBean> resultBeans) throws FlockException, IOException, ExecutionException, InterruptedException {

        Collection<TrackResultBean> logResults = new ArrayList<>();
        for (TrackResultBean resultBean : resultBeans) {
            logResults.add(processLogFromResult(fortress, resultBean));
        }
        //logger.debug("Logs processed. Proceeding to distribute changes");
        //distributeChanges(company, logResults);

        return logResults;

    }

    TrackResultBean processLogFromResult(Fortress fortress, TrackResultBean resultBean) throws FlockException, IOException, ExecutionException, InterruptedException {
        if (resultBean.getContentInput() == null)
            return resultBean;

        ContentInputBean contentInputBean = resultBean.getContentInput();
        logger.debug("writeLog {}", contentInputBean);
        TrackResultBean result = logRetryService.writeLog(fortress, resultBean);
        if (result.getLogResult().getStatus() == ContentInputBean.LogStatus.NOT_FOUND)
            throw new FlockException("Unable to find Entity ");
        // ToDo: KV should be written to first as a notional update
        kvService.doKvWrite(result); //ToDo: Consider KV not available. How to write the logs
        return result;
    }

    @Override
    public TrackResultBean writeLog(Entity entity, ContentInputBean input) throws FlockException, IOException, ExecutionException, InterruptedException {

        TrackResultBean resultBean = new TrackResultBean(entity);
        resultBean.setContentInput(input);
        ArrayList<TrackResultBean> logs = new ArrayList<>();
        logs.add(resultBean);
        Collection<TrackResultBean> results = processLogsSync(entity.getFortress(), logs);
        //logService.distributeChanges(entity.getFortress().getCompany(), results);
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

//    public void distributeChanges(Company company, Iterable<TrackResultBean> resultBeans) throws IOException {
//        logger.debug("Distributing changes to sub-services");
//        schemaService.registerConcepts(company, resultBeans);
//        searchService.makeChangesSearchable(resultBeans);
//        //logger.debug("Distributed changes to search service");
//    }

}
