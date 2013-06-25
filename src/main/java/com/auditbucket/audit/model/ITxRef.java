package com.auditbucket.audit.model;

import com.auditbucket.audit.repo.neo4j.model.TxRef;
import com.auditbucket.registration.model.ICompany;

import java.util.Set;

/**
 * User: mike
 * Date: 14/06/13
 * Time: 9:11 AM
 */
public interface ITxRef {

    public String getName();

    public ICompany getCompany();

    public Set<IAuditHeader> getHeaders();

    Long getId();

    public TxRef.TxStatus getTxStatus();

    public long getTxDate();
}
