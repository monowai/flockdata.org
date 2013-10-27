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
import com.auditbucket.dao.TagDao;
import com.auditbucket.engine.repo.neo4j.model.AuditHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.AuditTagRelationship;
import com.auditbucket.helper.AuditException;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.repo.neo4j.model.TagNode;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 11:07 PM
 */
@Repository("auditTagDAO")
public class AuditTagDaoRepo implements com.auditbucket.dao.AuditTagDao {
    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(AuditTagDaoRepo.class);

    @Override
    public AuditTag save(AuditHeader auditHeader, Tag tag, String relationship) {
        return save(auditHeader, tag, relationship, null);
    }


    public AuditTag save(AuditHeader auditHeader, Tag tag, String relationship, Map<String, Object> propMap) {
        // ToDo: this will only set properties for the "current" tag to Header. it will not version it.
        if (relationship == null) {
            relationship = "GENERAL_TAG";
        }
        Node headerNode = template.getPersistentState(auditHeader);
        Node tagNode = template.getPersistentState(tag);
        //Primary exploration relationship
        Relationship r = template.getRelationshipBetween(tagNode, headerNode, relationship);
        boolean recreated = false;
        if (r != null) {// Recreate
            recreated = true;
            r.delete();
        }

        template.createRelationshipBetween(tagNode, headerNode, relationship, propMap);
        if (recreated)
            return null;
        logger.trace("Created Relationship Tag[{}] of type {}", tag, relationship);

        return null;
    }

    @Autowired
    TagDao tagDao;

    @Override
    public void deleteAuditTags(AuditHeader auditHeader, Collection<AuditTag> auditTags) throws AuditException {
        Node auditNode = null;
        for (AuditTag tag : auditTags) {
            if (!tag.getAuditId().equals(auditHeader.getId()))
                throw new AuditException("Tags to not belong to the required AuditHeader");

            if (auditNode == null) {
                auditNode = template.getNode(tag.getAuditId());
            }

            Relationship r = template.getRelationship(tag.getId());
            r.delete();
            tagDao.deleteCompanyRelationship(auditHeader.getFortress().getCompany(), tag.getTag());
            template.getNode(tag.getTag().getId()).delete();
        }
    }

    /**
     * Rewrites the relationship type between the nodes copying the properties
     *
     * @param auditHeader audit
     * @param existingTag current
     * @param newType     new type name
     */
    @Override
    public void changeType(AuditHeader auditHeader, AuditTag existingTag, String newType) {
        if (!relationshipExists(auditHeader, existingTag.getTag(), newType)) {
            Relationship r = template.getRelationship(existingTag.getId());
            Iterable<String> propertyKeys = r.getPropertyKeys();
            Map<String, Object> properties = new HashMap<>();
            for (String propertyKey : propertyKeys) {
                properties.put(propertyKey, r.getProperty(propertyKey));
            }
            template.createRelationshipBetween(r.getStartNode(), r.getEndNode(), newType, properties);
            r.delete();
        }
    }

    @Override
    public Set<AuditHeader> findTagAudits(Tag tag) {
        String query = "start tag=node({tagId}) " +
                "       match tag-[]->audit" +
                "      return audit";
        Map<String, Object> params = new HashMap<>();
        params.put("tagId", tag.getId());
        Result<Map<String, Object>> result = template.query(query, params);
        Set<AuditHeader> results = new HashSet<>();
        for (Map<String, Object> row : result) {
            AuditHeader audit = template.convert(row.get("audit"), AuditHeaderNode.class);
            results.add(audit);
        }

        return results;
    }

    @Override
    public Boolean relationshipExists(AuditHeader auditHeader, Tag tag, String relationshipType) {
        Node end = template.getPersistentState(auditHeader);
        Node start = template.getPersistentState(tag);
        return (template.getRelationshipBetween(start, end, relationshipType) != null);

    }


    @Override
    public Set<AuditTag> getAuditTags(AuditHeader auditHeader, Long companyTagId) {
        String query = "start audit=node({auditId}), cTag=node({cTagId}) " +
                "MATCH audit<-[tagType]-(tag)<--cTag " +
                "return tag, tagType";

        Map<String, Object> params = new HashMap<>();
        params.put("auditId", auditHeader.getId());
        params.put("cTagId", companyTagId);
        Set<AuditTag> tagResults = new HashSet<>();
        for (Map<String, Object> row : template.query(query, params)) {
            TagNode tag = template.projectTo(row.get("tag"), TagNode.class);
            //TagNode tag = template.convert(row.get("tag"), TagNode.class);
            Relationship relationship = template.convert(row.get("tagType"), Relationship.class);

            AuditTagRelationship auditTag = new AuditTagRelationship(auditHeader, tag, relationship);
            tagResults.add(auditTag);
        }
        return tagResults;
    }


}
