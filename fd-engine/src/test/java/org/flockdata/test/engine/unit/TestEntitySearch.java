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
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.flockdata.data.EntityTag;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.DocumentNode;
import org.flockdata.engine.data.graph.EntityNode;
import org.flockdata.engine.data.graph.EntityTagOut;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.FortressUserNode;
import org.flockdata.engine.data.graph.TagNode;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.search.EntitySearchChange;
import org.flockdata.search.SearchTag;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 1/10/2014
 */
public class TestEntitySearch {
    @Test
    public void tags_ArrayWork() throws Exception {
        Collection<EntityTag> tags = new ArrayList<>();

        EntityNode e = getEntity("test", "blah", "asdf", "don'tcare");
        String relationship = "dupe";

        // ToDo: What is the diff between these relationships
        tags.add(new EntityTagOut(e, getTag("NameA", relationship), relationship, null));
        tags.add(new EntityTagOut(e, getTag("NameB", "Dupe"), "Dupe", null));
        tags.add(new EntityTagOut(e, getTag("NameC", relationship), relationship, null));

        EntitySearchChange entitySearchChange = new EntitySearchChange(e, "");

        entitySearchChange.setStructuredTags(tags);
        assertEquals(1, entitySearchChange.getTagValues().size());
        // Find by relationship
        Map<String, ArrayList<SearchTag>> values = entitySearchChange.getTagValues().get(relationship);
        //assertTrue (values.get("code") instanceof Collection);

        Collection mValues = values.get("tag");
        // Each entry has a Name and Code value
        assertNotNull("Could not find the Tag in the result set", mValues);
        assertEquals("Incorrect Values found for the relationship. Not ignoring case?", 3, mValues.size());

        System.out.println(entitySearchChange.getTagValues());
    }

    TagNode getTag(String tagName, String rlxName) {
        TagInputBean tagInputBean = new TagInputBean(tagName, null, rlxName);
        return new TagNode(tagInputBean);
    }

    EntityNode getEntity(String comp, String fort, String userName, String doctype) throws FlockException {
        // These are the minimum objects necessary to create Entity data
        FortressNode fortress = new FortressNode(new FortressInputBean(fort, false), new CompanyNode(comp));
        FortressUserNode user = new FortressUserNode(fortress, userName);
        DocumentNode doc = new DocumentNode(fortress, doctype);

        DateTime now = new DateTime();
        EntityInputBean mib = getEntityInputBean(doc, user, now.toString(), now);

        return new EntityNode(now.toString(), fortress.getDefaultSegment(), mib, doc, user);

    }

    EntityInputBean getEntityInputBean(DocumentNode docType, FortressUserNode fortressUser, String code, DateTime now) {

        return new EntityInputBean(fortressUser.getFortress(),
            fortressUser.getCode(),
            docType.getName(),
            now,
            code);

    }
}
