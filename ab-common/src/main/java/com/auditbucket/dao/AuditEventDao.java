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

package com.auditbucket.dao;

import com.auditbucket.audit.model.AuditChange;
import com.auditbucket.audit.model.AuditEvent;
import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditTag;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 9:55 PM
 */
public interface AuditEventDao {

    AuditEvent findEvent(Company company, String eventCode);

    AuditEvent createEvent(Company company, String eventCode);

    AuditEvent associate(AuditChange change, AuditEvent existingEvent, Company company);

    Set<AuditEvent> findCompanyEvents(Long id);
}
