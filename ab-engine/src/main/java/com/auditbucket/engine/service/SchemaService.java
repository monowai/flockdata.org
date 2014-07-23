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

import com.auditbucket.dao.SchemaDao;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.bean.ConceptInputBean;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.bean.TrackResultBean;
import com.auditbucket.track.model.DocumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * User: mike
 * Date: 16/06/14
 * Time: 7:43 AM
 */
@Service
@Transactional
public class SchemaService {
    @Autowired
    SchemaDao schemaDao;

    @Autowired
    EngineConfig engine;

    @Async
    public void ensureSystemIndexes(Company company) {
        schemaDao.ensureSystemIndexes(company, engine.getTagSuffix(company));
    }

    /**
     * @param fortress     system that has an interest
     * @param documentType name of the doc type
     * @return resolved document. Created if missing
     */
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
    public DocumentType resolveDocType(Fortress fortress, String documentType, Boolean createIfMissing) {
        if (documentType == null) {
            throw new IllegalArgumentException("DocumentType cannot be null");
        }

        return schemaDao.findDocumentType(fortress, documentType, createIfMissing);

    }

    public void registerConcepts(Company company, Iterable<TrackResultBean> resultBeans) {
        Map<DocumentType, Collection<ConceptInputBean>> payload = new HashMap<>();
        for (TrackResultBean resultBean : resultBeans) {
            if ( resultBean.getMetaHeader()!=null && resultBean.getMetaHeader().getId() !=null ){
                DocumentType docType = schemaDao.findDocumentType(resultBean.getMetaHeader().getFortress(), resultBean.getMetaHeader().getDocumentType(), false);
                Collection<ConceptInputBean> conceptInputBeans = payload.get(docType);
                if (conceptInputBeans == null) {
                    conceptInputBeans = new ArrayList<>();
                    payload.put(docType, conceptInputBeans);
                }

                MetaInputBean inputBean = resultBean.getMetaInputBean();
                if (inputBean != null && inputBean.getTags() != null) {
                    for (TagInputBean inputTag : resultBean.getMetaInputBean().getTags()) {
                        if (inputTag.getMetaLink() != null || !inputTag.getMetaLinks().isEmpty()) {
                            ConceptInputBean cib = new ConceptInputBean();
                            cib.setRelationships(inputTag.getMetaLinks().keySet());
                            cib.setName(inputTag.getIndex());
                            if (!conceptInputBeans.contains(cib))
                                conceptInputBeans.add(cib);
                        }
                    }
                }
            }
        }
        if ( !payload.isEmpty())
            schemaDao.registerConcepts(company, payload);
    }

    /**
     * Locates all tags in use by the associated document types
     *
     * @param company           who the caller works for
     * @param documents         labels to restrict the search by
     * @param withRelationships
     * @return tags that are actually in use
     */

    public Set<DocumentType> findConcepts(Company company, Collection<String> documents, boolean withRelationships) {

        return schemaDao.findConcepts(company, documents, withRelationships);

    }

    public void createDocTypes(Iterable<MetaInputBean> headers, Company company, Fortress fortress) {
        ArrayList<String>docTypes = new ArrayList<>();
        for (MetaInputBean header : headers) {
            if (!docTypes.contains(header.getDocumentType()))
                docTypes.add(header.getDocumentType());
        }
        schemaDao.createDocTypes(docTypes,  fortress);
    }
}
