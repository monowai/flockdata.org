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

package com.auditbucket.engine.repo.neo4j.dao;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.TagValue;
import com.auditbucket.engine.repo.neo4j.AuditTagRepo;
import com.auditbucket.engine.repo.neo4j.model.AuditTagRelationship;
import com.auditbucket.registration.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 11:07 PM
 */
@Repository("auditTagDAO")
public class AuditTagDaoRepo implements com.auditbucket.dao.AuditTagDao {
    @Autowired
    Neo4jTemplate template;

    @Autowired
    AuditTagRepo auditTagRepo;

    @Override
    public TagValue save(AuditHeader auditHeader, Tag tag, String tagValue) {
        AuditTagRelationship atv = new AuditTagRelationship(tag, auditHeader, tagValue);
        //Node headerNode = template.getNode(auditHeader.getId());
        //Node tagNode = template.getNode(tag.getId());
        //Primary exploration approach
        //template.createRelationshipBetween(headerNode, tagNode, tagValue, null);

        // Only keeping this so that we can efficiently find all the tags being used by a header/tag combo
        // could be simplified to all tags attached to a single Tag node.
        return template.save(atv);
    }

    @Override
    public Set<TagValue> find(Tag tagName, String tagValue) {
        if (tagName == null)
            return null;
        return auditTagRepo.findTagValues(tagName.getId(), tagValue);
    }

    @Override
    public Set<AuditHeader> findTagAudits(Tag tagName) {
        if (tagName == null)
            return null;
        return auditTagRepo.findTagAudits(tagName.getId());
    }


    @Override
    public Set<TagValue> getAuditTags(AuditHeader ah) {
        return auditTagRepo.findAuditTags(ah.getId());
    }

    public void update(Set<TagValue> newValues) {
        for (TagValue iTagValue : newValues) {
            auditTagRepo.save((AuditTagRelationship) iTagValue);
        }
    }
}
