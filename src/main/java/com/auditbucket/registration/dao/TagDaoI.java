package com.auditbucket.registration.dao;

import com.auditbucket.audit.model.IDocumentType;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ITag;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 8:12 PM
 */
public interface TagDaoI {
    ITag save(ITag tag);

    ITag findOne(String tagName, Long id);

    IDocumentType findOrCreate(String documentType, ICompany company);

}
