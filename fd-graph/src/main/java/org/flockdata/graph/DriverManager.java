package org.flockdata.graph;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * @author mikeh
 * @since 10/06/18
 */
@Service
@Configuration
@Order(99)
public class DriverManager implements NeoDriver {
  private Driver driver;


  @Autowired
  public void setDriverManager(
      @Value("${org.flockdata.neo4j.uri:bolt://localhost:7687}") String uri,
      @Value("${org.flockdata.neo4j.uesr:neo4j}") String user,
      @Value("${org.flockdata.neo4j.password:password}") String password) {
    driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
  }


  @Override
  public void close() {
    if (driver != null) {
      driver.close();
    }
  }

  public Session session() {
    return driver.session();
  }
}
