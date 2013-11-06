package com.auditbucket.helper;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * User: Mike Holdsworth
 * Since: 1/11/13
 */
public class AuditError {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String exceptionName;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> errors;

    protected AuditError() {
    }

    public AuditError(List<String> errors) {
        this.errors = errors;
    }

    public AuditError(String exceptionName, String message) {
        this(message);
        this.exceptionName = exceptionName;
    }

    public AuditError(String message) {
        this();
        this.message = message;
    }


    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public String getExceptionName() {
        return exceptionName;
    }

    public void setExceptionName(String exceptionName) {
        this.exceptionName = exceptionName;
    }

    public List<String> getErrors() {
        return errors;
    }
}
