/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.service;

import com.auditbucket.engine.repo.neo4j.dao.SchemaDaoNeo4j;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.bean.ConceptInputBean;
import com.auditbucket.track.bean.DocumentResultBean;
import com.auditbucket.track.bean.EntityInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * User: mike
 * Date: 16/06/14
 * Time: 7:43 AM
 */
@Service
public class SchemaServiceNeo4j implements com.auditbucket.track.service.SchemaService {
    @Autowired
    SchemaDaoNeo4j schemaDao;

    @Autowired
    EngineConfig engineConfig;
    static Logger logger = LoggerFactory.getLogger(SchemaServiceNeo4j.class);

    public Boolean ensureSystemIndexes(Company company) {
        return schemaDao.ensureSystemIndexes(company, engineConfig.getTagSuffix(company));
    }

    /**
     * @param fortress     system that has an interest
     * @param documentType name of the doc type
     * @return resolved document. Created if missing
     */
    @Override
    @Transactional
    public DocumentType resolveDocType(Fortress fortress, String documentType) {
        return resolveDocType(fortress, documentType, true);
    }

    /**
     * Finds or creates a Document Type for the caller's company
     * There should only exist one document type for a given company
     *
     * @param fortress        system that has an interest
     * @param documentType    name of the document
     * @param createIfMissing create document types that are missing
     * @return created DocumentType
     */
    @Override
    @Transactional
    public DocumentType resolveDocType(Fortress fortress, String documentType, Boolean createIfMissing) {
        if (documentType == null) {
            throw new IllegalArgumentException("DocumentType cannot be null");
        }

        return schemaDao.findDocumentType(fortress, documentType, createIfMissing);

    }

    @Override
    @Transactional
    public void registerConcepts(Company company, Iterable<TrackResultBean> resultBeans) {
        if (!engineConfig.isConceptsEnabled())
            return;
        logger.debug("Processing concepts for {}", company);
        Map<DocumentType, Collection<ConceptInputBean>> payload = new HashMap<>();
        for (TrackResultBean resultBean : resultBeans) {
            if (resultBean.getEntity() != null && resultBean.getEntity().getId() != null) {
                DocumentType docType = schemaDao.findDocumentType(resultBean.getEntity().getFortress(), resultBean.getEntity().getDocumentType(), false);
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
        if (!payload.isEmpty())
            schemaDao.registerConcepts(company, payload);
    }

    /**
     * Locates all tags in use by the associated document types
     *
     * @param company           who the caller works for
     * @param documents         labels to restrict the search by
     * @param withRelationships should the relationships also be returned
     * @return tags that are actually in use
     */

    @Override
    @Transactional
    public Set<DocumentResultBean> findConcepts(Company company, Collection<String> documents, boolean withRelationships) {

        return schemaDao.findConcepts(company, documents, withRelationships);

    }

    @Override
    @Transactional
    public void createDocTypes(Iterable<EntityInputBean> entities, Fortress fortress) {
        ArrayList<String> docTypes = new ArrayList<>();
        for (EntityInputBean entity : entities) {
            if (!docTypes.contains(entity.getDocumentType()))
                docTypes.add(entity.getDocumentType());
        }
        schemaDao.createDocTypes(docTypes, fortress);
    }

    @Override
    @Transactional
    public Collection<DocumentResultBean> getCompanyDocumentsInUse(Company company) {
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
    public boolean ensureUniqueIndexes(Company company, List<TagInputBean> tagInputs, Collection<String> existingIndexes) {
        return schemaDao.ensureUniqueIndexes(company, tagInputs, existingIndexes);
    }
}
