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

package com.auditbucket.registration.repo.neo4j.dao;

import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.engine.repo.neo4j.DocumentTypeRepo;
import com.auditbucket.engine.repo.neo4j.model.DocumentTypeNode;
import com.auditbucket.registration.dao.TagDaoI;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.repo.neo4j.TagRepository;
import com.auditbucket.registration.repo.neo4j.model.TagNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: Mike Holdsworth
 * Date: 26/06/13
 * Time: 8:33 PM
 */
@Repository
public class TagDaoImpl implements TagDaoI {

    @Autowired
    TagRepository tagRepo;

    @Autowired
    DocumentTypeRepo documentTypeRepo;

    public Tag save(Tag tag) {
        TagNode tagToCreate;
        if ((tag instanceof TagNode))
            tagToCreate = (TagNode) tag;
        else
            tagToCreate = new TagNode(tag);

        return tagRepo.save(tagToCreate);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Tag findOne(String tagName, Long id) {
        if (tagName == null || id == null)
            throw new IllegalArgumentException("Null can not be used to find a tag ");
        return tagRepo.findCompanyTag(tagName, id);
    }

    @Override
    public DocumentType findOrCreate(String documentType, Company company) {
        DocumentType result = documentTypeRepo.findCompanyDocType(company.getId(), documentType);
        if (result == null) {
            result = documentTypeRepo.save(new DocumentTypeNode(documentType, company));
        }
        return result;

    }
}
