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

package com.auditbucket.core.registration.service;

import com.auditbucket.audit.model.IDocumentType;
import com.auditbucket.core.helper.SecurityHelper;
import com.auditbucket.core.registration.dao.TagDaoI;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ITag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User: Mike Holdsworth
 * Date: 26/06/13
 * Time: 12:53 PM
 */

@Service
public class TagService {
    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private TagDaoI tagDao;

    @Transactional
    public ITag processTag(ITag input) {
        if (input == null)
            return input;

        // Check security access
        if (input.getCompany() == null) {
            ICompany company = securityHelper.getCompany();
            input.setCompany(company);
        }

        // Check exists
        ITag existingTag = tagDao.findOne(input.getName(), input.getCompany().getId());
        if (existingTag != null)
            return existingTag;

        // audit change
        return tagDao.save(input);
    }

    public ITag findTag(String tagName) {
        ICompany company = securityHelper.getCompany();
        if (company == null)
            return null;
        return tagDao.findOne(tagName, company.getId());
    }

    /**
     * finds or creates a Document Type for the caller's company
     *
     * @param documentType name of the document
     * @return created IDocumentType
     */
    @Transactional
    public IDocumentType resolveDocType(String documentType) {
        if (documentType == null) {
            throw new IllegalArgumentException("DocumentType cannot be null");
        }
        ICompany company = securityHelper.getCompany();
        if (company == null)
            return null;

        return tagDao.findOrCreate(documentType, company);

    }

}
