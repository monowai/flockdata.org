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

package org.flockdata.meta.service;

import org.flockdata.authentication.registration.bean.TagInputBean;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.meta.dao.ConceptDaoNeo;
import org.flockdata.model.Company;
import org.flockdata.model.DocumentType;
import org.flockdata.model.Fortress;
import org.flockdata.track.bean.ConceptInputBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Reporting/Schema monitoring service
 * Whenever an entity is tracked, it's TrackResultBean is sent to this service so that the top
 * down meta data can be logged.
 * <p/>
 * Concepts represent the Tags and Entities that are being tracked in this service
 * An Entity is represented as a DocumentType. It exists in both it's DocumentType.name index
 * and a generic Entity index
 * <p/>
 * Tags are also called Concepts. These are also indexed uniquely withing a Label that identifies
 * their type and a generic"Tags" Label.
 * <p/>
 * Created by mike on 19/06/15.
 */

@Service
@Transactional
public class ConceptServiceNeo implements ConceptService {
    @Autowired
    ConceptDaoNeo conceptDao;

    static Logger logger = LoggerFactory.getLogger(ConceptServiceNeo.class);

    /**
     * Entities being tracked as "DocumentTypes"
     *
     * @param company who owns the docs
     * @return Docs in use
     */
    @Override
    @Transactional
    public Collection<DocumentResultBean> getDocumentsInUse(Company company) {
        Collection<DocumentResultBean> results = new ArrayList<>();
        Collection<DocumentType> rawDocs = conceptDao.getCompanyDocumentsInUse(company);
        for (DocumentType rawDoc : rawDocs) {
            DocumentResultBean newDoc = new DocumentResultBean(rawDoc);
            if (!results.contains(newDoc))
                results.add(newDoc);
        }
        return results;
    }

    @Override
    public Set<DocumentResultBean> findConcepts(Company company, String documentName, boolean withRelationships) {
        Collection<String> documentNames = new ArrayList<>();
        documentNames.add(documentName);
        return conceptDao.findConcepts(company, documentNames, withRelationships);
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
    public Set<DocumentResultBean> findConcepts(Company company, Collection<String> documentNames, boolean withRelationships) {
        return conceptDao.findConcepts(company, documentNames, withRelationships);
    }

    /**
     * @param fortress     system that has an interest
     * @param documentCode name of the doc type
     * @return resolved document. Created if missing
     */
    @Override
    @Deprecated // use resolveDocumentType(Fortress fortress, DocumentType documentType)
    public DocumentType resolveByDocCode(Fortress fortress, String documentCode) {
        return resolveByDocCode(fortress, documentCode, true);
    }

    @Override
    public DocumentType findOrCreate(Fortress fortress, DocumentType documentType) {
        return conceptDao.findDocumentType(fortress, documentType, true);
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
    public DocumentType resolveByDocCode(Fortress fortress, String documentCode, Boolean createIfMissing) {
        if (documentCode == null) {
            throw new IllegalArgumentException("DocumentType cannot be null");
        }

        return conceptDao.findDocumentType(fortress, documentCode, createIfMissing);

    }

    /**
     * Tracks the fact that the sourceType is connected to the targetType with relationship name.
     *
     * This represents a fact that there is at least one (e:Entity)-[r:relationship]->(oe:Entity) existing
     * @param sourceType    node from
     * @param relationship  name
     * @param targetType    node to
     */
    @Override
    public void linkEntities(DocumentType sourceType, String relationship, DocumentType targetType) {
        conceptDao.linkEntities(sourceType, relationship, targetType);
    }

    /**
     * Analyses the TrackResults and builds up a meta analysis of the entities and tags
     * to track the structure of graph data
     *
     * Extracts DocTypes, Tags and relationship names. These can be found in the graph with a query
     * such as
     *
     * match ( c:DocType)-[r]-(x:Concept) return c,r,x;
     *
     * @param resultBeans payload to analyse
     */
    @Override
    public void registerConcepts(Iterable<TrackResultBean> resultBeans) {
        // ToDo: This could be established the first time a DocType is encountered. Option to suppress subsequent
        //       registration analysis once the docType exists. This would need to be configurable as
        //       evolving models of connected concepts also exist
        Map<DocumentType, ArrayList<ConceptInputBean>> payload = new HashMap<>();

        for (TrackResultBean resultBean : resultBeans) {
            if (resultBean.getEntity() != null && resultBean.getEntity().getId() != null) {
                DocumentType docType = resultBean.getDocumentType();
                ArrayList<ConceptInputBean> conceptInputBeans = payload.get(docType);
                if (conceptInputBeans == null) {
                    conceptInputBeans = new ArrayList<>();
                    payload.put(docType, conceptInputBeans);
                }

                EntityInputBean inputBean = resultBean.getEntityInputBean();
                if (inputBean != null && inputBean.getTags() != null) {
                    for (TagInputBean inputTag : resultBean.getEntityInputBean().getTags()) {
                        if (!inputTag.getEntityLinks().isEmpty()) {
                            ConceptInputBean cib = new ConceptInputBean(inputTag.getLabel());

                            if (!conceptInputBeans.contains(cib)) {
                                cib.setRelationships(inputTag.getEntityLinks().keySet());
                                conceptInputBeans.add(cib);
                            }else
                                conceptInputBeans.get(conceptInputBeans.indexOf(cib)).setRelationships(inputTag.getEntityLinks().keySet());
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
    public DocumentType save(DocumentType documentType) {
        return conceptDao.save(documentType);
    }

    @Override
    public DocumentType findDocumentType(Fortress fortress, String documentName) {
        return findDocumentType(fortress, documentName, false);
    }

    @Override
    public DocumentType findDocumentType(Fortress fortress, String documentName, boolean createIfMissing) {
        return conceptDao.findDocumentType(fortress, documentName, createIfMissing);
    }

}
