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

package org.flockdata.engine.track;

import org.flockdata.dao.EntityTagDao;
import org.flockdata.engine.PlatformConfig;
import org.flockdata.engine.track.model.EntityNode;
import org.flockdata.engine.track.model.EntityTagIn;
import org.flockdata.engine.track.model.EntityTagOut;
import org.flockdata.engine.track.model.LogTag;
import org.flockdata.geography.GeoSupportNeo4j;
import org.flockdata.helper.CypherHelper;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityTag;
import org.flockdata.track.model.Log;
import org.flockdata.track.service.TagService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * Data Access Object that manipulates tag nodes against track headers
 * <p>
 * User: Mike Holdsworth
 * Date: 28/06/13
 * Time: 11:07 PM
 */
@Repository("entityTagDao")
public class EntityTagDaoNeo4j {
    @Autowired
    Neo4jTemplate template;

    @Autowired
    GeoSupportNeo4j geoSupport;

    @Autowired
    TagService tagService;

    @Autowired
    PlatformConfig engineConfig;

    private Logger logger = LoggerFactory.getLogger(EntityTagDaoNeo4j.class);

    public void deleteEntityTags(Collection<EntityTag> entityTags) throws FlockException {

        for (EntityTag entityTag : entityTags) {
            template.delete(entityTag);
        }

    }

    /**
     * Rewrites the relationship type between the nodes copying the properties
     *
     * @param entity      track
     * @param existingTag current
     * @param newType     new type name
     */
    public void changeType(Entity entity, EntityTag existingTag, String newType) {
        EntityTag entityTag;
        if (existingTag.isReversed())
            entityTag = new EntityTagIn(entity, existingTag.getTag(), newType, existingTag.getProperties());
        else
            entityTag = new EntityTagOut(entity, existingTag.getTag(), newType, existingTag.getProperties());

        template.delete(existingTag);
        template.save(entityTag);
    }

    /**
     * Moves the entityTag relationships from the Entity to the Log
     * Purpose is to track at which version of a log the metadata covered2
     *
     * @param log pointer to the node we want to move the relationships to
     */
    public void moveTags(Log log, Collection<EntityTag> entityTags) {
        if (log == null)
            return;

        for (EntityTag entityTag : entityTags) {

            Relationship relationship = template.getRelationship(entityTag.getId());
            if (relationship != null) {
                // Relationships are immutable, so we have to destroy and recreate
                template.delete(entityTag);
                LogTag logTag = new LogTag(entityTag, log, relationship.getType().name());
                template.save(logTag);
            }
        }

    }

    /**
     * This version is used to relocate the tags associated with Log back to the Entity
     * <p>
     * This will examine the EntityTagDao.FD_WHEN property and >= fortressDate log when, it will be removed
     *
     * @param company       a validated company that the caller is allowed to work with
     * @param logToMoveFrom where the logs are currently associated
     * @param entity        entity to relocate them to
     */
    public void moveTags(Company company, Log logToMoveFrom, Entity entity) {
        if (logToMoveFrom == null)
            return;

        Collection<EntityTag> entityTags = getEntityTags(entity);
        Collection<EntityTag> logTags = findLogTags(company, logToMoveFrom);

        for (EntityTag entityTag : entityTags) {
            // Remove any Entity that are newer than the log being re-instated as the "current" truth
            // if entityTag.fdWhen moreRecentThan logToMoveFrom
            Long metaWhen = (Long) entityTag.getProperties().get(EntityTagDao.FD_WHEN);
            logger.trace("MoveTags - Comparing {} with {}", metaWhen, logToMoveFrom.getEntityLog().getFortressWhen());
            if (metaWhen.compareTo(logToMoveFrom.getEntityLog().getFortressWhen()) >= 0) {
                // This tag was added to the entity by a more recent log
                logger.trace("Removing {}", entityTag.getTag().getName());
                template.delete(entityTag);

            }
        }

        for (EntityTag logTag : logTags) {

            boolean isReversed = logTag.isReversed();

            EntityTag entityTag;
            if (isReversed)
                entityTag = new EntityTagIn(entity, logTag);
            else
                entityTag = new EntityTagOut(entity, logTag);

            template.delete(logTag);
            template.save(entityTag);
        }


    }

    public Set<Entity> findEntityTags(Tag tag) {
        String query = " match (tag:Tag)-[]-(entity:Entity) where id(tag)={tagId}" +
                " return entity";
        Map<String, Object> params = new HashMap<>();
        params.put("tagId", tag.getId());
        Iterable<Map<String, Object>> result = template.query(query, params);
        Set<Entity> results = new HashSet<>();
        for (Map<String, Object> row : result) {
            Entity entity = template.convert(row.get("entity"), EntityNode.class);
            results.add(entity);
        }

        return results;
    }

    public Collection<EntityTag> findLogTags(Company company, Log log) {
        Collection<EntityTag> logTags = new ArrayList<>();
        String query;
        if ("".equals(engineConfig.getTagSuffix(company)))
            query = "match (log:Log)-[logTag:ARCHIVED_RLX]-(tag:Tag) where id(log)={logId} return logTag";
        else
            query = "match (log:Log)-[logTag:ARCHIVED_RLX]-(tag:Tag" + engineConfig.getTagSuffix(company) + ") where id(log)={logId} return logTag";

        Map<String, Object> params = new HashMap<>();
        params.put("logId", log.getId());

        Iterable<Map<String, Object>> results = template.query(query, params);
        for (Map<String, Object> result : results) {
            logTags.add(template.projectTo(result.get("logTag"), LogTag.class));
        }

        return logTags;

    }

    public Iterable<EntityTag> getEntityTagsWithGeo(Entity entity) {
        Collection<EntityTag> entityTags = getEntityTags(entity);
        for (EntityTag entityTag : entityTags) {
            if (entityTag.getTag().getLocated() != null) {
                template.fetch(entityTag.getTag());
                entityTag.setGeoData(
                        geoSupport.getGeoData(entityTag.getTag().getLocated())
                );
            }
        }
        return entityTags;

    }

    public Collection<EntityTag> getEntityTags(Entity entity) {
        ArrayList<EntityTag> results = new ArrayList<>();
        if ((entity != null ? entity.getId() : null) == null)
            return results;

        results.addAll(eout.getEntityTagsOut(entity.getId()));

        results.addAll(eout.getEntityTagsIn(entity.getId()));

        for (EntityTag result : results) {
            result.setRelationship(template.getRelationship(result.getId()).getType().name());
        }
        return results;

    }

    @Autowired
    EntityTagRepo eout;

    @Autowired
    EntityTagInRepo ein;

    public Collection<Long> mergeTags(Tag fromTag, Tag toTag) {
        // DAT-279
        Node fromNode = template.getNode(fromTag.getId());
        Node toNode = template.getNode(toTag.getId());
        Collection<Long> results = moveRelationships(fromTag, fromNode, toNode);
        template.delete(fromNode);
        return results;
    }

    private Collection<Long> moveRelationships(Tag fromTag, Node fromNode, Node toNode) {

        Iterable<Relationship> fromRlxs = fromNode.getRelationships();
        Collection<Long> results = new ArrayList<>();
        for (Relationship fromRlx : fromRlxs) {
            RelationshipType rType = fromRlx.getType();
            Node startNode = fromRlx.getStartNode();
            Node endNode = fromRlx.getEndNode();
            Map<String, Object> properties = new HashMap<>();
            for (String key : fromRlx.getPropertyKeys()) {
                properties.put(key, fromRlx.getProperty(key));
            }
            if (startNode.getId() == fromTag.getId()) {
                template.createRelationshipBetween(toNode, endNode, rType.name(), properties);
                if (CypherHelper.isEntity(endNode))
                    results.add(endNode.getId());
            } else {
                template.createRelationshipBetween(endNode, toNode, rType.name(), properties);
                if (CypherHelper.isEntity(toNode))
                    results.add(toNode.getId());
            }
        }
        return results;
    }

    public void purgeUnusedTags(String label) {
        // ToDo: Pageable

        String query = "optional match (t:" + label + ")-[:HAS_ALIAS]-(a) where not (t)-[]-(:Entity) return t,a;";
        template.query(query, null);

    }

}
