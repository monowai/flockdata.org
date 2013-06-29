package com.auditbucket.audit.model;

import com.auditbucket.registration.model.ICompany;

/**
 * User: mike
 * Date: 30/06/13
 * Time: 10:06 AM
 */
public interface IDocumentType {

    public String getName();

    public ICompany getCompany();
}
