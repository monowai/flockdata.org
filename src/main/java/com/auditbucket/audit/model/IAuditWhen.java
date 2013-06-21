package com.auditbucket.audit.model;

/**
 * User: mike
 * Date: 21/06/13
 * Time: 1:21 PM
 */
public interface IAuditWhen {

    public IAuditLog getAuditLog();

    public IAuditHeader getAuditHeader();

    public Long getSysWhen();

    public Long getFortressWhen();
}
