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
import org.flockdata.model.EntityTag;
import org.flockdata.neo4j.service.EntityTagService;
import org.flockdata.neo4j.service.TagService;
import org.flockdata.track.EntityTagPayload;
import org.flockdata.track.bean.EntityTagInputBean;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.database.CypherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author mholdsworth
 * @since 15/07/2015
 */
@Path("/entitytag")
public class EntityTagManager {
    private Logger logger = LoggerFactory.getLogger(EntityTagManager.class);

    private EntityTagService entityTagService;

    private TagService tagService ;

    private GraphDatabaseService database;

    public EntityTagManager(@Context GraphDatabaseService database) {
        this.database = database;
        entityTagService = new EntityTagService(database);
        tagService = new TagService(database);

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addTag(EntityTagPayload entityTagPayload, @Context CypherExecutor cypherExecutor) throws FlockException {

        try (Transaction tx = database.beginTx()) {
            // Process the payload
            Node entity = database.getNodeById(entityTagPayload.getEntityId());
            entityTagService.addTags(entity, entityTagPayload);
            tx.success();
        }

        return Response.ok().entity("Ok").type(MediaType.APPLICATION_JSON).build();
    }
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{entityId}")
    public Response getEntityTags(@PathParam("entityId") Long entityId,
                                  @Context CypherExecutor cypherExecutor) throws FlockException {

        Transaction tx = null;
        try  {
            tx = database.beginTx();
            // Process the payload
            //Node entity = database.getNodeById(entityId);
            if ( entityId != null ){
                return Response.ok().entity(entityTagService.getEntityTags(entityId)).type(MediaType.APPLICATION_JSON).build();
            } else {
                return Response.noContent().status(Response.Status.NOT_FOUND).build();
            }

        } finally{
            if (tx != null)
                tx.success();
        }


    }
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{entityId}/{tagType}/{tagCode}/{relationshipType}")
    public Response getEntityTag(@PathParam("entityId") Long entityId,
                                 @PathParam("tagType") String tagType,
                                 @PathParam("tagCode") String tagCode,
                                 @PathParam("relationshipType") String relationship, @Context CypherExecutor cypherExecutor) throws FlockException {

        // "/{entityId}/{tagType}/{tagCode}/{relationshipType}}";
        Transaction tx = null;
        try  {
            tx = database.beginTx();
            EntityTag entityTag ;
            // Process the payload
            Node entity = database.getNodeById(entityId);
            Node tag = tagService.findTagNode("", tagType, tagCode);
            if ( entity != null &&  tag != null ){
                EntityTagInputBean params = new EntityTagInputBean(null, tagCode, relationship);
                params.setIndex(tagType);
                entityTag = entityTagService.getEntityTag(entity, tag, params);
                return Response.ok().entity(entityTag).type(MediaType.APPLICATION_JSON).build();
            } else {
                return Response.noContent().status(Response.Status.NOT_FOUND).build();
            }

        } finally{
            if (tx != null)
                tx.success();
        }


    }

}
