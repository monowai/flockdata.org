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

package org.flockdata.test.engine.mvc;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.util.Collection;
import java.util.TimeZone;
import org.flockdata.engine.data.graph.FortressSegmentNode;
import org.flockdata.helper.NotFoundException;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * @author mholdsworth
 * @since 16/02/2015
 */
public class TestFortressEP extends MvcBase {


  /**
   * Create a collection of DocumentTypeInputBeans for a fortress over the endpoint
   *
   * @throws Exception
   */
  @Test
  public void get_FortressSegments() throws Exception {

    FortressResultBean fortress = makeFortress(mike(), "make_DocTypes");

    Collection<FortressSegmentNode> segments = getDocumentWithSegments(mike(), fortress.getName());
    assertEquals(1, segments.size());

    assertEquals("Default segment not found", "Default", segments.iterator().next().getCode());

  }

  @Test
  public void get_FortressSegmentsForDocumentType() throws Exception {

    FortressResultBean fortress = makeFortress(mike(), "make_DocSegmentDocTypes");
    EntityInputBean eib = new EntityInputBean(fortress, new DocumentTypeInputBean("segTestDoc"))
        .setSegment("2014");
    track(mike(), eib);

    eib.setSegment("2015");
    track(mike(), eib);

    eib.setSegment("2016");
    track(mike(), eib);

    // 4 Segments including the default o

    Collection<DocumentResultBean> documentResultBean = getDocumentWithSegments(mike(), fortress.getName(), "segTestDoc");
    Assert.assertEquals(1, documentResultBean.size());
    assertEquals("Default fortress is not included", 3, documentResultBean.iterator().next().getSegments().size());

    documentResultBean = getDocumentWithSegments(mike(), fortress.getName(), "*");
    Assert.assertEquals(1, documentResultBean.size());
    assertEquals("Wild card DocType not working", 3, documentResultBean.iterator().next().getSegments().size());
  }

  @Test
  public void get_MultipleFortressesSameSegmentsAndDocType() throws Exception {

    FortressResultBean fortressA = makeFortress(mike(), "multi_FortA");
    FortressResultBean fortressB = makeFortress(mike(), "multi_FortB");
    String docType = "sameDocType";
    EntityInputBean eib = new EntityInputBean(fortressA, new DocumentTypeInputBean(docType))
        .setSegment("2014");
    track(mike(), eib);

    eib.setSegment("2015");
    track(mike(), eib);

    eib.setSegment("2016");
    track(mike(), eib);

    eib = new EntityInputBean(fortressB, new DocumentTypeInputBean(docType))
        .setSegment("2014");
    track(mike(), eib);

    eib.setSegment("2012");
    track(mike(), eib);

    eib.setSegment("2015");
    track(mike(), eib);

    eib.setSegment("2016");
    track(mike(), eib);

    // 4 Segments including the default o

    Collection<DocumentResultBean> documentResultBean = getDocumentWithSegments(mike(), fortressA.getName(), docType);
    Assert.assertEquals(1, documentResultBean.size());
    assertEquals("Default fortress should not be included", 3, documentResultBean.iterator().next().getSegments().size());

    documentResultBean = getDocumentWithSegments(mike(), fortressA.getName(), "*");
    Assert.assertEquals(1, documentResultBean.size());
    assertEquals("Wildcard fortress not working", 3, documentResultBean.iterator().next().getSegments().size());

    documentResultBean = getDocumentWithSegments(mike(), fortressB.getName(), docType);
    Assert.assertEquals(1, documentResultBean.size());
    assertEquals("Second fortress should have one extra segment", 4, documentResultBean.iterator().next().getSegments().size());

    documentResultBean = getDocumentWithSegments(mike(), fortressB.getName(), "*");
    Assert.assertEquals(1, documentResultBean.size());
    assertEquals("Wildcard doc type - second fortress should have one extra segment", 4, documentResultBean.iterator().next().getSegments().size());
  }

  @Test
  public void update_Fortress() throws Exception {

    FortressResultBean fortress = makeFortress(mike(), "update_Fortress");
    FortressInputBean update = new FortressInputBean(fortress.getName());

    update.setSearchEnabled(true);
    update.setStoreEnabled(true);
    update.setTimeZone(TimeZone.getTimeZone("PST").getID());
    update.setName("A new name");
    update.setSystem(true);

    FortressResultBean updated = updateFortress(mike(), fortress.getCode(), update, MockMvcResultMatchers.status().isOk());
    assertNotNull(updated);
    assertEquals("Search active did not change", update.isSearchEnabled(), updated.isSearchEnabled());
    assertEquals("Store active did not change", update.isStoreEnabled(), updated.isStoreEnabled());
    assertEquals(update.getName(), updated.getName());


    try {
      updateFortress(mike(), "doesNotExist", update);
      fail("Didn't get a not found exception");
    } catch (NotFoundException e) {
      logger.info("Correct exception thrown");
    }

    try {
      updateFortress(sally(), fortress.getCode(), update);
      fail("Didn't get a not found exception");
    } catch (NotFoundException e) {
      logger.info("Correct exception thrown");
    }

    try {
      updateFortress(harry(), fortress.getCode(), update);
      fail("Didn't get a not found exception");
    } catch (NotFoundException e) {
      logger.info("Correct exception thrown");
    }


  }

  @Test
  public void cant_updateFortressThatUserDoesntBelongTo() throws Exception {

    FortressResultBean fortress = makeFortress(mike(), "cant_updateFortressThatUserDoesntBelongTo");

    FortressInputBean update = new FortressInputBean(fortress.getName());

    update.setSearchEnabled(true);
    update.setStoreEnabled(true);
    update.setTimeZone(TimeZone.getTimeZone("PST").getID());
    update.setName("A new name");
    update.setSystem(true);

    // Sally works for a different company
    exception.expect(NotFoundException.class);
    updateFortress(sally(), fortress.getCode(), update, MockMvcResultMatchers.status().isNotFound());

  }

  @Test
  public void cant_updateFortressUnlessUserIsAdmin() throws Exception {

    FortressResultBean fortress = makeFortress(mike(), "cant_updateFortressUnlessUserIsAdmin");

    FortressInputBean update = new FortressInputBean(fortress.getName());

    update.setSearchEnabled(true);
    update.setStoreEnabled(true);
    update.setTimeZone(TimeZone.getTimeZone("PST").getID());
    update.setName("A new name");
    update.setSystem(true);

    // Sally works for a different company
    exception.expect(AccessDeniedException.class);
    updateFortress(harry(), fortress.getCode(), update, MockMvcResultMatchers.status().isForbidden());

  }

  @Test
  public void null_FortressCode() throws Exception {

    makeFortress(mike(), "null_FortressCode");

    FortressInputBean update = new FortressInputBean(null);

    update.setSearchEnabled(true);
    update.setStoreEnabled(true);
    update.setTimeZone(TimeZone.getTimeZone("NZST").getID());
    update.setName("fName"); // Setting the name should set the code if the code is null
    update.setSystem(true);

    makeFortress(mike(), update);

  }

  @Test
  public void system_Fortress() throws Exception {
    Collection<FortressResultBean> fortresses = getFortresses(mike());

    // In case other tests have left fortresses hanging around, lets get our base count
    int fortressCount = fortresses.size();
    logger.debug("existing fortress count {}", fortressCount);

    makeFortress(mike(), new FortressInputBean("Regular", true));

    FortressInputBean systemFortress = new FortressInputBean("TestSystem", true);
    systemFortress.setSystem(true);
    FortressResultBean fortress = makeFortress(mike(), systemFortress);
    assertTrue("Was not created as a system fortress", fortress.isSystem());
    assertTrue("system index prefix was not set [" + fortress.getRootIndex() + "]", fortress.getRootIndex().startsWith(".testfd."));
    fortresses = getFortresses(mike());
    assertEquals("System fortress should not have been returned", fortressCount + 1, fortresses.size());
  }

  @Test
  public void default_FortressInputWorks() throws Exception {
    FortressInputBean defaults = getDefaultFortress(mike());
    assertNotNull(defaults);
    assertFalse(defaults.isSearchEnabled());
    assertFalse(defaults.isStoreEnabled());
    assertNotNull(defaults.getTimeZone());

  }

  @Test
  public void customFortressDefaultsWork() throws Exception {
    FortressInputBean fortressInputBean = new FortressInputBean("demo-custom")
        .setStoreEnabled(true)
        .setSearchEnabled(true);
    FortressResultBean defaults = makeFortress(mike(), fortressInputBean);
    assertNotNull(defaults);
    assertTrue("Search wasn't enabled", defaults.isSearchEnabled());
    assertTrue("Store wasn't enabled", defaults.isStoreEnabled());
    assertNotNull(defaults.getTimeZone());

  }


  @Test
  public void find_withDotInCode() throws Exception {
    String name = "name.with.init";
    FortressResultBean fortress = makeFortress(mike(), name);
    assertNotNull(getFortress(mike(), name));
    makeDocuments(mike(), fortress, new DocumentTypeInputBean("TestingWithDots"));
    Collection<DocumentResultBean> docs = getFortressDocs(mike(), name);
    assertEquals(1, docs.size());
    deleteFortress(mike(), name, ACCEPTED);
//        deleteFortress()

  }

}
