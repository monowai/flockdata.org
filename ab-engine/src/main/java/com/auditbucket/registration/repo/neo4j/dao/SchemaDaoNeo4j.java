package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.dao.SchemaDao;
import com.auditbucket.engine.repo.neo4j.SchemaTypeRepo;
import com.auditbucket.engine.repo.neo4j.model.DocumentTypeNode;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.track.model.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Maintains company specific Schema deets
 * User: mike
 * Date: 3/04/14
 * Time: 7:30 AM
 * To change this template use File | Settings | File Templates.
 */
@Repository
public class SchemaDaoNeo4j implements SchemaDao {
    @Autowired
    SchemaTypeRepo schemaTypeRepo;

    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(SchemaDaoNeo4j.class);

    @Override
    public boolean registerTagIndex(Company company, String indexName) {
        if (isSystemIndex(indexName))
            return true;

        if (!tagExists(company, indexName)) {

            String cypher = "merge (tagLabel:_TagLabel { name:{name}, companyKey:{key}}) " +
                    "with tagLabel " +
                    "match (c:ABCompany) where id(c) = {cid} " +
                    "merge (c)<-[:TAG_INDEX]-(tagLabel:_TagLabel) " +
                    "return tagLabel";
            Map<String, Object> params = new HashMap<>();
            params.put("name", indexName);
            params.put("key", parseTagIndex(company, indexName));
            params.put("cid", company.getId());

            template.query(cypher, params);


        }
        return true;
    }

    public void getDocumentTypes(Company company){

    }

    /**
     * Tracks the DocumentTypes used by a Fortress that can be used to find MetaHeader objects
     *
     * @param fortress        fortress generating
     * @param indexName       name of the Label
     * @param createIfMissing if not found will create
     * @return the node
     */
    public DocumentType findDocumentType(Fortress fortress, String indexName, Boolean createIfMissing) {
        DocumentType docResult = documentExists(fortress, indexName);
        if (docResult == null && createIfMissing) {
            docResult = new DocumentTypeNode(fortress, indexName);
            String cypher = "merge (docType:_DocType :DocType{code:{code}, name:{name}, companyKey:{key}}) " +
                    "with docType " +
                    "match (f:Fortress) where id(f) = {fId} " +
                    "merge (f)<-[:FORTRESS_DOC]-(docType) " +
                    "return docType";

            Map<String, Object> params = new HashMap<>();
            params.put("code", docResult.getCode());
            params.put("name", docResult.getName());
            params.put("key", docResult.getCompanyKey());
            params.put("fId", fortress.getId());

            template.query(cypher, params);
            docResult = documentExists(fortress, indexName);

        }
        return docResult;

    }

    @Override
    public Collection<DocumentType> getFortressDocumentsInUse(Fortress fortress) {
        return schemaTypeRepo.getFortressDocumentsInUse(fortress.getId());
    }

    @Override
    public Collection<DocumentType> getCompanyDocumentsInUse(Company company) {
        return schemaTypeRepo.getCompanyDocumentsInUse(company.getId());
    }


    @Cacheable(value = "companyDocType", unless = "#result == null")
    private DocumentType documentExists(Fortress fortress, String indexName) {
        return schemaTypeRepo.findFortressDocType(fortress.getId(), indexName.toLowerCase().replaceAll("\\s", ""));
    }

    @Cacheable(value = "companySchemaTag", unless = "#result == false")
    private boolean tagExists(Company company, String indexName) {
        //logger.info("Looking for co{}, {}", company.getId(), parseTagIndex(company, indexName));
        Object o = schemaTypeRepo.findCompanyTag(company.getId(), parseTagIndex(company, indexName));
        return (o != null);
        //return schemaTypeRepo.findBySchemaPropertyValue("companyKey", parseTagIndex(company, indexName)) != null;
    }

    /**
     * Make sure a unique index exists for the tag
     * Being a schema alteration function this is synchronised to avoid concurrent modifications
     *
     * @param company   who owns the tags
     * @param tagInputs collection to process
     */
    public synchronized void ensureUniqueIndexes(Company company, Iterable<TagInputBean> tagInputs, Collection<String> added ) {

        for (TagInputBean tagInput : tagInputs) {
            String index = tagInput.getIndex();
            if (!added.contains(index)) {
                //if (index != null && !tagExists(company, index)) { // This check causes deadlocks in TagEP ?
                    ensureIndex(company, tagInput);
                //}
                added.add(tagInput.getIndex());
            }
            if (!tagInput.getTargets().isEmpty()){
                for(String key: tagInput.getTargets().keySet()){
                    ensureUniqueIndexes(company, tagInput.getTargets().get(key), added);
                }
            }

        }

    }

    @Transactional
    private void ensureIndex(Company company, TagInputBean tagInput) {
        // _Tag is a special label that can be used to find all tags so we have to allow it to handle duplicates
        if (tagInput.isDefault() || isSystemIndex(tagInput.getIndex()))
            return;
        String index = tagInput.getIndex();

        template.query("create constraint on (t:`" + index + "`) assert t.key is unique", null);
        logger.info("Creating constraint on [{}]", tagInput.getIndex());

    }

    @Async
    public void ensureSystemIndexes(Company company, String suffix) {
        // Performance issue with constraints?
        logger.debug("Creating System Indexes...");
        template.query("create constraint on (t:Country) assert t.key is unique", null);
        template.query("create constraint on (t:City) assert t.key is unique", null);
    }

    private boolean isSystemIndex(String index) {
        return (index.equals("Country") || index.equals("City"));
    }

    private String parseTagIndex(Company company, String indexName) {
        return company.getId() + ".t." + indexName.toLowerCase().replaceAll("\\s", "");
    }


}
