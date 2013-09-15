package com.auditbucket.test.unit;

import com.auditbucket.audit.model.AuditWhat;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: nabil
 * Date: 15/09/13
 * Time: 22:24
 * To change this template use File | Settings | File Templates.
 */
public class AuditNullWhatTest implements AuditWhat {
    @Override
    public String getId() {
        return "1";
    }

    @Override
    public String getWhat() {
        return null;
    }

    @Override
    public Map<String, Object> getWhatMap() {
        return null;
    }

    @Override
    public boolean isCompressed() {
        return false;
    }

    @Override
    public void setJsonWhat(String jsonWhat) {
    }
}
