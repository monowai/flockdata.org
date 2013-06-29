package com.auditbucket.registration.bean;

import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ITag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 1:20 PM
 */
public class TagInputBean implements ITag {

    @NotEmpty
    private ICompany company;
    @NotEmpty
    private String name;

    public TagInputBean() {
    }

    public TagInputBean(String tagName) {
        this();
        this.name = tagName;
    }

    public TagInputBean(ICompany company, String tagName) {
        this(tagName);
        this.company = company;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonIgnore
    public Long getId() {
        return null;
    }

    @Override
    public ICompany getCompany() {
        return company;
    }

    @Override
    public void setCompany(ICompany company) {
        this.company = company;
    }
}
