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
import org.flockdata.model.SystemUser;
import org.flockdata.test.engine.functional.EngineBase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collection;
import java.util.Map;

/**
 * Created by mike on 31/05/15.
 */
@WebAppConfiguration
public class TestTagEP extends EngineBase {

    @Autowired
    WebApplicationContext wac;

    @Test
    public void get_tags() throws Exception {

        setSecurity(mike_admin);
        SystemUser su = registerSystemUser("get_tags", "mike");
        engineConfig.setConceptsEnabled("true");

        TagInputBean zipCode = new TagInputBean("codeA", "ZipCode").
                setName("NameA");

        // Same code, but different label. Should create a new tag
        TagInputBean tractCode = new TagInputBean("codeB", "Tract").
                setCode("CodeA").
                setName("NameA");
        // Nest the tags
        zipCode.setTargets("located", tractCode);

        EngineEndPoints eip = new EngineEndPoints(wac);
        eip.login(mike_admin, "123");

        Collection<TagResultBean> tags = eip.createTag(zipCode);
        TestCase.assertEquals(1, tags.size());


        Map<String, Object> targetTags = eip.getConnectedTags(zipCode.getLabel(), zipCode.getCode(), "*", tractCode.getLabel());
        TestCase.assertEquals(1, targetTags.size());
        Collection<Map>tagResults = (Collection<Map>) targetTags.get("located");
        TestCase.assertEquals(tractCode.getCode(), tagResults.iterator().next().get("code"));


    }

}
