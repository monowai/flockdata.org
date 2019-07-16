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

package org.flockdata.test.engine.mvc;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

import java.util.Collection;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.test.engine.services.TestQueryResults;
import org.flockdata.track.bean.ConceptResultBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.bean.EntityTagResult;
import org.flockdata.track.bean.TrackRequestResult;
import org.joda.time.DateTime;
import org.junit.Test;

/**
 * @author mholdsworth
 * @since 16/02/2015
 */
public class TestDocEP extends MvcBase {

    @Test
    public void flow_docPoints() throws Exception {

        FortressResultBean fortress = makeFortress(mike(), "flow_docPoints");

        EntityInputBean inputBean = new EntityInputBean(fortress, new DocumentTypeInputBean("flow_docPoints"));
        inputBean.addTag(new TagInputBean("Apples", TestQueryResults.FRUIT, "likes"));
        inputBean.addTag(new TagInputBean("Potatoes", TestQueryResults.VEGETABLE, "likes"));
        engineConfig.setConceptsEnabled(true);
        TrackRequestResult entity = track(mike(), inputBean);

        Collection<EntityTagResult> entityTags = getEntityTags(mike(), entity.getKey());
        assertEquals(2, entityTags.size());

        Collection<DocumentResultBean> docResults = getDocuments(mike(), fortress.getCode());
        assertNotNull(docResults);
        assertEquals(1, docResults.size());
        DocumentResultBean docResult = docResults.iterator().next();
        assertEquals("flow_docPoints", docResult.getName());

        Collection<ConceptResultBean> labelResults = getLabelsForDocument(mike(), docResult.getName());
        assertFalse(labelResults.isEmpty());
        Collection<TagResultBean> tags;
        for (ConceptResultBean labelResult : labelResults) {
            switch (labelResult.getName()) {
                case "Vegetable":
                    tags = getTags(mike(), TestQueryResults.VEGETABLE);
                    assertNotNull(tags);
                    assertFalse(tags.isEmpty());
                    assertEquals(1, tags.size());
                    assertEquals("Potatoes", tags.iterator().next().getCode());
                    break;
                case "Fruit":
                    tags = getTags(mike(), TestQueryResults.FRUIT);
                    assertNotNull(tags);
                    assertFalse(tags.isEmpty());
                    assertEquals(1, tags.size());
                    assertEquals("Apples", tags.iterator().next().getCode());

                    break;
                default:
                    throw new Exception("Unexpected label " + labelResult.getName());
            }
        }
    }

    @Test
    public void find_tagValues() throws Exception {

        FortressResultBean fortress = makeFortress(mike(), "find_tagValues");

        EntityInputBean inputBean = new EntityInputBean(fortress, "mike", "Study", new DateTime(), "StudyA");
        inputBean.addTag(new TagInputBean("Apples", TestQueryResults.FRUIT, "likes"));
        inputBean.addTag(new TagInputBean("Pears", TestQueryResults.FRUIT, "likes"));
        inputBean.addTag(new TagInputBean("Oranges", TestQueryResults.FRUIT, "dislikes"));
        inputBean.addTag(new TagInputBean("Grapes", TestQueryResults.FRUIT, "allergic"));
        inputBean.addTag(new TagInputBean("Potatoes", TestQueryResults.VEGETABLE, "likes"));
        track(mike(), inputBean);


        Collection<TagResultBean> tags = getTags(mike(), TestQueryResults.VEGETABLE);
        assertNotNull(tags);
        assertFalse(tags.isEmpty());
        assertEquals(1, tags.size());
        assertEquals("Potatoes", tags.iterator().next().getCode());
    }

    /**
     * Create a collection of DocumentTypeInputBeans for a fortress over the endpoint
     *
     * @throws Exception
     */
    @Test
    public void make_DocTypes() throws Exception {

        FortressResultBean fortress = makeFortress(mike(), "make_DocTypes");

        DocumentTypeInputBean docType = new DocumentTypeInputBean("docName")
            .setCode("docCode");


        login(mike_admin, "123");


        DocumentResultBean result = makeDocuments(mike(), fortress, docType);

        assertEquals(docType.getName(), result.getName());
        assertEquals(1, getDocuments(mike(), fortress.getName()).size());
        assertNull(result.getSegments());

    }


}
