package org.flockdata.test.engine.endpoint;

import junit.framework.TestCase;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.registration.bean.TagResultBean;
import org.flockdata.test.engine.functional.EngineBase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collection;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by mike on 28/12/15.
 */
@WebAppConfiguration
public class TestPathEP extends EngineBase {
    @Autowired
    WebApplicationContext wac;

    @Test
    public void get_tags() throws Exception {

        setSecurity(mike_admin);
        SystemUser su = registerSystemUser("get_tags", "mike");
        engineConfig.setConceptsEnabled("true");

        // Creating a structure
        TagInputBean term = new TagInputBean("vw", "Term");
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

        EngineEndPoints eip = new EngineEndPoints(wac);
        eip.login(mike_admin, "123");

        eip.createTag(term);
        // Fix the resulting json
        Collection paths = eip.getTagPaths(term.getLabel(), term.getCode(), interest.getLabel());
        assertEquals(2, paths.size());

    }

}
