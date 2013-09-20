package com.auditbucket.test.unit;

import com.auditbucket.audit.model.*;
import com.auditbucket.engine.service.WhatService;
import com.auditbucket.registration.model.FortressUser;

/**
 * Created with IntelliJ IDEA.
 * User: nabil
 * Date: 15/09/13
 * Time: 22:19
 * To change this template use File | Settings | File Templates.
 */
public class AuditChangeTest implements AuditChange {
    FortressUser who;
    String comment, name;
    TxRef txRef;
    AuditChange previousChange;
    AuditWhat what;
    String storage;

    public void setAuditLog(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    AuditLog auditLog;

    public AuditChangeTest(String storage){
        this.storage=storage;
    }

    @Override
    public FortressUser getWho() {
        return null;
    }

    public void setWho(FortressUser who) {
        this.who = who;
    }

    @Override
    public String getComment() {
        return null;
    }

    @Override
    public void setComment(String comment) {
    }

    @Override
    public String getName() {
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public AuditEvent getEvent() {
        return null;
    }

    @Override
    public void setEvent(AuditEvent event) {
    }

    @Override
    public AuditChange getPreviousChange() {
        return null;
    }

    @Override
    public void setPreviousChange(AuditChange previousChange) {
    }

    @Override
    public AuditLog getAuditLog() {
        return this.auditLog;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public AuditWhat getWhat() {
        return new AuditNullWhatTest();
    }

    @Override
    public void setWhat(AuditWhat what) {
    }

    @Override
    public String getWhatStore() {
        return this.storage;
    }

    @Override
    public void setWhatStore(String storage) {
    }

    public TxRef getTxRef() {
        return txRef;
    }

    @Override
    public void setTxRef(TxRef txRef) {
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }
}

