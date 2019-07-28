/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.data.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.flockdata.data.AbstractEntityTag;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Log;
import org.flockdata.data.Tag;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.engine.configure.EngineConfig;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.EntityTagIn;
import org.flockdata.engine.data.graph.EntityTagOut;
import org.flockdata.engine.data.graph.LogNode;
import org.flockdata.engine.data.graph.LogTag;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.geography.dao.GeoSupportNeo;
import org.flockdata.helper.CypherHelper;
import org.flockdata.helper.FlockException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

/**
 * Data Access Object that manipulates tag nodes against track headers
 *
 * @author mholdsworth
 * @tag Cypher, Entity, Tag, Track, EntityTag, Geo
 * @since 28/06/2013
 */
@Repository("entityTagDao")
public class EntityTagDaoNeo {
  private final Neo4jTemplate template;

  private final GeoSupportNeo geoSupport;

  private final FortressService fortressService;

  private final EntityTagOutRepo etOut;

  private final EntityTagInRepo etIn;

  private final PlatformConfig engineConfig;

  private Logger logger = LoggerFactory.getLogger(EntityTagDaoNeo.class);

  @Autowired
  public EntityTagDaoNeo(Neo4jTemplate template, GeoSupportNeo geoSupport, FortressService fortressService, EntityTagOutRepo etOut, EntityTagInRepo etIn, EngineConfig engineConfig) {
    this.template = template;
    this.geoSupport = geoSupport;
    this.fortressService = fortressService;
    this.etOut = etOut;
    this.etIn = etIn;
    this.engineConfig = engineConfig;
  }

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
  public void changeType(EntityNode entity, EntityTag existingTag, String newType) {
    EntityTag entityTag;
    if (existingTag.isReversed()) {
      entityTag = new EntityTagIn(entity, existingTag.getTag(), newType, existingTag.getProperties());
    } else {
      entityTag = new EntityTagOut(entity, existingTag.getTag(), newType, existingTag.getProperties());
    }

    template.delete(existingTag);
    template.save(entityTag);
  }

  /**
   * Moves the entityTag relationships from the Entity to the Log
   * Purpose is to track at which version of a log the metadata covered2
   *
   * @param log        pointer to the node we want to move the relationships to
   * @param entityTags Tags to move
   */
  public void moveTags(Log log, Collection<EntityTag> entityTags) {
    if (log == null) {
      return;
    }

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
   * This will examine the EntityTagDao.FD_WHEN property and {@literal >}= fortressDate log when, it will be removed
   *
   * @param company       a validated company that the caller is allowed to work with
   * @param logToMoveFrom where the logs are currently associated
   * @param entity        entity to relocate them to
   */
  public void moveTags(CompanyNode company, LogNode logToMoveFrom, EntityNode entity) {
    if (logToMoveFrom == null) {
      return;
    }

    Collection<EntityTag> entityTags = getEntityTags(entity);
    Collection<EntityTag> logTags = findLogTags(company, logToMoveFrom);

    for (EntityTag entityTag : entityTags) {
      // Remove any Entity that are newer than the log being re-instated as the "current" truth
      // if entityTag.fdWhen moreRecentThan logToMoveFrom
      Long metaWhen = (Long) entityTag.getProperties().get(EntityTag.FD_WHEN);
      logger.trace("MoveTags - Comparing {} with {}", metaWhen, logToMoveFrom.getEntityLog().getFortressWhen());
      if (metaWhen == null || metaWhen.compareTo(logToMoveFrom.getEntityLog().getFortressWhen()) >= 0) {
        // This tag was added to the entity by a more recent log
        logger.trace("Removing {}", entityTag.getTag().getName());
        template.delete(entityTag);

      }
    }

    for (EntityTag logTag : logTags) {

      boolean isReversed = logTag.isReversed();

      AbstractEntityTag entityTag;
      if (isReversed) {
        entityTag = new EntityTagIn(entity, logTag);
      } else {
        entityTag = new EntityTagOut(entity, logTag);
      }

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
      EntityNode entity = template.convert(row.get("entity"), EntityNode.class);
      results.add(entity);
    }

    return results;
  }

  public Collection<EntityTag> findLogTags(CompanyNode company, Log log) {
    Collection<EntityTag> logTags = new ArrayList<>();
    String query;
    if ("".equals(engineConfig.getTagSuffix(company))) {
      query = "match (log:Log)-[logTag:ARCHIVED_RLX]-(tag:Tag) where id(log)={logId} return logTag";
    } else {
      query = "match (log:Log)-[logTag:ARCHIVED_RLX]-(tag:Tag" + engineConfig.getTagSuffix(company) + ") where id(log)={logId} return logTag";
    }

    Map<String, Object> params = new HashMap<>();
    params.put("logId", log.getId());

    Iterable<Map<String, Object>> results = template.query(query, params);
    for (Map<String, Object> result : results) {
      logTags.add(template.projectTo(result.get("logTag"), LogTag.class));
    }

    return logTags;

  }

  public Collection<EntityTag> getEntityTagsWithGeo(Entity entity) {
    Collection<EntityTag> entityTags = getEntityTags(entity);
    for (EntityTag entityTag : entityTags) {
      if (entityTag.isGeoRelationship()) {
        template.fetch(entityTag.getTag());
        String query = getQuery(entity);
        entityTag.setGeoData(
            geoSupport.getGeoData(query, entityTag.getTag())
        );
      }
    }
    return entityTags;

  }

  /**
   * Enables the overloading of the cypher query used to identify the geo path from the entity.
   * <p>
   * By default it will connect the shortestPath to a Country with up to 4 hops from the starting node.
   * Locates a path to the country via an optional query that can be associated with the entities DocType
   * Query MUST return a nodes(path)
   *
   * @param entity the entity
   * @return cypher query to execute
   */
  private String getQuery(Entity entity) {
    // DAT-495

    String geoQuery = fortressService.getGeoQuery(entity);

    if (geoQuery == null)
    // This is the default way we use if not otherwise defined against the doctype
    {
      geoQuery = "match (located:Tag)  , p= shortestPath((located:Tag)-[*0..4]->(c:Country)) where id(located)={locNode} return nodes(p) as nodes";
    }
    //String query = "match p=(located:Tag)-[r:state|address]->(o)-[*1..3]->(x:Country)  where id(located)={locNode} return nodes(p) as nodes" ;
    return geoQuery;
  }

  public Collection<EntityTag> getEntityTags(Entity entity) {
    ArrayList<EntityTag> results = new ArrayList<>();
    if ((entity != null ? entity.getId() : null) == null) {
      return results;
    }
    getEntityTagsDefault(entity, results);
    return results;

  }

  /**
   * The standard way that the entity tag payload is handled
   * The results are populated by reference
   */
  private void getEntityTagsDefault(Entity entity, Collection<EntityTag> results) {
//        String cypher = "match (e:Entity)-[r]-(:Tag) where id(e) = {entityId} return r, type(r) as rName";
//        Map<String,Object>params = new HashMap<>();
//
//        params.put("entityId", entity.getId());
//        Result<Map<String, Object>> query = template.query(cypher, params);
//        for (Map<String, Object> stringObjectMap : query) {
//            Relationship r = (Relationship) stringObjectMap.get("r");
//            String name = (String) stringObjectMap.get("rName");
//            EntityTag result ;
//            if ( r.getStartNode().getId()!= entity.getId()) {
//                result = etIn.findOne(r.getId());
//                result.setRelationship(name);
//            } else {
//                result = etOut.findOne(r.getId());
//                result.setRelationship(name);
//            }
//            results.add(result);
//        }

    results.addAll(etOut.getEntityTags(entity.getId()));
    results.addAll(etIn.getEntityTags(entity.getId()));

    for (EntityTag entityTag : results) {
      entityTag.setRelationship(template.getRelationship(entityTag.getId()).getType().name());
    }
    logger.debug("Found {} relationships", results.size());
  }

  public Collection<Long> mergeTags(Long fromTag, Long toTag) {
    // DAT-279
    Node fromNode = template.getNode(fromTag);
    Node toNode = template.getNode(toTag);
    Collection<Long> results = moveRelationships(fromTag, fromNode, toNode);
    template.delete(fromNode);
    return results;
  }

  private Collection<Long> moveRelationships(Long fromTag, Node fromNode, Node toNode) {

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
      if (startNode.getId() == fromTag) {
        template.createRelationshipBetween(toNode, endNode, rType.name(), properties);
        if (CypherHelper.isEntity(endNode)) {
          results.add(endNode.getId());
        }
      } else {
        template.createRelationshipBetween(endNode, toNode, rType.name(), properties);
        if (CypherHelper.isEntity(startNode)) // ToDo: This is not being tested !!
        {
          results.add(toNode.getId());
        }
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
