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

package org.flockdata.engine.concept.service;

import org.flockdata.engine.dao.ConceptDaoNeo;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.helper.FlockException;
import org.flockdata.model.*;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.*;
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

    private final ConceptDaoNeo conceptDao;

    private final FortressService fortressService;

    private static Logger logger = LoggerFactory.getLogger(ConceptServiceNeo.class);

    @Autowired
    public ConceptServiceNeo(FortressService fortressService, ConceptDaoNeo conceptDaoNeo) {
        this.fortressService = fortressService;
        this.conceptDao = conceptDaoNeo;
    }

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
        return conceptDao.findConcepts(company, documentNames);
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
        return conceptDao.findConcepts(company, documentNames);
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
     * <p>
     * This represents a fact that there is at least one (e:Entity)-[r:relationship]->(oe:Entity) existing
     * @param sourceType   Existing node
     * @param targetType   Existing node
     * @param entityKeyBean properties that describe the relationship
     */
    @Override
    public void linkEntities(DocumentType sourceType, DocumentType targetType, EntityKeyBean entityKeyBean) {
        conceptDao.linkEntities(sourceType, targetType, entityKeyBean);
    }

    /**
     * Analyses the TrackResults and builds up a meta analysis of the entities and tags
     * to track the structure of graph data
     * <p>
     * Extracts DocTypes, Tags and relationship names. These can be found in the graph with a query
     * such as
     * <p>
     * match ( c:DocType)-[r]-(x:Concept) return c,r,x;
     *
     * @param resultBeans payload to analyse
     */
    @Override
    public void registerConcepts(Iterable<TrackResultBean> resultBeans) {
        // ToDo: This could be established the first time a DocType is encountered. Option to suppress subsequent
        //       registration analysis once the docType exists. This would need to be configurable as
        //       evolving models of connected concepts also exist
        Map<DocumentType, ArrayList<ConceptInputBean>> docTypeToConcept = new HashMap<>();

        for (TrackResultBean resultBean : resultBeans) {
            if (resultBean.getEntity() != null ) {
                DocumentType docType = resultBean.getDocumentType();
                ArrayList<ConceptInputBean> conceptInputBeans = docTypeToConcept.get(docType);
                if (conceptInputBeans == null) {
                    conceptInputBeans = new ArrayList<>();
                    docTypeToConcept.put(docType, conceptInputBeans);
                }

                EntityInputBean inputBean = resultBean.getEntityInputBean();
                if (inputBean != null && inputBean.getTags() != null) {
                    for (TagInputBean inputTag : resultBean.getEntityInputBean().getTags()) {
                        if (inputTag.getEntityTagLinks() != null && !inputTag.getEntityTagLinks().isEmpty()) {
                            ConceptInputBean cib = new ConceptInputBean(inputTag.getLabel());

                            if (!conceptInputBeans.contains(cib)) {
                                cib.setRelationships(inputTag.getEntityTagLinks().keySet());
                                conceptInputBeans.add(cib);
                            } else
                                conceptInputBeans.get(conceptInputBeans.indexOf(cib)).setRelationships(inputTag.getEntityTagLinks().keySet());
                        }
                    }
                }
            }
        }
        logger.debug("About to register via SchemaDao");
        if (!docTypeToConcept.isEmpty())
            conceptDao.registerConcepts(docTypeToConcept);
    }

    @Override
    public DocumentType save(DocumentType documentType) {
        if ( documentType.getName().equalsIgnoreCase("Entity"))
            return documentType; // non-persistent??
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

    public Set<DocumentResultBean> getConceptsWithRelationships(Company company, Collection<String> documents) {
        return findConcepts(company, documents, true);

    }

    public Collection<DocumentResultBean> getDocumentsInUse(Company fdCompany, String fortress) throws FlockException {
        Collection<String> fortresses = new ArrayList<>();
        fortresses.add(fortress);
        return getDocumentsInUse(fdCompany, fortresses);
    }

    @Override
    public Collection<DocumentType> makeDocTypes(FortressSegment segment, List<EntityInputBean> inputBeans) {
        Collection<DocumentType> docTypes = new ArrayList<>();
        DocumentType master;
        for (EntityInputBean entityInputBean : inputBeans) {
            // Entity is a reserved DocType in FD
            if (!isSystemType(entityInputBean.getDocumentType())) {
                master = new DocumentType(segment, entityInputBean.getDocumentType());
                master = findOrCreate(segment.getFortress(), master);
                master = conceptDao.findDocumentTypeWithSegments(master);
                if (!master.getSegments().contains(segment)) {
                    master.getSegments().add(segment);
                    conceptDao.save(master);
                }
                docTypes.add(master);
                if (!entityInputBean.getEntityLinks().isEmpty()) {

                    // The entity being processed is linked to other entities.
                    // need to ensure that both the Fortress and DocumentType are also created
                    for (String relationship : entityInputBean.getEntityLinks().keySet()) {
                        for (EntityKeyBean entityKeyBean : entityInputBean.getEntityLinks().get(relationship)) {
                            Fortress fortress;

                            if (!segment.getFortress().getName().equals(entityKeyBean.getFortressName()))
                                fortress = fortressService.registerFortress(segment.getCompany(), entityKeyBean.getFortressName());
                            else
                                fortress = segment.getFortress();

                            DocumentType linkedDocument = new DocumentType(fortress, entityKeyBean.getDocumentType());
                            if (!docTypes.contains(linkedDocument)) {
                                linkedDocument = findOrCreate(fortress, linkedDocument);
                                docTypes.add(linkedDocument);
                                entityKeyBean.setRelationship(relationship); // ToDo: should be supplied in the EKB
                                linkEntities(master, linkedDocument, entityKeyBean);
                            }
                        }
                    }
                }

            }
        }
        logger.debug("Finished result = {}" + docTypes.size());
        return docTypes;
    }

    private boolean isSystemType(MetaDocument documentType) {
        return documentType.getName().equalsIgnoreCase("entity");
    }

    @Override
    public void delete(DocumentType documentType) {
        conceptDao.delete(documentType.getId());
    }

    @Override
    public void delete(DocumentType documentType, FortressSegment segment) {
        conceptDao.delete(documentType, segment);
    }

    @Override
    public MatrixResults getContentStructure(Company company, String fortress) {
        Fortress f = fortressService.findByCode(company, fortress);
        return conceptDao.getStructure(f)  ;
    }

    @Override
    public Map<String, DocumentResultBean> getParents(DocumentType documentType) {
        return conceptDao.getParents(documentType);
    }

    @Override
    public DocumentType findDocumentTypeWithSegments(DocumentType documentType) {
        return conceptDao.findDocumentTypeWithSegments(documentType);
    }

    @Override
    public DocumentResultBean findDocumentTypeWithSegments(Fortress f, String doc) {
        return conceptDao.findDocumentTypeWithSegments(f, doc);
    }

    public Collection<DocumentResultBean> getDocumentsInUse(Company fdCompany, Collection<String> fortresses) throws FlockException {
        ArrayList<DocumentResultBean> docs = new ArrayList<>();

        // ToDo: Optimize via Cypher, not a java loop
        //match (f:Fortress) -[:FORTRESS_DOC]-(d) return f,d
        if (fortresses == null) {
            Collection<FortressResultBean> forts = fortressService.findFortresses(fdCompany);
            for (FortressResultBean fort : forts) {
                docs.addAll(fortressService.getFortressDocumentsInUse(fdCompany, fort.getName()));
            }

        } else {
            for (String fortress : fortresses) {
                Collection<DocumentResultBean> documentTypes = fortressService.getFortressDocumentsInUse(fdCompany, fortress);
                docs.addAll(documentTypes);
            }
        }
        return docs;

    }


}
