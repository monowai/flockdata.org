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

package org.flockdata.test.engine;

/**
 * in-memory test database for Neo4j testing
 *
 * Created by mike on 31/03/15.
 */

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@EnableTransactionManagement
@EnableNeo4jRepositories(basePackages = {"org.flockdata.company.dao",
                                         "org.flockdata.geography.dao",
                                         "org.flockdata.engine.*",
                                         "org.flockdata.model.*"})
@Configuration
@Profile({"dev"})
public class Neo4jConfigTest extends Neo4jConfiguration {

    private Logger logger = LoggerFactory.getLogger("configuration");

    String getNeoStoreDir(){
            return "./target/data/" + System.currentTimeMillis();
    }

    @PostConstruct
    public void logFdNeoConfig() {
        logger.info("**** Neo4j Test configuration deployed");
    }

    GraphDatabaseService gds = null;
    @Bean
    public GraphDatabaseService graphDatabaseService() {
        if ( gds == null ) {
            setBasePackage("org.flockdata.*");
            gds = graphDatabaseFactory().newEmbeddedDatabase(getNeoStoreDir());
        }
        return gds;
    }

    @Bean
    GraphDatabaseFactory graphDatabaseFactory () {
        return new TestGraphDatabaseFactory();
    }


    Map<String, String> dbProperties() {
        Map<String, String> props = new HashMap<>();
        return props;
    }


}
