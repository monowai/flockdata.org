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

import org.flockdata.model.EntityTagRelationshipInput;
import org.flockdata.registration.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Verifies that EntityInputBeans can be merged into each other
 *
 * Created by mike on 23/01/16.
 */
public class TestEIBMerge {

    @Test
    public void testMerge() throws Exception {

        EntityInputBean movie = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentName("Movie")
                .addTag(new TagInputBean("Doug Liman", "Person", new EntityTagRelationshipInput("DIRECTED")));


        EntityInputBean brad = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentName("Movie")
                .addTag(new TagInputBean("Brad Pitt", "Person", new EntityTagRelationshipInput("ACTED")));

        EntityInputBean angie = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentName("Movie")
                .addTag(new TagInputBean("Angelina Jolie", "Person",new EntityTagRelationshipInput("ACTED")));

        movie.merge(brad,angie);
        assertEquals("Tag Inputs did not merge", 3, movie.getTags().size());

        EntityInputBean producer = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentName("Movie")
                .addTag(new TagInputBean("Angelina Jolie", "Person", new EntityTagRelationshipInput("PRODUCED")));

        movie.merge(producer);
        assertEquals("Existing tag with different relationship not recorded", 3, movie.getTags().size());
        TagInputBean angieTag = movie.getTags().get(movie.getTags().indexOf(producer.getTags().iterator().next()));

        assertEquals ("An acting and production relationship should exist", 2, angieTag.getEntityTagLinks().size());

        for (TagInputBean tagInputBean : movie.getTags()) {
            if ( tagInputBean.getCode().equals(brad.getCode())){
                assertEquals("Brad only acts",1, tagInputBean.getEntityTagLinks().size());
            }else if ( tagInputBean.getCode().equals(angie.getCode())){
                assertEquals("Angie produces and acts",2, tagInputBean.getEntityTagLinks().size());
            }
        }
        EntityInputBean harrison = new EntityInputBean()
                .setCode("tt0356910")
                .setDocumentName("Movie")
                .addTag(new TagInputBean("Harrison Ford", "Person", new EntityTagRelationshipInput("ACTED")));

        movie.merge(harrison);
        assertEquals("New Actor did not get added in", 4, movie.getTags().size());


    }
}
