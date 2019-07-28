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

package org.flockdata.engine.configure;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.GraphDatabaseAPI;
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

/**
 * @tag Neo4j, Configuration
 */
@EnableTransactionManagement
@EnableNeo4jRepositories(basePackages = {"org.flockdata.company.dao",
    "org.flockdata.geography.dao",
    "org.flockdata.engine.*"
})
@Configuration
@Profile( {"fd-server"})
public class Neo4jConfig extends Neo4jConfiguration {

  private Logger logger = LoggerFactory.getLogger("configuration");
  private String configFile;
  private String dbPath;


  @Bean
  public GraphDatabaseService graphDatabaseService(@Value("${org.neo4j.server.webserver.port:7474}") Integer port,
                                                   @Value("${org.neo4j.server.webserver.address:disable}") String address,
                                                   @Value("${org.neo4j.auth:true}") Boolean enableSecurity,
                                                   @Value("${org.neo4j.dbms.pagecache.memory:@null}") String pageCache,
                                                   @Value("${org.neo4j.path:.}") String props,
                                                   @Value("${org.neo4j.server.database.location:data/neo4j}") String dbPath) {
    try {
      configFile = props + "/neo4j.properties";
      logger.info("**** Neo4j configuration deploying from path [{}]", configFile);
      logger.info("**** Neo4j datafiles will be written to [{}]", dbPath);

      this.dbPath = dbPath;
      setBasePackage("org.flockdata.engine.data.graph");
      if (pageCache != null && pageCache.equals("@null")) {
        pageCache = null;
      }
      GraphDatabaseBuilder graphdbBuilder = new GraphDatabaseFactory()
          .newEmbeddedDatabaseBuilder(dbPath)
          .loadPropertiesFromFile(configFile);

      if (pageCache != null) {
        graphdbBuilder.setConfig("dbms.pagecache.memory", pageCache);
      }

      GraphDatabaseAPI graphdb = (GraphDatabaseAPI) graphdbBuilder
          .newGraphDatabase();
      if (port > 0) {
        logger.info("**** Neo4j browser enabled at url [{}] port [{}]", address, port);
        ServerConfigurator config = new ServerConfigurator(graphdb);
        config.configuration().setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, port);
        if (!address.equals("disable")) {
          config.configuration().setProperty(Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY, address);
        }
        config.configuration().setProperty("dbms.security.auth_enabled", enableSecurity);
        new WrappingCommunityNeoServer(graphdb, config).start();
      } else {
        logger.info("**** Disabling the neo4j browser ");
      }
      return graphdb;
    } catch (Exception fileNotFoundException) {
      logger.error("!!! Error initialising Neo4j from [" + configFile + "]. Path can be set via org.neo4j.path=");
      throw fileNotFoundException;
    }
  }


}
