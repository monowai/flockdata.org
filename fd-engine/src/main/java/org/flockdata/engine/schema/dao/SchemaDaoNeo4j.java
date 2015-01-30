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

package org.flockdata.engine.schema.dao;

import org.flockdata.engine.schema.model.ConceptNode;
import org.flockdata.engine.schema.model.DocumentTypeNode;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.Relationship;
import org.flockdata.track.bean.ConceptInputBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.model.Concept;
import org.flockdata.track.model.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Maintains company specific Schema details. Structure of the nodes that FD has established
 * based on Entities, DocumentTypes, Tags and Relationships
 * <p>
 * User: mike
 * Date: 3/04/14
 * Time: 7:30 AM
 */
@Repository
public class SchemaDaoNeo4j {
    @Autowired
    DocumentTypeRepo documentTypeRepo;

    @Autowired
    ConceptTypeRepo conceptTypeRepo;

    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(SchemaDaoNeo4j.class);

    /**
     * The general sSchema is tracked so that we know what the general structure is
     *
     * @param company   who owns the tags
     * @param labelName labelName being create
     * @return true if it was created for the first time
     */
    public boolean registerTag(Company company, String labelName) {
        if (isSystemLabel(labelName))
            return true;

        if (!tagExists(company, labelName)) {

            String cypher = "merge (tag:TagLabel { name:{name}, companyKey:{key}}) " +
                    "with tag " +
                    "match (c:ABCompany) where id(c) = {cid} " +
                    "merge (c)<-[:TAG_INDEX]-(tag) " +
                    "return tag";
            Map<String, Object> params = new HashMap<>();
            params.put("name", labelName);
            params.put("key", parseTagLabel(company, labelName));
            params.put("cid", company.getId());

            template.query(cypher, params);


        }
        return true;
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
            docResult = documentExists(fortress, docName);
            if (docResult == null) {

                docResult = template.save(new DocumentTypeNode(fortress, docName));
            }
        }
        return docResult;

    }

    @Transactional
    public Collection<DocumentType> getFortressDocumentsInUse(Fortress fortress) {
        return documentTypeRepo.getFortressDocumentsInUse(fortress.getId());
    }

    public Collection<DocumentType> getCompanyDocumentsInUse(Company company) {
        return documentTypeRepo.getCompanyDocumentsInUse(company.getId());
    }


    DocumentType documentExists(Fortress fortress, String docCode) {
        assert fortress != null;
        String arg = new StringBuilder().append(fortress.getCompany().getId()).append(".").append(DocumentTypeNode.parse(fortress, docCode)).toString();
        return documentTypeRepo.findFortressDocCode(arg);
        //return documentTypeRepo.findBySchemaPropertyValue("companyKey", fortress.getCompany().getId() + "." +  DocumentTypeNode.parse(fortress, docName));
        //logger.trace("Document Exists= {} - Looking for {}", dt != null, DocumentTypeNode.parse(fortress, docName));
//        return dt;
    }

    private boolean tagExists(Company company, String indexName) {
        Object o = documentTypeRepo.findCompanyTag(company.getId(), parseTagLabel(company, indexName));
        return (o != null);
    }

    /**
     * Make sure a unique index exists for the tag
     * Being a schema alteration function this is synchronised to avoid concurrent modifications
     *
     * @param company   who owns the tags
     * @param tagInputs collection to process
     */
    //@Async("fd-engine")
    @Transactional
    public Boolean ensureUniqueIndexes(Company company, Iterable<TagInputBean> tagInputs) {
        Collection<String> toCreate = new ArrayList<>();


        Collection<String> knownLabels = template.getGraphDatabase().getAllLabelNames();
        for (TagInputBean tagInput : tagInputs) {
            if (tagInput != null) {
                logger.trace("Checking label for {}", tagInput);
                String label = tagInput.getLabel();
                if (!knownLabels.contains(label) && !toCreate.contains(label)) {
                    logger.debug("Creating label for {}", tagInput);
                    //if (index != null && !tagExists(company, index)) { // This check causes deadlocks in TagEP ?
                    if (!(tagInput.isDefault() || isSystemLabel(tagInput.getLabel()))) {
                        //makeIndexForTag(tagInput);
                        toCreate.add(tagInput.getLabel());
                        knownLabels.add(tagInput.getLabel());
                    }
                }
                if (!tagInput.getTargets().isEmpty()) {
                    for (String key : tagInput.getTargets().keySet()) {
                        if (key != null)
                            ensureUniqueIndexes(company, tagInput.getTargets().get(key));
                    }
                }
            } else
                logger.debug("Why is this null?");

        }

        if (toCreate.size() > 0) {
            try {
                //labelLock.lock();
                return
                        makeLabelIndexes(toCreate);

            } finally {
                //labelLock.unlock();
            }

        }
        return true ;
    }


    public Boolean makeLabelIndexes(Collection<String> labels) {
        for (String label : labels) {
            makeLabelIndex(label);
        }
        return true;
    }

    boolean makeIndexForTag(TagInputBean tagInput) {
        // _Tag is a special label that can be used to find all tags so we have to allow it to handle duplicates
        String label = tagInput.getLabel();

        makeLabelIndex(label);
        return true;

    }

    static Lock labelLock = new ReentrantLock();

    private void makeLabelIndex(String label) {
        template.query("create constraint on (t:`" + label + "`) assert t.key is unique", null);
        // Tag alias also have a unique key
        template.query("create constraint on (t:`" + label + "Alias`) assert t.key is unique", null);
        logger.debug("Tag constraint created - [{}]", label);
    }

    public Boolean ensureSystemIndexes(Company company, String suffix) {
        logger.debug("Creating System Indexes for {} ", company.getName());
        template.query("create constraint on (t:Country) assert t.key is unique", null);
        // ToDo: Create a city node. The key should be country.{state}.city
        template.query("create constraint on (t:City) assert t.key is unique", null);
        logger.debug("Created the indexes");
        return true;
    }

    public void registerConcepts(Company company, Map<DocumentType, Collection<ConceptInputBean>> conceptInput) {
        logger.trace("Registering concepts");
        Set<DocumentType> documentTypes = conceptInput.keySet();
        Collection<String> docs = new ArrayList<>(documentTypes.size());
        for (String doc : docs) {
            docs.add(doc);
        }

        for (DocumentType docType : conceptInput.keySet()) {
            logger.trace("Looking for existing concepts {}", docType.getName());
            DocumentTypeNode documentTypeNode = (DocumentTypeNode) docType;
            template.fetch(documentTypeNode.getConcepts());

            Collection<Concept> concepts = documentTypeNode.getConcepts();
            logger.trace("[{}] - Found {} existing concepts", documentTypeNode.getName(), concepts.size());
            boolean save = false;
            for (ConceptInputBean concept : conceptInput.get(docType)) {
                //logger.debug("Looking to create [{}]", concept.getName());
                ConceptNode existingConcept = conceptTypeRepo.findBySchemaPropertyValue("name", concept.getName());

                for (String relationship : concept.getRelationships()) {
                    if (existingConcept == null) {
                        logger.debug("No existing concept found for [{}]. Creating it", relationship);
                        existingConcept = new ConceptNode(concept.getName(), relationship, docType);
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
                    if (!documentTypeNode.getConcepts().contains(existingConcept)) {
                        documentTypeNode.add(existingConcept);
                        logger.debug("Creating concept {}", existingConcept);
                        save = true;
                    }
                }
            }

            if (save) {
                logger.trace("About to register {} concepts", concepts.size());
                documentTypeRepo.save(documentTypeNode);
                logger.trace("{} Concepts registered", concepts.size());
            }
            logger.debug("Concepts registered");

        }
    }

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

        ConceptNode userConcept = new ConceptNode("User");
        for (DocumentType document : documents) {
            template.fetch(document.getFortress());
            DocumentResultBean fauxDocument = new DocumentResultBean(document);
            documentResults.add(fauxDocument);
            template.fetch(document.getConcepts());

            if (withRelationships) {
                for (Concept concept : document.getConcepts()) {

                    template.fetch(concept);
                    template.fetch(concept.getRelationships());
                    Concept fauxConcept = new ConceptNode(concept.getName());

                    fauxDocument.add(fauxConcept);
                    Collection<Relationship> fauxRlxs = new ArrayList<>();
                    for (Relationship existingRelationship : concept.getRelationships()) {
                        if (existingRelationship.hasDocumentType(document)) {
                            fauxRlxs.add(existingRelationship);
                        }
                    }
                    fauxConcept.addRelationships(fauxRlxs);
                }

                userConcept.addRelationship("CREATED_BY", document);
                if (!fauxDocument.getConcepts().isEmpty())
                    fauxDocument.getConcepts().add(userConcept);
            } else {
                // Just return the concepts
                for (Concept concept : document.getConcepts()) {
                    template.fetch(concept);
                    fauxDocument.add(concept);
                }
                fauxDocument.add(userConcept);
            }
        }
        return documentResults;
    }

    public void createDocTypes(ArrayList<String> docCodes, Fortress fortress) {
        for (String docType : docCodes) {
            findDocumentType(fortress, docType, true);
        }

    }

    public void purge(Fortress fortress) {

        String docRlx = "match (fort:Fortress)-[fd:FORTRESS_DOC]-(a:DocType)-[dr]-(o)-[k]-(p)" +
                "where id(fort)={fortId}  delete dr, k, o, fd;";

        // ToDo: Purge Unused Concepts!!
        HashMap<String, Object> params = new HashMap<>();
        params.put("fortId", fortress.getId());
        template.query(docRlx, params);
    }

    private boolean isSystemLabel(String index) {
        return (index.equals("Country") || index.equals("City"));
    }

    private String parseTagLabel(Company company, String label) {
        return company.getId() + ".t." + label.toLowerCase().replaceAll("\\s", "");
    }

    public DocumentType createDocType(String documentType, Fortress fortress) {
        return findDocumentType(fortress, documentType, true);
    }
}
