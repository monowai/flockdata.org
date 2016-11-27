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

import org.flockdata.helper.VersionHelper;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @author mholdsworth
 * @since 26/06/2015
 */
@Path("/")
public class PingManager {

    @GET
    public String ping () {
        return "pong";// + " " + VersionHelper.getFdVersion();
    }


    @GET
    @Path("/health")
    public String health () {
        return "fd-neo-extensions - " + VersionHelper.getFdVersion();
    }

}
