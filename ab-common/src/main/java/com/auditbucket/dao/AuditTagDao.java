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

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditTag;
import com.auditbucket.helper.AuditException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 9:55 PM
 */
public interface AuditTagDao {
    AuditTag save(AuditHeader auditHeader, Tag tag, String relationshipName);

    AuditTag save(AuditHeader ah, Tag tag, String relationshipName, Map<String, Object> propMap);

    Boolean relationshipExists(AuditHeader auditHeader, Tag tag, String relationshipName);

    Set<AuditTag> getAuditTags(AuditHeader auditHeader, Company company);

    void deleteAuditTags(AuditHeader auditHeader, Collection<AuditTag> auditTags) throws AuditException;

    void changeType(AuditHeader auditHeader, AuditTag existingTag, String newType);

    Set<AuditHeader> findTagAudits(Tag tag);
}
