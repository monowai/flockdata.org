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

package org.flockdata.neo4j;

import org.flockdata.helper.FlockException;
import org.flockdata.model.Entity;
import org.flockdata.neo4j.service.EntityService;
import org.flockdata.track.EntityHelper;
import org.flockdata.track.EntityPayload;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityResults;
import org.flockdata.track.bean.TrackResultBean;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.database.CypherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mike on 4/07/15.
 */
@Path("/entity")
public class EntityManager {

    private Logger logger = LoggerFactory.getLogger(EntityManager.class);

    private GraphDatabaseService database;

    EntityService entityService;

    //    EntityManager(){}
//
    public EntityManager(@Context GraphDatabaseService database) {
        this.database = database;
        entityService = new EntityService(database);
    }

    @Bean
    GraphDatabaseService getGraphDatabaseService (){
        return this.database;
    }
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response makeEntity(EntityPayload entityPayload, @Context CypherExecutor cypherExecutor) throws FlockException {
        EntityResults entityResults = new EntityResults();
        assert (entityPayload.getDocumentType() != null);
        assert (entityPayload.getDocumentType().getCode() != null);

        try (Transaction tx = database.beginTx()) {
            if (entityPayload.getEntity() != null) {
                // Update an existing entity

            } else {
                // Process the payload
                for (EntityInputBean entityInputBean : entityPayload.getEntities()) {
                    TrackResultBean result = entityService.createEntity(entityPayload, entityInputBean);
                    logger.trace("Batch Processed {}, callerRef=[{}], documentName=[{}]", result.getEntity().getId(), entityInputBean.getCallerRef(), entityPayload.getDocumentType());
                    entityResults.addResult(result);
                    entityResults.addResult(null);
                }
            }
            tx.success();
        }

        return Response.ok().entity(entityResults).type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{metaKey}")
    public Response findEntity(@PathParam(Entity.UUID_KEY) String metaKey, @Context CypherExecutor cypherExecutor) throws FlockException {
        try (Transaction tx = database.beginTx()) {
            Entity entity = entityService.findEntity("metaKey", metaKey);
            tx.success();
            if (entity == null)
                return Response.noContent().status(Response.Status.NOT_FOUND).build();

            return Response.ok().entity(entity).build();

        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/admin/{fortressId}/{label}/{skipTo}")
    public Response findLabeledEntities(@PathParam("fortressId") Long fortressId, @PathParam("label") String label, @PathParam("skipTo") int skipTo,
                                        @Context CypherExecutor cypherExecutor) throws FlockException {
        try (Transaction tx = database.beginTx()) {
            String cypher = "match (f:Fortress)-[:TRACKS]->(entity:`" + label + "`) where id(f)={fortressUser} return entity ORDER BY entity.dateCreated ASC skip {skip} limit 100 ";
            Map<String, Object> params = new HashMap<>();
            params.put("fortressUser", fortressId);
            params.put("skip", skipTo);
            Collection<Entity> results = new ArrayList<>();

            Result result = database.execute(cypher, params);
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                Node eNode = (Node) row.get("entity");
                Entity entity = new Entity(null, eNode);
                results.add(entity);
            }

            tx.success();
            return Response.ok().entity(results).build();

        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{fortressId}/{docId}/{callerRef}")
    public Response findByCallerRef(@PathParam("fortressId") Long fortressId, @PathParam("docId") Long docId, @PathParam("callerRef") String callerRef,
                                    @Context CypherExecutor cypherExecutor) throws FlockException {
        try (Transaction tx = database.beginTx()) {
            Entity entity = entityService.findEntity("callerKeyRef", EntityHelper.parseKey(fortressId, docId, callerRef));
            tx.success();

            if (entity == null)
                return Response.noContent().status(Response.Status.NOT_FOUND).build();

            return Response.ok().entity(entity).build();

        }
    }
}





