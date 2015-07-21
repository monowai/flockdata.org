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

import org.flockdata.helper.FlockException;
import org.flockdata.model.*;
import org.flockdata.neo4j.EntityManager;
import org.flockdata.neo4j.helper.CypherUtils;
import org.flockdata.registration.KeyGenService;
import org.flockdata.registration.bean.TagResultBean;
import org.flockdata.track.EntityHelper;
import org.flockdata.track.EntityPayload;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.TrackResultBean;
import org.flockdata.track.service.EntityTagService;
import org.flockdata.track.service.LogService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike on 10/07/15.
 */
public class EntityService {

    private Logger logger = LoggerFactory.getLogger(EntityManager.class);

    static final Label ENTITY_LABEL = DynamicLabel.label("Entity");
    static final DynamicRelationshipType CREATED_BY = DynamicRelationshipType.withName("CREATED_BY");
    static final DynamicRelationshipType LASTCHANGED_BY = DynamicRelationshipType.withName("LASTCHANGED_BY");

    static final KeyGenService keyGenService = new KeyGenService();

    private EntityTagService entityTagService;
    LogService logService;

    private GraphDatabaseService database;

    public EntityService(@Context GraphDatabaseService database) {
        this.database = database;
        this.entityTagService = new EntityTagService(database);
        this.logService = new LogService(database);
    }

    public TrackResultBean createEntity(EntityPayload payload, EntityInputBean entityInputBean) throws FlockException {

        Node entityNode = null;
        if (entityInputBean.getMetaKey() != null) {
            entityNode = findByMetaKey(entityInputBean.getMetaKey());
        }

        if (entityNode == null && (entityInputBean.getCallerRef() != null && !entityInputBean.getCallerRef().equals("")))
            entityNode = findByCallerRef(payload.getFortress(), payload.getDocumentType(), entityInputBean.getCallerRef());

        if (entityNode != null) {
            logger.trace("Existing entity found by Caller Ref [{}] found [{}]", entityInputBean.getCallerRef(), entityNode.getProperty(Entity.UUID_KEY));
            entityInputBean.setMetaKey(entityNode.getProperty(Entity.UUID_KEY).toString());

            logger.trace("Existing entity [{}]", entityNode);
            TrackResultBean trackResult = new TrackResultBean(payload.getFortress(), entityNode, entityInputBean);
            trackResult.entityExisted();
            trackResult.setContentInput(entityInputBean.getContent());
            trackResult.setDocumentType(payload.getDocumentType());

            // Process mutable properties for an entity
            if (entityInputBean.getContent() != null && entityInputBean.getContent().getWhen() != null) {
                // Communicating the POTENTIAL last update so it can be recorded in the tag relationships
                entityNode.setProperty("fortressLastWhen", entityInputBean.getContent().getWhen().getTime());
            }

            // Optimize? Remove existing properties and replace with the incoming payload
            if (entityInputBean.getProperties() != null) {
                for (String key : entityNode.getPropertyKeys()) {
                    if (key.startsWith(TagResultBean.PROPS_PREFIX))
                        entityNode.removeProperty(key);
                }
                for (String key : entityInputBean.getProperties().keySet()) {
                    entityNode.setProperty(TagResultBean.PROPS_PREFIX + key, entityInputBean.getProperties().get(key));
                }
            }
            // We can update the entity name?
            if (entityInputBean.getName() != null && !entityNode.getProperty("name").equals(entityInputBean.getName())) {
                entityNode.setProperty("name", entityInputBean.getName());
            }


            // Could be rewriting tags
            // DAT-153 - move this to the end of the process?
            // FixMe
            EntityLog entityLog = logService.getLastLog(entityNode.getId());
            trackResult.setTags(
                    entityTagService.associateTags(trackResult, entityNode, entityLog)
            );
            return trackResult;
        }

        try {
            entityNode = saveEntityNode(payload, entityInputBean);
        } catch (FlockException e) {
            logger.error(e.getMessage());
            return new TrackResultBean("Error processing entityInput [{}]" + entityInputBean + ". Error " + e.getMessage());
        }
        // Flag the entity as having been newly created. The flag is transient and
        // this saves on having to pass the property as a method variable when
        // associating the tags
        TrackResultBean trackResult = new TrackResultBean(payload.getFortress(), entityNode, entityInputBean);
        trackResult.setDocumentType(payload.getDocumentType());
        trackResult.setNew();

        // FixingMe
        trackResult.setTags(
                entityTagService.associateTags(trackResult, entityNode, logService.getLastLog(entityNode.getId()))
        );

        trackResult.setContentInput(entityInputBean.getContent());

        if (trackResult.isNew() && entityInputBean.getContent() != null) {
            // DAT-342
            // We prep the content up-front in order to get it distributed to other services
            // ASAP
            // Minimal defaults that are otherwise set in the LogService

            // FixMe
//            FortressUser contentUser = null;
//            if (entityInputBean.getContent().getFortressUser() != null)
//                contentUser = fortressService.getFortressUser(fortress, entityInputBean.getContent().getFortressUser());

            if (entityInputBean.getContent().getEvent() == null) {
                entityInputBean.getContent().setEvent(Log.CREATE);
            }
            // FixMe
            //Log log = entityDao.prepareLog(fortress.getCompany(), (contentUser != null ? contentUser : entity.getCreatedBy()), trackResult, null, null);

            DateTime contentWhen = (trackResult.getContentInput().getWhen() == null ? new DateTime(DateTimeZone.forID(payload.getFortress().getTimeZone())) : new DateTime(trackResult.getContentInput().getWhen()));
//            EntityLog entityLog = new EntityLog(entity, log, contentWhen);

            //if (trackResult.getContentInput().getWhen()!= null )

            // FixMe
//            logger.debug("Setting preparedLog for entity {}", entity);
//            LogResultBean logResult = new LogResultBean(trackResult.getContentInput());

//            logResult.setLogToIndex(entityLog);
            //trackResult.setEntityLog(logResult);
            //trackResult.setPreparedLog( entityLog );
        }

        return trackResult;

    }



    private Node saveEntityNode(EntityPayload payload, EntityInputBean entityInput) throws FlockException {
        String metaKey = (entityInput.isTrackSuppressed() ? null : keyGenService.getUniqueKey());
        Entity e = new Entity(metaKey, payload.getFortress(), entityInput, payload.getDocumentType());

        String cypher = "match (f:Fortress) where id(f) = {fortressId} " +
                "with f merge (entity:`" + payload.getDocumentType().getName() +
                "`:Entity {metaKey: {metaKey}, name:{name}, " + (entityInput.getEvent() != null ? " event:{event}, " : "") + "callerKeyRef:{callerKeyRef}, callerRef:{callerRef}, description:{description}, dateCreated:{dateCreated},lastUpdate:{lastUpdate} " +
                ", fortressLastWhen:{fortressLastWhen}, fortressCreate:{fortressCreate}, searchSuppressed:{searchSuppressed}";

        Map<String, Object> args = new HashMap<>();
        cypher = CypherUtils.buildAndSetProperties(cypher, entityInput.getProperties(), args);

        String closeEntity = "} ) <-[:TRACKS]-(f) ";
        cypher = cypher + closeEntity;

        String close = " return entity";
        cypher = cypher + close;

        args.put("fortressId", payload.getFortress().getId());

        args.put(Entity.UUID_KEY, e.getMetaKey());
        args.put("callerRef", e.getCallerRef());
        args.put("callerKeyRef", EntityHelper.parseKey(payload.getFortress().getId(), payload.getDocumentType().getId(), e.getCallerRef()));
        args.put("name", e.getName());
        args.put("description", entityInput.getDescription());
        args.put("dateCreated", e.getWhenCreated());
        args.put("lastUpdate", e.getLastUpdate());
        args.put("event", e.getEvent());
        //if ( e.getFortressDateCreated() !=null) {
        args.put("fortressCreate", e.getFortressCreate());
        args.put("fortressLastWhen", e.getFortressLastWhen());
        //}
        args.put("searchSuppressed", entityInput.isSearchSuppressed());
        Result result = database.execute(cypher, args);
        Node entityNode = (Node) result.next().get("entity");
        result.close();

        if (payload.getCreatedBy() != null) {
            Node fu = database.getNodeById(payload.getCreatedBy().getId());
            // Move in to main create statement
            entityNode.createRelationshipTo(fu, CREATED_BY);
            entityNode.createRelationshipTo(fu, LASTCHANGED_BY);
        }

        logger.debug("Creating {}", entityNode);

        return entityNode;

    }

    private Node findByMetaKey(String metaKey) {

        return database.findNode(ENTITY_LABEL, Entity.UUID_KEY, metaKey);
    }

    private Node findByCallerRef(Fortress fortress, DocumentType documentType, String callerRef) {
        if (logger.isTraceEnabled())
            logger.trace("findByCallerRef fortressUser [" + fortress + "] docType[" + documentType + "], callerRef[" + callerRef + "]");

        String keyToFind = EntityHelper.parseKey(fortress.getId(), documentType.getId(), callerRef);
        return database.findNode(ENTITY_LABEL, "callerKeyRef", keyToFind);

    }
    public Entity findEntity(Long entityId) {
        return findEntity("id", entityId);
    }
    /**
     * returns a consistent view of an entity by locating via a property
     *
     * @param column entity property to search b y
     * @param key    value to look for
     * @return
     */
    public Entity findEntity(String column, Object key) {
        String cypher;
        if ( column.equals("id"))
            cypher = "match (e:Entity)-[:TRACKS]-(f:Fortress)-[:OWNS]-(c:FDCompany) where id(e)={arg}  return e,f,c";
        else
            cypher = "match (e:Entity)-[:TRACKS]-(f:Fortress)-[:OWNS]-(c:FDCompany) where e." + column + "={arg}  return e,f,c";
        Map<String, Object> params = new HashMap<>();
        params.put("arg", key);
        Result dbResults = database.execute(cypher, params);
        if (dbResults.hasNext()) {
            Map<String, Object> row = dbResults.next();
            Node fortress = (Node) row.get("f");
            Node company = (Node) row.get("c");

            Fortress f = new Fortress(fortress, company);

            Node entityNode = (Node) row.get("e");
            Entity result = new Entity(f, entityNode);
            org.neo4j.graphdb.Relationship fur = entityNode.getSingleRelationship(CREATED_BY, Direction.OUTGOING);
            if (fur != null)
                result.setCreatedBy(new FortressUser(fur.getEndNode()));

            fur = entityNode.getSingleRelationship(LASTCHANGED_BY, Direction.OUTGOING);
            if (fur != null)
                result.setLastUser(new FortressUser(fur.getEndNode()));
            return result;
        }
        return null;
    }

}
