package com.auditbucket.engine.repo;

import com.auditbucket.audit.model.AuditHeader;

import java.io.IOException;

/**
 * User: Mike Holdsworth
 * Since: 31/01/14
 */
public interface KvRepo {
    public void add(AuditHeader auditHeader, Long key, byte[] value) throws IOException;

    public byte[] getValue(AuditHeader auditHeader, Long key);

    public void delete(AuditHeader auditHeader, Long key);
}
