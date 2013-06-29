package com.auditbucket.registration.service;

import com.auditbucket.audit.model.IDocumentType;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.dao.TagDaoI;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ITag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User: mike
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
        input.setCompany(securityHelper.getCompany());

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
            throw new IllegalArgumentException("Documentype/Company cannot be null");
        }
        ICompany company = securityHelper.getCompany();
        if (company == null)
            return null;

        return tagDao.findOrCreate(documentType, company);

    }

}
