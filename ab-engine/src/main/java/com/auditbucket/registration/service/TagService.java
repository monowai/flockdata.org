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

package com.auditbucket.registration.service;

import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.dao.TagDao;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles management of a companies tags.
 * All tags belong to the company across their fortresses
 * <p/>
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:53 PM
 */

@Service
@Transactional
public class TagService {
    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private TagDao tagDao;

    public Tag processTag(Tag tag) {
        if (tag == null)
            return tag;

        // Check security access
        if (tag.getCompany() == null) {
            Company company = securityHelper.getCompany();
            tag.setCompany(company);
        }

        // Check exists
        Tag existingTag = tagDao.findOne(tag.getName(), tag.getCompany().getId());
        if (existingTag != null)
            return existingTag;

        return tagDao.save(tag);
    }

    public Tag findTag(String tagName) {
        Company company = securityHelper.getCompany();
        if (company == null)
            return null;
        return tagDao.findOne(tagName, company.getId());
    }

    /**
     * finds or creates a Document Type for the caller's company
     *
     * @param documentType name of the document
     * @return created DocumentType
     */
    public DocumentType resolveDocType(String documentType) {
        if (documentType == null) {
            throw new IllegalArgumentException("DocumentTypeNode cannot be null");
        }
        Company company = securityHelper.getCompany();
        if (company == null)
            return null;

        return tagDao.findOrCreate(documentType, company);

    }

}
