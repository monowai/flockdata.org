/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

package com.auditbucket.audit.model;

import com.auditbucket.registration.model.FortressUser;

import java.util.Date;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 15/04/13
 * Time: 5:49 AM
 */
public interface AuditChange {

    String CREATE = "Create";
    String UPDATE = "Update";

    public abstract FortressUser getWho();

    public String getComment();

    /**
     * optional comment
     *
     * @param comment searchable.
     */
    public void setComment(String comment);

    public String getJsonWhat();

    String getName();

    public void setTxRef(TxRef txRef);

    String getEvent();

    Map<String, Object> getWhatMap();

    void setJsonWhat(String compressedJson);

    void setPreviousChange(AuditChange previousChange);

    AuditChange getPreviousChange();

    AuditLog getAuditLog();

    long getId();
}
