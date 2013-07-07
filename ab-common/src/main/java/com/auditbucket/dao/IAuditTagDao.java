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

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.registration.model.ITag;

import java.util.Set;

/**
 * User: mike
 * Date: 28/06/13
 * Time: 9:55 AM
 */
public interface IAuditTagDao {
    ITagValue save(ITag tagName, IAuditHeader header, String tagValue);

    Set<ITagValue> find(ITag tagName, String tagValue);

    Set<ITagValue> getAuditTags(IAuditHeader ah);

    void update(Set<ITagValue> modifiedSet);
}