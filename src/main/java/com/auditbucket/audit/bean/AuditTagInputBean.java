package com.auditbucket.audit.bean;

import javax.validation.constraints.NotNull;

/**
 * User: mike
 * Date: 28/06/13
 * Time: 9:58 AM
 */
public class AuditTagInputBean {
    @NotNull
    private String tagName;
    @NotNull
    private String auditKey;
    @NotNull
    private String value;

    public AuditTagInputBean() {
    }

    public AuditTagInputBean(String tagName, String auditKey, String value) {
        this.tagName = tagName;
        this.auditKey = auditKey;
        this.value = value;
    }

    public String getAuditKey() {
        return auditKey;
    }

    public String getTagName() {
        return tagName;
    }

    public String getValue() {
        return value;
    }
}
