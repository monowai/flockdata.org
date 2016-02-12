package org.flockdata.test.engine.functional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for Web App context driven classes
 * Created by mike on 12/02/16.
 */
@WebAppConfiguration
public class WacBase extends EngineBase{
    @Autowired
    public WebApplicationContext wac;


    static {
        // ToDo: Sort this out. The WAC creates a new context which deploys a new Neo4j DB without the previous
        // one shutting down. So here we give it it's dedicated work area
        System.setProperty("neo4j.datastore", "./target/data/wac/neo/");
    }
}
