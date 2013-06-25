package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ICompanyUser;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Set;

@NodeEntity
public class Company implements ICompany {
    @GraphId
    Long id;

    @Indexed(unique = true, indexName = "companyName")
    String name;

    @RelatedTo(elementClass = CompanyUser.class, type = "works", direction = Direction.INCOMING)
    private Set<ICompanyUser> companyUsers;

//	@RelatedTo (elementClass = SystemUser.class, type="administers", direction= Direction.INCOMING)
//	Set<ISystemUser> admins;

//	@RelatedTo (elementClass = Fortress.class, type="owns")
//	Set<IFortress>fortresses;

    public Company(String companyName) {
        setName(companyName);
    }

    public Company() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        return "Company{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
