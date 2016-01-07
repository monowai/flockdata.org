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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    public void linkEntities(DocumentType fromDoc, String relationship, DocumentType toDoc) {
        template.fetch(toDoc.getFortress());
        template.fetch(toDoc.getConcepts());

        ConceptInputBean conceptInputBean = new ConceptInputBean(fromDoc.getName())
                .setTag(false);
        Concept foundConcept = conceptTypeRepo.findBySchemaPropertyValue("key", Concept.toKey(conceptInputBean));
        if (foundConcept == null) {
            logger.debug("No existing concept found for [{}]. Creating it", relationship);
            foundConcept = new Concept(conceptInputBean, relationship, fromDoc);
            //foundConcept.setType("E");

        } else {
            // Entity->Entity registration
            // DocType is the DocumentType to connect
            // Relationship name is the Entity to Entity relationship being recorded
            // Save knownEntityRelationship
            setEntityRelationship(fromDoc, foundConcept, relationship);
        }

        if (foundConcept.getId()== null || !toDoc.getConcepts().contains(foundConcept)) {
            toDoc.add(foundConcept);
            logger.debug("Creating concept {}", foundConcept);
        }
        documentTypeRepo.save(toDoc);
    }

    public void registerConcepts(Map<DocumentType, ArrayList<ConceptInputBean>> documentConcepts) {
        logger.trace("Registering concepts");

        for (DocumentType docType : documentConcepts.keySet()) {
            logger.trace("Looking for existing concepts {}", docType.getName());

            template.fetch(docType.getConcepts());

            Collection<Concept> concepts = docType.getConcepts();
            logger.trace("[{}] - Found {} existing concepts", docType.getName(), concepts.size());
            boolean save = true;
            for (ConceptInputBean concept : documentConcepts.get(docType)) {
                //logger.debug("Looking to create [{}]", toDoc.getName());
                Concept existingConcept = conceptTypeRepo.findBySchemaPropertyValue("key", Concept.toKey(concept));

                for (String relationship : concept.getRelationships()) {
                    if (existingConcept == null) {
                        logger.debug("No existing concept found for [{}]. Creating it", relationship);
                        existingConcept = new Concept(concept, relationship, docType);
                    } else {
                        save = setTagRelationship(docType, existingConcept, relationship);
                    }
                    // DAT-112 removed save check. ToDo: Room for optimization?
                    if (!docType.getConcepts().contains(existingConcept)) {
                        docType.add(existingConcept);
                        logger.debug("Creating concept {}", existingConcept);
                        save = true;
                    }
                }
            }

            if (save) {
                logger.trace("About to register {} concepts", concepts.size());
                documentTypeRepo.save(docType);
                for (Concept concept : docType.getConcepts()) {
                    conceptTypeRepo.save(concept);
                }

                logger.trace("{} Concepts registered", concepts.size());
            }
        }
    }

    public boolean setTagRelationship(DocumentType docType, Concept existingConcept, String relationship) {
        boolean save = false;
        logger.trace("Found an existing concept {}", existingConcept);
        template.fetch(existingConcept.getKnownTags());
        Relationship existingR = existingConcept.hasTagRelationship(relationship, docType);
        if (existingR == null) {
            existingConcept.addTagRelationship(relationship, docType);
            save = true;
            logger.debug("Creating {} tag concept for{}", relationship, existingConcept);
        }
        return save;
    }

    public boolean setEntityRelationship(DocumentType docType, Concept existingConcept, String relationship) {
        boolean save = false;
        logger.trace("Found an existing concept {}", existingConcept);
        template.fetch(existingConcept.getKnownTags());
        Relationship existingR = existingConcept.hasEntityRelationship(relationship, docType);
        if (existingR == null) {
            existingConcept.addEntityRelationship(relationship, docType);
            save = true;
            logger.debug("Creating {} entity concept for{}", relationship, existingConcept);
        }
        return save;
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
        String docKey = DocumentType.toKey(fortress, docType );
        return documentTypeRepo.findFortressDocCode(docKey);
    }

    // Query Routines

    public Set<DocumentResultBean> findConcepts(Company company, Collection<String> docNames, boolean withRelationships) {

        // This is a hack to support DAT-126. It should be resolved via a query. At the moment, it's working, but that's it/
        // Query should have the Concepts that the user is interested in as well.

        // Query should look something link this:
        // match (a:DocType)-[:HAS_CONCEPT]-(c:Concept)-[:KNOWN_TAG]-(kr:Relationship)
        // where a.name="Sales" and c.name="Device"
        // with a, c, kr match (a)-[:DOC_RELATIONSHIP]-(t:Relationship) return a,t
        Set<DocumentResultBean> documentResults = new HashSet<>();
        Set<DocumentType> documents;
        if (docNames == null)
            documents = documentTypeRepo.findAllDocuments(company);
        else
            documents = documentTypeRepo.findDocuments(company, docNames);

        for (DocumentType document : documents) {
            template.fetch(document.getFortress());
            template.fetch(document.getConcepts());
            DocumentResultBean documentResult = new DocumentResultBean(document);
            documentResults.add(documentResult);

            if (withRelationships) {
                for (Concept concept : document.getConcepts()) {
                    Collection<Relationship> relationshipResults = new HashSet<>();

                    template.fetch(concept);
                    template.fetch(concept.getKnownTags());
                    template.fetch(concept.getKnownEntities());

                    ConceptResultBean conceptResult = new ConceptResultBean(concept.getName());
                    documentResult.add(conceptResult);

                    for (Relationship r : concept.getKnownTags()) {
                        if (r.hasDocumentType(document)) {
                            relationshipResults.add(r);
                        }
                    }
                    conceptResult.addRelationships(ConceptResultBean.TAG, relationshipResults);

                    relationshipResults.clear();
                    for (Relationship r : concept.getKnownEntities()) {
                        if (!r.hasDocumentType(document)) {
                            relationshipResults.add(r);
                        }
                    }
                    conceptResult.addRelationships(ConceptResultBean.ENTITY, relationshipResults);

                }
                // Disabled until we figure out a need for this
//                userConcept.addRelationship("CREATED_BY", document);
//                if (!fauxDocument.getConcepts().isEmpty())
//                    fauxDocument.getConcepts().add(new ConceptResultBean(userConcept));
            } else { // No relationship
                for (Concept concept : document.getConcepts()) {
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
