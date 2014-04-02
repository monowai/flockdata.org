package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.dao.SchemaDao;
import com.auditbucket.engine.repo.neo4j.SchemaTypeRepo;
import com.auditbucket.engine.repo.neo4j.model.DocumentTypeNode;
import com.auditbucket.registration.model.Fortress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

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

    public DocumentType findDocumentType(Fortress fortress, String documentType, Boolean createIfMissing) {
        DocumentType docResult = findFortressDocument(fortress, documentType);
        if (docResult == null && createIfMissing) {
            docResult = new DocumentTypeNode(fortress, documentType);
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
            docResult = findFortressDocument(fortress, documentType);

        }
        return docResult;

    }

    @Cacheable(value = "companyDocType", unless = "#result == null")
    private DocumentType findFortressDocument(Fortress fortress, String documentType) {
        String key = fortress.getCompany().getId() + "." + documentType.toLowerCase().replaceAll("\\s", "");
        //return documentTypeRepo.findFortressDocType(fortress.getId(), key);
        return schemaTypeRepo.findBySchemaPropertyValue("companyKey", key);
    }


}
