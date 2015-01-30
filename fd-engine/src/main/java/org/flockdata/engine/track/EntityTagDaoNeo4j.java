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

package org.flockdata.engine.track;

import org.flockdata.engine.FdEngineConfig;
import org.flockdata.engine.tag.model.TagNode;
import org.flockdata.engine.track.model.EntityNode;
import org.flockdata.engine.track.model.EntityTagRelationship;
import org.flockdata.helper.CypherHelper;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityTag;
import org.flockdata.track.model.GeoData;
import org.flockdata.track.model.Log;
import org.flockdata.track.service.TagService;
import org.neo4j.cypher.internal.compiler.v2_1.planner.logical.steps.optional;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
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
    TagService tagService;

    @Autowired
    FdEngineConfig engineConfig;

    private Logger logger = LoggerFactory.getLogger(EntityTagDaoNeo4j.class);

    static final String FD_WHEN = "fdWhen";

    public EntityTag save(Entity entity, Tag tag, String relationshipName) {
        return save(entity, tag, relationshipName, false, new HashMap<String, Object>());
    }

    public EntityTag save(Entity ah, Tag tag, String metaLink, boolean reverse) {
        return save(ah, tag, metaLink, reverse, null);
    }

    /**
     * creates the relationship between the entity and the tag of the name type.
     * If metaId== null, then an EntityTag for the caller to deal with otherwise the relationship
     * is persisted and null is returned.
     *
     * @param entity           valid entity
     * @param tag              tag
     * @param relationshipName name
     * @param isReversed       tag<-entity (false) or entity->tag (true)
     * @param propMap          properties to associate with the relationship
     * @return Null or EntityTag
     */
    public EntityTag save(Entity entity, Tag tag, String relationshipName, Boolean isReversed, Map<String, Object> propMap) {
        // ToDo: this will only set properties for the "current" tag to Entity. it will not version it.
        if (relationshipName == null) {
            relationshipName = Tag.UNDEFINED;
        }
        if (tag == null)
            throw new IllegalArgumentException("Tag must not be NULL. Relationship[" + relationshipName + "]");

        EntityTagRelationship rel = new EntityTagRelationship(entity, tag, relationshipName, propMap);

        if (entity.getId() == null)
            return rel;

        Node entityNode = template.getPersistentState(entity);
        // ToDo: Save a timestamp against the relationship

        Node tagNode;
        try {
            tagNode = template.getNode(tag.getId());
        } catch (RuntimeException e) {
            logger.error("Weird error looking for tag [{}] with ID [{}]", tag.getKey(), tag.getId());
            throw (e);
        }
        //Primary exploration relationship
        Node start = (isReversed ? entityNode : tagNode);
        Node end = (isReversed ? tagNode : entityNode);

        if ( !entity.isNew()) {
            Relationship r = template.getRelationshipBetween(start, end, relationshipName);

            if (r != null) {
                return rel;
            }
        }

        long lastUpdate = entity.getFortressDateUpdated();
        propMap.put(EntityTag.SINCE, (lastUpdate == 0 ? entity.getFortressDateCreated().getMillis() : lastUpdate));

        template.createRelationshipBetween(start, end, relationshipName, propMap);
        logger.trace("Created Relationship Tag[{}] of type {}", tag, relationshipName);
        return rel;
    }

    public void deleteEntityTags(Entity entity, Collection<EntityTag> entityTags) throws FlockException {
        Node entityNode = null;
        for (EntityTag tag : entityTags) {
            if (!tag.getPrimaryKey().equals(entity.getId()))
                throw new FlockException("Tags do not belong to the required Entity");

            if (entityNode == null) {
                entityNode = template.getNode(tag.getPrimaryKey());
            }

            Relationship r = template.getRelationship(tag.getId());
            r.delete();
            // ToDo - remove nodes that are not attached to other nodes.
            if (!r.getOtherNode(entityNode).getRelationships().iterator().hasNext())
                template.getNode(tag.getTag().getId()).delete();
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
        if (!relationshipExists(entity, existingTag.getTag(), newType)) {
            Relationship r = template.getRelationship(existingTag.getId());
            Iterable<String> propertyKeys = r.getPropertyKeys();
            Map<String, Object> properties = new HashMap<>();
            for (String propertyKey : propertyKeys) {
                properties.put(propertyKey, r.getProperty(propertyKey));
            }
            template.createRelationshipBetween(r.getStartNode(), r.getEndNode(), newType, properties);
            r.delete();
        }
    }

    /**
     * Moves the entityTag relationships from the Entity to the Log
     * Purpose is to track at which version of a log the metadata covered2
     *
     * @param log    pointer to the node we want to move the relationships to
     * @param entity where the tags are currently located
     */
    public void moveTags(Entity entity, Log log, Collection<EntityTag> entityTags) {
        if (log == null)
            return;

        Node logNode = template.getPersistentState(log);
        for (EntityTag entityTag : entityTags) {
            Node tagNode = template.getNode(entityTag.getTag().getId());

            Relationship relationship = template.getRelationship(entityTag.getId());
            if (relationship != null) {

                boolean isReversed = relationship.getStartNode().getId() == tagNode.getId();
                Node start = (isReversed ? logNode : tagNode);
                Node end = (isReversed ? tagNode : logNode);

                Map<String, Object> rlxProps = getRelationshipProperties(relationship);
                // Relationships are immutable, so we have to destroy and recreate
                template.delete(relationship);
                template.createRelationshipBetween(start, end, relationship.getType().name(), rlxProps);
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

        Collection<EntityTag> metaTags = getEntityTags(company, entity.getId());
        Collection<EntityTag> entityTags = findLogTags(company, logToMoveFrom);
        Node entityNode = template.getPersistentState(entity);

        for (EntityTag entityTag : metaTags) {
            // Remove any MetaTags that are newer than the log being re-instated as the "current" truth
            // if entityTag.abWhen moreRecentThan logToMoveFrom

            Long metaWhen = (Long) entityTag.getProperties().get(FD_WHEN);
            template.fetch(logToMoveFrom.getEntityLog());
            logger.trace("MoveTags - Comparing {} with {}", metaWhen, logToMoveFrom.getEntityLog().getFortressWhen());
            if (metaWhen.compareTo(logToMoveFrom.getEntityLog().getFortressWhen()) >= 0) {
                // This tag was added to the entity by a more recent log
                logger.trace("Removing {}", entityTag.getTag().getName());
                Relationship r = template.getRelationship(entityTag.getId());
                if (r != null)
                    template.delete(r);

            }
        }

        for (EntityTag entityTag : entityTags) {
            Node tagNode = template.getNode(entityTag.getTag().getId());

            Relationship relationship = template.getRelationship(entityTag.getId());
            if (relationship != null) {

                boolean isReversed = relationship.getStartNode().getId() == tagNode.getId();
                Node start = (isReversed ? entityNode : tagNode);
                Node end = (isReversed ? tagNode : entityNode);

                Map<String, Object> rlxProps = getRelationshipProperties(relationship);
                // Relationships are immutable, so we have to destroy and recreate
                template.delete(relationship);
                template.createRelationshipBetween(start, end, relationship.getType().name(), rlxProps);
            }
        }

    }

    private Map<String, Object> getRelationshipProperties(Relationship relationship) {
        Map<String, Object> rlxProps = new HashMap<>();
        for (String key : relationship.getPropertyKeys()) {
            // ToDo: System property checks
            rlxProps.put(key, relationship.getProperty(key));
        }
        return rlxProps;
    }

    public Set<Entity> findEntityTags(Tag tag) {
        String query = " match (tag:_Tag)-[]-(entity:_Entity) where id(tag)={tagId}" +
                " return entity";
        Map<String, Object> params = new HashMap<>();
        params.put("tagId", tag.getId());
        Result<Map<String, Object>> result = template.query(query, params);
        Set<Entity> results = new HashSet<>();
        for (Map<String, Object> row : result) {
            Entity entity = template.convert(row.get("entity"), EntityNode.class);
            results.add(entity);
        }

        return results;
    }

    public Boolean relationshipExists(Entity entity, Tag tag, String relationshipName) {
        Node end = template.getPersistentState(entity);
        Node start = template.getNode(tag.getId());
        return (template.getRelationshipBetween(start, end, relationshipName) != null);

    }

    public Collection<EntityTag> findLogTags(Company company, Log log) {
        String query;
        if ("".equals(engineConfig.getTagSuffix(company)))
            query = "match (log:_Log)-[tagType]-(tag:_Tag) where id(log)={logId} return tag, tagType";
        else
            query = "match (log:_Log)-[tagType]-(tag:_Tag" + engineConfig.getTagSuffix(company) + ") where id(log)={logId} return tag, tagType";

        Map<String, Object> params = new HashMap<>();
        params.put("logId", log.getId());

        Result<Map<String, Object>> results = template.query(query, params);
        return getEntityTags(log.getId(), results);

    }

    public Collection<EntityTag> getDirectedEntityTags(Company company, Entity entity, boolean outbound) {

        String tagDirection = "-[tagType]->";
        if (!outbound)
            tagDirection = "<-[tagType]-";

        List<EntityTag> tagResults = new ArrayList<>();
        if (null == entity.getId())
            return tagResults;
        String query = "match (track:_Entity)" + tagDirection + "(tag" + Tag.DEFAULT + engineConfig.getTagSuffix(company) + ") " +
                "where id(track)={id} \n" +
                "optional match tag-[:located]-(located)-[*0..2]-(country:Country) \n" +
                "optional match located-[*0..2]->(state:State) " +
                "return tag,tagType,located,state, country";

        return getEntityTags(entity.getId(), query);

    }

    public Collection<EntityTag> getEntityTags(Company company, Long entityid) {
        List<EntityTag> tagResults = new ArrayList<>();
        if (null == entityid)
            return tagResults;
        String query = "match (entity:_Entity)-[tagType]-(tag" + Tag.DEFAULT + engineConfig.getTagSuffix(company) + ") " +
                "where id(entity)={id} \n" +
                "optional match tag-[:located]-(located)-[*0..2]-(country:Country) \n" +
                "optional match located-[*0..2]->(state:State) " +
                "return tag,tagType,located,state, country " +
                "order by type(tagType), tag.name";

        //List<EntityTag> raw = getEntityTags(entity.getId(), query);
        //Collections.sort(raw, new BeanComparator<>("tagType"));
        return getEntityTags(entityid, query);
    }

    private Collection<EntityTag> getEntityTags(Long primaryKey, String query) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", primaryKey);
        Result<Map<String, Object>> queryResults = template.query(query, params);
        return getEntityTags(primaryKey, queryResults);
    }

    private Collection<EntityTag> getEntityTags(Long primaryKey, Result<Map<String, Object>> queryResults) {
        Set<EntityTag> tagResults = new TreeSet<>();
        for (Map<String, Object> row : queryResults) {
            Node n = (Node) row.get("tag");
            TagNode tag = template.projectTo(n, TagNode.class);
            Relationship relationship = template.convert(row.get("tagType"), Relationship.class);
            EntityTagRelationship entityTag = new EntityTagRelationship(primaryKey, tag, relationship);

            Node loc = (Node) row.get("located");

            if (loc != null) {
                String isoCode = null;
                String countryName = null;
                Double lat = null;
                Double lon = null;
                String stateName = null;

                Node country = (Node) row.get("country");
                Node state = (Node) row.get("state");
                //geoData.setCity((String) loc.getProperty("name"));
                String city = (String) loc.getProperty("name");

                if (country != null && country.hasProperty("name")) {
                    // ToDo: Need a Country object
                    isoCode = (String) country.getProperty("code");
                    countryName = (String) country.getProperty("name");
                    Object latitude = null;
                    Object longitude = null;

                    if (country.hasProperty("props-latitude"))
                        latitude = country.getProperty("props-latitude");

                    if (country.hasProperty("props-longitude"))
                        longitude = country.getProperty("props-longitude");

                    if ( (latitude != null && longitude != null) && ! (latitude.equals("") || longitude.equals(""))) {
                        lat = Double.parseDouble(latitude.toString());
                        lon = Double.parseDouble(longitude.toString());
                    }
                }
                if (state != null && state.hasProperty("name"))
                    stateName = (String) state.getProperty("name");

                GeoData geoData = new GeoData(isoCode, countryName, city, stateName, lat, lon);
                entityTag.setGeoData(geoData);
            }
            tagResults.add(entityTag);
        }
        return tagResults;

    }

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
            for (String key : properties.keySet()) {
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

        // Figure out the purge statement:
        //match (t:Politician ) where not (t)-[]-(:Entity) with t optional match t-[r:HAS_ALIAS]-(a) delete t,a,r;
        //match (t:Politician ) where not (t)-[]-(:Entity) with t optional match t-[r]-() delete t,r;

        String query = "optional match (t:"+label+")-[:HAS_ALIAS]-(a) where not (t)-[]-(:Entity) return t,a;";
        template.query(query, null);
//        Result<Map<String, Object>> result = template.query(query, null);
//        for (Map<String, Object> row : result) {
//            Tag tag = template.convert(row.get("entity"), TagNode.class);
//            Map<String, Object> params = new HashMap<>();
//            tagService.p(tag);
//        }

    }
}
