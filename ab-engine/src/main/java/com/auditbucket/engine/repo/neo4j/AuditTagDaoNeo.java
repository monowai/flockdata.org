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

package com.auditbucket.engine.repo.neo4j;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditTag;
import com.auditbucket.dao.AuditTagDao;
import com.auditbucket.dao.TagDao;
import com.auditbucket.engine.repo.neo4j.model.AuditHeaderNode;
import com.auditbucket.engine.repo.neo4j.model.AuditTagRelationship;
import com.auditbucket.engine.service.EngineConfig;
import com.auditbucket.helper.AuditException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.repo.neo4j.model.TagNode;
import com.auditbucket.registration.service.TagService;
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
 * Data Access Object that manipulates tag nodes against audit headers
 *
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 11:07 PM
 */
@Repository("auditTagDAO")
public class AuditTagDaoNeo implements AuditTagDao {
    @Autowired
    Neo4jTemplate template;

    @Autowired
    TagService tagService;

    private Logger logger = LoggerFactory.getLogger(AuditTagDaoNeo.class);

    @Override
    public AuditTag save(AuditHeader auditHeader, Tag tag, String relationshipName) {
        return save(auditHeader, tag, relationshipName, null);
    }

    /**
     * creates the relationship between the header and the tag of the name type.
     * If auditId == null, then an AuditTag for the caller to deal with otherwise the relationship
     * is persisted and null is returned.
     *
     * @param auditHeader  constructed header
     * @param tag          tag
     * @param relationshipName name
     * @param propMap      properties to associate with an audit tag (weight)
     * @return Null or AuditTag
     */
    public AuditTag save(AuditHeader auditHeader, Tag tag, String relationshipName, Map<String, Object> propMap) {
        // ToDo: this will only set properties for the "current" tag to Header. it will not version it.
        if (relationshipName == null) {
            relationshipName = "GENERAL_TAG";
        }
        if ( tag == null )
            throw new IllegalArgumentException("Tag must not be NULL. Relationship["+ relationshipName +"]");

        AuditTagRelationship rel = new AuditTagRelationship(auditHeader, tag, relationshipName, propMap);
        if (auditHeader.getId() == null)
            return rel;

        Node headerNode = template.getPersistentState(auditHeader);
        Node tagNode = template.getNode(tag.getId());
        //Primary exploration relationship
        Relationship r = template.getRelationshipBetween(tagNode, headerNode, relationshipName);

        if (r != null) {
            return null;
        }

        template.createRelationshipBetween(tagNode, headerNode, relationshipName, propMap);
        logger.trace("Created Relationship Tag[{}] of type {}", tag, relationshipName);
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
            //tagDao.deleteCompanyRelationship(auditHeader.getFortress().getCompany(), tag.getTag());
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
    public Boolean relationshipExists(AuditHeader auditHeader, Tag tag, String relationshipName) {
        Node end = template.getPersistentState(auditHeader);
        Node start = template.getNode(tag.getId());
        return (template.getRelationshipBetween(start, end, relationshipName) != null);

    }

    @Autowired
    EngineConfig engineAdmin;

    @Override
    public Set<AuditTag> getAuditTags(AuditHeader auditHeader, Company company) {
        String query = "start audit=node({auditId}) " +
                "MATCH audit<-[tagType]-(tag:Tag" + engineAdmin.getTagSuffix(company) + ") " +
                "return tag, tagType";

        Map<String, Object> params = new HashMap<>();
        params.put("auditId", auditHeader.getId());

        Set<AuditTag> tagResults = new HashSet<>();
        for (Map<String, Object> row : template.query(query, params)) {
            Node n = (Node) row.get("tag");
            TagNode tag = new TagNode(n);
            Relationship relationship = template.convert(row.get("tagType"), Relationship.class);

            AuditTagRelationship auditTag = new AuditTagRelationship(auditHeader, tag, relationship);
            tagResults.add(auditTag);
        }
        return tagResults;
    }


}
