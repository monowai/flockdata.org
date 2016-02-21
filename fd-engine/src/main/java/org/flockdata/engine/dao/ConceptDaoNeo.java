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

package org.flockdata.engine.dao;

import org.flockdata.helper.TagHelper;
import org.flockdata.model.*;
import org.flockdata.track.bean.ConceptInputBean;
import org.flockdata.track.bean.ConceptResultBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.RelationshipResultBean;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * IO routines to handle registration of concepts in Neo4j
 * <p/>
 * Created by mike on 19/06/15.
 */
@Repository
public class ConceptDaoNeo {
    @Autowired
    ConceptTypeRepo conceptTypeRepo;

    @Autowired
    DocumentTypeRepo documentTypeRepo;

    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(ConceptDaoNeo.class);

    public boolean linkEntities(DocumentType fromDoc, String relationship, DocumentType targetDoc) {
        Node from = template.getNode(fromDoc.getId());
        Node to = template.getNode(targetDoc.getId());
        org.neo4j.graphdb.Relationship r = from.getSingleRelationship(DynamicRelationshipType.withName(relationship), Direction.OUTGOING);
        if (r == null) {
            template.createRelationshipBetween(from, to, relationship, null);
            return true; // Link created
        }
        return false; // Link already existed
    }


    public boolean linkConcept(DocumentType fromDoc, String relationship, Concept concept) {
        Node from = template.getNode(fromDoc.getId());
        Node to = template.getNode(concept.getId());
        org.neo4j.graphdb.Relationship r = from.getSingleRelationship(DynamicRelationshipType.withName(relationship), Direction.OUTGOING);
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

            Collection<Concept> concepts = docType.getConcepts();
            logger.trace("[{}] - Found {} existing concepts", docType.getName(), concepts.size());
            for (ConceptInputBean conceptInput : documentConcepts.get(docType)) {
                Concept concept = conceptTypeRepo.findBySchemaPropertyValue("key", Concept.toKey(conceptInput));
                for (String relationship : conceptInput.getRelationships()) {
                    if (concept == null) {
                        logger.debug("No existing conceptInput found for [{}]. Creating it", relationship);
                        concept = template.save(new Concept(conceptInput));
                    }
                    linkConcept(docType, relationship, concept);

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
    public DocumentType findDocumentType(Fortress fortress, String docType, Boolean createIfMissing) {
        DocumentType docResult = documentExists(fortress, docType);

        if (docResult == null && createIfMissing) {
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

    DocumentType documentExists(Fortress fortress, String docType) {
        logger.debug("looking for document {}, fortress {}", docType, fortress);
        assert fortress != null;
        String docKey = DocumentType.toKey(fortress, docType);
        return documentTypeRepo.findFortressDocCode(docKey);
    }

    // Query Routines

    public Set<DocumentResultBean> findConcepts(Company company, Collection<String> docNames, boolean withRelationships) {
        Set<DocumentResultBean> documentResults = new HashSet<>();
        Set<DocumentType> documents;
        if (docNames == null)
            documents = documentTypeRepo.findAllDocuments(company);
        else
            documents = documentTypeRepo.findDocuments(company, docNames);

        for (DocumentType document : documents) {
//            template.fetch(document.getFortress());

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

    public boolean schemaTagDefExists(Company company, String labelName) {
        return documentTypeRepo.schemaTagDefExists(company.getId(), TagLabel.parseTagLabel(company, labelName)) != null;
    }

    /**
     * The general sSchema is tracked so that we know what the general structure is
     *
     * @param company   who owns the tags
     * @param labelName labelName being create
     * @return true if it was created for the first time
     */
    public boolean registerTag(Company company, String labelName) {
        if (TagHelper.isSystemLabel(labelName))
            return true;

        if (!schemaTagDefExists(company, labelName)) {
            createSchemaTagDef(company, labelName);
        }
        return true;
    }

    @Async
    public void createSchemaTagDef(Company company, String labelName) {
        TagLabel tagLabelNode = new TagLabel(company, labelName);
        template.saveOnly(tagLabelNode);
    }

    public DocumentType save(DocumentType documentType) {
        return template.save(documentType);
    }

}
