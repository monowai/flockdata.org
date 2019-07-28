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

package org.flockdata.test.engine.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import junit.framework.TestCase;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.TxRef;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.TxRefNode;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.IndexManager;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchChanges;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 22/06/2013
 */
public class TestInputBeans {
  //private Logger log = LoggerFactory.getLogger(TestInputBeans.class);

  @Test
  public void testTxStatus() {
    TxRefNode tx = new TxRefNode("abc", CompanyNode.builder().name("").build());

    // Current status should be created
    assertEquals(TxRef.TxStatus.TX_CREATED, tx.getTxStatus());
    TxRef.TxStatus previous = tx.commit();
    assertEquals(TxRef.TxStatus.TX_COMMITTED, tx.getTxStatus());
    assertEquals(TxRef.TxStatus.TX_CREATED, previous);
    previous = tx.rollback();
    assertEquals(TxRef.TxStatus.TX_ROLLBACK, tx.getTxStatus());
    assertEquals(TxRef.TxStatus.TX_COMMITTED, previous);

  }

  @Test
  public void testFortressInputBean() {

    FortressInputBean fib = new FortressInputBean("ABC");
    assertEquals(null, fib.isSearchEnabled());
    assertEquals(null, fib.isStoreEnabled());

    fib = new FortressInputBean("ABC", false);
    assertTrue(fib.isSearchEnabled());

    fib = new FortressInputBean("ABC", true);
    assertFalse(fib.isSearchEnabled());

    fib = new FortressInputBean("ABC", false);
    assertTrue(fib.isSearchEnabled());

    fib.setStoreEnabled(false);
    assertFalse(fib.isStoreEnabled());

    fib.setStoreEnabled(null);
    assertEquals(null, fib.isStoreEnabled());
  }

  @Test
  public void testEntityInputBean() throws Exception {
    DateTime now = DateTime.now();
    FortressNode fortress = new FortressNode(new FortressInputBean("fortress"),
        CompanyNode.builder().name("blah").build());
    EntityInputBean entityBean = new EntityInputBean(fortress, "user",
        "booking", now, "myRef");
    assertNull(entityBean.getKey());
    entityBean.setKey("AbC");
    assertNotNull(entityBean.getKey());

    // NonNull tx ref sets the inputBean to be transactional
    DateTime logNow = DateTime.now();
    ContentInputBean logBean = new ContentInputBean("user", "aaa", logNow, ContentDataHelper.getSimpleMap("abc", 0), "", "txreftest");
    entityBean.setContent(logBean); // Creation dates defer to the Log
    assertTrue(logBean.isTransactional());
    assertEquals(now.getMillis(), entityBean.getWhen().getTime());

    // Change the date on the log, should be the same in the entity
    logBean.setWhen(now.toDate());
    assertEquals(now.getMillis(), entityBean.getWhen().getTime());
    // Null the log
    entityBean.setContent(null);
    Date dateC = new Date();
    logBean.setWhen(dateC);
    entityBean.setContent(logBean);
    assertEquals(dateC.getTime(), logBean.getWhen().getTime());
    assertNotSame(dateC.getTime(), entityBean.getWhen().getTime());
    assertEquals(now.getMillis(), entityBean.getWhen().getTime());

    logBean = new ContentInputBean("user", "aaa", null, ContentDataHelper.getRandomMap());
    assertFalse(logBean.isTransactional());


  }

  @Test
  public void valuesDefaultCorrectly() {
    TagInputBean tib = new TagInputBean("Hello");
    assertEquals("Hello", tib.getCode());
    tib.setCode("hello");
    assertEquals("hello", tib.getCode());
    tib.setLabel("Testing");
    assertEquals("Testing", tib.getLabel());
  }

  @Test
  public void mergeTags() {
    TagInputBean dest = new TagInputBean("hello");
    TagInputBean source = new TagInputBean("hello");

    TagInputBean target = new TagInputBean("target");
    TagInputBean other = new TagInputBean("other");

    //source.addMetaLink("abc");
    source.setTargets("somerlx", target);
    source.setTargets("otherrlx", target);

    dest.setTargets("somerlx", other);// This one appends to somerlx collection

    dest.mergeTags(source);
    //assertEquals(1, dest.getEntityLinks().size());
    assertEquals("Should be 2 relationships", 2, dest.getTargets().size());
    assertEquals("TagInput did not merge into somerlx", 2, dest.getTargets().get("somerlx").size());
  }

  @Test
  public void entityTagLinksFromInput() {
    TagInputBean tag = new TagInputBean("SimpleName");
    tag.addEntityTagLink("myrlx");
    assertFalse(tag.getEntityTagLinks().isEmpty());
    assertTrue(tag.getEntityTagLinks().containsKey("myrlx"));
  }

  @Test
  public void serialize_SearchChanges() throws Exception {
    CompanyNode mockCompany = CompanyNode.builder().name("company").build();
    mockCompany.setName("company");

    FortressInputBean fib = new FortressInputBean("serialize_SearchChanges", false);
    FortressNode fortress = new FortressNode(fib, mockCompany);

    DateTime now = new DateTime();
    EntityInputBean eib = new EntityInputBean(fortress,
        "harry",
        "docType",
        now,
        "abc");

    Document doc = new DocumentNode(fortress, "docType");
    Entity entity = new EntityNode("abc", fortress, eib, doc);

    EntitySearchChange searchChange = new EntitySearchChange(entity, new IndexManager("fd.", true).toIndex(entity));
    SearchChanges changes = new SearchChanges(searchChange);
    String json = JsonUtils.toJson(changes);

    SearchChanges fromJson = JsonUtils.toObject(json.getBytes(), SearchChanges.class);

    TestCase.assertTrue("", fromJson.getChanges().size() == 1);

  }


}
