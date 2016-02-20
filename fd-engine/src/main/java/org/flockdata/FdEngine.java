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

package org.flockdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 *  To use spring-boot you will need to uncomment the maven pom dependencies. The rest of the pom
 *  should be good to go
 *
 *  Get fd-search working first. fd-engine has neo4j embedded rest interface that might cause problems
 *
 * User: mike
 * Date: 15/12/14
 * Time: 3:49 PM
 */
@SpringBootApplication(scanBasePackages = {"org.flockdata"})
public class FdEngine {
    public static void main(String[] args) {
        SpringApplication.run(FdEngine.class, args);
    }

}
