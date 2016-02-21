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

package org.flockdata.engine.dao;

import org.apache.commons.lang.StringUtils;
import org.flockdata.engine.schema.IndexRetryService;
import org.flockdata.helper.TagHelper;
import org.flockdata.model.Alias;
import org.flockdata.model.Company;
import org.flockdata.model.Tag;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.TagPayload;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Move to Neo4j server extension
 * <p>
 * Created by mike on 20/06/15.
 */

@Repository
public class TagWrangler {

    private Logger logger = LoggerFactory.getLogger(TagWrangler.class);

    @Autowired
    Neo4jTemplate template;

    @Autowired
    IndexRetryService indexRetryService;

    @Autowired
    TagRepo tagRepo;

    public Collection<TagResultBean> save(TagPayload payload) {
        Collection<TagResultBean> results = new ArrayList<>(payload.getTags().size());
        List<String> createdValues = new ArrayList<>();
        results.addAll(payload.getTags().stream().map(tagInputBean ->
                save(payload.getCompany(), tagInputBean, payload.getTenant(), createdValues, payload.isIgnoreRelationships())).collect(Collectors.toList()));
        return results;

    }

    @Autowired
    AliasDaoNeo aliasDao;

    // ToDo: Turn this in to ServerSide
    TagResultBean save(Company company, TagInputBean tagInput, String tagSuffix, Collection<String> cachedValues, boolean suppressRelationships) {
        // Check exists
        boolean isNew = false;
        TagResultBean tagResultBean;
        Tag startTag = findTagNode(tagSuffix, tagInput.getLabel(), tagInput.getKeyPrefix(), (tagInput.getCode() == null ? tagInput.getName() : tagInput.getCode()), false);
        if (startTag == null) {
            if (tagInput.isMustExist()) {

                tagInput.setServiceMessage("Tag [" + tagInput + "] should exist for [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
                if (tagInput.getNotFoundCode() != null && !tagInput.getNotFoundCode().equals("")) {
                    TagInputBean notFound = new TagInputBean(tagInput.getNotFoundCode())
                            .setLabel(tagInput.getLabel());

                    tagInput.setServiceMessage("Tag [" + tagInput + "] should exist as a [" + tagInput.getLabel() + "] but doesn't. Assigning to [" + tagInput.getNotFoundCode() + "]. An alias is been created for " + tagInput.getCode());
                    logger.info(tagInput.setServiceMessage());
                    ArrayList<AliasInputBean> aliases = new ArrayList<>();
                    // Creating an alias so that we don't have to process this all again. The alias will be against the undefined tag.
                    aliases.add(new AliasInputBean(tagInput.getCode()));
                    notFound.setAliases(aliases);
                    tagResultBean = save(company, notFound, tagSuffix, cachedValues, suppressRelationships);
                    startTag = tagResultBean.getTag();
                } else
                    return new TagResultBean(tagInput);
            } else {
                isNew = true;
                startTag = createTag(company, tagInput, tagSuffix);
            }
        } else {
            // Existing Tag
            if (tagInput.isMerge() && tagInput.hasTagProperties()) {
                boolean changed = false;
                for (String key : tagInput.getProperties().keySet()) {
                    startTag.addProperty(key, tagInput.getProperty(key));
                    changed = true;
                }
                if (changed)
                    startTag = template.save(startTag);
            }
        }
        String label = tagInput.getLabel();

        if (tagInput.hasAliases()) {
            Collection<Alias> aliases = new ArrayList<>();
            for (AliasInputBean newAlias : tagInput.getAliases()) {

                Alias alias = aliasDao.findAlias(label, newAlias, startTag);
                alias.setTag(startTag);
                aliases.add(alias);
            }
            if (!aliases.isEmpty())
                template.fetch(startTag.getAliases());
            for (Alias alias : aliases) {
                if (!startTag.hasAlias(label, alias.getKey())) {
                    template.saveOnly(alias);
                    startTag.addAlias(alias);
                }

            }
        }

        if (tagInput.hasTargets()) {
            Map<String, Collection<TagInputBean>> targets = tagInput.getTargets();
            for (String rlxName : targets.keySet()) {
                Collection<TagInputBean> associatedTag = targets.get(rlxName);
                for (TagInputBean tagInputBean : associatedTag) {
                    processAssociatedTags(company, tagSuffix, startTag, tagInputBean, rlxName, cachedValues, suppressRelationships);
                }

            }
        }

        return new TagResultBean(tagInput, startTag, isNew);
    }

    private Tag createTag(Company company, TagInputBean tagInput, String suffix) {
//        boolean schemaReady;
//        Collection<TagInputBean>tagInputBeans = new ArrayList<>();
//        tagInputBeans.add(tagInput);
//        do {
//            schemaReady = indexRetryService.ensureUniqueIndexes(company, tagInputBeans);
//        } while (!schemaReady);

        logger.trace("createTag {}", tagInput);
        // ToDo: Should a label be suffixed with company in multi-tenanted? - more time to think!!
        //       do we care that one company can see another companies tag value? Certainly not the
        //       track data.
        String label;
        if (tagInput.isDefault())
            label = Tag.DEFAULT_TAG + suffix;
        else {
            label = tagInput.getLabel();
        }

        resolveKeyPrefix(suffix, tagInput);

        Tag tag = new Tag(tagInput, label);

        logger.trace("Saving {}", tag);
        tag = template.save(tag);
        logger.debug("Saved {}", tag);
        return tag;

    }

    public Map<String, Collection<TagResultBean>> findAllTags(Tag sourceTag, String relationship, String targetLabel) {
        String query = "match (t) -[" + (!relationship.equals("") ? "r:" + relationship : "r") + "]-(targetTag:" + targetLabel + ") where id(t)={id}  return r, targetTag";
        Map<String, Object> params = new HashMap<>();
        params.put("id", sourceTag.getId());
        Iterable<Map<String, Object>> result = template.query(query, params);
        Map<String, Collection<TagResultBean>> tagResults = new HashMap<>();
        for (Map<String, Object> mapResult : result) {
            Tag n = template.projectTo(mapResult.get("targetTag"), Tag.class);

            String rType = ((org.neo4j.graphdb.Relationship) mapResult.get("r")).getType().name();
            Collection<TagResultBean> tagResultBeans = tagResults.get(rType);
            if (tagResultBeans == null) {
                tagResultBeans = new ArrayList<>();
                tagResults.put(rType, tagResultBeans);
            }
            tagResultBeans.add(new TagResultBean(n));

        }
        return tagResults;

    }

    public Collection<Tag> findDirectedTags(String tagSuffix, Tag startTag, Company company) {
        //Long coTags = getCompanyTagManager(companyId);
        //"MATCH track<-[tagType]-(tag:Tag"+engineAdmin.getTagSuffix(company)+") " +
        String query =
                " match (tag:Tag)-[]->(otherTag" + Tag.DEFAULT + tagSuffix + ") " +
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
            results.add(template.projectTo(row.get("otherTag"), Tag.class));
        }
        //
        return results;
    }

    private void resolveKeyPrefix(String suffix, TagInputBean tagInput) {
        String prefix = resolveKeyPrefix(tagInput.getKeyPrefix(), suffix);
        if (prefix != null)
            tagInput.setKeyPrefix(prefix);
    }

    private String resolveKeyPrefix(String keyPrefix, String suffix) {
        if (keyPrefix != null && keyPrefix.contains(":")) {
            // Label:Value to set the prefix
            // DAT-479 indirect lookup
            String[] values = StringUtils.split(keyPrefix, ":");
            if (values.length == 2) {
                Tag indirect = findTagNode(suffix, values[0], null, values[1], false);
                if (indirect == null) {
                    // ToDo: Exception or literal?
                    logger.debug("Indirect syntax was found but resolved to no tag");
                    throw new AmqpRejectAndDontRequeueException("Unable to resolve the indirect tag" + keyPrefix);
                } else {
                    return indirect.getCode();
                }

            }
        }
        return keyPrefix;
    }

    public Tag findTagNode(String suffix, String label, String keyPrefix, String tagCode, boolean inflate) {
        if (tagCode == null)
            throw new IllegalArgumentException("Null can not be used to find a tag (" + label + ")");

        String multiTennantedLabel = TagHelper.suffixLabel(label, suffix);
        String kp = resolveKeyPrefix(keyPrefix, suffix);

        Tag tag = tagByKey(multiTennantedLabel, kp, tagCode);
        if (tag != null && inflate)
            template.fetch(tag.getAliases());
        logger.trace("requested tag [{}:{}] foundTag [{}]", label, tagCode, (tag == null ? "NotFound" : tag));
        return tag;
    }

    /**
     * Attempts to find tag.key by prefix.tagcode. If that doesn't exist, then it will
     * attempt to locate the alias based on tagcode
     * <p>
     * ToDo: A version to located by user defined AliasLabel
     *
     * @param multiTennantedLabel  Type of tag to look for
     * @param keyPrefix optional prefix that the Key might have
     * @param tagCode   mandatory value of the code
     * @return resolved tag
     */
    private Tag tagByKey(String multiTennantedLabel, String keyPrefix, String tagCode) {
        if (keyPrefix != null && keyPrefix.contains(":"))
            throw new AmqpRejectAndDontRequeueException(String.format("Unresolved indirection %s %s for %s", multiTennantedLabel, tagCode, keyPrefix));
        String tagKey = TagHelper.parseKey(keyPrefix, tagCode);
        StopWatch watch = getWatch(multiTennantedLabel + " / " + tagKey);

        Collection<Tag> tags = tagRepo.findByKey(tagKey);

        if (tags.size() == 1) {
            Tag tag = tags.iterator().next();
            if (tag.getLabel().equals(multiTennantedLabel) || (multiTennantedLabel.equals(Tag.DEFAULT_TAG) || multiTennantedLabel.equals("_" + Tag.DEFAULT_TAG))) {
                stopWatch(watch, multiTennantedLabel, tagCode);
                return tag;
            }
        }

        logger.trace("{} Not found by key {}", multiTennantedLabel, tagKey);

        // See if the tagKey is unique for the requested label
        Tag tResult = null;
        for (Tag tag : tags) {
            if (tag.getLabel().equalsIgnoreCase(multiTennantedLabel)) {
                if (tResult == null) {
                    tResult = tag;
                } else {
                    // Deleting tags that should not exist here
                    template.delete(tag); // Concurrency issue under load ?
                }
            }
        }
        if (tResult != null) {
            stopWatch(watch, "byKey", multiTennantedLabel, tagCode);
            return tResult;
        }

        logger.trace("Locating by alias {}, {}", multiTennantedLabel, tagCode);

        String query;

        query = "match (:`" + multiTennantedLabel + "Alias` {key:{tagKey}})<-[HAS_ALIAS]-(a:`" + multiTennantedLabel + "`) return a";

        Map<String, Object> params = new HashMap<>();
        params.put("tagKey", TagHelper.parseKey(tagCode));
        Iterable<Map<String, Object>> result = template.query(query, params);
        Iterator<Map<String, Object>> results = result.iterator();
        Tag tagResult = null;
        while (results.hasNext()) {
            Map<String, Object> mapResult = results.next();

            if (mapResult != null && tagResult == null) {
                tagResult = getTag(mapResult);
            } else {
                Tag toDelete = getTag(mapResult);
                logger.debug("Deleting duplicate {}", toDelete);
                if (toDelete != null)
                    template.delete(toDelete);
            }

        }
        if (tagResult == null)
            logger.trace("Not found {}, {}", multiTennantedLabel, tagCode);
        else
            stopWatch(watch, "byAlias", multiTennantedLabel, tagCode);

        return tagResult;
    }

    StopWatch getWatch(String id) {
        StopWatch watch = null;

        if (logger.isDebugEnabled()) {
            watch = new StopWatch(id);
            watch.start(id);
        }
        return watch;
    }

    private void stopWatch(StopWatch watch, Object... args) {
        if (watch == null)
            return;

        watch.stop();
        logger.info(watch.prettyPrint());
    }

    private Tag getTag(Map<String, Object> mapResult) {
        Tag tagResult;
        Object o = null;
        if (mapResult.get("a") != null)
            o = mapResult.get("a");
        else if (mapResult.get("t") != null) { // Tag found by alias
            o = mapResult.get("t");
        }

        tagResult = (o == null ? null : template.projectTo(o, Tag.class));
        return tagResult;
    }

    /**
     * Create unique relationship between the tag and the node
     *
     * @param company               associate the tag with this company
     * @param tagSuffix
     * @param startTag              notional start node
     * @param associatedTag         tag to make or get
     * @param rlxName               relationship name
     * @param cachedValues          running list of values already created - performance op.
     * @param suppressRelationships @return the created tag
     */
    private void processAssociatedTags(Company company, String tagSuffix, Tag startTag, TagInputBean associatedTag, String rlxName, Collection<String> cachedValues, boolean suppressRelationships) {

        Tag endTag = save(company, associatedTag, tagSuffix, cachedValues, suppressRelationships).getTag();
        if (suppressRelationships)
            return;
        //Node endNode = template.getNode(tag.getId());

        Tag startId = (!associatedTag.isReverse() ? startTag : endTag);
        Tag endId = (!associatedTag.isReverse() ? endTag : startTag);
        String key = rlxName + ":" + startId.getId() + ":" + endId.getId();
        if (cachedValues.contains(key))
            return;

        cachedValues.add(createRelationship(rlxName, startId, endId, key));
    }


    private String createRelationship(String rlxName, Tag startTag, Tag endTag, String key) {
//        if ((template.getRelationshipBetween(startTag, endTag, rlxName) == null))
//            template.createRelationshipBetween(template.getNode(startTag.getId()), template.getNode(endTag.getId()), rlxName, null);

        Node start = template.getNode(startTag.getId());
        //start.createRelationshipTo()
        Node end = template.getNode(endTag.getId());
        Relationship r = template.getOrCreateRelationship("rlxNames", "rlx", rlxName + ":" + startTag.getId() + ":" + endTag.getId(), start, end, rlxName, null);

        return key;
    }


    public void createAlias(String suffix, Tag tag, String label, AliasInputBean aliasInput) {
        String theLabel = TagHelper.suffixLabel(label, suffix);
        template.fetch(tag);
        template.fetch(tag.getAliases());
        if (tag.hasAlias(theLabel, TagHelper.parseKey(aliasInput.getCode())))
            return;

        Alias alias = new Alias(theLabel, aliasInput, TagHelper.parseKey(aliasInput.getCode()), tag);

        alias = template.save(alias);
        logger.debug(alias.toString());

    }

    public Collection<Tag> findTags(String label) {
        Collection<Tag> tagResults = new ArrayList<>();
        // ToDo: Match to company - something like this.....
        //match (t:Law)-[:_TagLabel]-(c:FDCompany) where id(c)=0  return t,c;
        //match (t:Law)-[*..2]-(c:FDCompany) where id(c)=0  return t,c;
        String query = "match (tag:`" + label + "`) return distinct (tag) as tag";
        // Look at PAGE
        Iterable<Map<String, Object>> results = template.query(query, null);
        for (Map<String, Object> row : results) {
            Object o = row.get("tag");
            Tag t = template.projectTo(o, Tag.class);
            tagResults.add(t);

        }
        return tagResults;
    }

//    public Collection<Tag> findTags(String label, String code) {
//        Collection<Tag> tagResults = new ArrayList<>();
//        // ToDo: Match to company - something like this.....
//        //match (t:Law)-[:_TagLabel]-(c:FDCompany) where id(c)=0  return t,c;
//        //match (t:Law)-[*..2]-(c:FDCompany) where id(c)=0  return t,c;
//        String query = "match (tag:`" + label + "`) where tag.key = {key} return distinct (tag) as tag";
//        // Look at PAGE
//        Map<String,Object>params = new HashMap<>();
//        params.put("key", TagHelper.parseKey(null, code));
//        Iterable<Map<String, Object>> results = template.query(query, params);
//        for (Map<String, Object> row : results) {
//            Object o = row.get("tag");
//            Tag t = template.projectTo(o, Tag.class);
//            tagResults.add(t);
//
//        }
//        return tagResults;
//    }


}
