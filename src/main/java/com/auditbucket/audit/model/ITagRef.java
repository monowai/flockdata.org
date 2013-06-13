package com.auditbucket.audit.model;

import com.auditbucket.registration.model.ICompany;

import java.util.Set;

/**
 * User: mike
 * Date: 14/06/13
 * Time: 9:11 AM
 */
public interface ITagRef {
    public String getName();

    public ICompany getCompany();

    public Set<IAuditHeader> getHeaders();
}
