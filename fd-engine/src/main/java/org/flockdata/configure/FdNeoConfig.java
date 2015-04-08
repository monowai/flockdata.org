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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan ("org.flockdata")
@EnableNeo4jRepositories( "org.flockdata")
@EnableTransactionManagement
@EnableRetry
public class FdNeoConfig extends Neo4jConfiguration {

    private final Logger log = LoggerFactory.getLogger(FdNeoConfig.class);

    public FdNeoConfig() {
        super();
        log.info ("********** ");
    }
//    @Bean
//    public Neo4jServer neo4jServer() {
//        return new RemoteServer("http://localhost:7474");
//    }
//
//    @Bean
//    public SessionFactory getSessionFactory() {
//        // with domain entity base package(s)
//        return new SessionFactory("org.flockdata");
//    }
//
//    // needed for session in view in web-applications
//    @Bean
//    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
//    public Session getSession() throws Exception {
//        return super.getSession();
//    }

//    @Bean (name = "neo4jTemplate")
//    public Neo4jTemplate getNeo4jTemplate() throws Exception{
//
//        return new Neo4jTemplate(getSession());
//
//    }
//
//    @Bean (name = "neo4jMappingContent")
//    public Neo4jMappingContext getMappingContext() throws Exception{
//
//        return new Neo4jMappingContext();
//
//    }


}
