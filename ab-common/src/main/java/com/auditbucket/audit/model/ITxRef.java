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

import com.auditbucket.registration.model.ICompany;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 14/06/13
 * Time: 9:11 AM
 */
public interface ITxRef {

    public String getName();

    public ICompany getCompany();

    public Set<IAuditHeader> getHeaders();

    Long getId();

    public TxStatus getTxStatus();

    public long getTxDate();

    enum TxStatus {
        TX_CREATED, TX_ROLLBACK, TX_COMMITTED;
    }
}
