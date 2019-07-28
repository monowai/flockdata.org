package org.flockdata.graph.dao;

import static org.neo4j.driver.v1.Values.parameters;

import org.flockdata.data.Entity;
import org.flockdata.graph.DriverManager;
import org.flockdata.graph.model.EntityNode;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author mikeh
 * @since 10/06/18
 */
@Service
public class EntityRepo {

  private DriverManager driverManager;

  private static EntityNode createEntityNode(Transaction tx, Entity entity) {
    StatementResult statementResult = tx.run(
        "MERGE (entity:Entity:" + entity.getType() + " {name: $name, code: $code, key: $key}) return entity",
        parameters(
            "name", entity.getName(),
            "code", entity.getCode(),
            "key", entity.getKey()));
    Node eNode = statementResult.single().get("entity").asNode();
    return EntityNode.build(eNode);
  }

  private static Entity findByKey(Transaction tx, String key) {
    String cmd = "match (entity:Entity {key: $key}) return entity";
    StatementResult statementResult = tx.run(cmd, parameters("key", key));
    Node eNode = statementResult.single().get("entity").asNode();
    return EntityNode.build(eNode);

  }

  private static Entity findByTypeKey(Transaction tx, String key) {
    String cmd = "match (entity:Entity {key: $key}) return entity";
    StatementResult statementResult = tx.run(cmd, parameters("key", key));
    Node eNode = statementResult.single().get("entity").asNode();
    return EntityNode.build(eNode);

  }

  @Autowired
  void setDriverManager(DriverManager driverManager) {
    this.driverManager = driverManager;
  }

  public EntityNode create(Entity entity) {
    try (Session session = driverManager.session()) {
      return session.writeTransaction(tx -> createEntityNode(tx, entity));
    }
  }

  public Entity findByKey(Entity entity) {
    try (Session session = driverManager.session()) {
      return session.readTransaction(tx -> findByKey(tx, entity.getKey()));
    }
  }

}
