/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.test.helper;

import org.flockdata.model.*;
import org.flockdata.registration.FortressInputBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * Verifies that EntityInputBeans can be merged into each other
 *
 * Created by mike on 23/01/16.
 */
public class TestEntityInputBean {

    @Test
    public void testMerge() throws Exception {
        DocumentTypeInputBean movieDoc = new DocumentTypeInputBean("Movie");
        EntityInputBean movie = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentType(movieDoc)
                .addTag(new TagInputBean("Doug Liman", "Person", new EntityTagRelationshipInput("DIRECTED")));


        EntityInputBean brad = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentType(movieDoc)
                .addTag(new TagInputBean("Brad Pitt", "Person", new EntityTagRelationshipInput("ACTED")));

        EntityInputBean angie = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentType(movieDoc)
                .addTag(new TagInputBean("Angelina Jolie", "Person", new EntityTagRelationshipInput("ACTED")));

        movie.merge(brad,angie);
        assertEquals("Tag Inputs did not merge", 3, movie.getTags().size());

        EntityInputBean producer = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentType(movieDoc)
                .addTag(new TagInputBean("Angelina Jolie", "Person", new EntityTagRelationshipInput("PRODUCED")));

        movie.merge(producer);
        assertEquals("Existing tag with different relationship not recorded", 3, movie.getTags().size());
        TagInputBean angieTag = movie.getTags().get(movie.getTags().indexOf(producer.getTags().iterator().next()));
        assertEquals ("An acting and production relationship should exist", 2, angieTag.getEntityTagLinks().size());

    }

    @Test
    public void docTypeInArray(){
        Map<DocumentType, String> docTypes = new HashMap<>();
        Fortress fortress = new Fortress(new FortressInputBean("Testing"), new Company("Testling"));
        FortressSegment segment = new FortressSegment(fortress);
        DocumentType documentType = new DocumentType(segment, new DocumentTypeInputBean("Blah"));
        docTypes.put(documentType, "OK");
        assertNotNull ( docTypes.get(documentType));
    }
}
