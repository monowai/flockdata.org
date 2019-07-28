package org.flockdata.test.neo.integ;

import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

/**
 * @author mikeh
 * @since 15/06/18
 */
public class BaseNeo {
  GraphDatabaseSettings.BoltConnector bolt = GraphDatabaseSettings.boltConnector("0");

  File DB_PATH = new File("/tmp");
  GraphDatabaseService graphDb = new GraphDatabaseFactory()
      .newEmbeddedDatabaseBuilder(DB_PATH)
      .setConfig(bolt.type, "BOLT")
      .setConfig(bolt.enabled, "true")
      .setConfig(bolt.address, "localhost:7687")
      .newGraphDatabase();
  ;

  @After
  public void shutDown() {
    if (graphDb != null) {
      graphDb.shutdown();
    }
  }

  @Before
  public void purgeDb() {
    graphDb.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r");
  }

}
