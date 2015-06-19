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

package org.flockdata.engine.schema.service;

import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.schema.dao.SchemaDaoNeo4j;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.track.bean.ConceptInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.model.DocumentType;
import org.flockdata.track.service.SchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * User: mike
 * Date: 16/06/14
 * Time: 7:43 AM
 */
@Service
public class SchemaServiceNeo4j implements SchemaService {
    @Autowired
    SchemaDaoNeo4j schemaDao;

    @Autowired
    ConceptDaoNeo4j conceptDao;

    @Autowired
    PlatformConfig engineConfig;

    static Logger logger = LoggerFactory.getLogger(SchemaServiceNeo4j.class);

    public Boolean ensureSystemIndexes(Company company) {
        return schemaDao.ensureSystemConstraints(company);
    }

    @Override
    @Transactional
    public void registerConcepts(Fortress fortress, Iterable<TrackResultBean> resultBeans) {
        if (!engineConfig.isConceptsEnabled())
            return;
        assert fortress != null;
        logger.debug("Processing concepts for {}", fortress.getCompany());
        Map<DocumentType, Collection<ConceptInputBean>> payload = new HashMap<>();
        for (TrackResultBean resultBean : resultBeans) {
            if (resultBean.getEntityBean() != null && resultBean.getEntityBean().getId() != null) {
                DocumentType docType = resultBean.getDocumentType();
                Collection<ConceptInputBean> conceptInputBeans = payload.get(docType);
                if (conceptInputBeans == null) {
                    conceptInputBeans = new ArrayList<>();
                    payload.put(docType, conceptInputBeans);
                }

                EntityInputBean inputBean = resultBean.getEntityInputBean();
                if (inputBean != null && inputBean.getTags() != null) {
                    for (TagInputBean inputTag : resultBean.getEntityInputBean().getTags()) {
                        if (!inputTag.getEntityLinks().isEmpty()) {
                            ConceptInputBean cib = new ConceptInputBean();
                            cib.setRelationships(inputTag.getEntityLinks().keySet());
                            cib.setName(inputTag.getLabel());
                            if (!conceptInputBeans.contains(cib))
                                conceptInputBeans.add(cib);
                        }
                    }
                }
            }
        }
        logger.debug("About to register via SchemaDao");
        if (!payload.isEmpty())
            conceptDao.registerConcepts(payload);
    }

    @Override
    public void purge(Fortress fortress) {
        schemaDao.purge(fortress);
    }

    @Override
    public Boolean ensureUniqueIndexes(Collection<TagInputBean> tagPayload) {

        try {
            schemaDao.waitForIndexes();
            schemaDao.ensureUniqueIndexes(tagPayload).get();
            schemaDao.waitForIndexes();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Trying to ensure unique indexes");
        }

        return false;
    }

}
