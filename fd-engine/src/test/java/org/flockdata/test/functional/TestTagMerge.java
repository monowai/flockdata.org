/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.test.functional;

import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityTag;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * User: mike
 * Date: 7/11/14
 * Time: 11:42 AM
 */
public class TestTagMerge extends EngineBase {

    @Test
    public void merge_Simple() throws Exception {
        SystemUser su = registerSystemUser("merge_Simple");
        Fortress fortWP = fortressService.registerFortress(su.getCompany(),
                new FortressInputBean("merge_Simple", true));

        TagInputBean tagInputA = new TagInputBean("TagA", "MoveTag", "rlxA");
        TagInputBean tagInputB = new TagInputBean("TagB", "MoveTag", "rlxB");

        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "AAA");
        inputBean.addTag(tagInputA);
        Entity entityA =  mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "BBB");
        inputBean.addTag(tagInputB);
        Entity entityB =  mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();

        assertEquals(1, entityTagService.getEntityTags(su.getCompany(), entityA).size()) ;
        assertEquals(1, entityTagService.getEntityTags(su.getCompany(), entityB).size()) ;

        Tag tagA = tagService.findTag(su.getCompany(), tagInputA.getName());
        assertNotNull ( tagA);
        Tag tagB = tagService.findTag(su.getCompany(), tagInputB.getName());
        assertNotNull ( tagB);

        // The above is the setup. We will look to merge tagA into tagB. The end result will be that
        // entity

        Collection<Long>results =entityTagService.mergeTags(tagA, tagB);
        assertEquals("One Entity should have been affected by this operation", 1, results.size());
        assertEquals("The wrong Entity was affected by this operation", entityA.getId(), results.iterator().next());

        Collection<EntityTag> tags = entityTagService.getEntityTags(su.getCompany(), entityA);
        assertEquals(1, tags.size()) ;
        assertEquals(tagInputB.getName(), tags.iterator().next().getTag().getName());

        assertNull("TagA should have been deleted", tagService.findTag(su.getCompany(), tagInputA.getName()));
        tags = entityTagService.getEntityTags(su.getCompany(), entityB);
        assertEquals(1, tags.size()) ;
        assertEquals(tagInputB.getName(), tags.iterator().next().getTag().getName());

        assertEquals(2, entityTagService.findEntityTags(su.getCompany(), tagInputB.getName()).size());

        //assertEquals("rlxA", tags.iterator().next().);

    }
}
