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
    @Override
    public FortressUser getWho() {
        return null;
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

    @Override
    public void setTxRef(TxRef txRef) {
    }

    @Override
    public AuditEvent getEvent() {
        return null;
    }

    @Override
    public void setPreviousChange(AuditChange previousChange) {
    }

    @Override
    public AuditChange getPreviousChange() {
        return null;
    }

    @Override
    public AuditLog getAuditLog() {
        return null;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public AuditWhat getWhat() {
        return new AuditWhatTest();
    }

    @Override
    public void setWhat(AuditWhat what) {
    }

    @Override
    public String getWhatStore() {
        return WhatService.NEO4J;
    }

    @Override
    public void setWhatStore(String storage) {
    }

    @Override
    public void setEvent(AuditEvent event) {
    }
}

