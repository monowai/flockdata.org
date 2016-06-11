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

package org.flockdata.engine.meta.service;

import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.service.FortressService;
import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.kernel.api.exceptions.TransactionFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.transaction.HeuristicRollbackException;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/**
 * User: mike
 * Date: 2/12/14
 * Time: 7:48 AM
 */
@Service
@Async("fd-engine")
public class DocTypeRetryService {
    @Autowired
    ConceptService conceptService;

    @Autowired
    FortressService fortressService;

    private Logger logger = LoggerFactory.getLogger(DocTypeRetryService.class);

    /**
     * Creates document types for the input beans if they do not exist
     * Handles linked entities which may be part of the EntityInputBean.
     * Ensures the fortress also exists for the linked Entities
     *
     * @param fortress   all InputBeans are deemed to belong to this fortress
     * @param inputBeans collection of Entities from which to find DocumentTypes and linked Entities
     * @return Collection of DocumentType objects that were created
     */
    @Retryable(include = {TransactionFailureException.class, HeuristicRollbackException.class, DataRetrievalFailureException.class, InvalidDataAccessResourceUsageException.class, ConcurrencyFailureException.class, DeadlockDetectedException.class}, maxAttempts = 20, backoff = @Backoff(delay = 150, maxDelay = 500))
    public Future<Collection<DocumentType>> createDocTypes(Fortress fortress, List<EntityInputBean> inputBeans) {

        return new AsyncResult<>(makeInTransaction(fortress, inputBeans));
    }

    @Transactional
    Collection<DocumentType> makeInTransaction(Fortress fortress, List<EntityInputBean> inputBeans) {
        Collection<DocumentType> docTypes = new ArrayList<>();
        DocumentType master;
        for (EntityInputBean entityInputBean : inputBeans) {
            master = new DocumentType(fortress, entityInputBean.getDocumentType());
            if (!docTypes.contains(master)) {
                master = conceptService.findOrCreate(fortress, master);
                docTypes.add(master);
                if (!entityInputBean.getEntityLinks().isEmpty()) {

                    // The entity being processed is linked to other entities.
                    // need to ensure that both the Fortress and DocumentType are also created
                    for (String relationship : entityInputBean.getEntityLinks().keySet()) {
                        for (EntityKeyBean entityKeyBean : entityInputBean.getEntityLinks().get(relationship)) {
                            Fortress subFortress;

                            if (!fortress.getName().equals(entityKeyBean.getFortressName()))
                                subFortress = fortressService.registerFortress(fortress.getCompany(), entityKeyBean.getFortressName());
                            else
                                subFortress = fortress;

                            DocumentType linkedDocument = new DocumentType(subFortress, entityKeyBean.getDocumentType());
                            if (!docTypes.contains(linkedDocument)) {
                                linkedDocument = conceptService.findOrCreate(subFortress, linkedDocument);
                                docTypes.add(linkedDocument);
                                conceptService.linkEntities(master, relationship, linkedDocument);
                            }
                        }
                    }
                }

            }
        }
        logger.debug("Finished result = {}" + docTypes.size());
        return docTypes;
    }

}
