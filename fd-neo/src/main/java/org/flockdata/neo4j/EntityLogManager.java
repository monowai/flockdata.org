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
import org.flockdata.model.EntityLog;
import org.flockdata.neo4j.service.EntityService;
import org.flockdata.neo4j.service.LogService;
import org.flockdata.track.EntityLogs;
import org.flockdata.track.bean.LogPayload;
import org.flockdata.track.bean.TrackResultBean;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.database.CypherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

/**
 * Managed FlockData EntityLog and Log access requests
 *
 * Created by mike on 6/07/15.
 */

@Path("/log")
public class EntityLogManager {
    private Logger logger = LoggerFactory.getLogger(EntityLogManager.class);

    private final GraphDatabaseService database;

    private final EntityService entityService;
    private final LogService logService;


    public EntityLogManager(@Context GraphDatabaseService database) {
        this.database = database;
        this.entityService = new EntityService(database);
        this.logService = new LogService(database);
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{entityId}/{logId}")
    @GET
    public Response getLog(@PathParam("entityId") Long entityId, @PathParam("logId") Long logId, @Context CypherExecutor cypherExecutor) throws FlockException {
        EntityLog entityLog;
        try (Transaction tx = database.beginTx()) {


            entityLog = logService.getEntityLog(entityId, logId);
            tx.success();

            if (entityLog == null)
                return Response.status(Response.Status.NOT_FOUND).entity(new EntityLog()).build();

            return Response.ok().entity(entityLog).type(MediaType.APPLICATION_JSON).build();

        }
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{entityId}/last")
    @GET
    public Response latestLog(@PathParam("entityId") Long entityId, @Context CypherExecutor cypherExecutor) throws FlockException {
        EntityLog entityLog;
        try (Transaction tx = database.beginTx()) {

            entityLog = logService.getLastLog(entityId);
            tx.success();

            if (entityLog == null)
                return Response.status(Response.Status.NOT_FOUND).entity(new EntityLog()).build();

            return Response.ok().entity(entityLog).type(MediaType.APPLICATION_JSON).build();

        }
    }


    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{entityId}/before/{time}")
    @GET
    public Response logsSince(@PathParam("entityId") Long entityId, @PathParam("time") Long since, @Context CypherExecutor cypherExecutor) throws FlockException {
        EntityLogs result;
        try (Transaction tx = database.beginTx()) {

            result = logService.getLogs(entityId, since);
            tx.success();

            if (result == null)
                return Response.status(Response.Status.NOT_FOUND).entity(new EntityLog()).build();

            return Response.ok().entity(result).type(MediaType.APPLICATION_JSON).build();

        }
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{entityId}")
    @GET
    public Response getLogs(@PathParam("entityId") Long entityId, @Context CypherExecutor cypherExecutor) throws FlockException {
        EntityLogs entityLogs ;
        try (Transaction tx = database.beginTx()) {
            entityLogs = logService.getLogs(entityId);
            tx.success();

            return Response.ok().entity(entityLogs).type(MediaType.APPLICATION_JSON).build();

        }
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/")
    @DELETE
    public Response cancelLog(Collection<Integer> temp) throws FlockException {
        TrackResultBean trackResult ;
        try (Transaction tx = database.beginTx()) {
            Long entityId = Long.decode(temp.iterator().next().toString());
            Entity entity = entityService.findEntity(entityId);
            trackResult = logService.cancelLastLog(entity);
            tx.success();

            return Response.ok().entity(trackResult).type(MediaType.APPLICATION_JSON).build();

        }
    }

    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response handleLog(LogPayload logPayload, @Context CypherExecutor cypherExecutor) throws FlockException {
        EntityLog entityLog;
        try (Transaction tx = database.beginTx()) {
            entityLog = logService.writeLog(logPayload.getEntity(), logPayload.getLog(), logPayload.getFortressWhen());
            tx.success();
        }

        return Response.ok().entity(entityLog).type(MediaType.APPLICATION_JSON).build();
    }


}
