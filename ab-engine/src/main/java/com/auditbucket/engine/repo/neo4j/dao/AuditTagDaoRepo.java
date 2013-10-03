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
import com.auditbucket.audit.model.AuditTag;
import com.auditbucket.engine.repo.neo4j.AuditTagRepo;
import com.auditbucket.engine.repo.neo4j.model.AuditTagRelationship;
import com.auditbucket.registration.model.Tag;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private Logger logger = LoggerFactory.getLogger(AuditTagRepo.class);

    @Override
    public AuditTag save(AuditHeader auditHeader, Tag tag, String type) {
        if (type != null) {
            Node headerNode = template.getNode(auditHeader.getId());
            Node tagNode = template.getNode(tag.getId());
            //Primary exploration relationship
            if (template.getRelationshipBetween(tagNode, headerNode, type) == null)
                template.createRelationshipBetween(tagNode, headerNode, type, null);
            else
                return null; // we already know about this

            logger.debug("Created Tag[{}] Relationship for type {}", tag, type);
        }

        // Only keeping this so that we can efficiently find all the tags being used by a header/tag combo
        // could be simplified to all tags attached to a single Tag node.
        // type should be the Neo4J Node type when V2 is released.
        AuditTagRelationship atr = new AuditTagRelationship(auditHeader, tag, type);
        logger.debug("Creating Tag Relationship for audit [{}]", auditHeader.getId());
        return template.save(atr);
    }

    @Override
    public Set<AuditTag> find(Tag tagName, String type) {
        if (tagName == null)
            return null;
        return auditTagRepo.findTagValues(tagName.getId(), type);
    }

    @Override
    public Set<AuditHeader> findTagAudits(Long tagId) {
        if (tagId == null)
            return null;
        return auditTagRepo.findTagAudits(tagId);
    }


    @Override
    public Set<AuditTag> getAuditTags(Long id) {
        return auditTagRepo.findAuditTags(id);
    }

    public void update(Set<AuditTag> newValues) {
        for (AuditTag iTagValue : newValues) {
            auditTagRepo.save((AuditTagRelationship) iTagValue);
        }
    }

}
