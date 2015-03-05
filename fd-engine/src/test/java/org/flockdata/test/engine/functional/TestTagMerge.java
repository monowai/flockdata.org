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

package org.flockdata.test.engine.functional;

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

import static org.junit.Assert.*;

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

    @Test
    public void alias_TagsByAlias() throws Exception {
        SystemUser su = registerSystemUser("alias_Simple");
        Fortress fortWP = fortressService.registerFortress(su.getCompany(),
                new FortressInputBean("alias_Simple", true));

        TagInputBean tagInput = new TagInputBean("TagA", "AliasTest", "rlxA");

        EntityInputBean inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "AAA");
        inputBean.addTag(tagInput);
        // Creating the tag for an entity
        mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();

        Tag tag = tagService.findTag(su.getCompany(), tagInput.getName());
        assertNotNull ( tag);

        // The above is the setup.

        // Alias nodes are not stored in the _Tag bucket; then are connected to a _Tag. Since we're not searching by a Tag type(Label)
        // we can't find JUST by the Alias.key

        // Now create an alias for TagA such that when we track a new entity with zzz as the tag value
        // the entity will be mapped to TagA
        tagService.createAlias( su.getCompany(), tag, "AliasTest", "zzz");

        // Make sure creating it twice doesn't cause an error
        tagService.createAlias( su.getCompany(), tag, "AliasTest", "zzz");

        // An alias exists for this tag that points to TagA.
        tagInput = new TagInputBean("zzz", "AliasTest", "rlxA");
        inputBean = new EntityInputBean(fortWP.getName(), "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "BBB");
        inputBean.addTag(tagInput);
        mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        Tag aliasTag = tagService.findTag(su.getCompany(), "AliasTest", "zzz");
        assertNotNull(aliasTag);
        assertEquals("The call to find tag with an alias should find the aliased tag", tag.getId(), aliasTag.getId());

        assertEquals("Couldn't find via case-insensitive check", 2, entityTagService.findEntityTags(su.getCompany(), tag.getCode().toLowerCase()).size());
        assertEquals("Couldn't find via case-insensitive check", 2, entityTagService.findEntityTags(su.getCompany(), tag.getCode().toUpperCase()).size());

    }
}
