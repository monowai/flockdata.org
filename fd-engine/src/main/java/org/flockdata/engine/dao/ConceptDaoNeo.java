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

package org.flockdata.engine.dao;

import org.flockdata.helper.NotFoundException;
import org.flockdata.model.*;
import org.flockdata.query.EdgeResults;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.bean.ConceptInputBean;
import org.flockdata.track.bean.ConceptResultBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.RelationshipResultBean;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.kernel.DeadlockDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.transaction.HeuristicRollbackException;
import java.util.*;

/**
 * IO routines to handle registration of concepts in Neo4j
 * <p/>
 * Created by mike on 19/06/15.
 */
@Repository
public class ConceptDaoNeo {
    private final ConceptTypeRepo conceptTypeRepo;

    private final DocumentTypeRepo documentTypeRepo;

    private final Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(ConceptDaoNeo.class);

    @Autowired
    public ConceptDaoNeo(DocumentTypeRepo documentTypeRepo, ConceptTypeRepo conceptTypeRepo, Neo4jTemplate template) {
        this.documentTypeRepo = documentTypeRepo;
        this.conceptTypeRepo = conceptTypeRepo;
        this.template = template;
    }

    public boolean linkEntities(DocumentType fromDoc, String relationship, DocumentType targetDoc) {
        Node from = template.getNode(fromDoc.getId());
        Node to = template.getNode(targetDoc.getId());
        return linkNodesWithRelationship(relationship, from, to);
    }

    private boolean linkDocToConcept(DocumentType fromDoc, String relationship, Concept concept) {
        Node from = template.getNode(fromDoc.getId());
        Node to = template.getNode(concept.getId());
        return linkNodesWithRelationship(relationship, from, to);
    }

    private boolean linkConceptToConcept(Concept fromConcept, String relationship, Concept concept) {
        Node from = template.getNode(fromConcept.getId());
        Node to = template.getNode(concept.getId());
        return linkNodesWithRelationship(relationship, from, to);
    }

    private boolean linkNodesWithRelationship(String relationship, Node from, Node to) {
        org.neo4j.graphdb.Relationship r = from.getSingleRelationship(DynamicRelationshipType.withName(relationship), Direction.BOTH);
        if (r == null) {
            template.createRelationshipBetween(from, to, relationship, null);
            return true; // Link created
        }
        return false; // Link already existed
    }

    public void registerConcepts(Map<DocumentType, ArrayList<ConceptInputBean>> documentConcepts) {
        logger.trace("Registering concepts");

        for (DocumentType docType : documentConcepts.keySet()) {
            logger.trace("Looking for existing concepts {}", docType.getName());

//            Collection<Concept> concepts = docType.getConcepts();
            logger.trace("[{}] - Found {} existing concepts", docType.getName(), docType.getConcepts().size());
            for (ConceptInputBean conceptInput : documentConcepts.get(docType)) {
                Concept concept = conceptTypeRepo.findBySchemaPropertyValue("key", Concept.toKey(conceptInput));
                for (String relationship : conceptInput.getRelationships()) {
                    if (concept == null) {
                        logger.debug("No existing conceptInput found for [{}]. Creating it", relationship);
                        concept = template.save(new Concept(conceptInput));
                    }
                    linkDocToConcept(docType, relationship, concept);

                }
            }
        }
    }

    /**
     * Tracks the DocumentTypes used by a Fortress that can be used to find Entities
     *
     * @param fortress        fortress generating
     * @param docType         name of the Label
     * @param createIfMissing if not found will create
     * @return the node
     */
    @Cacheable(value = "documentType", unless = "#result==null")
    public DocumentType findDocumentType(Fortress fortress, String docType, Boolean createIfMissing) {
        DocumentType docResult = documentExists(fortress, docType);

        if ((docResult == null && createIfMissing) && (!docType.equalsIgnoreCase("entity"))) {
            docResult = documentTypeRepo.save(new DocumentType(fortress, docType));
        }

        return docResult;
    }

    /**
     * Tracks the DocumentTypes used by a Fortress that can be used to find Entities
     *
     * @param fortress        fortress generating
     * @param documentType    prototype
     * @param createIfMissing if not found will create
     * @return the node
     */
    public DocumentType findDocumentType(Fortress fortress, DocumentType documentType, Boolean createIfMissing) {
        DocumentType docResult = documentExists(fortress, documentType.getName());

        if (docResult == null && createIfMissing) {
            //documentType.setFortress(fortress); // fortress was set by the caller
            docResult = documentTypeRepo.save(documentType);
        }

        return docResult;
    }

    private DocumentType documentExists(Fortress fortress, String docType) {
        assert fortress != null;
        logger.debug("looking for document {}, fortress {}", docType, fortress);
        String docKey = DocumentType.toKey(fortress, docType);
        return documentTypeRepo.findFortressDocCode(docKey);
    }

    // Query Routines

    public Set<DocumentResultBean> findConcepts(Company company, Collection<String> docNames) {
        Set<DocumentResultBean> documentResults = new HashSet<>();
        Set<DocumentType> documents;
        if (docNames == null)
            documents = documentTypeRepo.findAllDocuments(company);
        else
            documents = documentTypeRepo.findDocuments(company, docNames);

        for (DocumentType document : documents) {

            DocumentResultBean documentResult = new DocumentResultBean(document);
            documentResults.add(documentResult);
            String query = "match (d:DocType)-[r]-(c:Concept) where id(d)={0} return type(r) as rType, c.name as name";
            Map<String, Object> params = new HashMap<>();
            params.put("0", document.getId());
            Result<Map<String, Object>> queryResults = template.query(query, params);
            Iterator<Map<String, Object>> rows = queryResults.iterator();
            Map<String, ConceptResultBean> concepts = new HashMap<>();
            while (rows.hasNext()) {
                Map<String, Object> row = rows.next();
                String relationship = (String) row.get("rType");
                String name = (String) row.get("name");
                ConceptResultBean conceptResult = concepts.get(name);
                if (conceptResult == null) {
                    conceptResult = new ConceptResultBean(name);
                    concepts.put(name, conceptResult);
                    documentResult.add(conceptResult);
                }
                conceptResult.addRelationship(new RelationshipResultBean(relationship));
            }


        }
        return documentResults;
    }

    public Collection<DocumentType> getFortressDocumentsInUse(Fortress fortress) {
        return documentTypeRepo.getFortressDocumentsInUse(fortress.getId());
    }

    public Collection<DocumentType> getCompanyDocumentsInUse(Company company) {
        return documentTypeRepo.findAllDocuments(company);
    }

    private Concept schemaTagDefExists(Company company, ConceptInputBean conceptInputBean) {

        return documentTypeRepo.schemaTagDefExists(company.getId(), Concept.toKey(conceptInputBean)) ;
    }

    /**
     * The general Schema is tracked to understand the general structure
     *
     * @param company who owns the tags
     * @return true if it was created for the first time
     */
    @Async
    @Transactional
    @Retryable(include = {HeuristicRollbackException.class, DataRetrievalFailureException.class, InvalidDataAccessResourceUsageException.class, ConcurrencyFailureException.class, DeadlockDetectedException.class}, maxAttempts = 20,
            backoff = @Backoff(maxDelay = 200, multiplier = 5, random = true))
    public Concept registerTag(Company company, TagResultBean tagResultBean) {
        Concept source ;
        if (tagResultBean.isNewTag() && !tagResultBean.getTag().isDefault()) {

            ConceptInputBean conceptInputBean = new ConceptInputBean(tagResultBean);
            source = schemaTagDefExists(company, conceptInputBean);
            if (source == null ) {
                source = createSchemaTag(conceptInputBean);
            }
            processNestedTags(company, source, tagResultBean.getTargets());
        }  else {
            if ( tagResultBean.getLabel() != null)
                source = findConcept(tagResultBean);
            else
                source = null;
            processNestedTags(company, source, tagResultBean.getTargets()); // Current tag is not new but sub tags may be
        }
        return source;
    }

    private Concept findConcept(TagResultBean tagResultBean) {
        return conceptTypeRepo.findByLabel( Concept.toKey(new ConceptInputBean(tagResultBean)));
    }

    private void processNestedTags(Company company, Concept source, Map<TagResultBean, Collection<String>> targets) {
        if (targets == null || targets.isEmpty())
            return;

        for (TagResultBean tagResultBean : targets.keySet()) {
            Concept target = registerTag(company, tagResultBean);
            if ( source!=null && target!=null){
                Collection<String>rlxs= targets.get(tagResultBean);
                for (String rlx : rlxs) {
                    linkConceptToConcept(source,rlx,target);
                }
            }
        }


    }

    //    @Async
    private Concept createSchemaTag(ConceptInputBean conceptInputBean) {
        Concept concept = new Concept(conceptInputBean);
        template.save(concept);
        return concept;
    }

    public DocumentType save(DocumentType documentType) {
        return template.save(documentType);
    }

    public void delete(Long documentTypeId) {
        documentTypeRepo.purgeDocumentAssociations(documentTypeId);
        documentTypeRepo.purgeDocumentSegments(documentTypeId);
        documentTypeRepo.delete(documentTypeId);
    }

    public DocumentType findDocumentTypeWithSegments(DocumentType documentType) {
        if (documentType == null)
            throw new NotFoundException("Unable to find the requested DocumentType");
        template.fetch(documentType);
        template.fetch(documentType.getSegments());
        return documentType;
    }

    public DocumentResultBean findDocumentTypeWithSegments(Fortress f, String doc) {
        DocumentType documentType = findDocumentType(f, doc, false);
        if (documentType == null)
            throw new NotFoundException("Failed to find DocumentType " + doc);

        template.fetch(documentType.getSegments());
        return new DocumentResultBean(documentType, documentType.getSegments());
//        result.addSegment(f.getDefaultSegment());
    }

    public void delete(DocumentType documentType, FortressSegment segment) {
        template.deleteRelationshipBetween(documentType, segment, "USES_SEGMENT");
    }

    public EdgeResults getStructure(Fortress fortress) {
        String query = "match (f:Fortress{code:{0})-[]-(d:DocType)-[]-(c:Concept) return c,d";
        Map<String, Object> params = new HashMap<>();
        params.put("fortress", fortress.getId());

        Result<Map<String, Object>> results = template.query(query, params);

        Iterator<Map<String, Object>> rows = results.iterator();

        EdgeResults edgeResults = new EdgeResults();

        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            row.get("c");
        }

        return edgeResults;

    }
}
