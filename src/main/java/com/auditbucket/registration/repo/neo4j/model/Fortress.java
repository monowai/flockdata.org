package com.auditbucket.registration.repo.neo4j.model;

import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.IFortress;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import java.util.UUID;

@NodeEntity
public class Fortress implements IFortress {

    private UUID fortressKey;

    @GraphId
    Long id;

    @Indexed(indexName = "fortressName")
    String name;

    @Fetch
    @RelatedTo(type = "owns", direction = Direction.INCOMING)
    Company company;

    private Boolean accumulatingChanges = false;
    private Boolean ignoreSearchEngine = true;

    public Fortress() {
    }

    public Fortress(FortressInputBean fortressInputBean, ICompany ownedBy) {
        setName(fortressInputBean.getName());
        setIgnoreSearchEngine(fortressInputBean.getIgnoreSearchEngine());
        setAccumulatingChanges(fortressInputBean.getAccumulateChanges());
        setCompany(ownedBy);
        fortressKey = UUID.randomUUID();
    }

    public String getFortressKey() {
        return fortressKey.toString();
    }

    @JsonIgnore
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
    @JsonIgnore
    public ICompany getCompany() {
        return company;
    }

    @Override
    public void setCompany(ICompany ownedBy) {
        this.company = (Company) ownedBy;

    }

    public Boolean isAccumulatingChanges() {
        return accumulatingChanges;
    }

    @Override
    public Boolean isIgnoreSearchEngine() {
        return ignoreSearchEngine;
    }

    public void setIgnoreSearchEngine(Boolean ignoreSearchEngine) {
        if (ignoreSearchEngine != null)
            this.ignoreSearchEngine = ignoreSearchEngine;
    }

    public void setAccumulatingChanges(Boolean addChanges) {
        this.accumulatingChanges = addChanges;
        if (addChanges)
            ignoreSearchEngine = false;
    }

}
