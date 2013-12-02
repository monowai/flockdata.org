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
import com.auditbucket.bean.TagInputBean;
import com.auditbucket.dao.TagDao;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        return processTags(tagInputs, company);
    }

    public Iterable<Tag> processTags(Iterable<TagInputBean> tagInputs, Company company) {

        List<Tag> result = new ArrayList<>();

        //ToDo: Figure out the bulk handing of tags
        for (TagInputBean tagInput : tagInputs) {
            result.add(tagDao.save(company, tagInput));
        }
        return result;
    }

    //ToDo: Figure out how to insert all tags at once
    public Iterable<Tag> processTagsFast(Iterable<TagInputBean> tagInputs) {
        Company company = securityHelper.getCompany();
        return tagDao.save(company, tagInputs);

    }

    public Tag findTag(String tagName, Company company) {
        return tagDao.findOne(tagName, company.getId());
    }


    public Tag findTag(String tagName) {
        Company company = securityHelper.getCompany();
        if (company == null)
            return null;
        return findTag(tagName, company);
    }

    public DocumentType resolveDocType(String documentType) {
        Company company = securityHelper.getCompany();
        if (company == null)
            return null;
        return resolveDocType(company, documentType);
    }

    /**
     * Finds a company document type and creates it if it is missing
     *
     * @param company
     * @param documentType
     * @return
     */
    public DocumentType resolveDocType(Company company, String documentType) {
        return resolveDocType(company, documentType, true);
    }

    /**
     * finds or creates a Document Type for the caller's company
     *
     * @param company
     * @param documentType    name of the document
     * @param createIfMissing
     * @return created DocumentType
     */
    public DocumentType resolveDocType(Company company, String documentType, Boolean createIfMissing) {
        if (documentType == null) {
            throw new IllegalArgumentException("DocumentTypeNode cannot be null");
        }

        return tagDao.findOrCreateDocument(documentType, company, createIfMissing);

    }

    public Long getCompanyTagManager(Long companyId) {
        return tagDao.getCompanyTagManager(companyId);
    }

    public void createCompanyTagManager(Long id, String companyName) {
        tagDao.createCompanyTagManager(id, companyName.toLowerCase());

    }

    public Collection<Tag> findDirectedTags(Tag startTag) {
        return tagDao.findDirectedTags(startTag, securityHelper.getCompany().getId(), true); // outbound
    }

}
