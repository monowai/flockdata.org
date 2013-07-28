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

package com.auditbucket.registration.dao;

import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:12 PM
 */
public interface TagDaoI {
    Tag save(Tag tag);

    Tag findOne(String tagName, Long id);

    DocumentType findOrCreate(String documentType, Company company);

}
