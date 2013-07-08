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

package com.auditbucket.core.repo.neo4j.dao;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.core.repo.neo4j.AuditTagRepo;
import com.auditbucket.core.repo.neo4j.model.AuditTagValue;
import com.auditbucket.dao.IAuditTagDao;
import com.auditbucket.registration.model.ITag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 11:07 AM
 */
@Repository("auditTagDAO")
public class AuditTagDao implements IAuditTagDao {
    @Autowired
    Neo4jTemplate template;

    @Autowired
    AuditTagRepo auditTagRepo;

    @Override
    public ITagValue save(ITag tagName, IAuditHeader header, String tagValue) {
        AuditTagValue atv = new AuditTagValue(tagName, header, tagValue);
        return template.save(atv);

    }

    @Override
    public Set<ITagValue> find(ITag tagName, String tagValue) {
        return auditTagRepo.findTagValues(tagName.getId(), tagValue);
    }

    @Override
    public Set<ITagValue> getAuditTags(IAuditHeader ah) {
        return auditTagRepo.findAuditTags(ah.getId());
    }

    public void update(Set<ITagValue> newValues) {
        //Set<ITagValue> existingTags = header.getTagValues();
        for (ITagValue iTagValue : newValues) {
            auditTagRepo.save((AuditTagValue) iTagValue);
        }
    }
}
