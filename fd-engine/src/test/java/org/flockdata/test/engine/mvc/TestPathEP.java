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

package org.flockdata.test.engine.mvc;

import static junit.framework.TestCase.assertEquals;

import java.util.Collection;
import junit.framework.TestCase;
import org.flockdata.helper.TagHelper;
import org.flockdata.registration.TagInputBean;
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * @author mholdsworth
 * @since 28/12/2015
 */
public class TestPathEP extends MvcBase {

  @Test
  public void get_tags() throws Exception {
    engineConfig.setConceptsEnabled(true);
    engineConfig.setMultiTenanted(false);
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

    createTag(mike(), term);
    // Fix the resulting json
    Collection paths = getTagPaths(mike(), term.getLabel(), term.getCode(), interest.getLabel());
    assertEquals(2, paths.size());

    String code = TagHelper.parseKey(division.getKeyPrefix(), division.getCode());
    TestCase.assertNotNull("Didn't find the tag when the code had a space in the name", getTag(mike(), "Division", code, MockMvcResultMatchers.status().isOk()));
    paths = getTagPaths(mike(), division.getLabel(), code, interest.getLabel());
    assertEquals(1, paths.size());
    Thread.sleep(1000); // Letting other threads catchup due to concepts being updated in a background thread
  }

}
