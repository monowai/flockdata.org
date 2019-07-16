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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.flockdata.data.ContentModel;
import org.flockdata.helper.TagHelper;
import org.flockdata.model.ContentValidationRequest;
import org.flockdata.registration.TagInputBean;
import org.flockdata.registration.TagResultBean;
import org.flockdata.transform.json.ContentModelDeserializer;
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * @author mholdsworth
 * @since 31/05/2015
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
        assertEquals(1, tags.size());


        Map<String, Object> targetTags = getConnectedTags(mike(), zipCode.getLabel(), zipCode.getCode(), "*", tractCode.getLabel());
        assertEquals(1, targetTags.size());
        Collection<Map> tagResults = (Collection<Map>) targetTags.get("located");
        assertEquals(tractCode.getCode(), tagResults.iterator().next().get("code"));


    }

    //    @Test(expected = org.springframework.security.web.firewall.RequestRejectedException.class)
    public void get_escapedTags() throws Exception {

        TagInputBean thingTag = new TagInputBean("This/That", "Things");


        login(mike_admin, "123");

        Collection<TagResultBean> tags = createTag(mike(), thingTag);
        assertEquals(1, tags.size());

        Collection<TagResultBean> targetTags = getTags(mike(), thingTag.getLabel());
        assertEquals(1, targetTags.size());

        TagResultBean tagResultBean = getTag(mike(), thingTag.getLabel(), TagHelper.parseKey(thingTag.getCode()), MockMvcResultMatchers.status().isOk());
        assertNotNull(tagResultBean);
        assertEquals("The / should have been converted to - in order to be found", thingTag.getCode(), tagResultBean.getCode());

        tagResultBean = getTag(mike(), thingTag.getLabel(), "This/That", MockMvcResultMatchers.status().isOk());
        assertNotNull(tagResultBean);
        assertEquals("Couldn't find by escape code", thingTag.getCode(), tagResultBean.getCode());

    }

    @Test
    public void get_prefixedTag() throws Exception {

        TagInputBean ignoredTag = new TagInputBean("TheTag", "PrefixTest")
            .setKeyPrefix("Ignore");

        TagInputBean prefixedTag = new TagInputBean("TheTag", "PrefixTest")
            .setKeyPrefix("Include");

        createTag(mike(), ignoredTag);// Same tag code, different prefix
        Collection<TagResultBean> tags = createTag(mike(), prefixedTag);
        assertEquals(1, tags.size());

        TagResultBean tagResultBean = getTagWithPrefix(mike(), prefixedTag.getLabel(), prefixedTag.getKeyPrefix(), prefixedTag.getCode());
        assertEquals(prefixedTag.getCode(), tagResultBean.getCode());
        assertNotNull(tagResultBean.getLabel());


    }

    @Test
    public void get_tagWithSpace() throws Exception {

        TagInputBean thingTag = new TagInputBean("This That", "Thing");

        Collection<TagResultBean> tags = createTag(mike(), thingTag);
        assertEquals(1, tags.size());

        Collection<TagResultBean> targetTags = getTags(mike(), thingTag.getLabel());
        assertEquals(1, targetTags.size());

        TagResultBean tagResultBean = getTag(mike(), thingTag.getLabel(), TagHelper.parseKey(thingTag.getCode()), MockMvcResultMatchers.status().isOk());
        assertNotNull(tagResultBean);
        assertEquals("The / should have been converted to - in order to be found", thingTag.getCode(), tagResultBean.getCode());

        tagResultBean = getTag(mike(), thingTag.getLabel(), "This That", MockMvcResultMatchers.status().isOk());
        assertNotNull(tagResultBean);
        assertEquals("Couldn't find by escape code", thingTag.getCode(), tagResultBean.getCode());

    }

    //    @Test(expected = org.springframework.security.web.firewall.RequestRejectedException.class)
    public void get_percentageScenario() throws Exception {
        // Spring Security upgrade stops such nonsense by default
        TagInputBean thingTag = new TagInputBean("1% Increase", "Thing2");

        Collection<TagResultBean> tags = createTag(mike(), thingTag);
        assertEquals(1, tags.size());

        Collection<TagResultBean> targetTags = getTags(mike(), thingTag.getLabel());
        assertEquals(1, targetTags.size());

        TagResultBean tagResultBean = getTag(mike(), thingTag.getLabel(), "1% Increase", MockMvcResultMatchers.status().isOk());
        assertNotNull(tagResultBean);
        assertEquals("Couldn't find by escape code", thingTag.getCode(), tagResultBean.getCode());

    }

    @Test
    public void notFound_Tag() throws Exception {

        makeDataAccessProfile("nf_tags", "mike");
        // DAT-526
        getTagNotFound(mike(), "zz", "123jja");

    }

    @Test
    public void countriesFoundOverEP() throws Exception {
        makeDataAccessProfile("countriesFoundOverEP", "mike");
        TagInputBean newZealand = new TagInputBean("NZ", "Country");
        createTag(mike(), newZealand);
        Collection<TagResultBean> countries = getCountries(mike());
        assertFalse(countries.isEmpty());

    }

    @Test
    public void pushTagsAndFindConcepts() throws Exception {
        makeDataAccessProfile("pushTagsAndFindConcepts", "mike");
        setSecurity();
        Collection<TagResultBean> tags = getTags(mike());
        assertTrue("should not be any tags", tags.isEmpty());

        ContentModel contentModel = ContentModelDeserializer.getContentModel("/models/test-tag-concepts.json");
        assertNotNull(contentModel);
        assertTrue("Model", contentModel.isTagModel());
        Map<String, Object> data = new HashMap<>();
        data.put("zip", 90210);
        data.put("city", "Beverly Hills");
        data.put("state", "CA");
        data.put("country", "USA");

        ContentValidationRequest validationRequest = new ContentValidationRequest(contentModel, Collections.singleton(data));
        assertNotNull(validationRequest.getContentModel());

        validationRequest = batchRequest(mike(), validationRequest);
        assertNotNull(validationRequest);
        assertFalse(validationRequest.getMessages().isEmpty());

        assertNotNull("City was not found", getTag(mike(), "City", "Beverly Hills", MockMvcResultMatchers.status().isOk()));
        assertNotNull("State was not found", getTag(mike(), "State", "CA", MockMvcResultMatchers.status().isOk()));
        assertNotNull("Country was not found", getTag(mike(), "Country", "USA", MockMvcResultMatchers.status().isOk()));

        assertEquals(4, getTags(mike()).size());
    }

}
