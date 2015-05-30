/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.test.engine.unit;

import org.flockdata.engine.schema.model.DocumentTypeNode;
import org.flockdata.engine.tag.model.TagNode;
import org.flockdata.engine.track.model.EntityNode;
import org.flockdata.engine.track.model.EntityTagOut;
import org.flockdata.company.model.FortressNode;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.company.model.CompanyNode;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.EntityTag;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 4/08/14
 * Time: 4:34 PM
 */
public class TestHashcodeAndEquality {
    public TestHashcodeAndEquality() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Test
    public void tagNodes() throws Exception{


        // We don't compare the relationships primary key for a tag
        TagNode tagNode = getTag("Samsung", "plantif", 12345l);
        TagNode tagNodeB =  getTag("Samsung", "plantif", 12345l);

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
    public void entityTags() throws Exception{

        TagNode tagNode = getTag("Samsung", "plantif", 12345l);
        TagNode tagNodeB = getTag("Apple", "defendant", 12343l);

        CompanyNode company = new CompanyNode("TestCo");
        company.setId(12313);
        FortressNode fortress = new FortressNode(new FortressInputBean("Testing",true ), company);
        DocumentTypeNode documentTypeNode = new DocumentTypeNode(fortress, "DocTest");
        EntityInputBean entityInput = new EntityInputBean();
        entityInput.setCallerRef("abc");

        EntityNode entityNode = new EntityNode("123abc", fortress, entityInput, documentTypeNode);
        EntityTagOut entityTagA = new EntityTagOut(entityNode, tagNode);
        EntityTagOut entityTagB = new EntityTagOut(entityNode, tagNodeB);

        ArrayList<EntityTag>existingTags = new ArrayList<>();
        existingTags.add(entityTagA);
        existingTags.add(entityTagB);
        assertEquals(2, existingTags.size());
        assertEquals(true, existingTags.contains(entityTagA));
        assertEquals(true, existingTags.contains(entityTagB));

    }
}
