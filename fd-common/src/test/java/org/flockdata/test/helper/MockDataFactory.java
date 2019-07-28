/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.test.helper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.flockdata.data.Alias;
import org.flockdata.data.Company;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.data.FortressUser;
import org.flockdata.data.Segment;
import org.flockdata.data.Tag;
import org.flockdata.integration.Base64;
import org.flockdata.integration.IndexManager;
import org.flockdata.integration.KeyGenService;
import org.flockdata.registration.AliasInputBean;
import org.flockdata.registration.TagInputBean;
import org.joda.time.DateTime;

/**
 * helper routines to return sensibly mocked data classes
 *
 * @author mike
 * @tag
 * @since 4/01/17
 */
public class MockDataFactory {

  public static final String DEFAULT_ET_NAME = "entity-tag-out";

  public static Entity getEntity(String comp, String fort, String userName, String docType, String code) {
    // These are the minimum objects necessary to create Entity data

    Company mockCompany = getCompany(comp);

    Fortress fortress = getFortress(fort, mockCompany);

    Segment segment = mock(Segment.class);
    when(segment.getCode()).thenReturn(Fortress.DEFAULT);
    when(segment.getFortress()).thenReturn(fortress);
    when(segment.getCompany()).thenReturn(mockCompany);
    when(segment.isDefault()).thenReturn(true);
    when(fortress.getDefaultSegment()).thenReturn(segment);
    DateTime now = DateTime.now();
    FortressUser user = mock(FortressUser.class);
    when(user.getCode()).thenReturn(userName);
    when(user.getName()).thenReturn(userName);

    Entity entity = mock(Entity.class);
    when(entity.getFortress()).thenReturn(fortress);
    when(entity.getCreatedBy()).thenReturn(user);
    String key = new KeyGenService(new Base64()).getUniqueKey();
    when(entity.getKey()).thenReturn(key);

    if (code != null) {
      when(entity.getCode()).thenReturn(code);
    } else {
      when(entity.getCode()).thenReturn(key);
    }

    when(entity.getType()).thenReturn(docType);
    when(entity.getSegment()).thenReturn(segment);
    when(entity.isNewEntity()).thenReturn(true);

    when(entity.getFortressCreatedTz()).thenReturn(now);
    when(entity.getDateCreated()).thenReturn(now.getMillis());
    when(entity.getId()).thenReturn(now.getMillis());
    when(entity.getName()).thenReturn("Entity Name");
    return entity;
//        return new Entity(Long.toString(System.currentTimeMillis()), fortress.getDefaultSegment(), entityInput, doc);

  }

  public static Company getCompany(String comp) {
    Company mockCompany = mock(Company.class);
    when(mockCompany.getCode()).thenReturn(comp);
    when(mockCompany.getName()).thenReturn(comp);
    return mockCompany;
  }

  /**
   * @param sharedName name
   * @return fortress + company using the same name
   */
  public static Fortress getFortress(String sharedName) {
    return getFortress(sharedName, getCompany(sharedName));
  }

  public static Fortress getFortress(String fort, Company mockCompany) {
    Fortress fortress = mock(Fortress.class);
    when(fortress.getCode()).thenReturn(fort);
    when(fortress.getName()).thenReturn(fort);
    when(fortress.getCompany()).thenReturn(mockCompany);
    IndexManager indexManager = new IndexManager("fd.", false);
    String rootIndex = indexManager.getIndexRoot(fortress);

    when(fortress.getRootIndex()).thenReturn(rootIndex);
    return fortress;
  }

  public static Document getDocument(Fortress fortress, String type) {

    Document documentType = mock(Document.class);
    when(documentType.getCode()).thenReturn(type);
    when(documentType.getName()).thenReturn(type);
    when(documentType.getFortress()).thenReturn(fortress);
    return documentType;
  }

  public static Entity getEntity(String company, String fortress, String user, String doc) {
    return getEntity(company, fortress, user, doc, null);
  }

  public static Alias getAlias(String label, AliasInputBean aliasInput, String key, Tag tag) {
    Alias alias = mock(Alias.class);
    when(alias.getLabel()).thenReturn(label);
    when(alias.getKey()).thenReturn(key);
    when(alias.getTag()).thenReturn(tag);
    when(alias.getName()).thenReturn(aliasInput.getCode());
    when(alias.getDescription()).thenReturn(aliasInput.getDescription());
    return alias;
  }

  public static Tag getTag(TagInputBean tagInputBean) {
    Tag tag = mock(Tag.class);
    when(tag.getName()).thenReturn(tagInputBean.getName());
    when(tag.getCode()).thenReturn(tagInputBean.getCode());
    when(tag.getLabel()).thenReturn(tagInputBean.getLabel());
    if (tagInputBean.hasTagProperties()) {
      when(tag.getProperties()).thenReturn(tagInputBean.getProperties());
      when(tag.hasProperties()).thenReturn(true);
    } else
    // prevent NPE
    {
      when(tag.getProperties()).thenReturn(new HashMap<>());
    }
    return tag;
  }

  public static EntityTag getEntityTag(Entity entity, TagInputBean tagInput) {
    return getEntityTag(entity, tagInput, DEFAULT_ET_NAME);
  }

//    public static EntityTag getTag(Entity entity, TagInputBean tagInput, Map<String,Object>props) {
//        return getTag(entity, tagInput, DEFAULT_ET_NAME, props);
//    }

  public static EntityTag getEntityTag(Entity entity, TagInputBean tagInput, String rlxName) {
    return getEntityTag(entity, tagInput, rlxName, null);
  }

  public static EntityTag getEntityTag(Entity entity, TagInputBean tagInput, String rlxname, Map<String, Object> properties) {
    Tag tag = getTag(tagInput);
    EntityTag entityTag = mock(EntityTag.class);
    when(entityTag.getTag()).thenReturn(tag);
    when(entityTag.getEntity()).thenReturn(entity);
    when(entityTag.getRelationship()).thenReturn(rlxname);
    when(entityTag.getProperties()).thenReturn(properties);
    return entityTag;
  }
}
