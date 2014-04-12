/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

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
