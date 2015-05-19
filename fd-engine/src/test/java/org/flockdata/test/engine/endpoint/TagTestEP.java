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

import junit.framework.TestCase;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.bean.TagResultBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.test.engine.functional.EngineBase;
import org.flockdata.test.engine.functional.TestQueryResults;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collection;

import static junit.framework.TestCase.assertEquals;

/**
 *
 *
 *
 * Created by mike on 16/02/15.
 */
@WebAppConfiguration
public class TagTestEP extends EngineBase {

    @Autowired
    WebApplicationContext wac;

    @Test
    public void companyLocators () throws Exception{
        setSecurity(mike_admin);
        SystemUser su = registerSystemUser("companyLocators", "mike");

        Fortress fortress = createFortress(su);

        EntityInputBean inputBean = new EntityInputBean(fortress.getName(), "mike", "Study", new DateTime(), "StudyA");
        inputBean.addTag(new TagInputBean("Apples", "likes").setLabel(TestQueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Pears", "likes").setLabel(TestQueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Oranges", "dislikes").setLabel(TestQueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Grapes", "allergic").setLabel(TestQueryResults.FRUIT));
        inputBean.addTag(new TagInputBean("Potatoes", "likes").setLabel(TestQueryResults.VEGETABLE));
        mediationFacade.trackEntity(su.getCompany(), inputBean).getEntity() ;

        EngineEndPoints eip = new EngineEndPoints(wac);
        eip.login(mike_admin, "123");

        Collection<TagResultBean> tags = eip.getTags(su, TestQueryResults.VEGETABLE);
        TestCase.assertNotNull(tags);
        TestCase.assertFalse(tags.isEmpty());
        assertEquals(1, tags.size());
        assertEquals("Potatoes", tags.iterator().next().getCode());
    }

}
