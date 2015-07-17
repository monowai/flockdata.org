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

import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Created by mike on 23/06/15.
 */
@Path("/schema")
public class SchemaManager {
    private final GraphDatabaseService database;

    private Logger logger = LoggerFactory.getLogger(SchemaManager.class);

    public SchemaManager(@Context GraphDatabaseService database) {
        this.database = database;
    }

    @POST
    public Response ensureSystemSchema() {
        database.execute("create constraint on (t:Country) assert t.key is unique");
        database.execute("create constraint on (t:CountryAlias) assert t.key is unique");
        database.execute("create constraint on (t:State) assert t.key is unique");
        database.execute("create constraint on (t:StateAlias) assert t.key is unique");
        // ToDo: Create a city node. The key should be country.{state}.city
        //template.query("create constraint on (t:City) assert t.key is unique", null);
        logger.debug("Created system constraints");
        return Response.ok().build();
    }

}

