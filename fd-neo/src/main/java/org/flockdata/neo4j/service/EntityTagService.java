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

import org.flockdata.dao.EntityTagDao;
import org.flockdata.helper.FlockException;
import org.flockdata.model.*;
import org.flockdata.neo4j.EntityTagNode;
import org.flockdata.authentication.registration.bean.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityTagInputBean;
import org.flockdata.track.EntityTagPayload;
import org.flockdata.track.bean.TrackResultBean;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates EntityTag access functionality
 *
 * @author mholdsworth
 * @since 14/07/2015
 */
public class EntityTagService {
    private Logger logger = LoggerFactory.getLogger(EntityTagService.class);

    private GraphDatabaseService database;

    private TagService tagService;

    public EntityTagService(@Context GraphDatabaseService database) {
        this.database = database;
        this.tagService = new TagService(database);
    }

    public Collection<EntityTag> associateTags(TrackResultBean trackResultBean, Node entityNode, EntityLog lastLog) throws FlockException {
        Collection<EntityTagInputBean> newEntityTags = new ArrayList<>();
        Collection<EntityTag> tagsToMove = new ArrayList<>();
        EntityInputBean entityInputBean = trackResultBean.getEntityInputBean();
        Collection<EntityTag> existingTags = (trackResultBean.isNewEntity() ? new ArrayList<EntityTag>() : getEntityTags(entityNode));

        for (TagInputBean tagInputBean : entityInputBean.getTags()) {

            Node existingTag = null;//getTag(existingTags, tagInputBean);
            Node tag;
            if (existingTag == null) {
                tag = tagService.createTag(tagInputBean, trackResultBean.getTenant());
            } else {
                tag = existingTag;
            }

//            if (existingTag == null) {
            newEntityTags.addAll(
                    getRelationshipsToCreate(entityNode, tag, tagInputBean)
            );
//            } else {
//                EntityTag entityTag = getEntityTag(existingTags, tagInputBean);
//                if (entityTag != null)
//                    newEntityTags.add(entityTag);
//            }
        }

        if (!entityInputBean.getTags().isEmpty() && !trackResultBean.isNewEntity()) {
            // We only consider relocating tags to the log if the caller passes at least one tag set
            for (EntityTag entityTag : existingTags) {
                if (!newEntityTags.contains(entityTag))
                    tagsToMove.add(entityTag);
            }
            if (entityInputBean.isArchiveTags())
                if (lastLog != null) {
                    if (lastLog.isMocked()) {
                        for (EntityTag entityTag : tagsToMove) {
                            // Nowhere to move the tags too so we just delete them
                            // FixMe
                            // template.delete(entityTag);
                            logger.info("FixMe: delete entityTag " + entityTag);
                        }
                    } else {
                        // FixMe
                        //moveTags(lastLog, tagsToMove);
                        logger.info("FixMe: move entityTags ");
                    }
                }
        }
        return saveEntityTags(entityNode, entityInputBean, newEntityTags);
    }

    private Collection<EntityTag> saveEntityTags(Node entityNode, EntityInputBean entityInputBean, Collection<EntityTagInputBean> entityTags) {
        if (entityInputBean.isTrackSuppressed())
            return new ArrayList<>();
        // Create
        Collection<EntityTag>results = new ArrayList<>();
        for (EntityTagInputBean entityTag : entityTags) {
            //Node tag = database.getNodeById(entityTag.getTag().getId());
            //EntityTagInputBean tagInputBean = new EntityTagInputBean(null, entityTag.getTagCode(), entityTag.getIndex());
            results.add(makeRelationship(entityNode, entityTag.getTag(), entityTag));
        }

        return results;

    }


    /**
     * Creates and sets the relationship objects to in to the entity
     * <p>
     * Does not save the entity
     *
     * @param tag          Tag to associated
     * @param tagInputBean Tag control
     * @return EntityTags that were added to the entity.
     */
    private Collection<EntityTagInputBean> getRelationshipsToCreate(Node entityNode, Node tag, TagInputBean tagInputBean) {
        Map<String, Object> entityLinks = tagInputBean.getEntityLinks();
        Entity entity = new Entity(entityNode);

        Collection<EntityTagInputBean> entityTags = new ArrayList<>();
        long when = (entity.getFortressLastWhen() == null ? 0 : entity.getFortressLastWhen());
        if (when == 0)
            when = entity.getWhenCreated();
        for (String key : entityLinks.keySet()) {
            if ( getRelationship(entityNode, tag, key) == null ) {
                // New relationship
                EntityTagInputBean entityTagInputBean = new EntityTagInputBean(null, tagInputBean.getCode(), key);
                entityTagInputBean.setSince(tagInputBean.isSince());
                entityTagInputBean.setTag(tag);

                Object properties = entityLinks.get(key);
                Map<String, Object> propMap;
                if (properties != null && properties instanceof Map) {
                    propMap = (Map<String, Object>) properties;
                } else {
                    propMap = new HashMap<>();
                }

                propMap.put(EntityTagDao.FD_WHEN, when);
                entityTagInputBean.setProperties(propMap);

                entityTags.add(entityTagInputBean);

                // Direction and properties

                //            EntityTag entityTagRelationship = makeRelationship(entityNode, tag, entityTagInputBean);
                //            if (entityTagRelationship != null) {
                //                entityTags.add(entityTagRelationship);
                //            }
            }

        }
        return entityTags;
    }

    /**
     * Calculates an object  between the entity and the tag of the requested type.
     * The relationship is not added to the entity and is just returned to teh caller
     * for processing and persistence.
     *
     * @param entity valid entity
     * @param tag    tag
     *               //     * @param relationshipName name
     *               //     * @param isReversed       tag<-entity (false) or entity->tag (true)
     *               //     * @param propMap          properties to associate with the relationship
     * @return Null or the EntityTag that was created
     */
    EntityTag makeRelationship(Node entity, Node tag, EntityTagInputBean entityTagInput) {

        Map<String, Object> propMap = new HashMap<>();
        Entity e = new Entity(entity);
        Tag t = new Tag(tag);

        if (entityTagInput.isSince()) {
            long lastUpdate = (e.getFortressLastWhen() == null ? 0 : e.getFortressLastWhen());
            propMap.put(EntityTag.SINCE, (lastUpdate == 0 ? e.getFortressCreate() : lastUpdate));
        }

        // FixMe - directed
        Relationship r = entity.createRelationshipTo(tag, DynamicRelationshipType.withName(entityTagInput.getType()));
        for (String s : propMap.keySet()) {
            r.setProperty(s, propMap.get(s));
        }
        logger.trace("Created Relationship Tag[{}] of type {}", t, entityTagInput.getIndex());
        return new EntityTagNode(entity, tag, r);

    }

    public Collection<EntityTag> addTags(Node entity, EntityTagPayload entityTagPayload) {
        Collection<EntityTag> results = new ArrayList<>();
        for (EntityTagInputBean entityTag : entityTagPayload.getEntityTags()) {
            results.add(addEntityTag(entity, entityTag));
        }
        return results;
    }

    public EntityTag addEntityTag(Node entity, EntityTagInputBean entityTagInput) {
        Node tagNode = tagService.findTagNode("", entityTagInput.getIndex(), entityTagInput.getTagCode());
        boolean existing = (getRelationship(entity, tagNode, entityTagInput.getType()) != null);
        if (existing)
            // We already have this tagged so get out of here
            return null;

        return makeRelationship(entity, tagNode, entityTagInput);
    }

    public EntityTag getEntityTag(Node entity, Node tag, EntityTagInputBean params) {
        Relationship r = getRelationship(entity, tag, params.getType());

        if (r == null)
            return null;
        return new EntityTagNode(r, tag);
    }

    private Relationship getRelationship(Node entity, Node tagNode, String relationshipType) {

        for (Relationship relationship : entity.getRelationships(DynamicRelationshipType.withName(relationshipType))) {
            if (relationship.getOtherNode(entity).getId() == tagNode.getId())
                return relationship;
        }
        return null;
    }

    public Collection<EntityTag> getEntityTags(Node entity) {
        return getEntityTags(entity.getId());
    }

    public Collection<EntityTag> getEntityTags(Long entityId) {
        ArrayList<EntityTag> results = new ArrayList<>();
        if ((entityId != null ?entityId : null) == null)
            return results;

        String query = "match (e:Entity)-[r]-(t:Tag) where id(e) = {id} return e,r,t";
        Map<String, Object> params = new HashMap<>();
        params.put("id",entityId);
        Result queryResults = database.execute(query, params);
        if (!queryResults.hasNext())
            return results;
        Entity e = null;
        while (queryResults.hasNext()) {
            Map<String, Object> row = queryResults.next();
            Relationship relationship = (Relationship) row.get("r");
            Node tag = (Node) row.get("t");
            if ( e== null )
                e = new Entity((Node)row.get("e")); // Entity is the same for all EntityTags
            results.add(new EntityTagNode(e, relationship, tag));
        }
        return results;

    }



}
