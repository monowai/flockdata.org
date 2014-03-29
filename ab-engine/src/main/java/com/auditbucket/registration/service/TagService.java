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

package com.auditbucket.registration.service;

import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.dao.TagDao;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

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

    private Logger logger = LoggerFactory.getLogger(TagService.class);

    public Tag processTag(TagInputBean inputBean) {
        Company company = securityHelper.getCompany();
        return processTag(inputBean, company);

    }

    public Tag processTag(TagInputBean tagInput, Company company) {
        //
        return tagDao.save(company, tagInput);
    }

    public Iterable<Tag> processTags(Iterable<TagInputBean> tagInputs) {
        Company company = securityHelper.getCompany();
        return processTags(company, tagInputs);
    }

    public Iterable<Tag> processTags(Company company, Iterable<TagInputBean> tagInputs) {
        return tagDao.save(company, tagInputs);
    }

    //ToDo: Figure out how to insert all tags at once
    public Iterable<Tag> processTagsFast(Iterable<TagInputBean> tagInputs) {
        Company company = securityHelper.getCompany();
        return tagDao.save(company, tagInputs);

    }

    public Tag findTag(String tagName, Company company) {
        return tagDao.findOne(company, tagName);
    }


    public Tag findTag(String tagName) {
        Company company = securityHelper.getCompany();
        if (company == null)
            return null;
        return findTag(tagName, company);
    }

    /**
     * Finds a company document type and creates it if it is missing
     *
     *
     * @param fortress
     * @param documentType
     * @return
     */
    public DocumentType resolveDocType(Fortress fortress, String documentType) {
        return resolveDocType(fortress, documentType, true);
    }

    /**
     * finds or creates a Document Type for the caller's company
     *
     *
     * @param fortress
     * @param documentType    name of the document
     * @param createIfMissing
     * @return created DocumentType
     */
    public DocumentType resolveDocType(Fortress fortress, String documentType, Boolean createIfMissing) {
        if (documentType == null) {
            throw new IllegalArgumentException("DocumentTypeNode cannot be null");
        }

        return tagDao.findDocumentType(fortress, documentType, createIfMissing);

    }

    public Collection<Tag> findDirectedTags(Tag startTag) {
        return tagDao.findDirectedTags(startTag, securityHelper.getCompany(), true); // outbound
    }

    public Map<String, Tag> findTags(String type) {
        Company company = securityHelper.getCompany();
        return tagDao.findTags(company, type);
    }
}
