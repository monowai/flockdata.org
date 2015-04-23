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

package org.flockdata.engine.schema.service;

import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.schema.dao.SchemaDaoNeo4j;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.track.bean.ConceptInputBean;
import org.flockdata.track.bean.DocumentResultBean;
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

import java.util.*;

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
    PlatformConfig engineConfig;
    static Logger logger = LoggerFactory.getLogger(SchemaServiceNeo4j.class);

    public Boolean ensureSystemIndexes(Company company) {
        if (engineConfig.createSystemConstraints())
            return schemaDao.ensureSystemConstraints(company);
        return true;
    }

    /**
     * @param fortress     system that has an interest
     * @param documentCode name of the doc type
     * @return resolved document. Created if missing
     */
    @Override
    @Transactional
    @Cacheable(value = "fortressDocType", key = "#fortress.id+#documentCode ", unless = "#result==null")
    public DocumentType resolveByDocCode(Fortress fortress, String documentCode) {
        return resolveByDocCode(fortress, documentCode, true);
    }

    /**
     * Finds or creates a Document Type for the caller's company
     * There should only exist one document type for a given company
     *
     * @param fortress        system that has an interest
     * @param documentCode    name of the document
     * @param createIfMissing create document types that are missing
     * @return created DocumentType
     */
    @Override
    @Transactional
    public DocumentType resolveByDocCode(Fortress fortress, String documentCode, Boolean createIfMissing) {
        if (documentCode == null) {
            throw new IllegalArgumentException("DocumentType cannot be null");
        }

        return schemaDao.findDocumentType(fortress, documentCode, createIfMissing);

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
            schemaDao.registerConcepts(fortress.getCompany(), payload);
    }

    /**
     * Locates all tags in use by the associated document types
     *
     * @param company           who the caller works for
     * @param documentNames     labels to restrict the search by
     * @param withRelationships should the relationships also be returned
     * @return tags that are actually in use
     */

    @Override
    @Transactional
    public Set<DocumentResultBean> findConcepts(Company company, Collection<String> documentNames, boolean withRelationships) {

        return schemaDao.findConcepts(company, documentNames, withRelationships);

    }

    @Override
    @Transactional
    public void createDocTypes(Iterable<EntityInputBean> entities, Fortress fortress) {
        ArrayList<String> docTypes = new ArrayList<>();
        for (EntityInputBean entity : entities) {
            if (!docTypes.contains(entity.getDocumentName()))
                docTypes.add(entity.getDocumentName());
        }
        schemaDao.createDocTypes(docTypes, fortress);
    }


    @Override
    @Transactional
    public Collection<DocumentResultBean> getDocumentsInUse(Company company) {
        Collection<DocumentResultBean> results = new ArrayList<>();
        Collection<DocumentType> rawDocs = schemaDao.getCompanyDocumentsInUse(company);
        for (DocumentType rawDoc : rawDocs) {
            results.add(new DocumentResultBean(rawDoc));
        }
        return results;
    }

    @Override
    public void purge(Fortress fortress) {
        schemaDao.purge(fortress);
    }

    @Override
    public Boolean ensureUniqueIndexes(Company company, List<TagInputBean> tagInputs) {
        schemaDao.waitForIndexes();

        schemaDao.ensureUniqueIndexes(tagInputs);
        schemaDao.waitForIndexes();
        return true;
    }

    public Collection<String> getKnownLabels() {
        return schemaDao.getAllLabels();
    }
}
