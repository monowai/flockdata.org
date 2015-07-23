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

package org.flockdata.engine.concept.service;

import org.flockdata.engine.dao.ConceptDaoNeo;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.model.Company;
import org.flockdata.model.Fortress;
import org.flockdata.track.bean.ConceptInputBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.model.DocumentType;
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
 *
 * Concepts represent the Tags and Entities that are being tracked in this service
 * An Entity is represented as a DocumentType. It exists in both it's DocumentType.name index
 * and a generic Entity index
 *
 * Tags are also called Concepts. These are also indexed uniquely withing a Label that identifies
 * their type and a generic"Tags" Label.
 *
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
     *
     */
    @Override
    @Transactional
    public Collection<DocumentResultBean> getDocumentsInUse(Company company) {
        Collection<DocumentResultBean> results = new ArrayList<>();
        Collection<DocumentType> rawDocs = conceptDao.getCompanyDocumentsInUse(company);
        for (DocumentType rawDoc : rawDocs) {
            results.add(new DocumentResultBean(rawDoc));
        }
        return results;
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
//    @Cacheable(value = "fortressDocType", key = "#fortress.id+#documentCode ", unless = "#result==null")
    @Override
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
    public DocumentType resolveByDocCode(Fortress fortress, String documentCode, Boolean createIfMissing) {
        if (documentCode == null) {
            throw new IllegalArgumentException("DocumentType cannot be null");
        }

        return conceptDao.findDocumentType(fortress, documentCode, createIfMissing);

    }

    @Override
    public void registerConcepts(Fortress fortress, Iterable<TrackResultBean> resultBeans) {
        assert fortress != null;
        logger.debug("Processing concepts for {}", fortress.getCompany());
        Map<DocumentType, Collection<ConceptInputBean>> payload = new HashMap<>();
        for (TrackResultBean resultBean : resultBeans) {
            if (resultBean.getEntity() != null && resultBean.getEntity().getId() != null) {
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
}
