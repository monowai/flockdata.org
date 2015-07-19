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

import org.flockdata.track.bean.ConceptInputBean;
import org.flockdata.track.bean.ConceptResultBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
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

    public void registerConcepts(Map<DocumentType, Collection<ConceptInputBean>> conceptInput) {
        logger.trace("Registering concepts");
        Set<DocumentType> documentTypes = conceptInput.keySet();
        Collection<String> docs = new ArrayList<>(documentTypes.size());
        for (String doc : docs) {
            docs.add(doc);
        }

        for (DocumentType docType : conceptInput.keySet()) {
            logger.trace("Looking for existing concepts {}", docType.getName());
            org.flockdata.model.DocumentType documentType = docType;

            template.fetch(documentType.getConcepts());

            Collection<org.flockdata.model.Concept> concepts = documentType.getConcepts();
            logger.trace("[{}] - Found {} existing concepts", documentType.getName(), concepts.size());
            boolean save = false;
            for (ConceptInputBean concept : conceptInput.get(docType)) {
                //logger.debug("Looking to create [{}]", concept.getName());
                Concept existingConcept = conceptTypeRepo.findBySchemaPropertyValue("name", concept.getName());

                for (String relationship : concept.getRelationships()) {
                    if (existingConcept == null) {
                        logger.debug("No existing concept found for [{}]. Creating it", relationship);
                        existingConcept = new Concept(concept.getName(), relationship, docType);
                        save = true;
                    } else {
                        logger.trace("Found an existing concept {}", existingConcept);
                        template.fetch(existingConcept.getRelationships());
                        Relationship existingR = existingConcept.hasRelationship(relationship, docType);
                        if (existingR == null) {
                            existingConcept.addRelationship(relationship, docType);
                            save = true;
                            logger.debug("Creating {} concept for{}", relationship, existingConcept);
                        }
                    }
                    // DAT-112 removed save check. ToDo: Room for optimization?
                    if (!documentType.getConcepts().contains(existingConcept)) {
                        documentType.add(existingConcept);
                        logger.debug("Creating concept {}", existingConcept);
                        save = true;
                    }
                }
            }

            if (save) {
                logger.trace("About to register {} concepts", concepts.size());
                documentTypeRepo.save(documentType);
                logger.trace("{} Concepts registered", concepts.size());
            }
        }
    }

    /**
     * Tracks the DocumentTypes used by a Fortress that can be used to find Entities
     *
     * @param fortress        fortress generating
     * @param docName         name of the Label
     * @param createIfMissing if not found will create
     * @return the node
     */
    public DocumentType findDocumentType(Fortress fortress, String docName, Boolean createIfMissing) {
        DocumentType docResult = documentExists(fortress, docName);

        if (docResult == null && createIfMissing) {
            docResult = documentTypeRepo.save(new DocumentType(fortress, docName));
        }

        return docResult;
    }

    DocumentType documentExists(Fortress fortress, String docCode) {
        assert fortress != null;
        String arg = String.valueOf(fortress.getCompany().getId()) + "." + DocumentType.parse(fortress, docCode);
        return documentTypeRepo.findFortressDocCode(arg);
    }

    // Query Routines

    public Set<DocumentResultBean> findConcepts(Company company, Collection<String> docNames, boolean withRelationships) {

        // This is a hack to support DAT-126. It should be resolved via a query. At the moment, it's working, but that's it/
        // Query should have the Concepts that the user is interested in as well.

        // Query should look something link this:
        // match (a:_DocType)-[:HAS_CONCEPT]-(c:_Concept)-[:KNOWN_RELATIONSHIP]-(kr:_Relationship)
        // where a.name="Sales" and c.name="Device"
        // with a, c, kr match (a)-[:DOC_RELATIONSHIP]-(t:Relationship) return a,t
        Set<DocumentResultBean> documentResults = new HashSet<>();
        Set<DocumentType> documents;
        if (docNames == null)
            documents = documentTypeRepo.findAllDocuments(company);
        else
            documents = documentTypeRepo.findDocuments(company, docNames);

//        ConceptNode userConcept = new ConceptNode("User");
        for (DocumentType document : documents) {
            template.fetch(document.getFortress());
            template.fetch(document.getConcepts());
            DocumentResultBean documentResult = new DocumentResultBean(document);
            documentResults.add(documentResult);

            if (withRelationships) {
                for (org.flockdata.model.Concept concept : document.getConcepts()) {

                    template.fetch(concept);
                    template.fetch(concept.getRelationships());
                    ConceptResultBean conceptResult = new ConceptResultBean(concept.getName());

                    documentResult.add(conceptResult);
                    Collection<Relationship> relationshipResults = new ArrayList<>();
                    for (Relationship existingRelationship : concept.getRelationships()) {
                        if (existingRelationship.hasDocumentType(document)) {
                            relationshipResults.add(existingRelationship);
                        }
                    }
                    conceptResult.addRelationships(relationshipResults);
                }
                // Disabled until we figure out a need for this
//                userConcept.addRelationship("CREATED_BY", document);
//                if (!fauxDocument.getConcepts().isEmpty())
//                    fauxDocument.getConcepts().add(new ConceptResultBean(userConcept));
            } else {
                // Just return the concepts
                for (org.flockdata.model.Concept concept : document.getConcepts()) {
                    template.fetch(concept);
                    documentResult.add(new ConceptResultBean(concept));
                }
                //fauxDocument.add(new ConceptResultBean(userConcept));
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
        if (SchemaDaoNeo4j.isSystemLabel(labelName))
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

}
