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

package com.auditbucket.track.model;

import com.auditbucket.registration.model.Company;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 15/06/13
 * Time: 9:11 AM
 */
public interface TxRef {

    public String getName();

    public Company getCompany();

    public Set<Entity> getEntities();

    Long getId();

    public TxStatus getTxStatus();

    public long getTxDate();

    enum TxStatus {
        TX_CREATED, TX_ROLLBACK, TX_COMMITTED
    }
}
