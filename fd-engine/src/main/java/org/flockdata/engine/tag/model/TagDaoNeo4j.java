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

import org.flockdata.engine.FdEngineConfig;
import org.flockdata.engine.schema.dao.SchemaDaoNeo4j;
import org.flockdata.helper.FlockDataTagException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
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
public class TagDaoNeo4j {

    @Autowired
    SchemaDaoNeo4j schemaDao;

    @Autowired
    Neo4jTemplate template;

    @Autowired
    FdEngineConfig engineAdmin;

    private Logger logger = LoggerFactory.getLogger(TagDaoNeo4j.class);

    public Tag save(Company company, TagInputBean tagInput) {
        String tagSuffix = engineAdmin.getTagSuffix(company);
        List<String> createdValues = new ArrayList<>();
        return convertNodeToTag(
                save(company, tagInput, tagSuffix, createdValues, false));
    }

    Tag save(Company company, TagInputBean tagInput, Collection<String> createdValues, boolean suppressRelationships) {
        String tagSuffix = engineAdmin.getTagSuffix(company);
        return convertNodeToTag(save(company, tagInput, tagSuffix, createdValues, suppressRelationships));
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
                results.add(
                        convertNodeToTag(save(company, tagInputBean, tagSuffix, createdValues, suppressRelationships))
                );
            } catch (FlockDataTagException te) {
                logger.error("Tag Exception [{}]", te.getMessage());
                tagInputBean.getServiceMessage(te.getMessage());
                errorResults.add(tagInputBean);
            }

        }
        return results;
    }

    private Tag convertNodeToTag(Node node) {
        return template.projectTo(node, TagNode.class);
    }

    Node save(Company company, TagInputBean tagInput, String tagSuffix, Collection<String> createdValues, boolean suppressRelationships) {
        // Check exists
        //Tag start = findTag(company, (tagInput.getCode() == null ? tagInput.getName() : tagInput.getCode()), tagInput.getLabel());
        Node startNode = findTagNode(company, (tagInput.getCode() == null ? tagInput.getName() : tagInput.getCode()), tagInput.getLabel());
        if (startNode == null) {
            if (tagInput.isMustExist()) {
                tagInput.getServiceMessage("Tag [" + tagInput + "] should exist for [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
                throw new AmqpRejectAndDontRequeueException("Tag [" + tagInput + "] should exist for [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
            } else {
                startNode = createTag(company, tagInput, tagSuffix);
            }
        }

        Map<String, Collection<TagInputBean>> targets = tagInput.getTargets();
        for (String rlxName : targets.keySet()) {
            Collection<TagInputBean> associatedTag = targets.get(rlxName);
            for (TagInputBean tagInputBean : associatedTag) {
                processAssociatedTags(company, startNode, tagInputBean, rlxName, createdValues, suppressRelationships);
            }

        }

        return startNode;
    }

    private Node createTag(Company company, TagInputBean tagInput, String suffix) {

        logger.trace("createTag {}", tagInput);
        // ToDo: Should a label be suffixed with company in multi-tenanted? - more time to think!!
        //       do we care that one company can see another companies tag value? Certainly not the
        //       track data.
        String label;
        if (tagInput.isDefault())
            label = Tag.DEFAULT_TAG + suffix;
        else {
            schemaDao.registerTag(company, tagInput.getLabel());
            label = tagInput.getLabel();
        }
        TagNode tag = new TagNode(tagInput, label);

        try {
            logger.trace("Saving {}", tag);
            tag = template.save(tag);
            if (tagInput.hasAliases()) {
                makeAliases(company, tag, label, tagInput.getAliases());
            }
            logger.debug("Saved {}", tag);
            return template.getNode(tag.getId());
        } catch (ConstraintViolationException e) {
            logger.debug("Error saving {}", tag);
            throw e;
        }

    }

    private void makeAliases(Company company, TagNode tag, String label, Collection<AliasInputBean> aliases) {
        Collection<AliasInputBean> newAliases = findAliasesToCreate(label, tag, company, aliases);
        //schemaDao.createAliasIndex(label);
        //}
        for (AliasInputBean alias : newAliases) {
            createAlias(company, tag, label, alias);
        }

    }

    private Collection<AliasInputBean> findAliasesToCreate(String label, Tag tag, Company company, Collection<AliasInputBean> aliases) {
        Collection<AliasInputBean> newAliases = new ArrayList<>();
        String suffix = engineAdmin.getTagSuffix(company);
        for (AliasInputBean alias : aliases) {
            String theLabel = resolveLabel(label, suffix);

            // ToDo: Figure out why the makeAlias cypher errors. Until that works we have to check for the existence of the tag
            if (!doesAliasExist(tag.getId(), theLabel, alias.getCode()))
                newAliases.add(alias);

        }
        return newAliases;

    }

    public void createAlias(Company company, Tag tag, String label, AliasInputBean aliasInput) {
        String theLabel = resolveLabel(label, engineAdmin.getTagSuffix(company));

        // ToDo: Figure out why the makeAlias cypher errors. Until that works we have to check for the existence of the tag
        if (doesAliasExist(tag.getId(), theLabel, aliasInput.getCode()))
            return;

        makeAlias(tag, theLabel, aliasInput);
    }

    void makeAlias(Tag tag, String theLabel, AliasInputBean aliasInput) {
        // match (c:Country) where c.code="NZ" create (ac:CountryAlias {name:"New Zealand", code:"New Zealand"}) , (c)-[:HAS_ALIAS]->(ac) return c, ac

        // This query will find the Tag, if it exists, or any alias that might exist
        // optional match (c:Country {code:"New Zealand"})
        // optional match (a:CountryAlias {code:"New Zealand"})
        // with c,a optional match (tag)-[:HAS_ALIAS]->(a)
        // return c, a,tag

        String query = "match (t:" + theLabel + ") where id(t)={id} with t " +
                "create unique (alias:`" + theLabel + "Alias" + "` {key:{key}, code:{code}, description:{description}}) <-[:HAS_ALIAS]-(t) ";
        Map<String, Object> params = new HashMap<>();
        params.put("key", parseKey(aliasInput.getCode()));
        params.put("code", aliasInput.getCode());
        params.put("description", aliasInput.getDescription());
        params.put("id", tag.getId());
        template.query(query, params);

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
    void processAssociatedTags(Company company, Node startNode, TagInputBean associatedTag, String rlxName, Collection<String> createdValues, boolean suppressRelationships) {
        // Careful - this is recursive
        // ToDo - idea = create all tagInputs first then just create the relationships
        Tag tag = save(company, associatedTag, createdValues, suppressRelationships);
        if (suppressRelationships)
            return;
        Node endNode = template.getNode(tag.getId());

        Node startId = (!associatedTag.isReverse() ? startNode : endNode);
        Node endId = (!associatedTag.isReverse() ? endNode : startNode);
        String key = rlxName + ":" + startId + ":" + endId;
        if (createdValues.contains(key))
            return;

        createRelationship(rlxName, createdValues, startId, endId, key);
    }

    private void createRelationship(String rlxName, Collection<String> createdValues, Node startId, Node endId, String key) {
        if ((template.getRelationshipBetween(startId, endId, rlxName) == null))
            template.createRelationshipBetween(startId, endId, rlxName, null);

        createdValues.add(key);
//        String cypher = "match startNode, endNode where " +
//                "id(startNode)={start} " +
//                "and id(endNode)={end} " +
//                "create unique (startNode) -[r:`" + rlxName + "`]->(endNode) return r";
//        Map<String, Object> params = new HashMap<>();
//        params.put("start", startId.getId());
//        params.put("end", endId.getId());
//        //params.put("timestamp", System.currentTimeMillis());
//        template.query(cypher, params);
//        createdValues.add(key);
    }

    public Collection<Tag> findDirectedTags(Tag startTag, Company company, boolean b) {
        //Long coTags = getCompanyTagManager(companyId);
        //"MATCH track<-[tagType]-(tag:Tag"+engineAdmin.getTagSuffix(company)+") " +
        String query =
                " match (tag:Tag)-->(otherTag" + Tag.DEFAULT + engineAdmin.getTagSuffix(company) + ") " +
                "   where id(tag)={tagId} return otherTag";
        Map<String, Object> params = new HashMap<>();
        params.put("tagId", startTag.getId());

        Result<Map<String, Object>> result = template.query(query, params);

        if (!((Result) result).iterator().hasNext())
            return new ArrayList<>();

        Iterator<Map<String, Object>> rows = result.iterator();

        Collection<Tag> results = new ArrayList<>();

        while (rows.hasNext()) {
            Map<String, Object> row = rows.next();
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
        //match (t:Law)-[:_TagLabel]-(c:FDCompany) where id(c)=0  return t,c;
        //match (t:Law)-[*..2]-(c:FDCompany) where id(c)=0  return t,c;
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
    @Cacheable(value = "companyTag", unless = "#result == null")
    public Tag findTag(Company company, String tagCode, String label) {
        Node n = findTagNode(company, tagCode, label);

        if (n == null) {
            logger.debug("findTag notFound {}, {}", tagCode, label);
            return null;
        }

        return convertNodeToTag(n);

    }

    Node tagByKey(String tagCode, String theLabel) {

        logger.debug("tagByKey request [{}]:[{}]", theLabel, tagCode);
        String query;
        logger.trace("findTag code [{}] label [{}]", tagCode, theLabel);
        //optional match ( c:Country {key:"zm"}) with c optional match (a:CountryAlias {key:"zambia"})<-[HAS_ALIAS]-(t:_Tag) return c,t;
        query = "optional match (t:`" + theLabel + "` {key:{tagKey}}) with t optional match (:`"+theLabel+"Alias` {key:{tagKey}})<-[HAS_ALIAS]-(a:`"+theLabel+"`) return t, a";

        Map<String, Object> params = new HashMap<>();
        params.put("tagKey", parseKey(tagCode));
        Result<Map<String, Object>> result = template.query(query, params);
        Iterator<Map<String, Object>> results = result.iterator();
        Node node = null;
        while (results.hasNext()) {
            Map<String, Object> mapResult = results.next();

            if (mapResult != null) {
                if (mapResult.get("t") != null)
                    node = (Node) mapResult.get("t");
                else if (mapResult.get("a")!=null )
                    node = (Node) mapResult.get("a");


                if (node == null) {
                    logger.debug("findTag notFound {}, {}", tagCode, theLabel);
                    return null;
                }
                return node;
            }
        }
        return null;
    }

    @Cacheable(value = "companyTag", unless = "#result == null")
    public Node findTagNode(Company company, String tagCode, String label) {
        if (tagCode == null || company == null)
            throw new IllegalArgumentException("Null can not be used to find a tag (" + label + ")");

        logger.debug("findTag request [{}]:[{}]", label, tagCode);
        String theLabel = resolveLabel(label, engineAdmin.getTagSuffix(company));

        String tagKey = parseKey(tagCode);
        return tagByKey(tagKey, theLabel);
    }

    public void purgeUnusedConcepts(Company company) {

        String query = "match (tag" + Tag.DEFAULT + ") delete tag";
        template.query(query, null);

        query = "match (tag:_Tag)-[r:TAG_INDEX]-(c:_FDCompany) where id(c)={company} delete r, tag";
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
        if (label.startsWith(":"))
            label = label.substring(1);

        if ("".equals(tagSuffix))
            return label;
        return label + tagSuffix;
    }


    private boolean doesAliasExist(Long tagId, String label, String key) {
        String query = "match (t:" + label + " )-[:HAS_ALIAS]->(alias:`" + label + "Alias" + "` {key:{key}}) where id(t) = {id} return alias";
        Map<String, Object> params = new HashMap<>();
        params.put("key", parseKey(key));
        params.put("id", tagId);
        Result<Map<String, Object>> result = template.query(query, params);
        Map<String, Object> mapResult = result.singleOrNull();
        return mapResult != null;

    }

    public static String parseKey(String key) {
        return key.toLowerCase().replaceAll("\\s", "");
    }

    public Collection<AliasInputBean> findTagAliases(Company company, String theLabel, String sourceTag) throws NotFoundException {
        Tag source = findTag(company, sourceTag, theLabel);
        if (source == null)
            throw new NotFoundException("Unable to find the requested tag " + sourceTag);
        theLabel = resolveLabel(theLabel, engineAdmin.getTagSuffix(company));
        String query = "match (t:" + theLabel + " ) -[:HAS_ALIAS]->(alias) where id(t)={id}  return alias";
        Map<String, Object> params = new HashMap<>();
        params.put("id", source.getId());
        Result<Map<String, Object>> result = template.query(query, params);
        Collection<AliasInputBean> aliasResults = new ArrayList<>();
        for (Map<String, Object> mapResult : result) {
            Node n = (Node) mapResult.get("alias");
            AliasInputBean alias = new AliasInputBean(n.getProperty("code").toString());
            if (n.hasProperty("description")) {
                alias.setDescription(n.getProperty("description").toString());
            }
            aliasResults.add(alias);

        }
        return aliasResults;
    }
}
