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

package org.flockdata.test.engine.mvc;

import junit.framework.TestCase;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Collection;
import java.util.Map;

/**
 * Created by mike on 31/05/15.
 */
public class TestTagEP extends MvcBase {


    @Test
    public void get_tags() throws Exception {

        TagInputBean zipCode = new TagInputBean("codeA", "ZipCode").
                setName("NameA");

        // Same code, but different label. Should create a new tag
        TagInputBean tractCode = new TagInputBean("codeB", "Tract").
                setCode("CodeA").
                setName("NameA");
        // Nest the tags
        zipCode.setTargets("located", tractCode);

        Collection<TagResultBean> tags = createTag(mike(), zipCode);
        TestCase.assertEquals(1, tags.size());


        Map<String, Object> targetTags = getConnectedTags(mike(), zipCode.getLabel(), zipCode.getCode(), "*", tractCode.getLabel());
        TestCase.assertEquals(1, targetTags.size());
        Collection<Map>tagResults = (Collection<Map>) targetTags.get("located");
        TestCase.assertEquals(tractCode.getCode(), tagResults.iterator().next().get("code"));


    }

    @Test
    public void get_escapedTags() throws Exception {

        TagInputBean thingTag = new TagInputBean("This/That", "Things");


        login(mike_admin, "123");

        Collection<TagResultBean> tags = createTag(mike(), thingTag);
        TestCase.assertEquals(1, tags.size());

        Collection<TagResultBean> targetTags = getTags(mike(), thingTag.getLabel());
        TestCase.assertEquals(1, targetTags.size());

        TagResultBean tagResultBean = getTag(mike(), thingTag.getLabel(), TagHelper.parseKey(thingTag.getCode()), MockMvcResultMatchers.status().isOk());
        TestCase.assertNotNull(tagResultBean);
        TestCase.assertEquals("The / should have been converted to - in order to be found", thingTag.getCode(), tagResultBean.getCode());

        tagResultBean = getTag(mike(), thingTag.getLabel(), "This/That", MockMvcResultMatchers.status().isOk());
        TestCase.assertNotNull ( tagResultBean);
        TestCase.assertEquals("Couldn't find by escape code", thingTag.getCode(), tagResultBean.getCode());

    }

    @Test
    public void get_prefixedTag() throws Exception {

        TagInputBean ignoredTag = new TagInputBean("TheTag", "PrefixTest")
                .setKeyPrefix("Ignore");

        TagInputBean prefixedTag = new TagInputBean("TheTag", "PrefixTest")
                .setKeyPrefix("Include");

        createTag(mike(), ignoredTag);// Same tag code, different prefix
        Collection<TagResultBean> tags = createTag(mike(), prefixedTag);
        TestCase.assertEquals(1, tags.size());

        TagResultBean tagResultBean = getTagWithPrefix(mike(), prefixedTag.getLabel(), prefixedTag.getKeyPrefix(), prefixedTag.getCode());
        TestCase.assertEquals(prefixedTag.getCode(), tagResultBean.getCode());
        TestCase.assertNotNull(tagResultBean.getLabel());


    }

    @Test
    public void get_tagWithSpace() throws Exception {

        TagInputBean thingTag = new TagInputBean("This That", "Thing");

        Collection<TagResultBean> tags = createTag(mike(), thingTag);
        TestCase.assertEquals(1, tags.size());

        Collection<TagResultBean> targetTags = getTags(mike(), thingTag.getLabel());
        TestCase.assertEquals(1, targetTags.size());

        TagResultBean tagResultBean = getTag(mike(), thingTag.getLabel(), TagHelper.parseKey(thingTag.getCode()), MockMvcResultMatchers.status().isOk());
        TestCase.assertNotNull(tagResultBean);
        TestCase.assertEquals("The / should have been converted to - in order to be found", thingTag.getCode(), tagResultBean.getCode());

        tagResultBean = getTag(mike(), thingTag.getLabel(), "This That", MockMvcResultMatchers.status().isOk());
        TestCase.assertNotNull ( tagResultBean);
        TestCase.assertEquals("Couldn't find by escape code", thingTag.getCode(), tagResultBean.getCode());

    }

    @Test
    public void get_percentageScenario() throws Exception {

        TagInputBean thingTag = new TagInputBean("1% Increase", "Thing2");

        Collection<TagResultBean> tags = createTag(mike(), thingTag);
        TestCase.assertEquals(1, tags.size());

        Collection<TagResultBean> targetTags = getTags(mike(), thingTag.getLabel());
        TestCase.assertEquals(1, targetTags.size());

        TagResultBean tagResultBean = getTag(mike(), thingTag.getLabel(), "1% Increase", MockMvcResultMatchers.status().isOk());
        TestCase.assertNotNull ( tagResultBean);
        TestCase.assertEquals("Couldn't find by escape code", thingTag.getCode(), tagResultBean.getCode());

    }

    @Test
    public void notFound_Tag() throws Exception {

        makeDataAccessProfile("nf_tags", "mike");
        // DAT-526
        getTagNotFound(mike(), "zz","123jja");

    }
}
