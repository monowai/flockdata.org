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

package org.flockdata.test.search.functional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchChanges;
import org.flockdata.search.SearchResult;
import org.flockdata.search.SearchResults;
import org.flockdata.search.SearchSchema;
import org.flockdata.test.helper.ContentDataHelper;
import org.flockdata.test.helper.MockDataFactory;
import org.flockdata.track.bean.ContentInputBean;
import org.flockdata.track.bean.GeoDataBean;
import org.flockdata.track.bean.GeoDataBeans;
import org.flockdata.track.bean.SearchChange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author mholdsworth
 * @since 15/08/2014
 */
@RunWith(SpringRunner.class)
public class TestMappings extends ESBase {

  private Logger logger = LoggerFactory.getLogger(TestMappings.class);

  @Test
  public void defaultTagQueryWorks() throws Exception {
    Map<String, Object> json = ContentDataHelper.getBigJsonText(20);

    String company = "test";
    String fortress = "defaultTagQueryWorks";
    String doc = "doc";
    String user = "mike";

    Entity entity = MockDataFactory.getEntity(company, fortress, user, doc);

    EntitySearchChange change = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity));
    change.setDescription("Test Description");
    change.setName("Test Name");
    change.setData(json);
    ArrayList<EntityTag> tags = new ArrayList<>();

    TagInputBean tagInput = new TagInputBean("myTag", "TheLabel", "rlxname").
        setCode("my TAG");

    tags.add(MockDataFactory.getEntityTag(entity, tagInput, "mytag"));

    change.setStructuredTags(tags);

    deleteEsIndex(entity);
    //entityWriter.ensureIndex(change.getRootIndex(), change.getType());
    SearchResults searchResults = esSearchWriter.createSearchableChange(new SearchChanges(change));
    SearchResult searchResult = searchResults.getSearchResults().iterator().next();
    Thread.sleep(1000);
    assertNotNull(searchResult);
    assertNotNull(searchResult.getSearchKey());
    when(entity.getSearchKey()).thenReturn(searchResult.getSearchKey());
    json = entityWriter.findOne(entity);

    doTermQuery(entity, "tag.mytag.thelabel.code", tagInput.getCode(), 1, "Exact match of tag code is not working");
    // Assert that the description  exists
    doTermQuery(entity, "name", change.getName());
    assertNotNull(json);

  }

  @Test
  public void count_CorrectSearchResults() throws Exception {
    Map<String, Object> json = ContentDataHelper.getBigJsonText(20);

    String fortress = "fort";
    String company = "test";
    String doc = "doc";
    String user = "mike";

    Entity entity = getEntity(company, fortress, user, doc);
    Entity entityB = getEntity(company, fortress, user, doc);

    EntitySearchChange change = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity));
    EntitySearchChange changeB = new EntitySearchChange(entityB, searchConfig.getIndexManager().toIndex(entityB));
    change.setDescription("Test Description");
    change.setData(json);
    changeB.setData(json);
    ArrayList<EntityTag> tags = new ArrayList<>();

    TagInputBean tagInput = new TagInputBean("myTag", "TheLabel", "rlxname");
    tagInput.setCode("my TAG");

    tags.add(MockDataFactory.getEntityTag(entity, tagInput, "mytag"));
    change.setStructuredTags(tags);

    deleteEsIndex(entity);

    Collection<SearchChange> changes = new ArrayList<>();
    changes.add(change);
    changes.add(changeB);
    SearchChanges searchChanges = new SearchChanges(changes);
    SearchResults searchResults = esSearchWriter.createSearchableChange(searchChanges);
    assertEquals("2 in 2 out", 2, searchResults.getSearchResults().size());
  }

  @Test
  public void testCustomMappingWorks() throws Exception {
    Map<String, Object> json = ContentDataHelper.getBigJsonText(20);
    Entity entityA = getEntity("cust", "fort", "anyuser", "fort");
    Entity entityB = getEntity("cust", "fortb", "anyuser", "fortb");

    deleteEsIndex(entityA);
    deleteEsIndex(entityB);

    EntitySearchChange changeA = new EntitySearchChange(entityA, new ContentInputBean(json), searchConfig.getIndexManager().toIndex(entityA));
    EntitySearchChange changeB = new EntitySearchChange(entityB, new ContentInputBean(json), searchConfig.getIndexManager().toIndex(entityB));

    // FortB will have
    changeA.setDescription("Test Description");
    changeB.setDescription("Test Description");

    indexMappingService.ensureIndexMapping(changeA);
    indexMappingService.ensureIndexMapping(changeB);
    changeA = entityWriter.handle(changeA);
    changeB = entityWriter.handle(changeB);
    Thread.sleep(1000);
    assertNotNull(changeA);
    assertNotNull(changeB);
    assertNotNull(changeA.getSearchKey());
    assertNotNull(changeB.getSearchKey());

    // by default we analyze the @description field
    doDescriptionQuery(entityA, changeA.getDescription(), 1);

    // In fortb.json we don't analyze the description (overriding the default) so it shouldn't be found
    // This will fail if the prefix is not fd. - see the README in fd.cust.fortb
    doDescriptionQuery(entityB, changeB.getDescription(), 0);

  }

  @Test
  public void sameIndexDifferentDocumentsHaveMappingApplied() throws Exception {
    Map<String, Object> json = ContentDataHelper.getBigJsonText(20);
    String fortress = "fortMapping";
    Entity entityA = getEntity("cust", fortress, "anyuser", "fortdoc");
    assertNotNull(entityA.getCode());
    assertNotNull(entityA.getKey());
    assertSame(entityA.getCode(), entityA.getKey()); // Mock key + code is the same if code not explicit
    Entity entityB = getEntity("cust", fortress, "anyuser", "doctype");
    assertNotNull(entityB.getCode());
    assertNotNull(entityB.getKey());
    assertSame(entityB.getCode(), entityB.getKey()); // Mock key + code is the same if code not explicit


    EntitySearchChange changeA = new EntitySearchChange(entityA, new ContentInputBean(json), searchConfig.getIndexManager().toIndex(entityA));
    EntitySearchChange changeB = new EntitySearchChange(entityB, new ContentInputBean(json), searchConfig.getIndexManager().toIndex(entityB));

    TagInputBean tag = new TagInputBean("my TAG", "TheLabel", "rlxname");
    assertEquals("my TAG", tag.getCode());
    ArrayList<EntityTag> tagsA = new ArrayList<>();
    tagsA.add(MockDataFactory.getEntityTag(entityA, tag, "mytag"));

    ArrayList<EntityTag> tagsB = new ArrayList<>();
    tagsB.add(MockDataFactory.getEntityTag(entityB, tag, "mytag"));

    changeA.setStructuredTags(tagsA);
    changeB.setStructuredTags(tagsB);

    deleteEsIndex(entityA);
    deleteEsIndex(entityB);

    indexMappingService.ensureIndexMapping(changeA);
    indexMappingService.ensureIndexMapping(changeB);
    changeA = entityWriter.handle(changeA);
    changeB = entityWriter.handle(changeB);
    Thread.sleep(2000);
    assertNotNull(changeA);
    assertNotNull(changeB);
    assertNotNull(changeA.getSearchKey());
    assertNotNull(changeB.getSearchKey());
    getMapping(entityA);
    doTermQuery(entityA, "tag.mytag.thelabel.code", tag.getCode());
    doQuery(entityA, tag.getCode().toLowerCase());
    doTermQuery(entityB, "tag.mytag.thelabel.code", tag.getCode());
    // Locate by raw text
    doQuery(entityB, tag.getCode().toLowerCase());
    String index = searchConfig.getIndexManager().getIndexRoot(entityA.getFortress()) + "*";

    doTermQuery(index, "*", "tag.mytag.thelabel.code", tag.getCode(), 2, "Not scanning across indexes");

  }

  @Test
  public void tagWithRelationshipNamesMatchingNodeNames() throws Exception {
    Map<String, Object> json = ContentDataHelper.getBigJsonText(20);
    Entity entity = getEntity("cust", "tagWithRelationshipNamesMatchingNodeNames", "anyuser", "fortdoc");
    deleteEsIndex(entity);
    EntitySearchChange changeA = new EntitySearchChange(entity, new ContentInputBean(json), searchConfig.getIndexManager().toIndex(entity));

    TagInputBean tag = new TagInputBean("aValue", "myTag", "myTag");

    ArrayList<EntityTag> tags = new ArrayList<>();
    tags.add(MockDataFactory.getEntityTag(entity, tag, "mytag"));
    changeA.setStructuredTags(tags);

    deleteEsIndex(entity);

    indexMappingService.ensureIndexMapping(changeA);

    changeA = entityWriter.handle(changeA);

    assertNotNull(changeA);
    assertNotNull(changeA.getSearchKey());

    // DAT-328
    Thread.sleep(3000);
    doTermQuery(entity, "tag.mytag.code", tag.getCode());

  }

  @Test
  public void geo_Points() throws Exception {
    String comp = "geo_Points";
    String fort = "geo_Points";
    String user = "mikey";

    Entity entity = getEntity(comp, fort, user, fort);
    deleteEsIndex(searchConfig.getIndexManager().toIndex(entity));

    Map<String, Object> what = ContentDataHelper.getSimpleMap(
        SearchSchema.WHAT_CODE, "GEO");
    what.put(SearchSchema.WHAT_NAME, "NameText");
    what.put(SearchSchema.WHAT_DESCRIPTION, "This is a description");

    TagInputBean tagInput = new TagInputBean("tagcode", "TagLabel", "tag-relationship");

    ArrayList<EntityTag> tags = new ArrayList<>();

    HashMap<String, Object> tagProps = new HashMap<>();
    tagProps.put("num", 100d);
    tagProps.put("str", "hello");
    EntityTag entityTag = MockDataFactory.getEntityTag(entity, tagInput, "entity-relationship", tagProps);
    // DAT-442 Geo refactoring
    GeoDataBeans geoPayLoad = new GeoDataBeans();
    GeoDataBean geoData = new GeoDataBean();
    GeoDataBean streetData = new GeoDataBean();
    streetData.add("street", "abc", "123 Main Street", -13.03, 168.0);
    geoData.add("country", "NZ", "New Zealand", -41.0, 174.0);
    geoPayLoad.add("country", geoData);
    geoPayLoad.add("street", streetData);
    geoData = geoPayLoad.getGeoBeans().get("country");
    TestCase.assertNotNull(geoData);
    when(entityTag.getGeoData()).thenReturn(geoPayLoad);
    tags.add(entityTag);

    EntitySearchChange change = new EntitySearchChange(entity, searchConfig.getIndexManager().toIndex(entity));

    change.setData(what);
    change.setStructuredTags(tags);

    indexMappingService.ensureIndexMapping(change);
    SearchChange searchResult = entityWriter.handle(change);
    TestCase.assertNotNull(searchResult);
    Thread.sleep(2000);

    String result = doQuery(entity, "*");
    logger.info(result);
    assertTrue("Couldn't find the country GeoPoints", result.contains("points\":{\"country\""));
    assertTrue("Should be two geo points", result.contains("\"street\""));
    assertTrue(result.contains("174"));
    assertTrue(result.contains("-41"));

    doCompletionQuery(entity, "nz", 1, "Couldn't autocomplete on geo tag for NZ. If there are results, then the field name may be in error");
    doCompletionQuery(entity, "new ze", 1, "Couldn't autocomplete on geo tag for New Zealand");
  }

}
