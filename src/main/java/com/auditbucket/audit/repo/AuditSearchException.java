package com.auditbucket.audit.repo;

import java.io.IOException;

/**
 * User: mike
 * Date: 19/06/13
 * Time: 7:18 PM
 */
public class AuditSearchException extends Throwable {
    public AuditSearchException(IOException e) {
        super(e);
    }
}
