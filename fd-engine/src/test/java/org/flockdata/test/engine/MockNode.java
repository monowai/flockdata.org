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

package org.flockdata.test.engine;

import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Traverser;

/**
 * @author mholdsworth
 * @since 16/08/2016
 */
public class MockNode implements Node {
  long id;
  Map<String, Object> properties = new HashMap();

  public MockNode(int id) {
    this.id = id;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public void delete() {

  }

  @Override
  public Iterable<Relationship> getRelationships() {
    return null;
  }

  @Override
  public boolean hasRelationship() {
    return false;
  }

  @Override
  public Iterable<Relationship> getRelationships(RelationshipType... types) {
    return null;
  }

  @Override
  public Iterable<Relationship> getRelationships(Direction direction, RelationshipType... types) {
    return null;
  }

  @Override
  public boolean hasRelationship(RelationshipType... types) {
    return false;
  }

  @Override
  public boolean hasRelationship(Direction direction, RelationshipType... types) {
    return false;
  }

  @Override
  public Iterable<Relationship> getRelationships(Direction dir) {
    return null;
  }

  @Override
  public boolean hasRelationship(Direction dir) {
    return false;
  }

  @Override
  public Iterable<Relationship> getRelationships(RelationshipType type, Direction dir) {
    return null;
  }

  @Override
  public boolean hasRelationship(RelationshipType type, Direction dir) {
    return false;
  }

  @Override
  public Relationship getSingleRelationship(RelationshipType type, Direction dir) {
    return null;
  }

  @Override
  public Relationship createRelationshipTo(Node otherNode, RelationshipType type) {
    return null;
  }

  @Override
  public Iterable<RelationshipType> getRelationshipTypes() {
    return null;
  }

  @Override
  public int getDegree() {
    return 0;
  }

  @Override
  public int getDegree(RelationshipType type) {
    return 0;
  }

  @Override
  public int getDegree(Direction direction) {
    return 0;
  }

  @Override
  public int getDegree(RelationshipType type, Direction direction) {
    return 0;
  }

  @Override
  public Traverser traverse(Traverser.Order traversalOrder, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, RelationshipType relationshipType, Direction direction) {
    return null;
  }

  @Override
  public Traverser traverse(Traverser.Order traversalOrder, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, RelationshipType firstRelationshipType, Direction firstDirection, RelationshipType secondRelationshipType, Direction secondDirection) {
    return null;
  }

  @Override
  public Traverser traverse(Traverser.Order traversalOrder, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, Object... relationshipTypesAndDirections) {
    return null;
  }

  @Override
  public void addLabel(Label label) {

  }

  @Override
  public void removeLabel(Label label) {

  }

  @Override
  public boolean hasLabel(Label label) {
    return false;
  }

  @Override
  public Iterable<Label> getLabels() {
    return null;
  }

  @Override
  public GraphDatabaseService getGraphDatabase() {
    return null;
  }

  @Override
  public boolean hasProperty(String key) {
    return properties.containsKey(key);
  }

  @Override
  public Object getProperty(String key) {
    return properties.get(key);
  }

  @Override
  public Object getProperty(String key, Object defaultValue) {
    return null;
  }

  @Override
  public void setProperty(String key, Object value) {
    properties.put(key, value);
  }

  @Override
  public Object removeProperty(String key) {
    return properties.remove(key);
  }

  @Override
  public Iterable<String> getPropertyKeys() {
    return properties.keySet();
  }

  @Override
  public Map<String, Object> getProperties(String... keys) {
    return null;
  }

  @Override
  public Map<String, Object> getAllProperties() {
    return properties;
  }
}
