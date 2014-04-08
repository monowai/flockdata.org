/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
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

import com.auditbucket.dao.SchemaDao;
import com.auditbucket.dao.TagDao;
import com.auditbucket.engine.service.EngineConfig;
import com.auditbucket.helper.DatagioTagException;
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
public class TagDaoNeo4j implements TagDao {

    @Autowired
    SchemaDao schemaDao;

    @Autowired
    Neo4jTemplate template;

    @Autowired
    EngineConfig engineAdmin;

    private Logger logger = LoggerFactory.getLogger(TagDaoNeo4j.class);

    public Tag save(Company company, TagInputBean tagInput) {
        String tagSuffix = engineAdmin.getTagSuffix(company);
        List<String> createdValues = new ArrayList<>();
        return save(company, tagInput, tagSuffix, createdValues, false);
    }

    Tag save(Company company, TagInputBean tagInput, Collection<String> createdValues, boolean suppressRelationships) {
        String tagSuffix = engineAdmin.getTagSuffix(company);
        return save(company, tagInput, tagSuffix, createdValues, suppressRelationships);
    }

    public Collection<TagInputBean> save(Company company, Iterable<TagInputBean> tagInputs) {
        return save(company, tagInputs, false);
    }

    @Override
    public Collection<TagInputBean> save(Company company, Iterable<TagInputBean> tagInputs, boolean suppressRelationships) {
        String tagSuffix = engineAdmin.getTagSuffix(company);
        List<TagInputBean> errorResults = new ArrayList<>();
        List<String> createdValues = new ArrayList<>();
        for (TagInputBean tagInputBean : tagInputs) {
            try {
                save(company, tagInputBean, tagSuffix, createdValues, suppressRelationships) ;
            } catch (DatagioTagException te){
                logger.error ("Tag Exception [{}]",te.getMessage());
                tagInputBean.setServerMessage(te.getMessage());
                errorResults.add(tagInputBean);
            }

        }
        return errorResults;
    }

    Tag save(Company company, TagInputBean tagInput, String tagSuffix, Collection<String> createdValues, boolean suppressRelationships) {
        // Check exists
        TagNode existingTag = (TagNode) findOne(company, tagInput.getName(), tagInput.getIndex());
        Node start;
        if (existingTag == null) {
            if (tagInput.isMustExist()) {
                tagInput.setServerMessage("Tag [" + tagInput.getName() + "] should exist for ["+tagInput.getIndex()+"] but doesn't. Ignoring this request.");
                throw new DatagioTagException("Tag [" + tagInput.getName() + "] should exist for ["+tagInput.getIndex()+"] but doesn't. Ignoring this request.");
            } else
                start = createTag(company, tagInput, tagSuffix);
        } else {
            start = template.getNode(existingTag.getId());
        }

        Map<String, Collection<TagInputBean>> tags = tagInput.getTargets();
        for (String rlxName : tags.keySet()) {
            Collection<TagInputBean> associatedTag = tags.get(rlxName);
            for (TagInputBean tagInputBean : associatedTag) {
                createRelationship(company, start, tagInputBean, rlxName, createdValues, suppressRelationships);
            }

        }
        return new TagNode(start);
    }

    private Node createTag(Company company, TagInputBean tagInput, String tagSuffix) {
        TagNode tag = new TagNode(tagInput);

        // ToDo: Should a type be suffixed with company in multi-tenanted? - more time to think!!
        //       do we care that one company can see another companies tag value? Certainly not the
        //       audit data.
        if (tagInput.isDefault())
            tagSuffix = Tag.DEFAULT + tagSuffix;
        else {
            schemaDao.registerTagIndex(company, tagInput.getIndex());
            tagSuffix = tagInput.getIndex() + " " + Tag.DEFAULT + tagSuffix;
        }

        // ToDo: Multi-tenanted custom tagInputs?
        // _Tag only exists for SDN projection
        String query = "merge (tag" + tagSuffix + " {code:{code}, name:{name}, key:{key}";
        Map<String, Object> params = new HashMap<>();
        params.put("code", tag.getCode());
        params.put("key", tag.getKey());
        //params.put("typeKey", tag.getKey());
        params.put("name", tag.getName());
        // ToDo: - set custom properties

//        Map<String, Object> properties = tagInput.getProperties();
//        for (Map.Entry<String, Object> prop : properties.entrySet()) {
//            if (! PropertyConversion.isSystemColumn(prop.getMetaKey())) {
//                if (prop.getValue() != null) {
//                    DefinedProperty property = PropertyConversion.convertProperty(1, prop.getValue());
//                    query = query + ", " + PropertyConversion.toJsonColumn(prop.getMetaKey(), property.value());
//                }
//            }
//        }

        query = query + "})  return tag";
        Result<Map<String, Object>> result = template.query(query, params);
        Map<String, Object> mapResult = result.singleOrNull();
        return (Node) mapResult.get("tag");
    }

    /**
     * Create unique relationship between the tag and the node
     *
     *
     * @param company       associate the tag with this company
     * @param startNode     notional start node
     * @param associatedTag tag to make or get
     * @param rlxName       relationship name
     * @param createdValues running list of values already created - performance op.
     * @param suppressRelationships
     * @return the created tag
     */
    Tag createRelationship(Company company, Node startNode, TagInputBean associatedTag, String rlxName, Collection<String> createdValues, boolean suppressRelationships) {
        // Careful - this save can be recursive
        // ToDo - idea = create all tagInputs first then just create the relationships
        Tag tag = save(company, associatedTag, createdValues, suppressRelationships);
        if (suppressRelationships)
            return tag;
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
                " match (tag)-->(otherTag" + Tag.DEFAULT + engineAdmin.getTagSuffix(company) + ") " +
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
            //results.add(new TagNode((Node) row.get("otherTag")));
            results.add(template.projectTo(row.get("otherTag"), TagNode.class));
        }
        //
        return results;
    }

    @Override
    public Map<String, Tag> findTags(Company company) {
        return findTags(company, Tag.DEFAULT+ (engineAdmin.getTagSuffix(company)));
    }
    @Override
    public Map<String, Tag> findTags(Company company, String index) {
        Map<String, Tag> tagResults = new HashMap<>();

        String query = "match (tag" + index+ ") return tag";
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
    public Collection<String> getExistingIndexes() {
        return template.getGraphDatabase().getAllLabelNames();

    }

    @Override
    @Cacheable(value = "companyTag", unless = "#result == null")
    public Tag findOne(Company company, String tagName, String index) {
        if (tagName == null || company == null)
            throw new IllegalArgumentException("Null can not be used to find a tag ");

        String query;
        if ("".equals(engineAdmin.getTagSuffix(company)))
            query = "match (tag" + index + ") where tag.key ={tagKey} return tag";
        else
            query = "match (tag" + index + engineAdmin.getTagSuffix(company) + ") where tag.key ={tagKey} return tag";

        Map<String, Object> params = new HashMap<>();
        params.put("tagKey", tagName.toLowerCase().replaceAll("\\s", "")); // ToDo- formula to static method
        Result<Map<String, Object>> result = template.query(query, params);
        Map<String, Object> mapResult = result.singleOrNull();
        if (mapResult != null)
            return new TagNode((Node) mapResult.get("tag"));
        else
            return null;

    }


}
