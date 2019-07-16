/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine;

/**
 * in-memory test database for Neo4j testing
 *
 * @author mholdsworth
 * @since 31/03/2015
 */

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.flockdata.engine.configure.WrappingCommunityNeoServer;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@EnableNeo4jRepositories(basePackages = {"org.flockdata.company.dao",
    "org.flockdata.geography.dao",
    "org.flockdata.engine.*",
})
@Configuration
@Profile( {"dev"})
public class Neo4jConfigTest extends Neo4jConfiguration {

    GraphDatabaseService graphdb = null;
    private Logger logger = LoggerFactory.getLogger("configuration");

    String getNeoStoreDir() {
        return "./target/data/" + System.currentTimeMillis();
    }

    @PostConstruct
    public void logFdNeoConfig() {
        logger.info("**** Neo4j Test configuration deployed");
    }

    @Bean
    public GraphDatabaseService graphDatabaseService(
        @Value("${org.neo4j.server.webserver.port:0}") Integer port) {
        setBasePackage("org.flockdata.engine.data.graph");
        graphdb = graphDatabaseFactory().newEmbeddedDatabase(getNeoStoreDir());
        ServerConfigurator config = new ServerConfigurator((GraphDatabaseAPI) graphdb);
        if (port > 0) {
            config.configuration().setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, port);
            new WrappingCommunityNeoServer((GraphDatabaseAPI) graphdb, config).start();
        }
        return graphdb;
    }

    @Bean
    GraphDatabaseFactory graphDatabaseFactory() {
        return new TestGraphDatabaseFactory();
    }


    Map<String, String> dbProperties() {
        Map<String, String> props = new HashMap<>();
        return props;
    }


}
