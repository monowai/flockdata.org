/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.schema.dao.SchemaDaoNeo4j;
import org.flockdata.engine.tag.TagRepo;
import org.flockdata.helper.FlockDataTagException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
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
    PlatformConfig engineAdmin;

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
                results.add(
                        save(company, tagInputBean, tagSuffix, createdValues, suppressRelationships)
                );
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
        //Tag start = findTag(company, (tagInput.getCode() == null ? tagInput.getName() : tagInput.getCode()), tagInput.getLabel());
        Tag startTag = findTagNode(company, (tagInput.getCode() == null ? tagInput.getName() : tagInput.getCode()), tagInput.getLabel());
        if (startTag == null) {
            if (tagInput.isMustExist()) {
                tagInput.getServiceMessage("Tag [" + tagInput + "] should exist for [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
                throw new AmqpRejectAndDontRequeueException("Tag [" + tagInput + "] should exist for [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
            } else {
                startTag = createTag(company, tagInput, tagSuffix);
            }
        }

        Map<String, Collection<TagInputBean>> targets = tagInput.getTargets();
        for (String rlxName : targets.keySet()) {
            Collection<TagInputBean> associatedTag = targets.get(rlxName);
            for (TagInputBean tagInputBean : associatedTag) {
                processAssociatedTags(company, startTag, tagInputBean, rlxName, createdValues, suppressRelationships);
            }

        }

        return startTag;
    }

    private Tag createTag(Company company, TagInputBean tagInput, String suffix) {

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

        logger.trace("Saving {}", tag);
        Collection<AliasNode> aliases = null;
        if (tagInput.hasAliases()) {
            aliases = new ArrayList<>();
            for (AliasInputBean newAlias : tagInput.getAliases()) {
                AliasNode alias = new AliasNode(label, newAlias, parseKey(newAlias.getCode()), tag);
                aliases.add(alias);
            }
        }
        tag = tagRepo.save(tag);
        if (aliases != null)
            for (AliasNode alias : aliases) {
                if (!tag.hasAlias(label, alias.getKey())) {
                    alias = template.save(alias);
                    tag.addAlias(alias);
                }

            }
        logger.debug("Saved {}", tag);
        return tag;

    }

    public void createAlias(Company company, Tag tag, String label, AliasInputBean aliasInput) {
        String theLabel = resolveLabel(label, engineAdmin.getTagSuffix(company));
        template.fetch(tag);
        template.fetch(tag.getAliases());
        if (tag.hasAlias(theLabel, parseKey(aliasInput.getCode())))
            return;

        AliasNode alias = new AliasNode(theLabel, aliasInput, parseKey(aliasInput.getCode()), tag);

        alias = template.save(alias);
        logger.debug(alias.toString());
    }

    /**
     * Create unique relationship between the tag and the node
     *
     * @param company               associate the tag with this company
     * @param startTag              notional start node
     * @param associatedTag         tag to make or get
     * @param rlxName               relationship name
     * @param createdValues         running list of values already created - performance op.
     * @param suppressRelationships
     * @return the created tag
     */
    void processAssociatedTags(Company company, Tag startTag, TagInputBean associatedTag, String rlxName, Collection<String> createdValues, boolean suppressRelationships) {
        // Careful - this is recursive
        // ToDo - idea = create all tagInputs first then just create the relationships
        Tag endTag = save(company, associatedTag, createdValues, suppressRelationships);
        if (suppressRelationships)
            return;
        //Node endNode = template.getNode(tag.getId());

        Tag startId = (!associatedTag.isReverse() ? startTag : endTag);
        Tag endId = (!associatedTag.isReverse() ? endTag : startTag);
        String key = rlxName + ":" + startId.getId() + ":" + endId.getId();
        if (createdValues.contains(key))
            return;

        createRelationship(rlxName, createdValues, startId, endId, key);
    }

    private void createRelationship(String rlxName, Collection<String> createdValues, Tag startTag, Tag endTag, String key) {
        if ((template.getRelationshipBetween(startTag, endTag, rlxName) == null))
            template.createRelationshipBetween(template.getNode(startTag.getId()), template.getNode(endTag.getId()), rlxName, null);

        createdValues.add(key);
    }

    public Collection<Tag> findDirectedTags(Tag startTag, Company company, boolean b) {
        //Long coTags = getCompanyTagManager(companyId);
        //"MATCH track<-[tagType]-(tag:Tag"+engineAdmin.getTagSuffix(company)+") " +
        String query =
                " match (tag:Tag)-->(otherTag" + Tag.DEFAULT + engineAdmin.getTagSuffix(company) + ") " +
                        "   where id(tag)={tagId} return otherTag";
        Map<String, Object> params = new HashMap<>();
        params.put("tagId", startTag.getId());

        Iterable<Map<String, Object>> result = template.query(query, params);

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
        String query = "match (tag:`" + label + "`) return distinct (tag) as tag";
        // Look at PAGE
        Iterable<Map<String, Object>> results = template.query(query, null);
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
    //@Cacheable(value = "companyTag", unless = "#result == null")
    public Tag findTag(Company company, String tagCode, String label) {
        Tag tag = findTagNode(company, tagCode, label);

        if (tag == null) {
            logger.debug("findTag notFound {}, {}", tagCode, label);
        }

        return tag;

    }

    @Autowired
    TagRepo tagRepo;

    @Cacheable(value = "companyTag", unless = "#result == null")
    Tag tagByKey(String theLabel, String tagCode) {

        String query;
        logger.debug("Cache miss, {}:{}", theLabel, tagCode);
        //optional match ( c:Country {key:"zm"}) with c optional match (a:CountryAlias {key:"zambia"})<-[HAS_ALIAS]-(t:_Tag) return c,t;
        query = "optional match (t:`" + theLabel + "` {key:{tagKey}}) with t optional match (:`" + theLabel + "Alias` {key:{tagKey}})<-[HAS_ALIAS]-(a:`" + theLabel + "`) return t, a";

        Map<String, Object> params = new HashMap<>();
        params.put("tagKey", parseKey(tagCode));
        Iterable<Map<String, Object>> result = template.query(query, params);
        Iterator<Map<String, Object>> results = result.iterator();
        Tag tagResult = null;
        while (results.hasNext()) {
            Map<String, Object> mapResult = results.next();

            if (mapResult != null && tagResult == null) {
                tagResult = getTag(mapResult);
            } else {
                Tag toDelete = getTag(mapResult);
                if (toDelete != null)
                    template.delete(toDelete);
                //logger.info("Should we delete {}", toDelete);

            }

        }
        return tagResult;
    }

    private Tag getTag(Map<String, Object> mapResult) {
        Tag tagResult;
        Object o = null;
        if (mapResult.get("t") != null)
            o = mapResult.get("t");
        else if (mapResult.get("a") != null) { // Tag found by alias
            o = mapResult.get("a");
        }

        tagResult = (o == null ? null : template.projectTo(o, TagNode.class));
        return tagResult;
    }

    public Tag findTagNode(Company company, String tagCode, String label) {
        if (tagCode == null || company == null)
            throw new IllegalArgumentException("Null can not be used to find a tag (" + label + ")");

        String theLabel = resolveLabel(label, engineAdmin.getTagSuffix(company));

        Tag tag = tagByKey(theLabel, parseKey(tagCode));
        logger.debug("requested tag [{}:{}] foundTag [{}]", label, tagCode, (tag == null ? "NotFound" : tag.getId()));
        return tag;
    }

    public void purgeUnusedConcepts(Company company) {

        String query = "match (tag" + Tag.DEFAULT + ") delete tag";
        template.query(query, null);

        query = "match (tag:Tag)-[r:TAG_INDEX]-(c:FDCompany) where id(c)={company} delete r, tag";
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

    public static String parseKey(String key) {
        return key.toLowerCase().replaceAll("\\s", "");
    }

    public Collection<AliasInputBean> findTagAliases(Company company, String theLabel, String sourceTag) throws NotFoundException {
        Tag source = findTag(company, sourceTag, theLabel);
        if (source == null)
            throw new NotFoundException("Unable to find the requested tag " + sourceTag);
        theLabel = resolveLabel(theLabel, engineAdmin.getTagSuffix(company));
        String query = "match (t) -[:HAS_ALIAS]->(alias) where id(t)={id}  return alias";
        Map<String, Object> params = new HashMap<>();
        params.put("id", source.getId());
        Iterable<Map<String, Object>> result = template.query(query, params);
        Collection<AliasInputBean> aliasResults = new ArrayList<>();
        for (Map<String, Object> mapResult : result) {
            AliasNode n = template.projectTo(mapResult.get("alias"), AliasNode.class);
            aliasResults.add(new AliasInputBean(n.getName()));

        }
        return aliasResults;
    }
}
