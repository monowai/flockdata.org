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

package org.flockdata.engine.data.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.transaction.HeuristicRollbackException;
import org.flockdata.data.Company;
import org.flockdata.data.Concept;
import org.flockdata.data.Document;
import org.flockdata.data.Fortress;
import org.flockdata.data.Segment;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.ConceptNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.matrix.EdgeResult;
import org.flockdata.engine.matrix.EdgeResults;
import org.flockdata.engine.matrix.FdNode;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.helper.NotFoundException;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.TagResultBean;
import org.flockdata.track.bean.ConceptInputBean;
import org.flockdata.track.bean.ConceptResultBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.FdTagResultBean;
import org.flockdata.track.bean.RelationshipResultBean;
import org.neo4j.graphalgo.impl.util.PathImpl;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.DeadlockDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import scala.collection.convert.Wrappers;

/**
 * IO routines to handle registration of concepts in Neo4j
 *
 * @author mholdsworth
 * @tag Neo4j, Concept, Tag
 * @since 19/06/2015
 */
@Repository
public class ConceptDaoNeo {
  public static final String PARENT = "parent";
  private final ConceptTypeRepo conceptTypeRepo;

  private final DocumentTypeRepo documentTypeRepo;

  private final Neo4jTemplate template;

  private Logger logger = LoggerFactory.getLogger(ConceptDaoNeo.class);

  @Autowired
  public ConceptDaoNeo(DocumentTypeRepo documentTypeRepo, ConceptTypeRepo conceptTypeRepo, Neo4jTemplate template) {
    this.documentTypeRepo = documentTypeRepo;
    this.conceptTypeRepo = conceptTypeRepo;
    this.template = template;
  }

  public boolean linkEntities(DocumentNode fromDoc, DocumentNode toDoc, EntityKeyBean entityKeyBean) {
    Node from = template.getNode(fromDoc.getId());
    Node to = template.getNode(toDoc.getId());
    if (!relationshipExists(from, to, entityKeyBean.getRelationshipName())) {
      Map<String, Object> props = new HashMap<>();
      Direction direction = Direction.BOTH; // Association
      props.put(ConceptDaoNeo.PARENT, entityKeyBean.isParent());
      if (entityKeyBean.isParent()) {
        direction = Direction.OUTGOING; // Point to the parent
      }
      template.getOrCreateRelationship(from, to, DynamicRelationshipType.withName(entityKeyBean.getRelationshipName()), direction, props);
      return true; // Link created
    }
    return false;
  }

  private boolean linkDocToConcept(Document fromDoc, ConceptNode toConcept, String relationship) {
    Node from = template.getNode(fromDoc.getId());
    Node to = template.getNode(toConcept.getId());
    return linkNodesWithRelationship(from, to, relationship);
  }

  private boolean linkConceptToConcept(ConceptNode fromConcept, ConceptNode toConcept, String relationship) {
    Node from = template.getNode(fromConcept.getId());
    Node to = template.getNode(toConcept.getId());
    return linkNodesWithRelationship(from, to, relationship);
  }

  private boolean linkNodesWithRelationship(Node from, Node to, String relationship) {
    if (!relationshipExists(from, to, relationship)) {
      template.getOrCreateRelationship(from, to, DynamicRelationshipType.withName(relationship), Direction.OUTGOING, null);
      return true; // Link created
    }
    return false; // Link already existed
  }

  private boolean relationshipExists(Node from, Node to, String relationship) {
    assert relationship != null;
    return template.getRelationshipBetween(from, to, relationship) != null;
  }

  public void registerConcepts(Map<Document, ArrayList<ConceptInputBean>> documentConcepts) {
    logger.trace("Registering concepts");

    for (Document docType : documentConcepts.keySet()) {
      logger.trace("Looking for existing concepts {}", docType.getName());

//            Collection<Concept> concepts = docType.getConcepts();
//            logger.trace("[{}] - Found {} existing concepts", docType.getName(), docType.getConcepts().size());
      for (ConceptInputBean conceptInput : documentConcepts.get(docType)) {
        ConceptNode concept = conceptTypeRepo.findBySchemaPropertyValue("key", Concept.toKey(conceptInput));
        for (String relationship : conceptInput.getRelationships()) {
          if (concept == null) {
            logger.debug("No existing conceptInput found for [{}]. Creating it", relationship);
            concept = template.save(new ConceptNode(conceptInput));
          }
          linkDocToConcept(docType, concept, relationship);

        }
      }
    }
  }

  /**
   * Tracks the DocumentTypes used by a Fortress that can be used to find Entities
   *
   * @param fortress        fortress generating
   * @param docType         name of the Label
   * @param createIfMissing if not found will create
   * @return the node
   */
  @Cacheable(value = "documentType", unless = "#result==null")
  public DocumentNode findDocumentType(Fortress fortress, String docType, Boolean createIfMissing) {
    DocumentNode docResult = documentExists(fortress, docType);

    if ((docResult == null && createIfMissing) && (!docType.equalsIgnoreCase("entity"))) {
      docResult = documentTypeRepo.save(new DocumentNode(fortress, docType));
    }

    return docResult;
  }

  /**
   * Tracks the DocumentTypes used by a Fortress that can be used to find Entities
   *
   * @param fortress        fortress generating
   * @param documentType    prototype
   * @param createIfMissing if not found will create
   * @return the node
   */
  public DocumentNode findDocumentType(Fortress fortress, DocumentNode documentType, Boolean createIfMissing) {
    DocumentNode docResult = documentExists(fortress, documentType.getName());

    if (docResult == null && createIfMissing) {
      //documentType.setFortress(fortress); // fortress was set by the caller
      docResult = documentTypeRepo.save(documentType);
    }

    return docResult;
  }

  private DocumentNode documentExists(Fortress fortress, String docType) {
    assert fortress != null;
    logger.debug("looking for document {}, fortress {}", docType, fortress);
    String docKey = DocumentNode.toKey(fortress, docType);
    return documentTypeRepo.findFortressDocCode(docKey);
  }

  // Query Routines

  public Set<DocumentResultBean> findConcepts(Company company, Collection<String> docNames) {
    Set<DocumentResultBean> documentResults = new HashSet<>();
    Set<DocumentNode> documents;
    if (docNames == null) {
      documents = documentTypeRepo.findAllDocuments(company);
    } else {
      documents = documentTypeRepo.findDocuments(company, docNames);
    }

    for (DocumentNode document : documents) {

      DocumentResultBean documentResult = new DocumentResultBean(document);
      documentResults.add(documentResult);
      String query = "match (d:DocType)-[r]-(c:Concept) where id(d)={0} return type(r) as rType, c.name as name";
      Map<String, Object> params = new HashMap<>();
      params.put("0", document.getId());
      Result<Map<String, Object>> queryResults = template.query(query, params);
      Iterator<Map<String, Object>> rows = queryResults.iterator();
      Map<String, ConceptResultBean> concepts = new HashMap<>();
      while (rows.hasNext()) {
        Map<String, Object> row = rows.next();
        String relationship = (String) row.get("rType");
        String name = (String) row.get("name");
        ConceptResultBean conceptResult = concepts.get(name);
        if (conceptResult == null) {
          conceptResult = new ConceptResultBean(name);
          concepts.put(name, conceptResult);
          documentResult.add(conceptResult);
        }
        conceptResult.addRelationship(new RelationshipResultBean(relationship));
      }


    }
    return documentResults;
  }

  public Collection<DocumentNode> getFortressDocumentsInUse(Fortress fortress) {
    return documentTypeRepo.getFortressDocumentsInUse(fortress.getId());
  }

  public Collection<DocumentNode> getCompanyDocumentsInUse(Company company) {
    return documentTypeRepo.findAllDocuments(company);
  }

  private ConceptNode schemaTagDefExists(CompanyNode company, ConceptInputBean conceptInputBean) {

    return documentTypeRepo.schemaTagDefExists(company.getId(), Concept.toKey(conceptInputBean));
  }

  /**
   * The general Schema is tracked to understand the general structure
   *
   * @param company       who owns the tags
   * @param tagResultBean Internal flockdata payload result class
   * @return true if it was created for the first time
   */
  @Async
  @Transactional
  @Retryable(include = {HeuristicRollbackException.class, DataRetrievalFailureException.class, InvalidDataAccessResourceUsageException.class, ConcurrencyFailureException.class, DeadlockDetectedException.class}, maxAttempts = 20,
      backoff = @Backoff(maxDelay = 200, multiplier = 5, random = true))
  public ConceptNode registerTag(CompanyNode company, FdTagResultBean tagResultBean) {
    ConceptNode source;
    TagHelper.isDefault(tagResultBean.getTag());
    if (tagResultBean.isNewTag() && !TagHelper.isDefault(tagResultBean.getTag())) {

      ConceptInputBean conceptInputBean = new ConceptInputBean(tagResultBean);
      source = schemaTagDefExists(company, conceptInputBean);
      if (source == null) {
        source = createSchemaTag(conceptInputBean);
      }
      processNestedTags(company, source, tagResultBean.getTargets());
    } else {
      if (tagResultBean.getLabel() != null) {
        source = findConcept(tagResultBean);
      } else {
        source = null;
      }
      processNestedTags(company, source, tagResultBean.getTargets()); // Current tag is not new but sub tags may be
    }
    return source;
  }

  private ConceptNode findConcept(TagResultBean tagResultBean) {
    return conceptTypeRepo.findByLabel(Concept.toKey(new ConceptInputBean(tagResultBean)));
  }

  private void processNestedTags(CompanyNode company, ConceptNode source, Map<FdTagResultBean, Collection<String>> targets) {
    if (targets == null || targets.isEmpty()) {
      return;
    }

    for (FdTagResultBean tagResultBean : targets.keySet()) {
      ConceptNode target = registerTag(company, tagResultBean);
      if (source != null && target != null) {
        Collection<String> rlxs = targets.get(tagResultBean);
        for (String rlx : rlxs) {
          linkConceptToConcept(source, target, rlx);
        }
      }
    }


  }

  //    @Async
  private ConceptNode createSchemaTag(ConceptInputBean conceptInputBean) {
    ConceptNode concept = new ConceptNode(conceptInputBean);
    template.save(concept);
    return concept;
  }

  public DocumentNode save(DocumentNode documentType) {
    return template.save(documentType);
  }

  public void delete(Long documentTypeId) {
    documentTypeRepo.purgeDocumentAssociations(documentTypeId);
    documentTypeRepo.purgeDocumentSegments(documentTypeId);
    documentTypeRepo.delete(documentTypeId);
  }

  public DocumentNode findDocumentTypeWithSegments(DocumentNode documentType) {
    if (documentType == null) {
      throw new NotFoundException("Unable to find the requested DocumentType");
    }
    template.fetch(documentType);
    template.fetch(documentType.getSegments());
    return documentType;
  }

  public DocumentResultBean findDocumentTypeWithSegments(FortressNode f, String doc) {
    DocumentNode documentType = findDocumentType(f, doc, false);
    if (documentType == null) {
      throw new NotFoundException("Failed to find DocumentType " + doc);
    }

    template.fetch(documentType.getSegments());
    return new DocumentResultBean(documentType, documentType.getSegments());
//        result.addSegment(f.getDefaultSegment());
  }

  public void delete(DocumentNode documentType, Segment segment) {
    template.deleteRelationshipBetween(documentType, segment, "USES_SEGMENT");
  }

  public MatrixResults getStructure(FortressNode fortress) {
    String query = "match (f:Fortress)-[]-(d:DocType) where id(f)={fortress} with f,d match (d)-[r*1..2]-(c) where c:DocType or c:Concept return r";
    Map<String, Object> params = new HashMap<>();
    params.put("fortress", fortress.getId());

    Result<Map<String, Object>> results = template.query(query, params);

    Iterator<Map<String, Object>> rows = results.iterator();

    EdgeResults edgeResults = new EdgeResults();
    Collection<FdNode> nodes = new ArrayList<>();
    String filter = "Concept DocType";
    while (rows.hasNext()) {
      Map<String, Object> row = rows.next();
      Relationship relationship = (Relationship) ((Wrappers.SeqWrapper) row.get("r")).get(0);

      FdNode source = new FdNode(relationship.getStartNode());
      FdNode target = new FdNode(relationship.getEndNode());
      if (filter.contains(source.getLabel()) && filter.contains(target.getLabel())) {
        if (!nodes.contains(source)) {
          nodes.add(source);
        }
        if (!nodes.contains(target)) {
          nodes.add(target);
        }

        EdgeResult er = new EdgeResult(target, source, relationship.getType().name());
        for (String key : relationship.getPropertyKeys()) {
          er.addProperty(key, relationship.getProperty(key));
        }
        edgeResults.addResult(er);
      }

    }
    return new MatrixResults(edgeResults).setNodes(nodes);


  }

  public Map<String, DocumentResultBean> getParents(Document documentType) {
    if (documentType == null) {
      return new HashMap<>();
    }
    String query = "match p=shortestPath( (d:DocType)-[*]->(o:DocType)) where id(d) = {start}  return p";
    Map<String, Object> params = new HashMap<>();
    params.put("start", documentType.getId());
    Result<Map<String, Object>> results = template.query(query, params);
    Iterator<Map<String, Object>> rows = results.iterator();
    Map<String, DocumentResultBean> parents = new HashMap<>();
    while (rows.hasNext()) {
      Object o = rows.next().get("p");
      PathImpl path = (PathImpl) o;
      for (Relationship relationship : path.relationships()) {
        boolean parent = false;
        if (relationship.hasProperty(PARENT)) {
          parent = Boolean.parseBoolean(relationship.getProperty(PARENT).toString());
        }
        if (parent) {
          logger.debug("{}-{}-{}", relationship.getStartNode().getId(), relationship.getType().name(), relationship.getEndNode().getId());
          parents.put(relationship.getType().name(), new DocumentResultBean(new DocumentNode(relationship.getEndNode().getProperty("name").toString())));
        }
      }
    }
    return parents;
  }
}
