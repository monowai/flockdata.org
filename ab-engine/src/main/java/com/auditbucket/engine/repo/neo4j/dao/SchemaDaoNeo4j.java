package com.auditbucket.engine.repo.neo4j.dao;

import com.auditbucket.engine.repo.neo4j.ConceptTypeRepo;
import com.auditbucket.engine.repo.neo4j.DocumentTypeRepo;
import com.auditbucket.engine.repo.neo4j.model.ConceptNode;
import com.auditbucket.engine.repo.neo4j.model.DocumentTypeNode;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.Relationship;
import com.auditbucket.track.bean.ConceptInputBean;
import com.auditbucket.track.bean.DocumentResultBean;
import com.auditbucket.track.model.Concept;
import com.auditbucket.track.model.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Maintains company specific Schema details
 * User: mike
 * Date: 3/04/14
 * Time: 7:30 AM
 * To change this template use File | Settings | File Templates.
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
            params.put("key", parseTagIndex(company, labelName));
            params.put("cid", company.getId());

            template.query(cypher, params);


        }
        return true;
    }

    private Lock lock = new ReentrantLock();

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
            try {
                lock.lock();
                docResult = documentExists(fortress, docName);
                if (docResult == null) {

                    docResult = new DocumentTypeNode(fortress, docName);
                    template.save(docResult);
                }
            } finally{
                lock.unlock();
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


    //@Cacheable(value = "companyDocType", unless = "#result == null")
    private DocumentType documentExists(Fortress fortress, String docName) {

        DocumentType dt = documentTypeRepo.findFortressDocCode(fortress.getId(), DocumentTypeNode.parse(fortress, docName));
        logger.trace("Document Exists= {} - Looking for {}", dt!=null, DocumentTypeNode.parse(fortress, docName));
        return dt;
    }

    //@Cacheable(value = "companySchemaTag", unless = "#result == false")
    private boolean tagExists(Company company, String indexName) {
        Object o = documentTypeRepo.findCompanyTag(company.getId(), parseTagIndex(company, indexName));
        return (o != null);
    }

    /**
     * Make sure a unique index exists for the tag
     * Being a schema alteration function this is synchronised to avoid concurrent modifications
     *
     * @param company   who owns the tags
     * @param tagInputs collection to process
     */
    public synchronized boolean ensureUniqueIndexes(Company company, Iterable<TagInputBean> tagInputs, Collection<String> added) {

        for (TagInputBean tagInput : tagInputs) {
            if (tagInput != null) {
                logger.trace("Checking label for {}", tagInput);
                String label = tagInput.getLabel();
                if (!added.contains(label)) {
                    logger.debug("Creating label for {}", tagInput);
                    //if (index != null && !tagExists(company, index)) { // This check causes deadlocks in TagEP ?
                    if (!(tagInput.isDefault() || isSystemLabel(tagInput.getLabel()))) {
                        ensureIndex(tagInput);
                        added.add(tagInput.getLabel());
                    }
                }
                if (!tagInput.getTargets().isEmpty()) {
                    for (String key : tagInput.getTargets().keySet()) {
                        if (key != null)
                            ensureUniqueIndexes(company, tagInput.getTargets().get(key), added);
                    }
                }
            } else
                logger.debug("Why is this null?");

        }
        return true;

    }

    boolean ensureIndex(TagInputBean tagInput) {
        // _Tag is a special label that can be used to find all tags so we have to allow it to handle duplicates
        String index = tagInput.getLabel();

        template.query("create constraint on (t:`" + index + "`) assert t.key is unique", null);
        logger.debug("Creating constraint on [{}]", tagInput.getLabel());
        return true;

    }

    public Boolean ensureSystemIndexes(Company company, String suffix) {
        logger.debug("Creating System Indexes for {} ", company.getName());
        template.query("create constraint on (t:Country) assert t.key is unique", null);
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
            for (ConceptInputBean concept : conceptInput.get(docType)) {
                //logger.debug("Looking to create [{}]", concept.getName());
                ConceptNode existingConcept = conceptTypeRepo.findBySchemaPropertyValue("name", concept.getName());

                boolean save = false; // DAT-112 ensure the DocType has the concept
                for (String relationship : concept.getRelationships()) {
                    if (existingConcept == null) {
                        logger.debug("No existing concept found for [{}]. Creating it", relationship);
                        existingConcept = new ConceptNode(concept.getName(), relationship, docType);
                    } else {
                        logger.trace("Found an existing concept {}", existingConcept);
                        template.fetch(existingConcept.getRelationships());
                        Relationship existingR = existingConcept.hasRelationship(relationship, docType);
                        if (existingR == null) {
                            existingConcept.addRelationship(relationship, docType);
                            logger.debug("Creating {} concept for{}", relationship, existingConcept);
                        }
                    }
                    // DAT-112 removed save check. ToDo: Room for optimization?
                    documentTypeNode.add(existingConcept);
                    logger.debug("Creating concept {}", existingConcept);
                }
            }
            logger.trace("About to register {} concepts", concepts.size());
            documentTypeRepo.save(documentTypeNode);
            logger.trace("{} Concepts registered", concepts.size());
        }
    }

    public Set<DocumentResultBean> findConcepts(Company company, Collection<String> docNames, boolean withRelationships) {

        // This is a hack to support DAT-126. It should be resolved via a query. At the moment, it's working, but that's it/
        // Query should have the Concepts that the user is interested in as well.

        // Query should look something link this:
        // match (a:_DocType)-[:HAS_CONCEPT]-(c:_Concept)-[:KNOWN_RELATIONSHIP]-(kr:_Relationship)
        // where a.name="Sales" and c.name="Device"
        // with a, c, kr match (a)-[:DOC_RELATIONSHIP]-(t:Relationship) return a,t

        TreeSet<DocumentResultBean> fauxDocuments = new TreeSet<>();
        Set<DocumentType> documents;
        if (docNames == null)
            documents = documentTypeRepo.findAllDocuments(company);
        else
            documents = documentTypeRepo.findDocuments(company, docNames);

        for (DocumentType document : documents) {
            template.fetch(document.getFortress());
            DocumentResultBean fauxDocument = new DocumentResultBean(document);

            fauxDocuments.add(fauxDocument);
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
            } else {
                // Just return the concepts
                for (Concept concept : document.getConcepts()) {
                    template.fetch(concept);
                    fauxDocument.add(concept);
                }
            }
        }
        return fauxDocuments;
    }

    public void createDocTypes(ArrayList<String> docTypes, Fortress fortress) {
        for (String docType : docTypes) {
            findDocumentType(fortress, docType, true);
        }

    }

    public void purge(Fortress fortress) {

        String docRlx = "match (fort:Fortress)-[fd:FORTRESS_DOC]-(a:DocType)-[dr]-(o)-[k]-(p)" +
                "where id(fort)={fortId}  delete dr, k, o,  p,a,fd ;";

        HashMap<String,Object> params = new HashMap<>();
        params.put("fortId", fortress.getId());
        template.query(docRlx, params);
    }

    private boolean isSystemLabel(String index) {
        return (index.equals("Country") || index.equals("City"));
    }

    private String parseTagIndex(Company company, String indexName) {
        return company.getId() + ".t." + indexName.toLowerCase().replaceAll("\\s", "");
    }


}
