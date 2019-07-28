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

package org.flockdata.test.engine.services;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import junit.framework.TestCase;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.matrix.EdgeResult;
import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.helper.JsonUtils;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.ConceptResultBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityKeyBean;
import org.flockdata.track.bean.RelationshipResultBean;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Non transactional tests - these are slower due to cleaning the DB down for each test run
 *
 * @author mholdsworth
 * @since 19/06/2014
 */
public class TestConcepts extends EngineBase {
  private Logger logger = LoggerFactory.getLogger(TestConcepts.class);

  @Override
  public void cleanUpGraph() {
    engineConfig.setTestMode(true);
    super.cleanUpGraph();
  }

  @Test
  public void documentType_InputThroughToDb() throws Exception {
    // DAT-540
    DocumentTypeInputBean documentTypeInputBean = new DocumentTypeInputBean("DTIB")
        .getVersionStrategy(Document.VERSION.DISABLE)
        .setTagStructure(EntityTag.TAG_STRUCTURE.TAXONOMY)
        .setGeoQuery("Testing GeoQuery");

    String json = JsonUtils.toJson(documentTypeInputBean);
    documentTypeInputBean = JsonUtils.toObject(json.getBytes(), DocumentTypeInputBean.class);

    FortressNode fortress = new FortressNode(new FortressInputBean("DocTypes"),
        CompanyNode.builder().name("Testing").build());
    DocumentNode documentType = new DocumentNode(fortress.getDefaultSegment(), documentTypeInputBean);
    TestCase.assertEquals(EntityTag.TAG_STRUCTURE.TAXONOMY, documentType.getTagStructure());
    TestCase.assertEquals(Document.VERSION.DISABLE, documentType.getVersionStrategy());
    TestCase.assertEquals("Testing GeoQuery", documentType.getGeoQuery());
  }

  @Test
  public void multipleDocsSameFortress() throws Exception {
    try {
      logger.debug("### multipleDocsSameFortress");

      setSecurity();
      engineConfig.setConceptsEnabled(true);

      Transaction t = beginManualTransaction();
      SystemUser su = registerSystemUser("multipleDocsSameFortress", mike_admin);
      assertNotNull(su);

      FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("multipleDocsSameFortress", true));
      DocumentNode dType = conceptService.resolveByDocCode(fortress, "ABC123", true);
      commitManualTransaction(t);// Should only be only one docTypes

      assertNotNull(dType);
      Long id = dType.getId();
      dType = conceptService.resolveByDocCode(fortress, "ABC123", false);
      assertEquals(id, dType.getId());

      EntityInputBean input = new EntityInputBean(fortress, "jinks", "DocA", new DateTime());
      input.addTag(new TagInputBean("cust123", "Customer", "purchased").setLabel("Customer"));
      mediationFacade.trackEntity(su.getCompany(), input).getEntity();
      validateConcepts("DocA", su, 1);

      // Different docs, same concepts
      input = new EntityInputBean(fortress, "jinks", "DocB", new DateTime());
      input.addTag(new TagInputBean("cust123", "Customer", "purchased").setLabel("Customer"));
      mediationFacade.trackEntity(su.getCompany(), input).getEntity();

      validateConcepts((Collection<String>) null, su, 3); // 3 Doc types.
      assertEquals("Docs In Use not supporting 'null args' for fortress'", 3, conceptService.getDocumentsInUse(su.getCompany()).size());

      // DAT-112
      Set<DocumentResultBean> found = validateConcepts("DocA", su, 1);
      assertEquals(1, found.size());
      assertEquals(1, found.iterator().next().getConcepts().size());
      found = validateConcepts("DocB", su, 1);
      assertEquals(1, found.size());
      // Removed the mock user
      assertEquals("Didn't find the Document ", 1, found.iterator().next().getConcepts().size());
    } finally {
      cleanUpGraph();
    }


  }

  @Test
  public void multipleFortressesSameTag() throws Exception {
    try {
      logger.debug("### multipleFortressesSameTag");

      setSecurity();
      engineConfig.setConceptsEnabled(true);

      Transaction t = beginManualTransaction();
      SystemUser su = registerSystemUser("multipleFortressesSameTag", mike_admin);
      assertNotNull(su);

      FortressNode fortressA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("multipleFortressesSameTagA", true));
      FortressNode fortressB = fortressService.registerFortress(su.getCompany(), new FortressInputBean("multipleFortressesSameTagB", true));
      commitManualTransaction(t);

      EntityInputBean input = new EntityInputBean(fortressA, "jinks", "DocA", new DateTime());
      input.addTag(new TagInputBean("cust123", "Customer", "purchased").setLabel("Customer"));
      mediationFacade.trackEntity(su.getCompany(), input).getEntity();
      Collection<String> documents = new ArrayList<>();
      documents.add("DocA");
      Set<DocumentResultBean> results = conceptService.findConcepts(su.getCompany(), documents, false);

      assertEquals(1, results.size());

      input = new EntityInputBean(fortressB, "jinks", "DocB", new DateTime())
          .addTag(new TagInputBean("cust123", "Customer", "purchased")
              .setLabel("Customer"));

      mediationFacade.trackEntity(su.getCompany(), input).getEntity();
      documents.add("DocB");
      results = conceptService.findConcepts(su.getCompany(), documents, false);
      assertEquals(2, results.size());
      adminService.purge(fortressB);
      waitAWhile();// Previous call is Async
      results = conceptService.findConcepts(su.getCompany(), documents, false);
      assertEquals(1, results.size());
      Collection<DocumentResultBean> docsInUse = conceptService.getDocumentsInUse(su.getCompany());
      assertEquals(1, docsInUse.size());

    } finally {
      cleanUpGraph();
    }


  }

  @Test
  public void fortressConcepts() throws Exception {
    try {
      logger.debug("### fortressConcepts");

      Transaction t = beginManualTransaction();
      setSecurity();
      SystemUser su = registerSystemUser("fortressConcepts", mike_admin);
      assertNotNull(su);
      engineConfig.setConceptsEnabled(true);
      engineConfig.setTestMode(true);

      FortressNode fortA = fortressService.registerFortress(su.getCompany(), new FortressInputBean("fortressConcepts", true));

      DocumentNode dType = conceptService.resolveByDocCode(fortA, "ABC123", true);
      commitManualTransaction(t);// Should only be only one docTypes

      assertNotNull(dType);
      Long id = dType.getId();
      dType = conceptService.resolveByDocCode(fortA, "ABC123", false);
      assertEquals(id, dType.getId());

      EntityInputBean input = new EntityInputBean(fortA, "jinks", "DocA", new DateTime());
      input.addTag(new TagInputBean("cust123", "Customer", "purchased"));
      Entity meta = mediationFacade.trackEntity(su.getCompany(), input).getEntity();

      assertNotNull(entityService.getEntity(su.getCompany(), meta.getKey()));

      input = new EntityInputBean(fortA, "jinks", "DocA", new DateTime());
      input.addTag(new TagInputBean("cust124", "Customer", "purchased").setLabel("Customer"));

      mediationFacade.trackEntity(su.getCompany(), input).getEntity();

      Collection<String> docs = new ArrayList<>();
      docs.add("DocA");
      Collection<DocumentResultBean> documentTypes = conceptService.findConcepts(su.getCompany(), docs, false);
      assertNotNull(documentTypes);
      assertEquals(1, documentTypes.size());

      // add a second docTypes
      input = new EntityInputBean(fortA, "jinks", "DocA", new DateTime());
      input.addTag(new TagInputBean("cust123", "Rep", "sold").setLabel("Rep"));
      mediationFacade.trackEntity(su.getCompany(), input);

      documentTypes = conceptService.getConceptsWithRelationships(su.getCompany(), docs);
      assertEquals("Only one doc type should exist", 1, documentTypes.size());

      Boolean foundCustomer = false, foundRep = false;

      for (DocumentResultBean docTypes : documentTypes) {
        for (ConceptResultBean concept : docTypes.getConcepts()) {
          if (concept.getName().equals("Customer")) {
            foundCustomer = true;
            assertEquals(1, concept.getRelationships().size());
            Assert.assertEquals("purchased", concept.getRelationships().iterator().next().getName());
            assertEquals(true, concept.toString().contains(concept.getName()));
          }
          if (concept.getName().equals("Rep")) {
            foundRep = true;
            assertEquals(1, concept.getRelationships().size());
            Assert.assertEquals("sold", concept.getRelationships().iterator().next().getName());
            assertEquals(true, concept.toString().contains(concept.getName()));
          }
        }

      }
      assertTrue("Didn't find Customer concept", foundCustomer);
      assertTrue("Didn't find Rep concept", foundRep);
    } finally {
      cleanUpGraph();
    }

  }

  @Test
  public void multipleRelationships() throws Exception {
    try {
      logger.debug("### multipleRelationships");
      setSecurity();
      engineConfig.setConceptsEnabled(true);
      Transaction t = beginManualTransaction();

      SystemUser su = registerSystemUser("multipleRelationships", mike_admin);
      assertNotNull(su);

      FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("multipleRelationships", true));

      DocumentNode dType = conceptService.resolveByDocCode(fortress, "ABC123", true);
      commitManualTransaction(t);// Should only be only one docTypes

      assertNotNull(dType);
      Long id = dType.getId();
      dType = conceptService.resolveByDocCode(fortress, "ABC123", false);
      assertEquals(id, dType.getId());

      EntityInputBean input = new EntityInputBean(fortress, "jinks", "DocA", new DateTime());
      input.addTag(new TagInputBean("cust123", "Customer", "purchased").setLabel("Customer"));
      input.addTag(new TagInputBean("harry", "Customer", "soldto").setLabel("Customer"));
      mediationFacade.trackEntity(su.getCompany(), input).getEntity();
      Set<DocumentResultBean> docResults = conceptService.findConcepts(su.getCompany(), "DocA", true);
      assertEquals(1, docResults.size());
      assertEquals(1, docResults.iterator().next().getConcepts().size());
      assertEquals("should have been two relationships", 2, docResults.iterator().next().getConcepts().iterator().next().getRelationships().size());


      input = new EntityInputBean(fortress, "jinks", "DocA", new DateTime());
      input.addTag(new TagInputBean("cust121", "Customer", "purchased").setLabel("Customer"));
      input.addTag(new TagInputBean("harry", "Customer", "soldto").setLabel("Customer"));
      mediationFacade.trackEntity(su.getCompany(), input).getEntity();
      validateConcepts("DocA", su, 1);

      Collection<String> docs = new ArrayList<>();
      docs.add("DocA");
      Set<DocumentResultBean> docTypes = conceptService.getConceptsWithRelationships(su.getCompany(), docs);
      for (DocumentResultBean docType : docTypes) {
        Collection<ConceptResultBean> concepts = docType.getConcepts();
        for (ConceptResultBean concept : concepts) {
          Collection<RelationshipResultBean> relationships = concept.getRelationships();
          for (RelationshipResultBean relationship : relationships) {
            logger.debug(relationship.getName());
          }
          if (concept.getName().equals("User")) {
            // Currently only tracking the created. Should be 2 when tracking the updated
            assertEquals(1, relationships.size());
          } else {
            assertEquals(2, relationships.size());
          }

        }
      }
      assertEquals("Docs In Use not supporting 'null args'", 2, conceptService.getConceptsWithRelationships(su.getCompany(), null).size());
    } finally {
      cleanUpGraph();
    }

  }

  @Test
  public void relationshipWorkForMultipleDocuments() throws Exception {
    try {
      logger.debug("### relationshipWorkForMultipleDocuments");
      setSecurity();
      engineConfig.setConceptsEnabled(true);
      engineConfig.setTestMode(true);

      Transaction t = beginManualTransaction();

      SystemUser su = registerSystemUser("relationshipWorkForMultipleDocuments", mike_admin);
      assertNotNull(su);

      FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("relationshipWorkForMultipleDocuments", true));

      DocumentNode docA = conceptService.resolveByDocCode(fortress, "DOCA", true);
      DocumentNode docB = conceptService.resolveByDocCode(fortress, "DOCB", true);
      commitManualTransaction(t);// Should only be only one docTypes

      assertNotNull(docA);
      Long idA = docA.getId();
      docA = conceptService.resolveByDocCode(fortress, docA.getName(), false);
      assertEquals(idA, docA.getId());

      EntityInputBean input = new EntityInputBean(fortress, "jinks", "DocA", new DateTime());
      input.addTag(new TagInputBean("cust123", "Customer", "purchased").setLabel("Customer"));
      mediationFacade.trackEntity(su.getCompany(), input).getEntity();
      input = new EntityInputBean(fortress, "jinks", docB.getName(), new DateTime());
      input.addTag(new TagInputBean("cust121", "Customer", "purchased").setLabel("Customer"));
      mediationFacade.trackEntity(su.getCompany(), input).getEntity();

      Collection<String> docs = new ArrayList<>();
      docs.add(docA.getName());
      docs.add(docB.getName());

      Set<DocumentResultBean> docTypes = conceptService.getConceptsWithRelationships(su.getCompany(), docs);
      for (DocumentResultBean docType : docTypes) {
        Collection<ConceptResultBean> concepts = docType.getConcepts();
        for (ConceptResultBean concept : concepts) {
          Collection<RelationshipResultBean> relationships = concept.getRelationships();
          TestCase.assertEquals(1, relationships.size());
//                    for (RelationshipResultBean relationship : relationships) {
//                        assertEquals(1, relationship.getDocumentTypes().size());
//                        if (docType.getName().equals(docA.getName()))
//                            docAFound = true;
//                        else if (docType.getName().equals(docB.getName()))
//                            docBFound = true;
//                    }
        }
      }
      // ToDo: it is unclear if we should track in this manner
//            assertTrue("DocA Not Found in the concept", docAFound);
//            assertTrue("DocB Not Found in the concept", docBFound);
      assertEquals("Docs In Use not supporting 'null args'", 2, conceptService.getConceptsWithRelationships(su.getCompany(), null).size());
    } finally {
      cleanUpGraph();
    }

  }

  /**
   * Assert that we only get back relationships for a the selected document type. Checks that
   * Relationships, created via an association to a tag (Linux:Tag), can be filtered by doc type.
   * e.g. Sales and Promo both have a differently named relationship to the Device tag. When retrieving
   * Sales, we should only get the "purchased" relationship. Likewise with Promo, we should only get the "offer"
   *
   * @throws Exception
   */
  @Test
  public void uniqueRelationshipByDocType() throws Exception {
    try {
      logger.debug("### uniqueRelationshipByDocType");
      setSecurity();
      engineConfig.setConceptsEnabled(true);
      engineConfig.setTestMode(true);

      Transaction t = beginManualTransaction();

      SystemUser su = registerSystemUser("uniqueRelationshipByDocType", mike_admin);
      assertNotNull(su);

      FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("fortA", true));

      DocumentNode sale = conceptService.resolveByDocCode(fortress, "Sale", true);
      commitManualTransaction(t);
      t = beginManualTransaction();
      DocumentNode promo = conceptService.resolveByDocCode(fortress, "Promotion", true);
      commitManualTransaction(t);

      EntityInputBean promoInput = new EntityInputBean(fortress, "jinks", promo.getName(), new DateTime());
      promoInput.addTag(new TagInputBean("Linux", "Device", "offer").setLabel("Device"));
      //promoInput.addTag(new TagInputBean("Mike", "sold").setLabel("Person"));
      mediationFacade.trackEntity(su.getCompany(), promoInput).getEntity();

      EntityInputBean salesInput = new EntityInputBean(fortress, "jinks", sale.getName(), new DateTime());
      salesInput.addTag(new TagInputBean("Linux", "Device", "purchased").setLabel("Device"));
      //promoInput.addTag(new TagInputBean("Gary", "authorised").setLabel("Person"));
      mediationFacade.trackEntity(su.getCompany(), salesInput).getEntity();

      Collection<String> docs = new ArrayList<>();
      docs.add(promo.getName());
      docs.add(sale.getName());
      validateConcepts(docs, su, 2);
      docs.clear();
      docs.add(promo.getName());
      Set<DocumentResultBean> foundDocs = validateConcepts(docs, su, 1);
      for (DocumentResultBean foundDoc : foundDocs) {
        assertEquals("Promotion", foundDoc.getName());
        Collection<ConceptResultBean> concepts = foundDoc.getConcepts();
        assertEquals(1, concepts.size());
        boolean deviceFound = false;
//                boolean userFound = false;
        for (ConceptResultBean concept : concepts) {
          if (concept.getName().equalsIgnoreCase("Device")) {
            deviceFound = true;
            assertEquals(1, concept.getRelationships().size());
          }

        }
        assertEquals(true, deviceFound);
      }
    } finally {
      cleanUpGraph();
    }


  }

  @Test
  public void purgeFortressRemovesConcepts() throws Exception {
    try {
      logger.debug("### uniqueRelationshipByDocType");
      setSecurity();

      engineConfig.setConceptsEnabled(true);
      engineConfig.setTestMode(true);

      Transaction t;

      SystemUser su = registerSystemUser("relationshipWorkForMultipleDocuments", mike_admin);
      assertNotNull(su);

      FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("relationshipWorkForMultipleDocuments", true));

      t = beginManualTransaction();
      DocumentNode claim = conceptService.resolveByDocCode(fortress, "Claim", true);
      commitManualTransaction(t);

      EntityInputBean promoInput = new EntityInputBean(fortress,
          "jinks",
          claim.getName(),
          new DateTime());
      promoInput.addTag(
          new TagInputBean("a1065", "Claim", "identifier").setLabel("Claim"));

      mediationFacade.trackEntity(su.getCompany(), promoInput).getEntity();

      Collection<String> docs = new ArrayList<>();
      docs.add(claim.getName());
      validateConcepts(docs, su, 1);
      docs.clear();
      docs.add(claim.getName());
      Set<DocumentResultBean> foundDocs = validateConcepts(docs, su, 1);
      for (DocumentResultBean foundDoc : foundDocs) {
        assertEquals("Claim", foundDoc.getName());
        Collection<ConceptResultBean> concepts = foundDoc.getConcepts();
        assertEquals(1, concepts.size());
        boolean claimFound = false;
        for (ConceptResultBean concept : concepts) {
          if (concept.getName().equalsIgnoreCase("Claim")) {
            claimFound = true;
            assertEquals(1, concept.getRelationships().size());
          }

        }

        assertEquals(true, claimFound);
        logger.info(foundDoc.toString());
      }
      mediationFacade.purge(fortress);
      waitAWhile("Waiting for Async processing to complete");
      assertEquals(0, conceptService.getDocumentsInUse(su.getCompany()).size());
    } finally {
      cleanUpGraph();
    }

  }

  @Test
  public void testEntityConceptsLink() throws Exception {
    // Initial setup
    cleanUpGraph();

    engineConfig.setConceptsEnabled(true);
    engineConfig.setTestMode(true);

    SystemUser su = registerSystemUser("testEntityConceptsLink", mike_admin);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("testEntityConceptsLink", true));

    EntityInputBean staff = new EntityInputBean(fortress, "wally", "Staff", new DateTime(), "ABC123");

    mediationFacade.trackEntity(su.getCompany(), staff);
    assertEquals(1, conceptService.getDocumentsInUse(su.getCompany()).size());

    // Checking that the entity is linked when part of the track request
    EntityInputBean workRecord = new EntityInputBean(fortress, "wally", "Work", new DateTime(), "ABC321")
        .addTag(new TagInputBean("someTag", "SomeLabel", "somerlx"))
        .addEntityLink(new EntityKeyBean("Staff", fortress.getName(), "ABC123").setRelationshipName("worked"));

    mediationFacade.trackEntity(su.getCompany(), workRecord);
    assertEquals(2, conceptService.getDocumentsInUse(su.getCompany()).size());

    Collection<String> docs = new ArrayList<>();
    docs.add("Staff");
    docs.add("Work");
    Set<DocumentResultBean> documentResults = conceptService.findConcepts(su.getCompany(), docs, true);
    assertEquals(2, documentResults.size());
    for (DocumentResultBean documentResultBean : documentResults) {
      switch (documentResultBean.getName()) {
        case "Staff":
          break;
        case "Work":
          // nothing to assert yet
          assertEquals(1, documentResultBean.getConcepts().size());
          assertEquals("SomeLabel", documentResultBean.getConcepts().iterator().next().getName());
          break;
        default:
          fail("Unexpected Document Type " + documentResultBean);
          break;
      }
    }

    // We should be able to find that a Staff entity has a worked link to a Timesheet
  }

  @Test
  public void testEntityConceptsLinkProperties() throws Exception {
    // Initial setup
    cleanUpGraph();

    engineConfig.setConceptsEnabled(true);
    engineConfig.setTestMode(true);

    SystemUser su = registerSystemUser("testEntityConceptsLinkProperties", mike_admin);
    FortressNode fortress = fortressService.registerFortress(su.getCompany(), new FortressInputBean("testEntityConceptsLinkProperties", true));

    EntityInputBean staff = new EntityInputBean(fortress, "wally", "Staff", new DateTime(), "ABC123");

    mediationFacade.trackEntity(su.getCompany(), staff);
    assertEquals(1, conceptService.getDocumentsInUse(su.getCompany()).size());

    // Checking that the entity is linked when part of the track request
    EntityInputBean workRecord = new EntityInputBean(fortress, "wally", "Work", new DateTime(), "ABC321")
        .addTag(new TagInputBean("someTag", "SomeLabel", "somerlx"))
        .addEntityLink(new EntityKeyBean("Staff", fortress.getName(), "ABC123")
            .setRelationshipName("worked")
            .setParent(true));

    mediationFacade.trackEntity(su.getCompany(), workRecord);
    assertEquals(2, conceptService.getDocumentsInUse(su.getCompany()).size());

    Collection<String> docs = new ArrayList<>();
    docs.add("Staff");
    docs.add("Work");
    Set<DocumentResultBean> documentResults = conceptService.findConcepts(su.getCompany(), docs, true);
    assertEquals(2, documentResults.size());
    MatrixResults structure = conceptService.getContentStructure(su.getCompany(), fortress.getName());
    assertEquals(3, structure.getNodes().size());
    assertEquals(2, structure.getEdges().size());
    for (EdgeResult edgeResult : structure.getEdges()) {
      if (edgeResult.getRelationship().equals("worked")) {  // EntityLink relationship
        assertTrue("parent property was not set", edgeResult.getData().containsKey("parent"));
        assertTrue("Parent not true", Boolean.parseBoolean(edgeResult.getData().get("parent").toString()));
      }
    }
    // We should be able to find that a Staff entity has a worked link to a Timesheet
  }

  private Set<DocumentResultBean> validateConcepts(String document, SystemUser su, int expected) throws Exception {
    Collection<String> docs = new ArrayList<>();

    docs.add(document);
    return validateConcepts(docs, su, expected);
  }

  private Set<DocumentResultBean> validateConcepts(Collection<String> docs, SystemUser su, int expected) throws Exception {
    Set<DocumentResultBean> concepts = conceptService.findConcepts(su.getCompany(), docs, true);
    String message = "Collection";
    if (docs != null && docs.size() == 1) {
      message = docs.iterator().next();
    }
    assertEquals(message + " concepts", expected, concepts.size()); // Purchased docTypes
    return concepts;

  }

}
