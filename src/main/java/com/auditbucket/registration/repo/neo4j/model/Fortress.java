package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.IFortress;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import java.util.UUID;

@NodeEntity
public class Fortress implements IFortress {
    @GraphId
    Long id;

    @Indexed(indexName = "fortressName")
    String name;

    @Fetch
    @RelatedTo(type = "owns", direction = Direction.INCOMING)
    Company company;

    private UUID uuID;

    public Fortress() {
    }

    public Fortress(String name, ICompany ownedBy) {
        setName(name);
        setCompany(ownedBy);
        uuID = UUID.randomUUID();
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
    public ICompany getCompany() {
        return company;
    }

    @Override
    public void setCompany(ICompany ownedBy) {
        this.company = (Company) ownedBy;

    }

    @Override
    public String getRepo() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getUsers() {
        return "monowai";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UUID getUUID() {
        return uuID;
    }
}
