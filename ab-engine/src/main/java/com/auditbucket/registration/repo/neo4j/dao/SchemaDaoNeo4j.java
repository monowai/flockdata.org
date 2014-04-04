package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.dao.SchemaDao;
import com.auditbucket.engine.repo.neo4j.SchemaTypeRepo;
import com.auditbucket.engine.repo.neo4j.model.DocumentTypeNode;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

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
    public boolean registerTagIndex (Company c, String indexName ){
        if ( !tagExists(c, indexName)){
            //logger.info("Create tag "+Thread.currentThread().getName() );
            String cypher = "merge (tagLabel:_TagLabel :TagLabel{ name:{name}, companyKey:{key}}) " +
                    "with tagLabel " +
                    "match (c:Company) where id(c) = {cid} " +
                    "merge (c)<-[:TAG_INDEX]-(tagLabel) " +
                    "return tagLabel";
            Map<String, Object> params = new HashMap<>();
            params.put("name", indexName);
            params.put("key", parseTagIndex(c,indexName));
            params.put("cid", c.getId());

            template.query(cypher, params);


        }
        return true;
    }
    /**
     * Tracks the DocumentTypes used by a Fortress that can be used to find MetaHeader objects
     *
     * @param fortress          fortress generating
     * @param indexName         name of the Label
     * @param createIfMissing   if not found will create
     * @return  the node
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
    public Collection<DocumentType> getDocumentsInUse(Fortress fortress) {
        return schemaTypeRepo.getDocumentsInUse(fortress.getId());
    }

    @Cacheable(value = "companySchemaType", unless = "#result == null")
    private DocumentType documentExists(Fortress fortress, String indexName) {
        //String key = fortress.getId() + ".d." + indexName.toLowerCase().replaceAll("\\s", "");
        //return schemaDao.findFortressDocType(fortress.getId(), key);
        return schemaTypeRepo.findFortressDocType(fortress.getId(), indexName.toLowerCase().replaceAll("\\s", ""));
        //return schemaDao.findBySchemaPropertyValue("companyKey", key);
    }

    @Cacheable(value = "companySchemaType", unless = "#result == null")
    public boolean tagExists(Company company, String indexName) {
        return schemaTypeRepo.findBySchemaPropertyValue("companyKey", parseTagIndex(company, indexName))!=null;
    }

    public String parseTagIndex(Company company, String indexName){
        return company.getId() + ".t." + indexName.toLowerCase().replaceAll("\\s", "");
    }



}
