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

package org.flockdata.engine.admin;

import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.query.service.SearchServiceFacade;
import org.flockdata.helper.FlockException;
import org.flockdata.kv.service.KvService;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityLog;
import org.flockdata.track.model.SearchChange;
import org.flockdata.track.service.EntityService;
import org.flockdata.track.service.FortressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

/**
 * General maintenance activities across fortress entities. Async in nature
 *
 * Created by mike on 25/03/15.
 */
@Service
public class AdminService implements EngineAdminService {

    @Autowired
    EntityService entityService;

    @Autowired
    SearchServiceFacade searchService;

    @Autowired
    KvService kvService;

    @Autowired
    FortressService fortressService;

    @Autowired
    PlatformConfig engineConfig;

    private Logger logger = LoggerFactory.getLogger(AdminService.class);

    @Async("fd-engine")
    public Future<Boolean> purge(Company company, Fortress fortress) throws FlockException {
        // ToDo: Needs to be heavily reworked
        // Rename the exiting fortress and flag it as deleted.
        // Batch the entities for deletion. Log Content could be stored across multiple KVstores for a
        // single fortress
        String indexName = fortress.getIndexName();
        entityService.purge(fortress);
        if ( fortress.isStoreEnabled() && engineConfig.getKvStore()!= KvService.KV_STORE.NONE) {
            logger.info("Purging KV");
            kvService.purge(fortress.getIndexName());
        }
        fortressService.purge(fortress);
        engineConfig.resetCache();
        searchService.purge(fortress.getIndexName());
        logger.info ("Completed purge of indexed data [{}]", indexName);
        return new AsyncResult<>(true);

    }

    @Override
    @Async("fd-track")
    @Transactional
    public Future<Long> doReindex(Fortress fortress) throws FlockException {
        long result = reindex(fortress);
        logger.info("Reindex Search request completed. Processed [" + result + "] entities for [" + fortress.getName() + "]");
        return new AsyncResult<>(result);
    }

    @Override
    @Async("fd-engine")
    @Transactional
    public Future<Long> doReindex(Fortress fortress, String docType) {
        long result = reindexByDocType(fortress, docType);
        logger.info("Reindex Search request completed. Processed [" + result + "] entities for [" + fortress.getName() + "]");
        return new AsyncResult<>(result);

    }

    long reindex(Fortress fortress) {
        Long processCount =0l;
        Collection<Entity> entities;
        do {
            entities = entityService.getEntities(fortress, processCount);
            if (entities.isEmpty())
                return processCount;
            processCount = processCount + entities.size();
            reindexEntities(fortress.getCompany(), entities, processCount);

        } while ( !entities.isEmpty());
        return processCount;
    }

    long reindexByDocType(Fortress fortress, String docType) {
        Long processCount =0l;
        Collection<Entity> entities;
        do {
            entities = entityService.getEntities(fortress, docType, processCount);
            if (entities.isEmpty())
                return processCount;
            processCount = processCount + entities.size();
            reindexEntities(fortress.getCompany(), entities, processCount);

        } while ( !entities.isEmpty());
        return processCount;
    }

    @Async("fd-track")
    @Transactional
    Long reindexEntities(Company company, Collection<Entity> entities, Long skipCount) {
        Collection<SearchChange> searchDocuments = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            EntityLog lastLog = entityService.getLastEntityLog(entity.getId());
            if ( !lastLog.isMocked()) {
                EntitySearchChange searchDoc = searchService.rebuild(entity, lastLog);
                if (searchDoc != null && entity.getFortress().isSearchActive() && !entity.isSearchSuppressed())
                    searchDocuments.add(searchDoc);
            }
        }
        searchService.makeChangesSearchable(searchDocuments);
        return skipCount;
    }
}
