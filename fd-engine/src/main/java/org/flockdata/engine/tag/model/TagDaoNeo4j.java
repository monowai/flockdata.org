/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.tag.model;

import org.flockdata.engine.admin.EngineConfig;
import org.flockdata.engine.schema.dao.SchemaDaoNeo4j;
import org.flockdata.helper.FlockDataTagException;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class TagDaoNeo4j {

    @Autowired
    SchemaDaoNeo4j schemaDao;

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

    public Collection<Tag> save(Company company, Iterable<TagInputBean> tagInputs) {
        return save(company, tagInputs, false);
    }

    public Collection<Tag> save(Company company, Iterable<TagInputBean> tagInputs, boolean suppressRelationships) {
        String tagSuffix = engineAdmin.getTagSuffix(company);
        List<TagInputBean> errorResults = new ArrayList<>();
        List<String> createdValues = new ArrayList<>();
        Collection<Tag> results = new ArrayList<>();
        for (TagInputBean tagInputBean : tagInputs) {
            try {
                results.add(save(company, tagInputBean, tagSuffix, createdValues, suppressRelationships));
            } catch (FlockDataTagException te) {
                logger.error("Tag Exception [{}]", te.getMessage());
                tagInputBean.getServiceMessage(te.getMessage());
                errorResults.add(tagInputBean);
            }

        }
        return results;
    }

    Tag save(Company company, TagInputBean tagInput, String tagSuffix, Collection<String> createdValues, boolean suppressRelationships) {
        // Check exists
        Tag start = findTag(company, (tagInput.getCode() == null ? tagInput.getName() : tagInput.getCode()), tagInput.getLabel());
        if (start == null) {
            if (tagInput.isMustExist()) {
                tagInput.getServiceMessage("Tag [" + tagInput + "] should exist for [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
                throw new FlockDataTagException("Tag [" + tagInput + "] should exist for [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
            } else {
                start = createTag(company, tagInput, tagSuffix);
            }
        }

        Map<String, Collection<TagInputBean>> tags = tagInput.getTargets();
        for (String rlxName : tags.keySet()) {
            Collection<TagInputBean> associatedTag = tags.get(rlxName);
            for (TagInputBean tagInputBean : associatedTag) {
                createRelationship(company, start, tagInputBean, rlxName, createdValues, suppressRelationships);
            }

        }
        return template.findOne(start.getId(), TagNode.class);
    }

    private Tag createTag(Company company, TagInputBean tagInput, String suffix) {

        logger.trace("createTag {}", tagInput);
        // ToDo: Should a label be suffixed with company in multi-tenanted? - more time to think!!
        //       do we care that one company can see another companies tag value? Certainly not the
        //       track data.
        String label;
        if (tagInput.isDefault())
            label = "_Tag" + suffix;
        else {
            schemaDao.registerTag(company, tagInput.getLabel());
            label = tagInput.getLabel();
        }
        TagNode tag = new TagNode(tagInput, label);

        // ToDo: Multi-tenanted custom tagInputs?
        // _Tag only exists for SDN projection
//        String query = "merge (tag" + label + " {code:{code}, name:{name}, key:{key}";
//        Map<String, Object> params = new HashMap<>();
//        params.put("code", tag.getCode());
//        params.put("key", tag.getKey());
//        //params.put("typeKey", tag.getKey());
//        params.put("name", tag.getName());
//        // ToDo: - set custom properties
//
//        Map<String, Object> properties = tagInput.getProperties();
//        if (properties != null)
//            for (Map.Entry<String, Object> prop : properties.entrySet()) {
//                if (!PropertyConversion.isSystemColumn(prop.getKey())) {
//                    if (prop.getValue() != null) {
//                        DefinedProperty property = PropertyConversion.convertProperty(1, prop.getValue());
//                        query = query + ", " + PropertyConversion.toJsonColumn(prop.getKey(), property.value());
//                    }
//                }
//            }
//
//        query = query + "})  return tag";
//        Result<Map<String, Object>> result = template.query(query, params);


        //Map<String, Object> mapResult = result.singleOrNull();
        //return (Node) mapResult.get("tag");
        try {
            logger.debug("Saving {}", tag);
            tag = template.save(tag);
            logger.debug("Saved {}", tag);
            return tag;
        } catch (ConstraintViolationException e) {
            logger.debug("Error saving {}", tag);
            throw e;
            //return findTag(company, tagInput.getCode(), label);

        }

    }

    /**
     * Create unique relationship between the tag and the node
     *
     * @param company               associate the tag with this company
     * @param startNode             notional start node
     * @param associatedTag         tag to make or get
     * @param rlxName               relationship name
     * @param createdValues         running list of values already created - performance op.
     * @param suppressRelationships
     * @return the created tag
     */
    Tag createRelationship(Company company, Tag startNode, TagInputBean associatedTag, String rlxName, Collection<String> createdValues, boolean suppressRelationships) {
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
        //params.put("timestamp", System.currentTimeMillis());
        template.query(cypher, params);
        createdValues.add(key);
        return tag;
    }

    public Collection<Tag> findDirectedTags(Tag startTag, Company company, boolean b) {
        //Long coTags = getCompanyTagManager(companyId);
        //"MATCH track<-[tagType]-(tag:Tag"+engineAdmin.getTagSuffix(company)+") " +
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

    public Collection<Tag> findTags(Company company) {
        return findTags(company, Tag.DEFAULT + (engineAdmin.getTagSuffix(company)));
    }

    public Collection<Tag> findTags(Company company, String label) {
        Collection<Tag> tagResults = new ArrayList<>();
        // ToDo: Match to company - something like this.....
        //match (t:Law)-[:_TagLabel]-(c:ABCompany) where id(c)=0  return t,c;
        //match (t:Law)-[*..2]-(c:ABCompany) where id(c)=0  return t,c;
        String query = "match (tag:`" + label + "`) return tag";
        // Look at PAGE
        Result<Map<String, Object>> results = template.query(query, null);
        for (Map<String, Object> row : results) {
            Object o = row.get("tag");
            Tag t = template.projectTo(o, TagNode.class);
            tagResults.add(t);

        }
        return tagResults;
    }

    public Collection<String> getExistingLabels() {
        return template.getGraphDatabase().getAllLabelNames();

    }

    /**
     * Locates a tag fro the company of the supplied label including searching for it by alias
     *
     * @param company company to restrict by
     * @param tagCode value to search for. generally this is the Code value of the tag
     * @param label   Neo4j label for the node
     * @return null if not found
     */

    public Tag findTag(Company company, String tagCode, String label) {
        if (tagCode == null || company == null)
            throw new IllegalArgumentException("Null can not be used to find a tag (" + label + ")");

        logger.debug("findTag request {}, {}", tagCode, label);
        if (label.startsWith(":"))
            label = label.substring(1);
        String query;
        String theLabel = resolveLabel(label, engineAdmin.getTagSuffix(company));
        logger.debug("findTag code [{}] label [{}]", tagCode, label);
        query = "optional match (t:`" + theLabel + "` {key:{tagKey}}) " +
                "optional match (a:`" + theLabel + "Alias` {key:{tagKey}}) " +
                "with t,a " +
                "optional match (tag)-[:HAS_ALIAS]->(a) " +
                "return t,a,tag ";

        Map<String, Object> params = new HashMap<>();
        params.put("tagKey", parseKey(tagCode)); // ToDo- formula to static method
        Result<Map<String, Object>> result = template.query(query, params);
        Map<String, Object> mapResult = result.singleOrNull();
        if (mapResult != null) {

            Node n = null;
            if (mapResult.get("t") != null)
                n = (Node) mapResult.get("t");
            else if (mapResult.get("tag") != null)
                n = (Node) mapResult.get("tag");

            if (n == null) {
                logger.debug("findTag notFound {}, {}", tagCode, label);
                return null;
            }

            Tag tag = template.projectTo(n, TagNode.class);
            logger.debug("findTag found {}, {}", tag.getCode(), label);
            return tag;
        }
        logger.debug("findTag notFound {}, {}", tagCode, label);
        return null;// No tag found

    }

    public void purgeUnusedConcepts(Company company) {

        String query = "match (tag" + Tag.DEFAULT + ") delete tag";
        template.query(query, null);

        query = "match (tag:_Tag)-[r:TAG_INDEX]-(c:_ABCompany) where id(c)={company} delete r, tag";
        Map<String, Object> params = new HashMap<>();
        params.put("company", company.getId());
        template.query(query, params);

        // Remove all missing fortress/doctype relationships
        query = " MATCH (d:DocType) optional match(d)-[]-(:Fortress) delete d";
        template.query(query, null);

    }

    public void purge(Company company, String label) {
        String query;
        query = "match (tag:`" + resolveLabel(label, engineAdmin.getTagSuffix(company)) + "`) optional match(tag)-[r]-() delete r,tag";

        // ToDo: Tidy up concepts in use
        template.query(query, null);
    }

    private String resolveLabel(String label, String tagSuffix) {
        if ("".equals(tagSuffix))
            return label;
        return label + tagSuffix;
    }

    public void createAlias(Company company, Tag tag, String label, String aliasKeyValue) {
        // ToDo
        // match (c:Country) where c.code="NZ" create (ac:CountryAlias {name:"New Zealand", code:"New Zealand"}) , (c)-[:HAS_ALIAS]->(ac) return c, ac

        // This query will find the Tag, if it exists, or any alias that might exist
        // optional match (c:Country {code:"New Zealand"})
        // optional match (a:CountryAlias {code:"New Zealand"})
        // with c,a optional match (tag)-[:HAS_ALIAS]->(a)
        // return c, a,tag
        String theLabel = resolveLabel(label, engineAdmin.getTagSuffix(company));
        if (doesAliasExist(tag.getId(), theLabel, aliasKeyValue))
            return;

        String query = "match (t:" + theLabel + ") where id(t)={id} create (alias:`" + theLabel + "Alias" + "` {key:{key}}) ,(t)-[:HAS_ALIAS]->(alias) return t, alias";
        Map<String, Object> params = new HashMap<>();
        params.put("key", parseKey(aliasKeyValue));
        params.put("id", tag.getId());
        Result<Map<String, Object>> result = template.query(query, params);
        Map<String, Object> mapResult = result.singleOrNull();
        if (mapResult != null)
            logger.debug("Created alias {} for tag {}", mapResult.get("alias"), tag);
    }

    private boolean doesAliasExist(Long tagId, String label, String key) {
        String query = "match (t:_Tag )-[:HAS_ALIAS]->(alias:`" + label + "Alias" + "` {key:{key}}) where id(t) = {id} return alias";
        Map<String, Object> params = new HashMap<>();
        params.put("key", parseKey(key));
        params.put("id", tagId);
        Result<Map<String, Object>> result = template.query(query, params);
        Map<String, Object> mapResult = result.singleOrNull();
        return mapResult != null && mapResult.get("alias") != null;

    }

    public static String parseKey(String key) {
        return key.toLowerCase().replaceAll("\\s", "");
    }
}
