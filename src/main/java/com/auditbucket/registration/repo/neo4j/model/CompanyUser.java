package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ICompanyUser;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

@NodeEntity
public class CompanyUser implements ICompanyUser {
    @GraphId
    Long id;

    @Indexed(indexName = "companyUserName")
    private String name = null;

    @RelatedTo(elementClass = Company.class, type = "works", direction = Direction.OUTGOING)
    ICompany company;

//    @RelatedTo (elementClass = SystemUser.class, type ="isA", direction = Direction.INCOMING)
//    private ISystemUser systemUser;

    public CompanyUser() {
    }

    public CompanyUser(String name, ICompany company) {
        setName(name);
        setCompany(company);
    }

    public void setCompany(ICompany company) {
        this.company = company;
    }

    public ICompany getCompany() {
        return company;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.toLowerCase();
    }

}
