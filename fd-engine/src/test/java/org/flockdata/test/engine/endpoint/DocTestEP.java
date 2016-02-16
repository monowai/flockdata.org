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

package org.flockdata.test.engine.endpoint;

import org.flockdata.authentication.registration.bean.TagInputBean;
import org.flockdata.authentication.registration.bean.TagResultBean;
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.model.SystemUser;
import org.flockdata.test.engine.functional.TestQueryResults;
import org.flockdata.test.engine.functional.WacBase;
import org.flockdata.track.bean.ConceptResultBean;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.DocumentTypeInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;

/**
 * Created by mike on 16/02/15.
 */
@WebAppConfiguration
public class DocTestEP extends WacBase {

    @Test
    public void flow_docPoints() throws Exception {

        setSecurity(mike_admin);
        SystemUser su = registerSystemUser("docEp", "mike");
        engineConfig.setConceptsEnabled("true");
        Fortress fortress = createFortress(su);

        EntityInputBean inputBean = new EntityInputBean(fortress, "mike", "StudyDoc", new DateTime());
        inputBean.addTag(new TagInputBean("Apples", TestQueryResults.FRUIT, "likes"));
        inputBean.addTag(new TagInputBean("Potatoes", TestQueryResults.VEGETABLE, "likes"));
        Entity entity = mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();

        assertEquals(2, entityTagService.findEntityTags(entity).size());

        EngineEndPoints eip = new EngineEndPoints(wac);
        eip.login(mike_admin, "123");
        Collection<DocumentResultBean> docResults = eip.getDocuments(fortress.getCode());
        assertNotNull(docResults);
        assertEquals(1, docResults.size());
        DocumentResultBean docResult = docResults.iterator().next();
        assertEquals("StudyDoc", docResult.getName());

        Collection<ConceptResultBean> labelResults = eip.getLabelsForDocument(fortress.getCode(), docResult.getName());
        assertFalse(labelResults.isEmpty());
        Collection<TagResultBean> tags;
        for (ConceptResultBean labelResult : labelResults) {
            switch (labelResult.getName()) {
                case "Vegetable":
                    tags = eip.getTags(TestQueryResults.VEGETABLE);
                    assertNotNull(tags);
                    assertFalse(tags.isEmpty());
                    assertEquals(1, tags.size());
                    assertEquals("Potatoes", tags.iterator().next().getCode());
                    break;
                case "Fruit":
                    tags = eip.getTags(TestQueryResults.FRUIT);
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
        setSecurity(mike_admin);
        SystemUser su = registerSystemUser("companyLocators", "mike");

        Fortress fortress = createFortress(su);

        EntityInputBean inputBean = new EntityInputBean(fortress, "mike", "Study", new DateTime(), "StudyA");
        inputBean.addTag(new TagInputBean("Apples", TestQueryResults.FRUIT, "likes"));
        inputBean.addTag(new TagInputBean("Pears", TestQueryResults.FRUIT, "likes"));
        inputBean.addTag(new TagInputBean("Oranges", TestQueryResults.FRUIT, "dislikes"));
        inputBean.addTag(new TagInputBean("Grapes", TestQueryResults.FRUIT, "allergic"));
        inputBean.addTag(new TagInputBean("Potatoes", TestQueryResults.VEGETABLE, "likes"));
        mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity();

        EngineEndPoints eip = new EngineEndPoints(wac);
        eip.login(mike_admin, "123");

        Collection<TagResultBean> tags = eip.getTags(TestQueryResults.VEGETABLE);
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
        setSecurity(mike_admin);
        SystemUser su = registerSystemUser("make_DocTypes", "mike");

        Fortress fortress = createFortress(su);

        DocumentTypeInputBean docType = new DocumentTypeInputBean("docName")
                .setCode("docCode");


        EngineEndPoints eip = new EngineEndPoints(wac);
        eip.login(mike_admin, "123");

        Collection<DocumentTypeInputBean>docTypes = new ArrayList<>();
        docTypes.add(docType);

        Collection<DocumentResultBean> docs = eip.makeDocuments(su, fortress, docTypes);
        assertEquals(1, docs.size());

        DocumentResultBean result = docs.iterator().next();
        assertEquals(docType.getName(), result.getName());
        assertEquals(1, eip.getDocuments(fortress.getName()).size());

    }


}
