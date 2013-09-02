package com.auditbucket.demo.domain;

import com.auditbucket.spring.annotations.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ACCOUNT")
@Auditable
public class Account {
    // Technical Id
    @Id
    @GeneratedValue
    @NoAudit
    private long id;

    @AuditKey
    private String auditKey;

    @AuditClientRef
    private String accountNumber;

    @AuditTag
    private String iban;

    private String status;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }


    public String getAuditKey() {
        return auditKey;
    }

    public void setAuditKey(String auditKey) {
        this.auditKey = auditKey;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
