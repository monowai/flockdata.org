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
import com.auditbucket.engine.service.EngineConfig;
import com.auditbucket.helper.TagException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.repo.neo4j.model.TagNode;
import org.neo4j.graphdb.Node;
import org.neo4j.kernel.impl.core.NodeProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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

    @Autowired
    DocumentTypeRepo documentTypeRepo;

    @Autowired
    Neo4jTemplate template;

    @Autowired
    EngineConfig engineAdmin;

    private Logger logger = LoggerFactory.getLogger(TagDaoNeo4J.class);

    public Tag save(Company company, TagInputBean tagInput) {
        String tagSuffix = engineAdmin.getTagSuffix(company);
        List<String> createdValues = new ArrayList();
        return save(company, tagInput, tagSuffix, createdValues);
    }

    Tag save(Company company, TagInputBean tagInput, Collection<String> createdValues) {
        String tagSuffix = engineAdmin.getTagSuffix(company);
        return save(company, tagInput, tagSuffix, createdValues);
    }

    public Iterable<Tag> save(Company company, Iterable<TagInputBean> tags) {
        String tagSuffix = engineAdmin.getTagSuffix(company);
        List<Tag> result = new ArrayList<>();
        List<String> createdValues = new ArrayList();
        for (TagInputBean tag : tags) {
            result.add(save(company, tag, tagSuffix, createdValues));
        }
        return result;
    }

    Tag save(Company company, TagInputBean tagInput, String tagSuffix, Collection createdValues) {
        // Check exists
        TagNode existingTag = (TagNode) findOne(tagInput.getName(), company);
        Node start;
        if (existingTag == null) {
            if ( tagInput.isMustExist()){
                throw new TagException("Tag "+tagInput.getName()+" is expected to exist. Illegal tag, ignoring request.");
            }
            else
                start = createTag(tagInput, tagSuffix);
        } else {
            start = template.getNode(existingTag.getId());
        }

        Map<String, TagInputBean[]> tags = tagInput.getTargets();
        for (String rlxName : tags.keySet()) {
            TagInputBean[] associatedTag = tags.get(rlxName);
            for (TagInputBean tagInputBean : associatedTag) {
                createRelationship(company, start, tagInputBean, rlxName, createdValues);
            }

        }
        return new TagNode(start);
    }

    private Node createTag(TagInputBean tagInput, String tagSuffix) {
        TagNode tag = new TagNode(tagInput);

        // ToDo: Should a type be suffixed with company in multi-tenanted? - more time to think!!
        //       do we care that one company can see another companies tag value? Certainly not the
        //       audit data.
        if (tagInput.getIndex() != null && !":".equals(tagInput.getIndex()))
            tagSuffix = tagSuffix + " " + tagInput.getIndex();

        // ToDo: Multi-tenanted custom tags
        //logger.info("About to merge Tag {}", tag.getKey());
        String query = "merge (tag:Tag" + tagSuffix + " {code:{code}, name:{name}, key:{key}";
        Map<String, Object> params = new HashMap<>();
        params.put("code", tag.getCode());
        params.put("key", tag.getKey());
        params.put("name", tag.getName());
        // ToDo: - set custom properties

//        Map<String, Object> properties = tagInput.getProperties();
//        for (Map.Entry<String, Object> prop : properties.entrySet()) {
//            if (! PropertyConversion.isSystemColumn(prop.getKey())) {
//                if (prop.getValue() != null) {
//                    DefinedProperty property = PropertyConversion.convertProperty(1, prop.getValue());
//                    query = query + ", " + PropertyConversion.toJsonColumn(prop.getKey(), property.value());
//                }
//            }
//        }

        query = query + "})  return tag";
        Result<Map<String, Object>> result = template.query(query, params);
        Map<String, Object> mapResult = result.singleOrNull();
        Node end = (Node) mapResult.get("tag");
        //existingTag.setId(end.getId());
        return end;
    }

    /**
     * Create unique relationship between the tag and the node
     *
     * @param company       associate the tag with this company
     * @param startNode     notional start node
     * @param associatedTag tag to make or get
     * @param rlxName       relationship name
     * @param createdValues
     * @return the created tag
     */
    Tag createRelationship(Company company, Node startNode, TagInputBean associatedTag, String rlxName, Collection<String> createdValues) {
        // Careful - this save can be recursive
        // ToDo - idea = create all tags first then just create the relationships
        Tag tag = save(company, associatedTag, createdValues);
        Node endNode = template.getNode(tag.getId());

        Long startId = (!associatedTag.isReverse() ? startNode.getId() : endNode.getId());
        Long endId = (!associatedTag.isReverse() ? endNode.getId() : startNode.getId());

        String key = rlxName + ":" + startId + ":" + endId;
        if (createdValues.contains(key))
            return tag;

        //logger.info("Creating RLX {}, {}, {}", rlxName, startId, endId);
        String cypher = "match startNode, endNode where " +
                "id(startNode)={start} " +
                "and id(endNode)={end} " +
                "create unique (startNode) -[r:`" + rlxName + "`]->(endNode) return r";
        Map<String, Object> params = new HashMap<>();
        params.put("start", startId);
        params.put("end", endId);
        template.query(cypher, params);
        createdValues.add(key);
        return tag;
    }

    @Override
    public Collection<Tag> findDirectedTags(Tag startTag, Company company, boolean b) {
        //Long coTags = getCompanyTagManager(companyId);
        //"MATCH audit<-[tagType]-(tag:Tag"+engineAdmin.getTagSuffix(company)+") " +
        String query = "start tag=node({tagId}) " +
                " match (tag)-->(otherTag:Tag" + engineAdmin.getTagSuffix(company) + ") " +
                "       return otherTag";
        Map<String, Object> params = new HashMap<>();
        params.put("tagId", startTag.getId());
        //params.put("coTags", coTags);
        Result<Map<String, Object>> result = template.query(query, params);

        if (!((Result) result).iterator().hasNext())
            return new ArrayList<>();

        Iterator<Map<String, Object>> rows = result.iterator();

        Collection<Tag> results = new ArrayList<>();

        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
            results.add(new TagNode((Node) row.get("otherTag")));
        }
        //
        return results;
    }

    @Override
    public Map<String, Tag> findTags(Company company, String type) {
        Map<String, Tag> tagResults = new HashMap<>();
        String query = "match (tag:" + type + (engineAdmin.getTagSuffix(company)) + ") return tag";
        // Look at PAGE
        Result<Map<String, Object>> results = template.query(query, null);
        for (Map<String, Object> row : results) {
            Object o = row.get("tag");
            Tag t = new TagNode((NodeProxy) o);
            tagResults.put(t.getName(), t);

        }
        return tagResults;
    }

    @Override
    @Cacheable(value = "companyTag", unless = "#result == null")
    public Tag findOne(String tagName, Company company) {
        if (tagName == null || company == null)
            throw new IllegalArgumentException("Null can not be used to find a tag ");

        String query = "match (tag:Tag" + engineAdmin.getTagSuffix(company) + ") where tag.key ={tagKey} return tag";
        Map<String, Object> params = new HashMap<>();
        params.put("tagKey", tagName.toLowerCase().replaceAll("\\s", "")); // ToDo- formula to static method
        Result<Map<String, Object>> result = template.query(query, params);
        Map<String, Object> mapResult = result.singleOrNull();
        if (mapResult != null)
            return new TagNode((Node) mapResult.get("tag"));
        else
            return null;

    }

    @Cacheable(value = "companyDocType", unless = "#result == null")
    public DocumentType findCompanyDocument(String documentType, Company company) {
        return documentTypeRepo.findCompanyDocType(company.getId(), company.getId() + "." + documentType.toLowerCase().replaceAll("\\s", ""));
    }

    public DocumentType findOrCreateDocument(String documentType, Company company, Boolean createIfMissing) {

        DocumentType docResult = findCompanyDocument(documentType, company);
        if (docResult == null && createIfMissing) {
            DocumentTypeNode docType = new DocumentTypeNode(documentType, company);
            logger.debug("Creating document type {}", documentType);
            docResult = documentTypeRepo.save(docType);

        }
        return docResult;

    }
}
