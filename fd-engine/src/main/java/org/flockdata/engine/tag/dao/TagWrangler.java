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

package org.flockdata.engine.tag.dao;

import org.flockdata.engine.tag.model.AliasNode;
import org.flockdata.engine.tag.model.TagNode;
import org.flockdata.helper.NotFoundException;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.bean.TagResultBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.TagPayload;
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
 *
 * Created by mike on 20/06/15.
 */

@Repository
public class TagWrangler {

    private Logger logger = LoggerFactory.getLogger(TagWrangler.class);

    @Autowired
    Neo4jTemplate template;

    @Autowired
    TagRepo tagRepo;

    public Collection<TagResultBean>save(TagPayload payload){
        Collection<TagResultBean>results = new ArrayList<>(payload.getTags().size());
        List<String> createdValues = new ArrayList<>();
        results.addAll(payload.getTags().stream().map(tagInputBean -> new TagResultBean(
                save(payload.getCompany(), tagInputBean, payload.getSuffix(), createdValues, payload.isIgnoreRelationships()))).collect(Collectors.toList()));
        return results;

    }

    // ToDo: Turn this in to ServerSide
    Tag save(Company company, TagInputBean tagInput, String tagSuffix, Collection<String> createdValues, boolean suppressRelationships) {
        // Check exists
        Tag startTag = findTagNode(tagSuffix, tagInput.getLabel(), (tagInput.getCode() == null ? tagInput.getName() : tagInput.getCode()), false);
        if (startTag == null) {
            if (tagInput.isMustExist()) {

                tagInput.getServiceMessage("Tag [" + tagInput + "] should exist for [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
                if (tagInput.getNotFoundCode() !=null && !tagInput.getNotFoundCode().equals("")){
                    TagInputBean notFound = new TagInputBean(tagInput.getNotFoundCode())
                            .setLabel(tagInput.getLabel());

                    tagInput.getServiceMessage("Tag [" + tagInput + "] should exist as a [" + tagInput.getLabel() + "] but doesn't. Assigning to [" + tagInput.getNotFoundCode() + "]. An alias is been created for " +tagInput.getCode());
                    logger.info(tagInput.getServiceMessage());
                    ArrayList<AliasInputBean>aliases = new ArrayList<>();
                    // Creating an alias so that we don't have to process this all again. The alias will be against the undefined tag.
                    aliases.add( new AliasInputBean(tagInput.getCode()));
                    notFound.setAliases(aliases);
                    startTag = save(company, notFound, tagSuffix, createdValues, suppressRelationships);
                } else
                    throw new AmqpRejectAndDontRequeueException("Tag [" + tagInput + "] should exist as a [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
            } else {
                startTag = createTag(tagInput, tagSuffix);
            }
        }

        Map<String, Collection<TagInputBean>> targets = tagInput.getTargets();
        for (String rlxName : targets.keySet()) {
            Collection<TagInputBean> associatedTag = targets.get(rlxName);
            for (TagInputBean tagInputBean : associatedTag) {
                processAssociatedTags(company, tagSuffix, startTag, tagInputBean, rlxName, createdValues, suppressRelationships);
            }

        }

        return startTag;
    }

    private Tag createTag(TagInputBean tagInput, String suffix) {

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
        TagNode tag = new TagNode(tagInput, label);

        logger.trace("Saving {}", tag);
        tag = template.save(tag);
        Collection<AliasNode> aliases = null;
        if (tagInput.hasAliases()) {
            aliases = new ArrayList<>();
            for (AliasInputBean newAlias : tagInput.getAliases()) {
                AliasNode alias = new AliasNode(label, newAlias, TagHelper.parseKey(newAlias.getCode()), tag);
                alias.setTag(tag);
                aliases.add(alias);
            }
        }
        if (aliases != null)
            for (AliasNode alias : aliases) {
                if (!tag.hasAlias(label, alias.getKey())) {
                    template.saveOnly(alias);
                    tag.addAlias(alias);
                }

            }
        logger.debug("Saved {}", tag);
        return tag;

    }

    private Tag save(Company company, String tagSuffix, TagInputBean tagInput, Collection<String> createdValues, boolean suppressRelationships) {

        return save(company, tagInput, tagSuffix, createdValues, suppressRelationships);
    }

    public Map<String, Collection<TagResultBean>> findAllTags(Tag sourceTag, String relationship, String targetLabel) {
        String query = "match (t) -["+ (!relationship.equals("")? "r:"+relationship :"r")+"]-(targetTag:"+targetLabel+") where id(t)={id}  return r, targetTag";
        Map<String, Object> params = new HashMap<>();
        params.put("id", sourceTag.getId());
        Iterable<Map<String, Object>> result = template.query(query, params);
        Map<String,Collection<TagResultBean>> tagResults = new HashMap<>();
        for (Map<String, Object> mapResult : result) {
            TagNode n = template.projectTo(mapResult.get("targetTag"), TagNode.class);

            String rType= ((org.neo4j.graphdb.Relationship)mapResult.get("r")).getType().name();
            Collection<TagResultBean>tagResultBeans = tagResults.get(rType);
            if ( tagResultBeans == null ){
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
            results.add(template.projectTo(row.get("otherTag"), TagNode.class));
        }
        //
        return results;
    }

    public Tag findTagNode(String suffix, String label, String tagCode, boolean inflate) {
        if (tagCode == null )
            throw new IllegalArgumentException("Null can not be used to find a tag (" + label + ")");

        String theLabel = TagHelper.resolveLabel(label, suffix);

        Tag tag = tagByKey(theLabel, TagHelper.parseKey(tagCode));
        if ( tag!=null && inflate)
            template.fetch(tag.getAliases());
        logger.trace("requested tag [{}:{}] foundTag [{}]", label, tagCode, (tag == null ? "NotFound" : tag));
        return tag;
    }

    private Tag tagByKey(String theLabel, String tagKey) {
        StopWatch watch =getWatch(theLabel + " / " + tagKey);

        Collection<TagNode>tags = tagRepo.findByKey(tagKey);

        if ( tags.size() ==1 ){
            Tag tag = tags.iterator().next();
            if ( tag.getLabel().equals(theLabel) ||(theLabel.equals(Tag.DEFAULT_TAG) || theLabel.equals("_"+Tag.DEFAULT_TAG))) {
                stopWatch(watch, theLabel, tagKey);
                return tag;
            }
        }

        logger.trace("{} Not found by key {}", theLabel, tagKey);

        // See if the tagKey is unique for the requested label
        Tag tResult = null;
        for (Tag tag : tags) {
            if (tag.getLabel().equalsIgnoreCase(theLabel) ) {
                if ( tResult == null) {
                    tResult = tag;
                } else {
                    // Deleting tags that should not exist here
                    template.delete(tag); // Concurrency issue under load ?
                }
            }
        }
        if ( tResult != null ) {
            stopWatch(watch, "byKey", theLabel, tagKey);
            return tResult;
        }

        logger.trace("Locating by alias {}, {}", theLabel, tagKey);

        String query;

        query = "match (:`" + theLabel + "Alias` {key:{tagKey}})<-[HAS_ALIAS]-(a:`" + theLabel + "`) return a";

        Map<String, Object> params = new HashMap<>();
        params.put("tagKey", tagKey);
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
        if ( tagResult == null )
            logger.trace("Not found {}, {}", theLabel, tagKey);
        else
            stopWatch(watch, "byAlias", theLabel, tagKey);

        return tagResult;
    }

    StopWatch getWatch(String id){
        StopWatch watch = null;

        if (logger.isDebugEnabled()) {
            watch = new StopWatch(id);
            watch.start(id);
        }
        return watch;
    }

    private void stopWatch(StopWatch watch, Object... args){
        if ( watch == null )
            return;

        watch.stop();
        logger.info( watch.prettyPrint());
    }

    private Tag getTag(Map<String, Object> mapResult) {
        Tag tagResult;
        Object o = null;
        if (mapResult.get("a") != null)
            o = mapResult.get("a");
        else if (mapResult.get("t") != null) { // Tag found by alias
            o = mapResult.get("t");
        }

        tagResult = (o == null ? null : template.projectTo(o, TagNode.class));
        return tagResult;
    }

    /**
     * Create unique relationship between the tag and the node
     *
     * @param company               associate the tag with this company
     * @param tagSuffix
     *@param startTag              notional start node
     * @param associatedTag         tag to make or get
     * @param rlxName               relationship name
     * @param createdValues         running list of values already created - performance op.
     * @param suppressRelationships      @return the created tag
     */
    private void processAssociatedTags(Company company, String tagSuffix, Tag startTag, TagInputBean associatedTag, String rlxName, Collection<String> createdValues, boolean suppressRelationships) {

        Tag endTag = save(company, tagSuffix, associatedTag, createdValues, suppressRelationships);
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


    public void createAlias(String suffix, Tag tag, String label, AliasInputBean aliasInput) {
        String theLabel = TagHelper.resolveLabel(label, suffix);
        template.fetch(tag);
        template.fetch(tag.getAliases());
        if (tag.hasAlias(theLabel, TagHelper.parseKey(aliasInput.getCode())))
            return;

        AliasNode alias = new AliasNode(theLabel, aliasInput, TagHelper.parseKey(aliasInput.getCode()), tag);

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
            Tag t = template.projectTo(o, TagNode.class);
            tagResults.add(t);

        }
        return tagResults;
    }

    public Collection<AliasInputBean> findTagAliases(Tag sourceTag) throws NotFoundException {

        String query = "match (t) -[:HAS_ALIAS]->(alias) where id(t)={id}  return alias";
        Map<String, Object> params = new HashMap<>();
        params.put("id", sourceTag.getId());
        Iterable<Map<String, Object>> result = template.query(query, params);
        Collection<AliasInputBean> aliasResults = new ArrayList<>();
        for (Map<String, Object> mapResult : result) {
            AliasNode n = template.projectTo(mapResult.get("alias"), AliasNode.class);
            aliasResults.add(new AliasInputBean(n.getName()));

        }
        return aliasResults;
    }


}
