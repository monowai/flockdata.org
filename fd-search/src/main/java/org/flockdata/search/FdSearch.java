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

package org.flockdata.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

/**
 * Preparatory work for Spring Boot. Before we can embrace this we need to do a few things
 *  ToDo: Logging looks odd
 *  ToDo: Servlet specification
 *  ToDo: Figure out server port numbers - couldn't ping
 *  ToDo: How to run debug mode -- complains that web-inf resources are unavailable
 *
 *  Get fd-search working first. fd-engine has neo4j embedded rest interface that might cause problems
 *
 * User: mike
 * Date: 16/12/14
 * Time: 7:36 AM
 */
@SpringBootApplication
@PropertySource(value = "classpath:/application.properties,file:${fd.config},file:${fd.auth.config}", ignoreResourceNotFound = true)
public class FdSearch {
    public static void main(String[] args) {
        SpringApplication.run(FdSearch.class, args);

    }
}
