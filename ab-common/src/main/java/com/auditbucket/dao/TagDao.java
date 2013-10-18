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

import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:12 PM
 */
public interface TagDao {
    Tag save(Company company, Tag tag);

    /**
     * Locates a tag
     *
     * @param tagName   name to find
     * @param companyId Company that owns the tag
     * @return the tag if it exists or null
     */
    Tag findOne(String tagName, Long companyId);

    DocumentType findOrCreate(String documentType, Company company);

    Long createCompanyTagManager(Long companyId, String tagCollectionName);

    Long getCompanyTagManager(Long companyId);

    /**
     * Removes the relationship between the company and the tag
     *
     * @param company that owns the tag
     * @param tag     tag to remove the relationship from
     */
    void deleteCompanyRelationship(Company company, Tag tag);
}
