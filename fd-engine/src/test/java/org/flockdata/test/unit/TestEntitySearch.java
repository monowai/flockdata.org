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

package org.flockdata.test.unit;

import org.flockdata.engine.repo.neo4j.model.*;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.dao.neo4j.model.CompanyNode;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.FortressUser;
import org.flockdata.registration.model.Tag;
import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.TrackTag;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * User: mike
 * Date: 1/10/14
 * Time: 9:44 AM
 */
public class TestEntitySearch {
    @Test
    public void tags_ArrayWork() throws Exception{
        Collection<TrackTag> tags = new ArrayList<>();

        Entity e = getEntity("test", "blah", "asdf", "don'tcare");

        // ToDo: What is the diff between these relationships
        tags.add( new TrackTagRelationship(e, getTag("NameA", "dupe"), "dupe", null ));
        tags.add( new TrackTagRelationship(e, getTag("NameB", "Dupe"), "Dupe", null ));
        tags.add( new TrackTagRelationship(e, getTag("NameC", "dupe"), "dupe", null ));

        EntitySearchChange entitySearchChange = new EntitySearchChange(e);
        entitySearchChange.setTags(tags);
        assertEquals(1,entitySearchChange.getTagValues().size());
        // Find by relationship
        Map<String, Object> values = entitySearchChange.getTagValues().get("dupe");
        assertTrue (values.get("name") instanceof Collection);
        Collection mValues = (Collection) values.get("name");
        // Each entry has a Name and Code value
        assertEquals("Incorrect Values found for the relationship. Not ignoring case?", 3,mValues.size() );

        System.out.println(entitySearchChange.getTagValues());
    }

    Tag getTag (String tagName, String rlxName){
        TagInputBean tagInputBean = new TagInputBean(tagName, rlxName);
        return new TagNode(tagInputBean);
    }

    Entity getEntity(String comp, String fort, String userName, String doctype) throws FlockException {
        // These are the minimum objects necessary to create Entity data
        Fortress fortress = new FortressNode(new FortressInputBean(fort, false), new CompanyNode(comp));
        FortressUser user = new FortressUserNode(fortress, userName);
        DocumentTypeNode doc = new DocumentTypeNode(fortress, doctype);

        DateTime now = new DateTime();
        EntityInputBean mib = getEntityInputBean(doc, user, now.toString(), now);

        return new EntityNode(now.toString(), fortress, mib, doc, user);

    }

    EntityInputBean getEntityInputBean(DocumentTypeNode docType, FortressUser fortressUser, String callerRef, DateTime now) {

        return new EntityInputBean(fortressUser.getFortress().getName(),
                fortressUser.getCode(),
                docType.getName(),
                now,
                callerRef);

    }
}