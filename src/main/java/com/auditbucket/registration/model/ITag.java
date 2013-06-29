package com.auditbucket.registration.model;

/**
 * User: mike
 * Date: 14/06/13
 * Time: 9:11 AM
 */
public interface ITag {

    public String getName();

    public ICompany getCompany();

    void setCompany(ICompany company);

    void setName(String floppy);

    Long getId();
}
