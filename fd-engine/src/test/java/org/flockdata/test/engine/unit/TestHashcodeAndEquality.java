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

import org.flockdata.authentication.registration.bean.FortressInputBean;
import org.flockdata.authentication.registration.bean.TagInputBean;
import org.flockdata.model.*;
import org.flockdata.track.bean.EntityInputBean;
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
        Tag tagNode = getTag("Samsung", "plantif", 12345l);
        Tag tagNodeB =  getTag("Samsung", "plantif", 12345l);

        assertEquals(tagNode, tagNodeB);
        ArrayList<Tag> tags = new ArrayList<>();
        tags.add(tagNode);
        assertEquals(true, tags.contains(tagNodeB));

    }

    private Tag getTag(String name, String relationship, Long l) {
        TagInputBean tagInputBean = new TagInputBean(name, null, relationship);
        Tag tagNode = new Tag(tagInputBean);
        tagNode.setId(l);
        return tagNode;
    }

    @Test
    public void entityTags() throws Exception{

        Tag tagNode = getTag("Samsung", "plantif", 12345l);
        Tag tagNodeB = getTag("Apple", "defendant", 12343l);

        Company company = new Company("TestCo");
        company.setId(12313);
        Fortress fortress = new Fortress(new FortressInputBean("Testing",true ), company);
        DocumentType documentTypeNode = new DocumentType(fortress, "DocTest");
        EntityInputBean entityInput = new EntityInputBean();
        entityInput.setCode("abc");

        Entity entityNode = new Entity("123abc", fortress.getDefaultSegment(), entityInput, documentTypeNode);
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
