package com.auditbucket.audit.bean;

import java.util.Map;

/**
 */
public class AuditDeltaBean {
    private Map<String, Object> added;
    private Map<String, Object> changed;
    private Map<String, Object> removed;
    private Map<String, Object> unchanged;

    public Map getAdded() {
        return added;
    }

    public Map getChanged() {
        return changed;
    }

    public Map getRemoved() {
        return removed;
    }

    public void setAdded(Map<String, Object> added) {
        this.added = added;
    }

    public void setRemoved(Map<String, Object> removed) {
        this.removed = removed;
    }

    public void setChanged(Map<String, Object> changed) {
        this.changed = changed;
    }


    public void setUnchanged(Map<String, Object> unchanged) {
        this.unchanged = unchanged;
    }

    public Map<String, Object> getUnchanged() {
        return unchanged;
    }
}
