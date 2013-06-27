package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ITag;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 8:35 PM
 */
@NodeEntity
public class Tag implements ITag {
    @GraphId
    Long Id;

    @RelatedTo(elementClass = Company.class, type = "tags", direction = Direction.INCOMING)
    ICompany company;

    @Indexed(indexName = "tagName")
    private String name;

    public Tag() {
    }

    public Tag(ITag tag) {
        this.company = tag.getCompany();
        this.name = tag.getName();
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public ICompany getCompany() {
        return company;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setCompany(ICompany company) {
        this.company = company;
    }

    @Override
    public void setName(String floppy) {
        this.name = floppy;
    }

    public Long getId() {
        return Id;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
