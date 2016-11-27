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

import org.flockdata.helper.FlockDataTagException;
import org.flockdata.neo4j.service.TagService;
import org.flockdata.authentication.registration.bean.TagInputBean;
import org.flockdata.authentication.registration.bean.TagResultBean;
import org.flockdata.track.TagPayload;
import org.flockdata.track.bean.AliasPayload;
import org.flockdata.track.bean.TagResults;
import org.neo4j.graphdb.DynamicLabel;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * Serverside Tag routines to support FlockData
 * <p>
 * @author mholdsworth
 * @since 18/06/2015
 */
@Path("/tag")
public class TagManager {

    private TagService tagService ;

    private Logger logger = LoggerFactory.getLogger(TagManager.class);
    private final GraphDatabaseService database;

    public TagManager(@Context GraphDatabaseService database) {
        this.database = database;
        tagService = new TagService(database);
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response makeTags(TagPayload tagPayload, @Context CypherExecutor cypherExecutor) throws FlockDataTagException {
        TagResults tagResults = new TagResults();
        Collection<String> createdValues = new ArrayList<>();

        try {
            tagService.ensureConstraints(tagPayload.getTags()).get();
        } catch (InterruptedException | ExecutionException e ) {
            logger.error("Error ensuring constraints", e);
            throw new FlockDataTagException("Error ensuring unique constraints");
        }

        try (Transaction tx = database.beginTx()) {
            for (TagInputBean tagInputBean : tagPayload.getTags()) {
                Node node = tagService.save(tagInputBean, tagPayload.getTenant(), createdValues);
                TagResultBean resultBean = new TagResultBean(tagInputBean, node);
                tagResults.addTag(resultBean);

            }
            tx.success();
        }

        return Response.ok().entity(tagResults).type(MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/alias")
    public Response makeAlias(AliasPayload aliasPayload, @Context CypherExecutor cypherExecutor) throws FlockDataTagException {

        try (Transaction tx = database.beginTx()) {
            Node existing = database.findNode(DynamicLabel.label(aliasPayload.getLabel()), "key", aliasPayload.getAliasInput().getCode());
            if ( existing == null ){
                // create it
                aliasPayload = tagService.makeAlias(aliasPayload.getLabel() + "Alias", aliasPayload.getTagId(), aliasPayload.getAliasInput());
            }
            tx.success();
        }

        return Response.ok().entity(aliasPayload).type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{label}/{code}")
    public Response findTag( @PathParam("label") String label, @PathParam("code") String code) {
        String suffix = "";
        Response result;
        try (Transaction tx = database.beginTx()) {
            Node node = tagService.findTagNode(suffix, label, code);
            tx.success();

            if ( node !=null ) {
                TagResultBean resultBean = new TagResultBean(node);
                result= Response.ok().entity(resultBean).build();
            } else {
                TagResultBean resultBean = new TagResultBean();
                result= Response.status(Response.Status.NO_CONTENT).entity(resultBean).build();
            }

            return result;

        }

    }


}