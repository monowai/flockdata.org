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
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.repo.neo4j.model.TagNode;
import org.neo4j.graphdb.Node;
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

    public Iterable<Tag> save(Company company, Iterable<TagInputBean> tags) {

        return null;
    }

    public Tag save(Company company, TagInputBean tagInput) {
        // Check exists
        TagNode sourceTag = (TagNode) findOne(tagInput.getName(), company);
        Node end;
        if (sourceTag == null) {
            sourceTag = new TagNode(tagInput);
            String tagSuffix = engineAdmin.getTagSuffix(company);
            // ToDo: Should a type be suffixed with company in multi-tenanted? - more time to think!!
            //       do we care that one company can see another companies tag value? Certainly not the
            //       audit data.
            if ( tagInput.getType() != null && !"".equals(tagInput.getType()))
                tagSuffix = tagSuffix +" " + tagInput.getType();


            String query = "merge (tag:Tag" + tagSuffix + " {code:{code}, name:{name}, key:{key}})  return tag";
            Map<String, Object> params = new HashMap<>();
            params.put("code", sourceTag.getCode());
            params.put("key", sourceTag.getKey());
            params.put("name", sourceTag.getName());
            Result<Map<String, Object>> result = template.query(query, params);
            Map<String, Object> mapResult = result.singleOrNull();
            end = (Node) mapResult.get("tag");
            sourceTag.setId(end.getId());

        } else {
            end = template.getNode(sourceTag.getId());
        }

        Map<String, TagInputBean[]> tags = tagInput.getTargets();
        for (String rlxName : tags.keySet()) {
            TagInputBean[] associatedTag = tags.get(rlxName);
            for (TagInputBean tagInputBean : associatedTag) {
                saveAssociated(company, end, tagInputBean, rlxName);
            }

        }
        return sourceTag;
    }


    Tag saveAssociated(Company company, Node startNode, TagInputBean associatedTag, String rlxName) {
        Tag tagToCreate = save(company, associatedTag);
        Node endNode = template.getNode(tagToCreate.getId());
        if (associatedTag.isReverse())
            template.createRelationshipBetween(endNode, startNode, rlxName, null);
        else
            template.createRelationshipBetween(startNode, endNode, rlxName, null);

        return tagToCreate;
    }

    @Override
    public Collection<Tag> findDirectedTags(Tag startTag, Company company, boolean b) {
        //Long coTags = getCompanyTagManager(companyId);
        //"MATCH audit<-[tagType]-(tag:Tag"+engineAdmin.getTagSuffix(company)+") " +
        String query = "start tag=node({tagId}) " +
                " match tag-->(otherTag:Tag" + engineAdmin.getTagSuffix(company) + ") " +
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
    @Cacheable(value = "companyTag", unless = "#result == null")
    public Tag findOne(String tagName, Company company) {
        if (tagName == null || company == null)
            throw new IllegalArgumentException("Null can not be used to find a tag ");

        String query = "match (tag:Tag" + (engineAdmin.isMultiTenanted() ? company.getCode() : "") + ") where tag.key ={tagKey} return tag";
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
