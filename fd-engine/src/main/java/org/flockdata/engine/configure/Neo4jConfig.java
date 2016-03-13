/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.engine.configure;

/**
 * Neo4j embedded database
 * <p>
 * Created by mike on 31/03/15.
 */

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
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

@EnableTransactionManagement
@EnableNeo4jRepositories(basePackages = { "org.flockdata.company.dao",
                                          "org.flockdata.geography.dao",
                                          "org.flockdata.engine.*"
                                          })
@Configuration
@Profile({"integration", "production"})
public class Neo4jConfig extends Neo4jConfiguration {

    private Logger logger = LoggerFactory.getLogger("configuration");
    private String configFile;
    private String dbPath;


    @PostConstruct
    public void logFdNeoConfig() {
        logger.info("**** Neo4j configuration deployed from config [{}]", configFile);
        logger.info("**** Neo4j datafiles are being written to [{}]", dbPath);
    }

    @Bean
    public GraphDatabaseService graphDatabaseService(@Value("${org.neo4j.server.webserver.port:7474}") Integer port,
                                                     @Value("${org.neo4j.server.webserver.address:disable}") String address,
                                                     @Value("${org.neo4j.auth:true}") Boolean enableSecurity,
                                                     @Value("${org.neo4j.path:.}") String props,
                                                     @Value("${org.neo4j.server.database.location:data/neo4j}") String dbPath) {
        try {
            logger.info("**** Neo4j configuration deploying from config [{}]", configFile);
            logger.info("**** Neo4j datafiles [{}]", dbPath);
            logger.info ("**** Neo4j url [{}] port [{}]", address, port );

            configFile = props + "/neo4j.properties";
            this.dbPath = dbPath;
            setBasePackage("org.flockdata.model");
            GraphDatabaseAPI graphdb = (GraphDatabaseAPI) new GraphDatabaseFactory()
                    .newEmbeddedDatabaseBuilder(dbPath)
                    .loadPropertiesFromFile(configFile)
                    .newGraphDatabase();
            if ( port >0 ) {
                ServerConfigurator config = new ServerConfigurator(graphdb);
                config.configuration().setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, port);
                if ( !address.equals("disable"))
                    config.configuration().setProperty(Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY, address );
                config.configuration().setProperty("dbms.security.auth_enabled", enableSecurity);
                new WrappingNeoServerBootstrapper(graphdb, config).start();
            }
            return graphdb;
        } catch (Exception fileNotFoundException) {
            logger.error("!!! Error initialising Neo4j from [" + configFile + "]. Path can be set via org.neo4j.path=");
            throw fileNotFoundException;
        }
    }


}
