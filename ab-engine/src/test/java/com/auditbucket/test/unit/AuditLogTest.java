package com.auditbucket.test.unit;

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditLog;

public class AuditLogTest implements AuditLog {
    Long id;

    public AuditLogTest(Long id) {
        this.id = id;
    }

    @Override
    public AuditChange getAuditChange() {
        return null;
    }

    @Override
    public AuditHeader getAuditHeader() {
        return null;
    }

    @Override
    public boolean isIndexed() {
        return false;
    }

    @Override
    public void setIsIndexed() {
    }

    @Override
    public Long getSysWhen() {
        return null;
    }

    @Override
    public Long getFortressWhen() {
        return null;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
