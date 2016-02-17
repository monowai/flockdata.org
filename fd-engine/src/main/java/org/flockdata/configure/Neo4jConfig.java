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

package org.flockdata.configure;

/**
 * Created by mike on 31/03/15.
 */

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
                                         "org.flockdata.engine.tag.dao",
                                         "org.flockdata.engine.dao",
                                         "org.flockdata.geography.dao",
                                         "org.flockdata.meta.dao",
                                         "org.flockdata.model.*"})
@Configuration
@Profile({"integration","production"})
public class Neo4jConfig extends Neo4jConfiguration {

    private Logger logger = LoggerFactory.getLogger("configuration");

    // ToDo: The bean is initialized before the storeDir property is set
    @Value("${neo4j.datastore}")
    String neoStoreDir;

    String getNeoStoreDir(){
        if ( neoStoreDir == null ) {
            String systemProperty= System.getProperty("neo4j.datastore");
            if ( systemProperty== null )
                systemProperty = "./data/neo4j/fd";
            return systemProperty;
        }
        return neoStoreDir;
    }

    @PostConstruct
    public void logFdNeoConfig() {
        logger.info("**** Neo4j configuration deployed");
    }

    GraphDatabaseService gds = null;
    @Bean
    public GraphDatabaseService graphDatabaseService() {
        if ( gds == null ) {
            setBasePackage("org.flockdata.*");
            gds = new GraphDatabaseFactory().newEmbeddedDatabase(getNeoStoreDir());
        }
        return gds;
    }

    Map<String, String> dbProperties() {
        Map<String, String> props = new HashMap<>();
        return props;
    }


}
