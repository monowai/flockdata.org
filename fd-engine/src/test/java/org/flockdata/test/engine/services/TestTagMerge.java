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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.SystemUser;
import org.flockdata.data.Tag;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityTagRelationshipInput;
import org.joda.time.DateTime;
import org.junit.Test;

/**
 * @author mholdsworth
 * @tag Test, Tag, Alias, Merge
 * @since 7/11/2014
 */
public class TestTagMerge extends EngineBase {

    @Test
    public void merge_Simple() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("merge_Simple");
        FortressNode fortress = fortressService.registerFortress(su.getCompany(),
            new FortressInputBean("merge_Simple", true));

        TagInputBean tagInputA = new TagInputBean("TagA", "MoveTag", new EntityTagRelationshipInput("rlxA").setReverse(true));
        TagInputBean tagInputB = new TagInputBean("TagB", "MoveTag", new EntityTagRelationshipInput("rlxB").setReverse(true));

        EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "AAA");
        inputBean.addTag(tagInputA);
        Entity entityA = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();

        inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "BBB");
        inputBean.addTag(tagInputB);
        Entity entityB = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();

        assertEquals(1, entityTagService.findEntityTags(entityA).size());
        assertEquals(1, entityTagService.findEntityTags(entityB).size());

        Tag tagA = tagService.findTag(su.getCompany(), null, tagInputA.getCode());
        assertNotNull(tagA);
        Tag tagB = tagService.findTag(su.getCompany(), null, tagInputB.getCode());
        assertNotNull(tagB);

        // The above is the setup. We will look to merge tagA into tagB. The end result will be that
        // entity

        Collection<Long> results = entityTagService.mergeTags(tagA.getId(), tagB.getId());
        assertEquals("One Entity should have been affected by this operation", 1, results.size());
        Long entityResult = results.iterator().next();
        assertEquals("The wrong Entity was affected by this operation", entityA.getId(), entityResult);

        entityA = entityService.getEntity(su.getCompany(), entityA.getKey());
        Collection<EntityTag> tags = entityTagService.findEntityTags(entityA);
        assertEquals(1, tags.size());
        assertEquals(tagInputB.getName(), tags.iterator().next().getTag().getName());

        assertNull("TagA should have been deleted", tagService.findTag(su.getCompany(), null, tagInputA.getCode()));
        tags = entityTagService.findEntityTags(entityB);
        assertEquals(1, tags.size());
        assertEquals(tagInputB.getName(), tags.iterator().next().getTag().getName());

        assertEquals(2, entityTagService.findEntityTagResults(su.getCompany(), tagInputB.getCode()).size());

        //assertEquals("rlxA", tags.iterator().next().);

    }

    @Test
    public void alias_TagsByAlias() throws Exception {
        cleanUpGraph();
        SystemUser su = registerSystemUser("alias_Simple");
        FortressNode fortress = fortressService.registerFortress(su.getCompany(),
            new FortressInputBean("alias_Simple", true));

        TagInputBean tagInput = new TagInputBean("TagA", "AliasTest", "rlxA");

        EntityInputBean inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "AAA");
        inputBean.addTag(tagInput);
        // Creating the tag for an entity
        mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();

        Tag tag = tagService.findTag(su.getCompany(), null, tagInput.getCode());
        assertNotNull(tag);

        // The above is the setup.

        // Alias nodes are not stored in the _Tag bucket; then are connected to a _Tag. Since we're not searching by a Tag type(Label)
        // we can't find JUST by the Alias.key

        // Now create an alias for TagA such that when we track a new entity with zzz as the tag value
        // the entity will be mapped to TagA
        tagService.createAlias(su.getCompany(), tag, "AliasTest", "zzz");

        // Make sure creating it twice doesn't cause an error
        tagService.createAlias(su.getCompany(), tag, "AliasTest", "zzz");

        // An alias exists for this tag that points to TagA.
        tagInput = new TagInputBean("zzz", "AliasTest", "rlxA");
        inputBean = new EntityInputBean(fortress, "olivia@sunnybell.com", "CompanyNode", DateTime.now(), "BBB");
        inputBean.addTag(tagInput);
        mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();
        Tag aliasTag = tagService.findTag(su.getCompany(), "AliasTest", null, "zzz");
        assertNotNull(aliasTag);
        assertEquals("The call to find tag with an alias should find the aliased tag", tag.getId(), aliasTag.getId());

        assertEquals("Couldn't find via case-insensitive check", 2, entityTagService.findEntityTagResults(su.getCompany(), tag.getCode().toLowerCase()).size());
        assertEquals("Couldn't find via case-insensitive check", 2, entityTagService.findEntityTagResults(su.getCompany(), tag.getCode().toUpperCase()).size());

    }
}
