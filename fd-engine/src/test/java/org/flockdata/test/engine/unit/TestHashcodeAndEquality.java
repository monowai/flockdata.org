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

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import junit.framework.TestCase;
import org.flockdata.data.Document;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.EntityTagOut;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.TagNode;
import org.flockdata.helper.JsonUtils;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 4/08/2014
 */
public class TestHashcodeAndEquality {
  public TestHashcodeAndEquality() {
    super();    //To change body of overridden methods use File | Settings | File Templates.
  }

  @Test
  public void tagNodes() throws Exception {


    // We don't compare the relationships primary key for a tag
    TagNode tagNode = getTag("Samsung", "plantif", 12345l);
    TagNode tagNodeB = getTag("Samsung", "plantif", 12345l);

    assertEquals(tagNode, tagNodeB);
    ArrayList<TagNode> tags = new ArrayList<>();
    tags.add(tagNode);
    assertEquals(true, tags.contains(tagNodeB));

  }

  private TagNode getTag(String name, String relationship, Long l) {
    TagInputBean tagInputBean = new TagInputBean(name, null, relationship);
    TagNode tagNode = new TagNode(tagInputBean);
    tagNode.setId(l);
    return tagNode;
  }

  @Test
  public void entityTags() throws Exception {

    TagNode tagNode = getTag("Samsung", "plantif", 12345l);
    TagNode tagNodeB = getTag("Apple", "defendant", 12343l);

    CompanyNode company = CompanyNode.builder().name("TestCo").build();
    company.setId(12313);
    FortressNode fortress = new FortressNode(new FortressInputBean("Testing", true), company);
    DocumentNode documentTypeNode = new DocumentNode(fortress, "DocTest");
    EntityInputBean entityInput = new EntityInputBean();
    entityInput.setCode("abc");

    EntityNode entityNode = new EntityNode("123abc", fortress.getDefaultSegment(), entityInput, documentTypeNode);
    EntityTagOut entityTagA = new EntityTagOut(entityNode, tagNode);
    EntityTagOut entityTagB = new EntityTagOut(entityNode, tagNodeB);

    ArrayList<EntityTag> existingTags = new ArrayList<>();
    existingTags.add(entityTagA);
    existingTags.add(entityTagB);
    assertEquals(2, existingTags.size());
    assertEquals(true, existingTags.contains(entityTagA));
    assertEquals(true, existingTags.contains(entityTagB));

  }

  @Test
  public void defaults_Serialize() throws Exception {
    // Fundamental assertions are the payload is serialized

    Document dib = new DocumentTypeInputBean("MyDoc")
        .setTagStructure(EntityTag.TAG_STRUCTURE.TAXONOMY)
        .setVersionStrategy(Document.VERSION.DISABLE);

    CompanyNode company = CompanyNode.builder().name("CompanyName").build();
    Fortress fortress = new FortressNode(new FortressInputBean("FortressName"), company)
        .setSearchEnabled(true);

    byte[] bytes = JsonUtils.toJsonBytes(dib);
    TestCase.assertEquals(dib.getTagStructure(), JsonUtils.toObject(bytes, DocumentTypeInputBean.class).getTagStructure());

    EntityInputBean compareFrom = new EntityInputBean(fortress, dib);
    TestCase.assertEquals(dib.getTagStructure(), compareFrom.getDocumentType().getTagStructure());

    EntityInputBean deserialize
        = JsonUtils.toObject(JsonUtils.toJsonBytes(compareFrom), EntityInputBean.class);
    assertNotNull(deserialize);

    TestCase.assertEquals(compareFrom.getDocumentType().getCode(), deserialize.getDocumentType().getCode());
    TestCase.assertEquals(compareFrom.getDocumentType().getTagStructure(), deserialize.getDocumentType().getTagStructure());
    TestCase.assertEquals(compareFrom.getDocumentType().getVersionStrategy(), deserialize.getDocumentType().getVersionStrategy());

  }
}
