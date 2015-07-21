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

package org.flockdata.neo4j.service;

import org.flockdata.helper.TagHelper;
import org.flockdata.model.Tag;
import org.flockdata.neo4j.helper.CypherUtils;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.AliasPayload;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.AsyncResult;

import javax.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by mike on 12/07/15.
 */
public class TagService {

    private Logger logger = LoggerFactory.getLogger(TagService.class);

    private GraphDatabaseService database;

    TagService () {}

    public TagService(@Context GraphDatabaseService database) {
        this();
        this.database = database;
    }


    public Node findTagNode(String tenant, String label, String tagCode) {

        if (tagCode == null)
            throw new IllegalArgumentException("Null can not be used to find a tag (" + label + ")");


        String theLabel = TagHelper.suffixLabel(label, tenant);

        Node tag = tagByKey(theLabel, TagHelper.parseKey(tagCode));
        //if ( tag!=null && inflate)
        logger.trace("requested tag [{}:{}] foundTag [{}]", label, tagCode, (tag == null ? "NotFound" : tag));
        return tag;
    }

    Node tagByKey(String tenantedLabel, String tagKey) {

        ResourceIterator<Node> tags = database.findNodes(DynamicLabel.label(tenantedLabel), "key", tagKey);//tagRepo.findByKey(tagKey);
        Node result = null;
        int count = 0;
        while (tags.hasNext()) {
            if (count == 0) {
                result = tags.next();
                count++;
            }
        }
        tags.close();

        if (result != null)
            return result;

        logger.trace("{} Not found by key, looking by label {}", tenantedLabel, tagKey);

        String query = "match (:`" + tenantedLabel + "Alias` {key:{tagKey}})<-[HAS_ALIAS]-(tag:`" + tenantedLabel + "`) return tag";

        Map<String, Object> params = new HashMap<>();
        params.put("tagKey", tagKey);
        Result results = database.execute(query, params);

        Node tagResult = null;
        while (results.hasNext()) {
            if (tagResult == null)
                tagResult = (Node) results.next().get("tag");

        }
        if (tagResult == null)
            logger.trace("Not found {}, {}", tenantedLabel, tagKey);
        else
            logger.trace("byAlias {}, {}", tenantedLabel, tagKey);

        return tagResult;
    }

    public Node save(TagInputBean tagInput, String tenant, Collection<String> createdValues) {
        // Check exists
        Node startTag = findTagNode(tenant, tagInput.getLabel(), (tagInput.getCode() == null ? tagInput.getName() : tagInput.getCode()));
        if (startTag == null) {
            if (tagInput.isMustExist()) {
                if (tagInput.hasNotFoundCode() ) {
                    TagInputBean notFound = new TagInputBean(tagInput.getNotFoundCode())
                            .setLabel(tagInput.getLabel());

                    tagInput.getServiceMessage("Tag [" + tagInput + "] should exist as a [" + tagInput.getLabel() + "] but doesn't. Assigning to [" + tagInput.getNotFoundCode() + "]. An alias is been created for " + tagInput.getCode());
                    logger.info(tagInput.getServiceMessage());
                    ArrayList<AliasInputBean> aliases = new ArrayList<>();
                    // Creating an alias so that we don't have to process this all again. The alias will be against the undefined tag.
                    aliases.add(new AliasInputBean(tagInput.getCode()));
                    notFound.setAliases(aliases);
                    startTag = save(notFound, tenant, createdValues);
                } else {
                    tagInput.getServiceMessage("Tag [" + tagInput + "] should exist for [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
                    return null;
                }
                // ToDo: Figure out exception handling and propagation over SI
                //throw new FlockDataTagException("Tag [" + tagInput + "] should exist as a [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
            } else {
                startTag = createTag(tagInput, tenant);
            }
        }

        Map<String, Collection<TagInputBean>> targets = tagInput.getTargets();
        for (String rlxName : targets.keySet()) {
            Collection<TagInputBean> associatedTag = targets.get(rlxName);
            for (TagInputBean tagInputBean : associatedTag) {
                processAssociatedTags(tenant, startTag, tagInputBean, rlxName, createdValues);
            }

        }

        return startTag;
    }

    private void processAssociatedTags(String tagSuffix, Node startTag, TagInputBean associatedTag, String rlxName, Collection<String> createdValues) {

        Node endTag = save(associatedTag, tagSuffix, createdValues);

        Node startNode = (!associatedTag.isReverse() ? startTag : endTag);
        Node endNode = (!associatedTag.isReverse() ? endTag : startTag);
        String key = rlxName + ":" + startNode.getId() + ":" + endNode.getId();
        if (createdValues.contains(key))
            return;

        createRelationship(rlxName, createdValues, startNode, endNode, key);
    }


    private void createRelationship(String rlxName, Collection<String> createdValues, Node startNode, Node endNode, String key) {

        String rName;
        if (rlxName.contains(" "))
            rName = "`" + rlxName + "`";
        else
            rName = rlxName;

        String query = "match (start:Tag), (end:Tag) where id(start)={startId} and id(end)={endId}" +
                "create unique (start)-[r:" + rName + "]->(end) return r";

        Map<String, Object> args = new HashMap<>();
        args.put("startId", startNode.getId());
        args.put("endId", endNode.getId());
        Result r = database.execute(query, args);
        r.next();
        createdValues.add(key);
    }

    public Node createTag(TagInputBean tagInput, String tenant) {

        logger.trace("createTag {}", tagInput);

        String label;
        String uniqueLabel;
        if (tagInput.isDefault()) {
            label = Tag.DEFAULT_TAG + tenant;
            //uniqueLabel = label;
        } else {
            uniqueLabel = "`"+tagInput.getLabel() + tenant +"`";
            label = uniqueLabel + Tag.DEFAULT;
        }

        logger.trace("Saving {}", tagInput);
        String query = " merge (tag:" + label + " {code:{code}, " + (tagInput.getName() == null ? "" : "name:{name},") + " key:{key} ";
        String closeQuery = " }) return tag ";
        Map<String, Object> args = new HashMap<>();
        args.put("code", tagInput.getCode());
        args.put("name", tagInput.getName());
        args.put("key", TagHelper.parseKey(tagInput.getCode()));

        // Set user defined properties
        query = CypherUtils.buildAndSetProperties(query, tagInput.getProperties(), args);

        query = query + closeQuery;

        Result result = database.execute(query, args);
        Node tag = (Node) result.next().get("tag");

        if (tagInput.hasAliases()) {
            for (AliasInputBean newAlias : tagInput.getAliases()) {
                makeAlias(tagInput.getLabel() + "Alias", tag.getId(), newAlias);

            }
        }

        return tag;

    }

    /**
     *
     * @param aliasLabel unquoted tag label. Alias will be suffixed
     * @param tagId      pk of the tag
     * @param aliasInput
     * @return
     */
    public AliasPayload makeAlias(String aliasLabel, Long tagId, AliasInputBean aliasInput) {
        String aliasQuery = "match (tag:Tag) where id(tag)={tagId}" +
                "create unique (tag)-[:HAS_ALIAS]->(alias:Alias:`"
                + aliasLabel +
                "` {key:{key}, name:{name}, description:{description}}) " +
                " return alias";

        HashMap<String, Object> args = new HashMap<>();
        args.put("tagId", tagId);
        args.put("key", TagHelper.parseKey(aliasInput.getCode()));
        args.put("name", aliasInput.getCode());
        args.put("description", aliasInput.getDescription()== null ?"na":aliasInput.getDescription());
        Result results = database.execute(aliasQuery, args);

        return new AliasPayload(aliasLabel, null, aliasInput);
    }

    public Future<Boolean> ensureConstraints(Collection<TagInputBean> tagPayload) {

        Transaction tx = database.beginTx();
        try {
            Collection<String> labels = new ArrayList<>();
            for (TagInputBean tagInputBean : tagPayload) {
                if (!tagInputBean.isDefault())
                    if (!database.schema().getConstraints(DynamicLabel.label(tagInputBean.getLabel())).iterator().hasNext())
                        labels.add(tagInputBean.getLabel());
            }
            int size = labels.size();

            if (size > 0) {
                logger.debug("Making " + size + " constraints");
                for (String label : labels) {

                    boolean quoted = label.contains(" ") || label.contains("/");

                    String cLabel = quoted ? "`" + label : label;

                    database.execute("create constraint on (t:" + cLabel + (quoted ? "`" : "") + ") assert t.key is unique");
                    database.execute("create constraint on (t:" + cLabel + "Alias " + (quoted ? "`" : "") + ") assert t.key is unique");

                }

            }
            tx.success();
        } finally {
            tx.close();
        }
        logger.debug("No label constraints required");

        return new AsyncResult<>(Boolean.TRUE);
    }
}
