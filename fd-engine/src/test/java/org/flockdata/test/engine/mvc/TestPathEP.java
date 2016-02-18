package org.flockdata.test.engine.mvc;

import junit.framework.TestCase;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.TagInputBean;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collection;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by mike on 28/12/15.
 */
@WebAppConfiguration
public class TestPathEP extends WacBase {
    @Autowired
    WebApplicationContext wac;

    @Test
    public void get_tags() throws Exception {

        engineConfig.setConceptsEnabled("true");

        // Creating a structure
        TagInputBean term = new TagInputBean("volvo 244", "Term");
        TagInputBean division = new TagInputBean("luxury cars", "Division")
                .setKeyPrefix("motor");
        TagInputBean category = new TagInputBean("cars", "Category")
                .setKeyPrefix("motor");
        TagInputBean interest = new TagInputBean("Motors", "Interest");

        term.setTargets("classifying", division);

        division.setTargets("typed", category);
        category.setTargets("is", interest);


        TagInputBean sedan = new TagInputBean("sedan", "Division")
                .setKeyPrefix("motor");
        TagInputBean bodies = new TagInputBean("bodies", "Division")
                .setKeyPrefix("motor");
        sedan.setTargets("typed", bodies);
        term.setTargets("classifying", sedan);
        bodies.setTargets("typed", category);

        login(mike_admin, "123");

        createTag(mike(), term);
        // Fix the resulting json
        Collection paths = getTagPaths(term.getLabel(), term.getCode(), interest.getLabel());
        assertEquals(2, paths.size());

        String code = TagHelper.parseKey(division.getKeyPrefix(), division.getCode());
        TestCase.assertNotNull("Didn't find the tag when the code had a space in the name", getTag(mike(), "Division", code));
        paths = getTagPaths(division.getLabel(), code, interest.getLabel());
        assertEquals (1, paths.size());
    }

}
