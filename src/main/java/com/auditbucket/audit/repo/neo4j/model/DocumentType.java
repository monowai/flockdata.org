package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.model.IDocumentType;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.repo.neo4j.model.Company;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * User: mike
 * Date: 30/06/13
 * Time: 10:02 AM
 */
@NodeEntity
public class DocumentType implements IDocumentType {

    @GraphId
    Long id;

    @RelatedTo(elementClass = Company.class, type = "classifies", direction = Direction.INCOMING)
    private ICompany company;

    @Indexed(indexName = "documentTypeName")
    private String name;

    protected DocumentType() {
    }

    public DocumentType(String documentTypeName, ICompany company) {
        this.name = documentTypeName;
        this.company = company;
    }

    public String getName() {
        return name;
    }

    public ICompany getCompany() {
        return company;
    }
}
