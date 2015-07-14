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

package org.flockdata.track;

import org.flockdata.helper.FlockDataTagException;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.bean.AliasInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.bean.TagResultBean;
import org.flockdata.registration.model.Tag;
import org.neo4j.graphdb.*;
import org.neo4j.server.database.CypherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.AsyncResult;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Serverside Tag routines to support FlockData
 * <p>
 * Created by mike on 18/06/15.
 */
@Path("/tags")
public class TagManager {
    private final GraphDatabaseService database;

    private Logger logger = LoggerFactory.getLogger(TagManager.class);

    public TagManager(@Context GraphDatabaseService database) {
        this.database = database;
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/")
    public Response hello(TagPayload tagPayload, @Context CypherExecutor cypherExecutor) {
        System.out.println("Hello World " + tagPayload.getTags().size());
        Collection<TagResultBean> results = new ArrayList<>();
        Collection<String> createdValues = new ArrayList<>();

        ensureUniqueIndexes(tagPayload.getTags());

        try (Transaction tx = database.beginTx()) {
            for (TagInputBean tagInputBean : tagPayload.getTags()) {
                Node node = save(tagInputBean, tagPayload.getSuffix(), createdValues);
                TagResultBean resultBean = new TagResultBean(tagInputBean, new TagNode(node));
                results.add(resultBean);

            }
            tx.success();
        }

        return Response.ok().entity(results).type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{label}/{code}")
    public Response findTag(@PathParam("label") String label, @PathParam("code") String code) {
        String suffix = "";
        try (Transaction tx = database.beginTx()) {
            Node node = findTagNode(suffix, label, code);
            TagResultBean resultBean = new TagResultBean(new TagNode(node));
            tx.success();
            return Response.ok().entity(resultBean).build();
        }

    }

    Node findTagNode(String suffix, String label, String tagCode) {

        if (tagCode == null)
            throw new IllegalArgumentException("Null can not be used to find a tag (" + label + ")");


        String theLabel = TagHelper.resolveLabel(label, suffix);

        Node tag = tagByKey(theLabel, TagHelper.parseKey(tagCode));
        //if ( tag!=null && inflate)
        logger.trace("requested tag [{}:{}] foundTag [{}]", label, tagCode, (tag == null ? "NotFound" : tag));
        return tag;
    }

    Node tagByKey(String theLabel, String tagKey) {

        ResourceIterator<Node> tags = database.findNodes(DynamicLabel.label(theLabel), "key", tagKey);//tagRepo.findByKey(tagKey);
        Node result = null;
        int count = 0;
        while (tags.hasNext()) {
            if (count == 0) {
                result = tags.next();
                count++;
            }
        }

        if (result != null)
            return result;

        logger.trace("{} Not found by key, looking by label {}", theLabel, tagKey);

        String query = "match (:`" + theLabel + "Alias` {key:{tagKey}})<-[HAS_ALIAS]-(tag:`" + theLabel + "`) return tag";

        Map<String, Object> params = new HashMap<>();
        params.put("tagKey", tagKey);
        Result results = database.execute(query, params);

        Node tagResult = null;
        while (results.hasNext()) {
            if (tagResult == null)
                tagResult = (Node) results.next().get("tag");

        }
        if (tagResult == null)
            logger.trace("Not found {}, {}", theLabel, tagKey);
        else
            logger.trace("byAlias {}, {}", theLabel, tagKey);

        return tagResult;
    }

    Node save(TagInputBean tagInput, String tagSuffix, Collection<String> createdValues) {
        // Check exists
        Node startTag = findTagNode(tagSuffix, tagInput.getLabel(), (tagInput.getCode() == null ? tagInput.getName() : tagInput.getCode()));
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
                    startTag = save(notFound, tagSuffix, createdValues);
                } else
                    throw new FlockDataTagException("Tag [" + tagInput + "] should exist as a [" + tagInput.getLabel() + "] but doesn't. Ignoring this request.");
            } else {
                startTag = createTag(tagInput, tagSuffix);
            }
        }

        Map<String, Collection<TagInputBean>> targets = tagInput.getTargets();
        for (String rlxName : targets.keySet()) {
            Collection<TagInputBean> associatedTag = targets.get(rlxName);
            for (TagInputBean tagInputBean : associatedTag) {
                processAssociatedTags(tagSuffix, startTag, tagInputBean, rlxName, createdValues);
            }

        }


        return startTag;
    }

    private void processAssociatedTags(String tagSuffix, Node startTag, TagInputBean associatedTag, String rlxName, Collection<String> createdValues) {

        Node endTag = save(associatedTag, tagSuffix, createdValues);
//        if (suppressRelationships)
//            return;

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

        String query = "match (start:Tag) where id(start)={startId}, (end:Tag) where id(end)={endId}" +
                "create unique (start)-[r:" + rName + "]-(end) return r";

        Map<String, Object> args = new HashMap<>();
        args.put("startId", startNode.getId());
        args.put("endId", endNode.getId());
        Result r = database.execute(query, args);
        r.next();
        createdValues.add(key);
    }

    private Node createTag(TagInputBean tagInput, String suffix) {

        logger.trace("createTag {}", tagInput);

        String label;
        String uniqueLabel;
        if (tagInput.isDefault()) {
            label = Tag.DEFAULT_TAG + suffix;
            uniqueLabel = label;
        } else {
            uniqueLabel = tagInput.getLabel();
            label = tagInput.getLabel() + ":Tag";
        }

        logger.trace("Saving {}", tagInput);
        String query = " merge (tag:" + label + " {code:{code}, " + (tagInput.getName() == null ? "" : "name:{name},") + " key:{key}} ) return tag ";
        Map<String, Object> props = new HashMap<>();
        props.put("code", tagInput.getCode());
        props.put("name", tagInput.getName());
        props.put("key", TagHelper.parseKey(tagInput.getCode()));

        Result result = database.execute(query, props);
        Node tag = (Node) result.next().get("tag");

        if (tagInput.hasAliases()) {
            String aliasQuery = "match (tag:Tag) where id(tag)={tagId}" +
                    "create unique (tag)-[:HAS_ALIAS]->(alias:Alias:" + uniqueLabel + "Alias {key:{key}, name:{name}, description:{description}}) return alias";

            for (AliasInputBean newAlias : tagInput.getAliases()) {
                HashMap<String, Object> alias = new HashMap<>();
                alias.put("tagId", tag.getId());
                alias.put("key", TagHelper.parseKey(newAlias.getCode()));
                alias.put("name", newAlias.getCode());
                alias.put("description", newAlias.getDescription());
                result = database.execute(aliasQuery, alias);
                Node created = (Node) result.next().get("alias");

            }
        }


        return tag;

    }
    public Future<Boolean> ensureUniqueIndexes(Collection<TagInputBean> tagPayload) {

        Transaction tx = database.beginTx();
        try {
            Collection<String> labels = new ArrayList<>();
            for (TagInputBean tagInputBean : tagPayload) {
                if (!database.schema().getConstraints(DynamicLabel.label(tagInputBean.getLabel())).iterator().hasNext())
                    labels.add(tagInputBean.getLabel());
            }
            int size = labels.size();

            if (size > 0) {
                logger.debug("Making " + size + " constraints");
                for (String label : labels) {
                    boolean quoted = label.contains(" ") || label.contains("/");

                    String cLabel = quoted ? "`" + label : label;

                    database.execute("create constraint on (t:" + cLabel + (quoted ? "'" : "") + ") assert t.key is unique");
                    database.execute("create constraint on (t:" + cLabel + "Alias " + (quoted ? "'" : "") + ") assert t.key is unique");

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