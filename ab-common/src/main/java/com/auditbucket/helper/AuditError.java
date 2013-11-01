package com.auditbucket.helper;

/**
 * User: Mike Holdsworth
 * Since: 1/11/13
 */
public class AuditError {
    private String status;
    private String message;

    protected AuditError() {
    }


    public void setMessage(String message) {
        this.message = message;
    }
}
