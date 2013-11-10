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
import com.auditbucket.bean.TagInputBean;
import com.auditbucket.engine.repo.neo4j.DocumentTypeRepo;
import com.auditbucket.engine.repo.neo4j.model.DocumentTypeNode;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.repo.neo4j.TagRepository;
import com.auditbucket.registration.repo.neo4j.model.TagNode;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 8:33 PM
 */
@Repository
public class TagDaoNeo4J implements com.auditbucket.dao.TagDao {

    public static final String COMPANY_TAGS = "COMPANY_TAGS";
    @Autowired
    TagRepository tagRepo;

    @Autowired
    DocumentTypeRepo documentTypeRepo;

    @Autowired
    Neo4jTemplate template;

    private Logger logger = LoggerFactory.getLogger(TagDaoNeo4J.class);

    public Iterable<Tag> save(Company company, Iterable<TagInputBean> tags) {

        // ToDo: Experimental - figuring out how to batch load tags
        // Doesnt' update the index so find by name fails
        Long cTag = null;
        Map<String, Object> params = new HashMap<>();
        String cypher = null;
        String retclause = " return tag0"; // Dynamic return clause - 1 per InputBean
        int count = 0;

        for (TagInputBean next : tags) {
            if (cTag == null) {
                TagNode tn = new TagNode(next);
                cTag = getCompanyTagManager(company.getId());
                params.put("cTag", cTag);
                cypher = "start tagManager=node({cTag}) create unique " +
                        "tagManager-[:TAG_COLLECTION]->(tag0 {name:\"" + tn.getName() + "\", " +
                        "code:\"" + tn.getTagSearchName() + "\", __type__:\"ab.Tag\"}) ";
            } else {
                TagNode tn = new TagNode(next);
                count++;
                cypher = cypher + ", " +
                        "tagManager-[:TAG_COLLECTION]->(tag" + count + " {name:\"" + tn.getName() + "\", " +
                        "code:\"" + tn.getTagSearchName() + "\", __type__:\"ab.Tag\"}) ";
                retclause = retclause + ", tag" + count;
            }
        }
        if (cypher == null)
            return null;

        cypher = cypher + retclause;
        EndResult<Map<String, Object>> r = template.query(cypher, params);
        Map<String, Object> mapResult = r.single();
        ArrayList<Tag> returnResult = new ArrayList<>();

        int max = count;
        for (int i = 0; i < max; i++) {
            returnResult.add(template.projectTo(mapResult.get("tag" + i), TagNode.class));

        }
        return returnResult;
    }

    public Tag save(Company company, TagInputBean tagInput) {
        // Check exists
        // ToDo: Neo4j2 - don't associate with the company rather a tag node type
        Tag existingTag = findOne(tagInput.getName(), company.getId());
        if (existingTag != null)
            return existingTag;

        TagNode tagToCreate = new TagNode(tagInput);
        tagToCreate = tagRepo.save(tagToCreate);

        Node end = template.getPersistentState(tagToCreate);
        Node start = getCompanyTagManagerNode(company.getId());
        Relationship r = template.getRelationshipBetween(start, end, COMPANY_TAGS);
        if (r == null)
            template.createRelationshipBetween(start, end, COMPANY_TAGS, null);

        Map<String, TagInputBean> tags = tagInput.getAssociatedTags();
        for (String rlxName : tags.keySet()) {
            TagInputBean associatedTag = tags.get(rlxName);
            saveAssociated(company, end, associatedTag, rlxName);
        }
        return tagToCreate;
    }

    Tag saveAssociated(Company company, Node startNode, TagInputBean associatedTag, String rlxName) {
        Tag tagToCreate = save(company, associatedTag);
        Node endNode = template.getPersistentState(tagToCreate);
        if (associatedTag.isOutbound())
            template.createRelationshipBetween(startNode, endNode, rlxName, null);
        else
            template.createRelationshipBetween(endNode, startNode, rlxName, null);

        return tagToCreate;
    }

    @Cacheable(value = "companyTagManager", unless = "#result == null")
    private Node getCompanyTagManagerNode(Long companyId) {
        String query = "start company=node({companyId}) match company-[:TAG_COLLECTION]->ct return ct";
        Map<String, Object> params = new HashMap<>();
        params.put("companyId", companyId);
        Result<Map<String, Object>> result = template.query(query, params);
        Map<String, Object> mapResult = result.singleOrNull();
        return ((Node) mapResult.get("ct"));
    }

    public Long getCompanyTagManager(Long companyId) {
        return getCompanyTagManagerNode(companyId).getId();
    }

    @Override
    public void deleteCompanyRelationship(Company company, Tag tag) {
        Node coTags = getCompanyTagManagerNode(company.getId());
        Node tagNode = template.getNode(tag.getId());
        template.deleteRelationshipBetween(coTags, tagNode, COMPANY_TAGS);
    }

    @Override
    public Collection<Tag> findDirectedTags(Tag startTag, long companyId, boolean b) {
        Long coTags = getCompanyTagManager(companyId);
        String query = "start tag=node({tagId}), coTags=node({coTags}) " +
                " match tag-->otherTags<--coTags" +
                "       return otherTags";
        Map<String, Object> params = new HashMap<>();
        params.put("tagId", startTag.getId());
        params.put("coTags", coTags);
        EndResult<Map<String, Object>> r = template.query(query, params);

        if (!((EndResult) r).iterator().hasNext())
            return new ArrayList<>();

        Object o = r.single();
        Map<String, Object> mapResult = (Map<String, Object>) o;
        Collection<Tag> results = new ArrayList<>(mapResult.size());

        for (String s : mapResult.keySet()) {
            results.add(template.projectTo(mapResult.get(s), TagNode.class));
        }

        //
        return results;
    }

    /**
     * each company has exactly one companyTagCollection to which each tag is associated.
     * this method should be called whenever a company is created.
     *
     * @param companyId         company PK
     * @param tagCollectionName name to know this by
     * @return pk of the tag manager.
     */
    public Long createCompanyTagManager(Long companyId, String tagCollectionName) {
        assert (tagCollectionName != null);
        String query = "start company=node({companyId}) " +
                "       create unique company-[:TAG_COLLECTION]->(ct {name:{tagName}}) " +
                "       return ct";
        Map<String, Object> params = new HashMap<>();
        params.put("companyId", companyId);
        params.put("tagName", tagCollectionName + " Tags");
        Result<Map<String, Object>> result = template.query(query, params);
        Map<String, Object> mapResult = result.single();
        return ((Node) mapResult.get("ct")).getId();
    }

    @Override
    @Cacheable(value = "companyTag", unless = "#result == null")
    public Tag findOne(String tagName, Long companyId) {
        if (tagName == null || companyId == null)
            throw new IllegalArgumentException("Null can not be used to find a tag ");
        Tag tag = tagRepo.findCompanyTagBySearchName(companyId, tagName.toLowerCase().replaceAll("\\s", ""));
        return tag;
    }

    @Override
    @Cacheable(value = "companyDocType", unless = "#result == null")
    public DocumentType findOrCreate(String documentType, Company company) {
        DocumentType result = documentTypeRepo.findCompanyDocType(company.getId(), documentType.toLowerCase().replaceAll("\\s", ""));

        if (result == null) {
            logger.debug("Creating document type {}", documentType);
            result = documentTypeRepo.save(new DocumentTypeNode(documentType, company));
        }
        return result;

    }
}
